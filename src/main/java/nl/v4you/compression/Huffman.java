package nl.v4you.compression;

import java.util.Arrays;

// <version+type> [UNQ] [uncompressed_length] [partial selection] [length table(full or partial)] <data>

// byte 0 = version (4 bits, 0b1111=reserved) + type (4 bits)

// 0 : uncompressed (uncompressed_length + data)
// 1 : UNQ=1 (uncompressed_length + byte value(1 byte))
// 2 : compressed (uncompressed_length + UNQ (1 byte) + partial_table (if UNQ<256) + code_length_table + data)
// 3 : reserved
// 4 : reserved
// 5 : reserved
// 6 : reserved

// partial table: byte values are written from high to low, except for ranges that are denoted low-high: "d","a" denotes 2 values, "a","d" denotes the range "a"-"d"
//                TODO: because bytes are written high to low, towards the end less bits are needed to write the values

// length table: 1 byte for the maximum length, 3 bits for the minimum length (to support 1-8), then the table itself (UNQ entries)

public class Huffman {
    private static int PREDICT_SIZE = 2;
    private static int PREDICT_UNCOMPRESSED = 0;
    private static int PREDICT_PARTIAL_TABLE = 1;

    private static int MODE_UNCOMPRESSED = 0;
    private static int MODE_UNQ_EQ_1 = 1;
    private static int MODE_COMPRESSED = 2;

    private int tPtr = 256;
    private int freq[] = new int[256];
    private int weight[] = new int[3*256];
    private int tree[] = new int[3*256];
    private long codes[] = new long[256];
    private int codeLen[] = new int[256];
    private int UNQ = 0;
    private int minCodeLen = 10000;
    private int maxCodeLen = 0;

