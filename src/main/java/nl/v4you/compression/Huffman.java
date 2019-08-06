package nl.v4you.compression;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

// <version+type> <length> [partial selection] [length table(full or partial)] <data>

// todo: number_of_bits(maximal length - minimal length) = maximum bits that need to be used

// byte 0 = version (5 bits, 0b11111=reserved) + type (3 bits)

// 0 = uncompressed
// 1 = compressed with ascii table (98 entries: tab/lf/cr/space until tilde
// 2 = compressed with full table (256 entries)
// 3 = compressed with partial table
// 4 = reserved
// 5 = reserved
// 6 = reserved
// 7 = reserved

// when type=1, 1 byte for max length + 98 length bytes follow
// when type=2: 1 byte for max length + 256 length bytes for codes
// when type=3, 3 bits for min length, 1 byte for max length, partial table follows (for ascii: space-tilde,cr,lf,tab)

// length table: 1 byte for the number of entries, 3 bits for the minimum length (to support 1-8), 1 byte for the maximum length, then the table itself
// partial table: byte values are written from high to low, except for ranges that are denoted low-high: "d","a" denotes 2 values, "a","d" denotes the range "a"-"d"
//                because bytes are written high to low, towards the end less bits are needed to write the values
//
// Special cases:
// <version+type=d/c> <length=0> : input was 0 bytes
// <version+type=3> <length=X> <partial_table_length=1> <char> : input is X times <char>

public class Huffman {
    private static int PREDICT_SIZE = 4;
    private static int PREDICT_UNCOMPRESSED = 0;
    private static int PREDICT_ASCII_TABLE = 1;
    private static int PREDICT_FULL_TABLE = 2;
    private static int PREDICT_PARTIAL_TABLE = 3;

    private int tPtr = 256;
    private int freq[] = new int[256];
    private int weight[] = new int[2*256];
    private int tree[] = new int[2*256];
    private long codes[] = new long[256];
    private int codeLen[] = new int[256];
    private int unique = 0;
    private int minCodeLen = 10000;
    private int maxCodeLen = 0;

    private int inSize = 0; // length of the input byte array

    private boolean isAscii = false;

    private byte encodedBuf[] = new byte[32];
    private int encodedSize = 0; // length of the out buffer
    private int encodedMask = 0;

    private byte decodedBuf[] = new byte[32];
    private int decodedSize;

    private void writeBits(long b, int L) {
        int m = 1<<(L-1);
        while (m!=0) {
            if (encodedMask == 0) {
                encodedMask = 0x80;
                encodedSize++;
                if (encodedSize > encodedBuf.length) {
                    encodedBuf = Arrays.copyOf(encodedBuf, 2 * encodedBuf.length);
                }
            }
            if ((b & m) != 0) {
                encodedBuf[encodedSize - 1] |= encodedMask;
            }
            encodedMask >>>= 1;
            m >>>= 1;
        }
    }

    private void reset() {
        encodedSize=0;
        encodedMask=0;
    }

    private void writeSize(long L) {
        while (L>0x7FL) {
            encodedBuf[encodedSize++]=(byte)(0x80L | L & 0x7FL);
            L>>>=7;
        }
        encodedBuf[encodedSize++]=(byte)(L & 0x7FL);
    }

    private long readSize() {
        int L=0;
        int i=encodedBuf[encodedSize++] & 0xFF;
        while ((i&0x80)!=0) {
            L|=i&0x7F;
            L<<=7;
            i=encodedBuf[encodedSize++] & 0xFF;
        }
        return L | i;
    }

    private long readBits(int L) {
        long x = 0;
        int m = 1<<(L-1);
        while (m!=0) {
            if (encodedMask == 0) {
                encodedMask = 0x80;
                encodedSize++;
            }
            if ((encodedBuf[encodedSize-1] & encodedMask) != 0) {
                x |= m;
            }
            encodedMask >>>= 1;
            m >>>= 1;
        }
        return x;
    }

    private void calcCodeLengths(int t[], int ptr, int len) {
        if (t[ptr]<256) {
            codeLen[t[ptr]] = len;
            if (minCodeLen >len) {
                minCodeLen =len;
            }
            if (maxCodeLen <len) {
                maxCodeLen =len;
            }
        }
        else {
            calcCodeLengths(t, t[ptr], len+1); // left
        }
        if (t[ptr+1]<256) {
            codeLen[t[ptr+1]] = len;
            if (minCodeLen >len) {
                minCodeLen =len;
            }
            if (maxCodeLen <len) {
                maxCodeLen =len;
            }
        }
        else {
            calcCodeLengths(t, t[ptr+1], len+1); // right
        }
    }

    private int bits(int a) {
        if ((a&0b10000000)!=0) return 8;
        if ((a&0b01000000)!=0) return 7;
        if ((a&0b00100000)!=0) return 6;
        if ((a&0b00010000)!=0) return 5;
        if ((a&0b00001000)!=0) return 4;
        if ((a&0b00000100)!=0) return 3;
        if ((a&0b00000010)!=0) return 2;
        if ((a&0b00000001)!=0) return 1;
        return -1;
    }