    private int inSize = 0; // length of the input byte array

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
        int shift=0;
        int L=0;
        int i=encodedBuf[encodedSize++] & 0xFF;
        while ((i&0x80)!=0) {
            L|=(i&0x7F)<<shift;
            shift += 7;
            i=encodedBuf[encodedSize++] & 0xFF;
        }
        return L | (i<<shift);
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
        return 0;
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
                    if (done== UNQ) {
                        break outer;
                    }
                }
            }
            L++;
            code<<=1;
        }
    }

    private void countFrequency(byte a[], int inLen) {
        Arrays.fill(freq, 0);
        for (int i=0; i<inLen; i++) {
            freq[a[i] & 0xff]++;
        }
        UNQ=0;
        for (int i=0; i<256; i++) {
            weight[i] = freq[i];
            if (freq[i]>0) {
                UNQ++;
            }
        }
    }

    private void buildTree() {
        if (UNQ<2) {
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

    private void compressInput(byte a[], int inLen) {
        for (int i=0; i<inLen; i++) {
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

    public void encode(byte a[], int inLen) throws CompressionException {
        encodedSize=0;
        if (a==null && inLen>0) {
            throw new CompressionException("a[]==null and inLen>0");
        }
        if (a==null) {
            return;
        }
        if (a.length<inLen) {
            throw new CompressionException("a[].length < inLen");
        }
        if (inLen==0) {
            return;
        }
        inSize = inLen;
        countFrequency(a, inLen);
        if (UNQ==1) {
            encodedSize=0;
            writeBits(MODE_UNQ_EQ_1, 8);
            writeSize(inLen);
            for (int n=0; n<0x100; n++) {
                if (freq[n]>0) {
                    writeBits(n, 8);
                    break;
                }
            }
            return;
        }
        buildTree();
        calcCodeLengths(tree, tPtr -2, 1);
        assignCanonicalCodes();
        //listCodes();

        int predict[] = new int[PREDICT_SIZE];
        int maxCodeBitLen = bits(maxCodeLen-1);

        predict[PREDICT_UNCOMPRESSED] = 8 * inSize;
        int partialTableLength = getTableLength();
        predict[PREDICT_PARTIAL_TABLE] = 8 /*UNQ*/ + 8 * partialTableLength + 8 /*codeMaxLen*/ + 3 /*codeMinLen*/ + maxCodeBitLen * UNQ;
        int encodedDataBits = 0;
        for (int i=0; i<256; i++) {
            if (freq[i]!=0) {
                encodedDataBits += freq[i] * codeLen[i];
            }
        }
        predict[PREDICT_PARTIAL_TABLE] += encodedDataBits;

        int predictMin = 0;
        for (int i=1; i<PREDICT_SIZE; i++) {
            if (predict[i]<predict[predictMin]) {
                predictMin=i;
            }
        }

        if (predictMin==PREDICT_UNCOMPRESSED) {
            writeBits(MODE_UNCOMPRESSED, 8);
            writeSize(inSize);
            for (int i=0; i<inSize; i++) {
                writeBits(a[i] & 0xff, 8);
            }
        }
        else if (predictMin==PREDICT_PARTIAL_TABLE) {
            writeBits(MODE_COMPRESSED, 8);
            writeBits(UNQ-1, 8);
            writeSize(inSize);
            if (UNQ!=256) {
                byte T[] = getTable(partialTableLength);
                for (int i = 0; i < partialTableLength; i++) {
                    writeBits(T[i], 8);
                }
            }
            writeBits(maxCodeLen-1, 8);
            writeBits(minCodeLen-1, 3);
            int nrOfBits = bits(maxCodeLen-minCodeLen);
            if (nrOfBits!=0) {
                for (int i = 0; i < 0x100; i++) {
                    if (freq[i] > 0) {
                        writeBits(codeLen[i] - minCodeLen, nrOfBits);
                    }
                }
            }
            compressInput(a, inLen);
        }
//        System.out.println("Insize: "+inSize);
//        System.out.println("# of unique byte values: "+ UNQ);
//        System.out.println("Outsize: "+ encodedSize +" ("+((float) encodedSize /inSize)+")");
//        System.out.println("Minimal code length: "+ minCodeLen);
//        System.out.println("Maximal code length: "+ maxCodeLen);
//        System.out.println("Bits needed for code length: "+bits(maxCodeLen - minCodeLen));
    }

    public void decode() throws CompressionException {
        reset();
        long tmp = readBits(8);
        int version = (int)(tmp & 0b11111000);
        if (version!=0) {
            throw new CompressionException("Huffman: unknown version " + version);
        }
        int mode = (int)(tmp & 0b1111);
        if (mode==MODE_UNCOMPRESSED) {
            long size = readSize();
            decodedSize=(int)size;
            if (size>decodedBuf.length) {
                decodedBuf =new byte[(int)size];
            }
            for (int i=0; i<size; i++) {
                decodedBuf[i] = (byte)readBits(8);
            }
            return;
        }
        else if (mode==MODE_UNQ_EQ_1) {
            long size = readSize();
            decodedSize=(int)size;
            if (size>decodedBuf.length) {
                decodedBuf =new byte[(int)size];
            }
            byte ch = (byte)readBits(8);
            for (int i=0; i<size; i++) {
                decodedBuf[i] = ch;
            }
            return;
        }
        else {
            UNQ = (int)readBits(8) + 1;
            long size = readSize();
            decodedSize=(int)size;
            if (size>decodedBuf.length) {
                decodedBuf =new byte[(int)size];
            }
            if (UNQ==256) {
                maxCodeLen = (int) readBits(8) + 1;
                minCodeLen = (int) readBits(3) + 1;
                int nrOfBits = bits(maxCodeLen - minCodeLen);
                Arrays.fill(codeLen, 0);
                for (int n = 0; n < 256; n++) {
                    codeLen[n] = minCodeLen;
                    if (nrOfBits!=0) {
                        codeLen[n] += (int) readBits(nrOfBits);
                    }
                }
            }
            else {
                Arrays.fill(freq, 0);
                int c = 0;
                int a = (int) readBits(8);
                while (c < UNQ) {
                    int b = (int) readBits(8);
                    if (a < b) {
                        for (int i = a; i <= b; i++) {
                            freq[i]++;
                            c++;
                        }
                    } else {
                        freq[a]++;
                        c++;
                    }
                    a = b;
                }
                maxCodeLen = (int) readBits(8) + 1;
                minCodeLen = (int) readBits(3) + 1;
                int nrOfBits = bits(maxCodeLen - minCodeLen);
                Arrays.fill(codeLen, 0);
                for (int n = 0; n < 0x100; n++) {
                    if (freq[n] != 0) {
                        codeLen[n] = minCodeLen;
                        if (nrOfBits!=0) {
                            codeLen[n] += (int)readBits(nrOfBits);
                        }
                    }
                }
            }
            assignCanonicalCodes();
            //listCodes();
            for (int i=0; i<size; i++) {
                long code = readBits(minCodeLen);
                int cl = minCodeLen;
                outer:
                while (true) {
                    for (int n = 0; n < 0x100; n++) {
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
}