    private String charForDisplay(char ch) {
        if (ch==0x9) return "tab";
        if (ch==0xa) return " lf";
        if (ch==0xd) return " cr";
        if (ch==0x20) return " sp";
        else return "  "+ch;
    }

    private void listCodes() {
        for (int i=0; i<256; i++) {
            if (codeLen[i]!=0) {
                System.out.println(String.format("%02x %s = %s", i, charForDisplay((char)i), codeToString(codes[i], codeLen[i])));
            }
        }
    }

    private String codeToString(long code, int len) {
        String s = "";
        long p = 1<<(len-1);
        while (p!=0) {
            if ((code&p)!=0) {
                s+="1";
            }
            else {
                s+="0";
            }
            p>>>=1;
        }
        return s;
    }

    private void assignCanonicalCodes() {
        long code = 0L;
        int done = 0;
        int L = minCodeLen;
        outer: while (true) {
            for (int i = 0; i < 256; i++) {
                if (codeLen[i]== L) {
                    codes[i] = code;
                    code++;
                    done++;
                    if (done== unique) {
                        break outer;
                    }
                }
            }
            L++;
            code<<=1;
        }
    }

    private void countFrequency(byte a[]) {
        Arrays.fill(freq, 0);
        for (int i=0; i<a.length; i++) {
            freq[a[i] & 0xff]++;
        }
        isAscii=true;
        for (int i=0; i<9; i++) {
            isAscii = isAscii && freq[i]==0;
        }
        isAscii = isAscii && freq[0xb]==0 && freq[0xc]==0;
        if (isAscii) {
            for (int i = 0xe; i < 0x20; i++) {
                isAscii = isAscii && freq[i] == 0;
            }
        }
        if (isAscii) {
            for (int i = 0x7f; i < 0x100; i++) {
                isAscii = isAscii && freq[i] == 0;
            }
        }
    }

    private void buildTree() {
        unique = 0;
        for (int i=0; i<256; i++) {
            weight[i] = freq[i];
            if (freq[i]>0) {
                unique++;
            }
        }
        if (unique==1) {
            return;
        }
        while (true) {
            int from = -1;
            int b;
            for (b=0; b< tPtr; b++) {
                if (weight[b]>0) {
                    from = b;
                    break;
                }
            }
            if (b== tPtr -2) {
                break;
            }
            int c;
            for (c=b+1; c< tPtr; c++) {
                if (weight[c]>0) {
                    from = c;
                    if (weight[c]<weight[b]) {
                        int t=b;
                        b=c;
                        c=t;
                    }
                    break;
                }
            }
            for (int i = from+1; i < tPtr; i++) {
                if (weight[i] > 0 && weight[i] < weight[c]) {
                    if (weight[i] < weight[b]) {
                        c = b;
                        b = i;
                    }
                    else {
                        c = i;
                    }
                }
            }
            weight[tPtr] = weight[b] + weight[c];
            weight[b] = 0;
            weight[c] = 0;
            tree[tPtr] = b;
            tree[tPtr +1] = c;
            tPtr += 2;
        }
    }

    private void compressInput(byte a[]) {
        for (int i=0; i<a.length; i++) {
            writeBits(codes[a[i]&0xff], codeLen[a[i]&0xff]);
        }
    }

    public int getEncodedSize() {
        return encodedSize;
    }

    public int getDecodedSize() {
        return decodedSize;
    }

    public byte[] getDecodedBuf() {
        return decodedBuf;
    }

    public int getTableLength() {
        int L = 0;
        int i=255;
        for (; i>=0 && freq[i] == 0; i--);
        int j=i;
        while (i>=0) {
            for (; i>=0 && freq[i] != 0; i--);
            i++;
            L++;
            if (i!=j) {
                L++;
            }
            i--;
            for (; i>=0 && freq[i] == 0; i--);
            j=i;
        }
        return L;
    }

    public byte[] getTable(int L) {
        byte T[] = new byte[L];
        int idx=0;
        int i=255;
        for (; i>=0 && freq[i] == 0; i--);
        int j=i;
        while (i>=0) {
            for (; i>=0 && freq[i] != 0; i--);
            i++;
            T[idx++]=(byte)i;
            if (i!=j) {
                T[idx++]=(byte)j;
            }
            i--;
            for (; i>=0 && freq[i] == 0; i--);
            j=i;
        }
        return T;
    }

    public void encode(byte a[]) {
        inSize = a.length;
        if (a.length==0) {
            encodedSize=0;
            writeBits(0, 8);
            writeBits(0, 8);
            return;
        }
        countFrequency(a);
        getTableLength();
        if (a.length==1) {
            encodedSize=0;
            writeBits(3, 8);
            writeSize(a.length);
            //writeBits();
        }
        buildTree();
        calcCodeLengths(tree, tPtr -2, 1);
        assignCanonicalCodes();
        listCodes();

        int predict[] = new int[4];

        predict[PREDICT_UNCOMPRESSED] = 8 + 8 * inSize;
        predict[PREDICT_FULL_TABLE] = 8 + 8 + bits(maxCodeLen-1) * 256;
        predict[PREDICT_ASCII_TABLE] = 8 + 8 + bits(maxCodeLen-1) * (isAscii ? 98 : 999);
        int partialTableLength = getTableLength();
        predict[PREDICT_PARTIAL_TABLE] = 8 + 8 + bits(maxCodeLen-1) * partialTableLength;
        int encodedDataBits = 0;
        for (int i=0; i<256; i++) {
            if (freq[i]!=0) {
                encodedDataBits += freq[i] * codeLen[i];
            }
        }
        predict[PREDICT_FULL_TABLE] += encodedDataBits;
        predict[PREDICT_ASCII_TABLE] += encodedDataBits;
        predict[PREDICT_PARTIAL_TABLE] += encodedDataBits;

        int predictMin = 0;
        for (int i=1; i<PREDICT_SIZE; i++) {
            if (predict[i]<predict[predictMin]) {
                predictMin=i;
            }
        }

        if (predictMin==PREDICT_UNCOMPRESSED) {
            encodedSize =0;
            writeBits(PREDICT_UNCOMPRESSED, 8);
            writeSize(inSize);
            for (int i=0; i<inSize; i++) {
                writeBits(a[i] & 0xff, 8);
            }
        }
        else if (predictMin==PREDICT_ASCII_TABLE) {
            encodedSize =0;
            writeBits(PREDICT_ASCII_TABLE, 8);
            writeSize(inSize);
            writeBits(maxCodeLen, 8);
            int nrOfBits = bits(maxCodeLen);
            writeBits(codeLen[0x9], nrOfBits);
            writeBits(codeLen[0xa], nrOfBits);
            writeBits(codeLen[0xd], nrOfBits);
            for (int i=0x20; i<0x80; i++) {
                writeBits(codeLen[i], nrOfBits);
            }
            compressInput(a);
        }
        else if (predictMin==PREDICT_FULL_TABLE) {
            encodedSize =0;
            writeBits(PREDICT_FULL_TABLE, 8);
            writeSize(inSize);
            writeBits(maxCodeLen, 8);
            int nrOfBits = bits(maxCodeLen);
            for (int i=0; i<0x100; i++) {
                writeBits(codeLen[i], nrOfBits);
            }
            compressInput(a);
        }
        else if (predictMin==PREDICT_PARTIAL_TABLE) {
            encodedSize =0;
            writeBits(PREDICT_PARTIAL_TABLE, 8);
            writeSize(inSize);
            writeBits(maxCodeLen, 8);
            int nrOfBits = bits(maxCodeLen);
            byte T[] = getTable(partialTableLength);
            for (int i=0; i<partialTableLength; i++) {
                writeBits(T[i], 8);
            }
            for (int i=0; i<0x100; i++) {
                if (freq[i]>0) {
                    writeBits(codeLen[i], nrOfBits);
                }
            }
            compressInput(a);
        }
        System.out.println("Insize: "+inSize);
        System.out.println("# of unique byte values: "+ unique);
        System.out.println("Outsize: "+ encodedSize +" ("+((float) encodedSize /inSize)+")");
        System.out.println("Minimal code length: "+ minCodeLen);
        System.out.println("Maximal code length: "+ maxCodeLen);
        System.out.println("Bits needed for code length: "+bits(maxCodeLen - minCodeLen));
        System.out.println("IsAscii: "+isAscii);
    }

    public void decode() throws CompressionException {
        if (decodedSize<inSize) {
            decodedSize=inSize;
            decodedBuf =new byte[decodedSize];
        }
        reset();
        long tmp = readBits(8);
        int version = (int)(tmp & 0b11111000);
        if (version!=0) {
            throw new CompressionException("Huffman: unknown version " + version);
        }
        int type = (int)(tmp & 0b111);
        long size = readSize();
        if (type==PREDICT_UNCOMPRESSED) {
            for (int i=0; i<size; i++) {
                decodedBuf[i] = (byte)readBits(8);
            }
        }
        else if (type==PREDICT_ASCII_TABLE){
            for (int i=0; i<size; i++) {
                long code = readBits(minCodeLen);
                int cl = minCodeLen;
                outer:
                while (true) {
                    for (int n = 0; n < 256; n++) {
                        if (codes[n] == code && codeLen[n] == cl) {
                            decodedBuf[i] = (byte) n;
                            break outer;
                        }
                    }
                    code <<= 1;
                    code |= readBits(1);
                    cl++;
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        File f = new File("c:/data/temp/068794215.xml");
        FileInputStream fis = new FileInputStream(f);
        byte buf[] = new byte[(int)f.length()];
        fis.read(buf);

        //buf = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes();

        Huffman h = new Huffman();
        h.encode(buf);
        h.decode();
        if (buf.length!=h.decodedBuf.length) {
            System.err.println("uncompressed and compressed lengths are different!");
            System.exit(1);
        }
        else {
            for (int i=0; i<buf.length; i++) {
                if (buf[i]!=h.decodedBuf[i]) {
                    System.err.println("content changed!");
                    System.exit(1);
                }
            }
        }
        System.out.println(new String(Arrays.copyOf(h.decodedBuf, h.decodedSize), "UTF-8"));
    }
}
