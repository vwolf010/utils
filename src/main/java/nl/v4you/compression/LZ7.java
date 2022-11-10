package nl.v4you.compression;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * LZV compression using only ascii chars in compressed bytes
 */
public class LZ7 {
    private static final int MATCH_LEN_MIN = 3;
    private static final int WIN_MAX = 0b1111111111; // offset 2 x 5 bits

    private int SPACE = 32;

    public byte[] compress(byte strAsBytes[]) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        bos.write("LZ7".getBytes(StandardCharsets.UTF_8), 0, 3); // magic
        bos.write('1'); // version

        writeIntAsAscii(strAsBytes.length, bos, true);

        int ptr=0;

        int litLen=0;
        int prefixLen=0;

        int lastIndexOfPrefix=-1;
        while (ptr<strAsBytes.length) {
            int lastIndexOf;
            if (lastIndexOfPrefix!=-1) {
                if (strAsBytes[ptr]==strAsBytes[lastIndexOfPrefix+prefixLen]) {
                    lastIndexOf = lastIndexOfPrefix;
                } else {
                    int winStart = ptr - WIN_MAX;
                    if (winStart<0) {
                        winStart=0;
                    }
                    lastIndexOf = byteArrayLastIndexOf(
                            strAsBytes,
                            winStart,
                            (lastIndexOfPrefix + prefixLen + 1) - winStart,
                            ptr - prefixLen,
                            prefixLen + 1);
                }
            } else {
                int winStart = ptr - WIN_MAX;
                if (winStart<0) {
                    winStart=0;
                }
                lastIndexOf = byteArrayLastIndexOf(
                        strAsBytes,
                        winStart,
                        ptr-winStart,
                        ptr - prefixLen,
                        prefixLen + 1);
            }
            if (lastIndexOf!=-1) {
                prefixLen++;
                lastIndexOfPrefix = lastIndexOf;
            } else {
                if (prefixLen==0) {
                    litLen++;
                } else if (prefixLen < MATCH_LEN_MIN) {
                    litLen+=prefixLen;
                    ptr--; // retry byte we could not find earlier
                } else {
                    int back = ptr-prefixLen-lastIndexOfPrefix;
                    if (writeSequence(strAsBytes, ptr-prefixLen-litLen, litLen, back, prefixLen-MATCH_LEN_MIN, bos, false)) {
                        litLen = 0;
                    } else {
                        litLen += prefixLen;
                    }
                    ptr--; // retry byte we could not find earlier
                }
                prefixLen=0;
                lastIndexOfPrefix=-1;
            }
            ptr++;
        }
        if (prefixLen==0) {
            writeSequence(strAsBytes, ptr-prefixLen-litLen, litLen, 0, 0, bos, true);
        } else {
            int back = ptr-prefixLen-lastIndexOfPrefix;
            int prefixLenNormalized = prefixLen - MATCH_LEN_MIN;
            if (prefixLenNormalized<0) {
                prefixLenNormalized=0;
            }
            writeSequence(strAsBytes, ptr-prefixLen-litLen, litLen, back, prefixLenNormalized, bos, true);
        }
        return bos.toByteArray();
    }

    public byte[] decompress(byte in[]) throws IllegalStateException {

        if (in[0]!='L' || in[1]!='Z' || in[2]!='7' || in[3]!='1') {
            throw new IllegalStateException("Unknown LZ7 header");
        }

        int pIn=4;
        int flen = 0;
        {
            int v = readByteAsAscii(in, pIn++);
            while ((v & 0x20) != 0) {
                flen |= (v & 0x1f);
                flen <<= 5;
                v = readByteAsAscii(in, pIn++);
            }
            flen |= (v & 0x1f);
        }

        byte out[] = new byte[flen];
        int bytesLeft = flen; // decompressed size
        int pOut = 0;
        while (bytesLeft > 0) {
            int litLen = 0;
            int repLen = 0;

            // combined
            int combined = readByteAsAscii(in, pIn++);
            int litCount = (combined >>> 2) & 0xf;
            int repCount = combined & 3;
            litLen += litCount;
            repLen += repCount;
            while (litCount==0xf && repCount==0x3) {
                combined = readByteAsAscii(in, pIn++);
                litCount = (combined >>> 2) & 0xf;
                repCount = combined & 3;
                litLen += litCount;
                repLen += repCount;
            }

            // more_literal_length_bytes
            if (litCount==0xf) litCount=0x3f;
            while (litCount==0x3f) {
                litCount = readByteAsAscii(in, pIn++);
                litLen += litCount;
            }

            // more_repeat_length_bytes
            if (repCount==0x3) repCount=0x3f;
            while (repCount==0x3f) {
                repCount = readByteAsAscii(in, pIn++);
                repLen += repCount;
            }

            // offset
            int offset = 0;
            int v = readByteAsAscii(in, pIn++);
            while ((v & 0x20) != 0) {
                offset |= v & 0x1f;
                offset <<= 5;
                v = readByteAsAscii(in, pIn++);
            }
            offset |= v & 0x1f;

            // literals
            if (litLen!=0) {
                bytesLeft -= litLen;
                System.arraycopy(in, pIn, out, pOut, litLen);
                pIn += litLen;
                pOut += litLen;
            }

            repLen += MATCH_LEN_MIN;
            if (repLen>bytesLeft) {
                repLen=bytesLeft;
            }
            bytesLeft -= repLen;
            while (repLen>0) {
                int cStart = pOut - offset;
                int len=repLen;
                if ((pOut-cStart)<repLen) {
                    len = pOut-cStart;
                }
                System.arraycopy(out, cStart, out, pOut, len);
                pOut += len;
                repLen -= len;
            }
        }
        return out;
    }

    private boolean writeSequence(byte strAsBytes[], int litStart, int litLen, int offset, int repLen, ByteArrayOutputStream bos, boolean forceWrite) {
        // <combined_literal_and_repeat_length_bytes>
        // [literal_length_bytes]
        // [repeat_length_bytes]
        // [literals]
        // <offset>

        int[] buf = new int[100];
        int bufPtr = 0;

        int litLeft = litLen;
        int repLeft = repLen;

        while (litLeft >= 0xf && repLeft >= 0x3) {
            //writeByteAsAscii(0x3f, bos);
            buf[bufPtr++] = 0x3f;
            litLeft -= 0xf;
            repLeft -= 0x3;
        }
        {
            int combined = (litLeft >= 0xf ? (0xf << 2) : ((litLeft & 0xf) << 2)) | (repLeft >= 0x3 ? 0x3 : (repLeft & 0x3));
            //writeByteAsAscii(combined, bos);
            buf[bufPtr++] = combined;
            litLeft -= 0xf;
            repLeft -= 0x3;
        }

        // more_literal_length_bytes
        if (litLeft>=0) {
            while (litLeft >= 0x3f) {
                //writeByteAsAscii(0x3f, bos);
                buf[bufPtr++] = 0x3f;
                litLeft -= 0x3f;
            }
            //writeByteAsAscii(litLeft, bos);
            buf[bufPtr++] = litLeft;
        }

        // more_repeat_length_bytes
        if (repLeft>=0) {
            while (repLeft >= 0x3f) {
                //writeByteAsAscii(0x3f, bos);
                buf[bufPtr++] = 0x3f;
                repLeft -= 0x3f;
            }
            //writeByteAsAscii(repLeft, bos);
            buf[bufPtr++] = repLeft;
        }

        // offset
        if (((bufPtr+writeIntAsAscii(offset, bos, false))<=(repLen+MATCH_LEN_MIN)) || forceWrite) {
            for (int i=0; i<bufPtr; i++) {
                writeByteAsAscii(buf[i], bos);
            }
            writeIntAsAscii(offset, bos, true);
            // literals
            bos.write(strAsBytes, litStart, litLen);
            return true;
        } else {
            return false;
        }
    }

    private int byteArrayLastIndexOf(byte str[], int strStart, int strLen, int prefixStart, int prefixLen) {
        if (strLen==0) {
            return -1;
        }
        strStart--;

        int matchLen = 1;
        int lastByte = prefixStart + prefixLen - 1;
        int n = strLen;

        startSearchForLastByte:
        while (true) {
            // find last byte
            while (n!=0 && str[strStart + n] != str[lastByte]) n--;
            if (n==0) break;
            n--;
            // will the rest match?
            while (n!=0 && matchLen!=prefixLen) {
                if (str[strStart + n] != str[lastByte - matchLen]) {
                    n+=matchLen-1;
                    matchLen=1;
                    continue startSearchForLastByte;
                }
                matchLen++;
                n--;
            }
            if (matchLen == prefixLen) return strStart + n + 1;
        }
        return -1;
    }

    void writeByteAsAscii(int b, ByteArrayOutputStream bos) {
        bos.write(b + SPACE);
    }

    int readByteAsAscii(byte[] in, int pIn) {
        return in[pIn] - SPACE;
    }

    int writeIntAsAscii(int b, ByteArrayOutputStream os, boolean doWrite) {
        int count = 0;
        // write big-endian for fast decompression
        int shift = 0;
        int i = b;
        while (i > 0x1f) {
            i >>>= 5;
            shift += 5;
        }
        while (shift != 0) {
            if (doWrite) {
                writeByteAsAscii(0x20 | ((b >>> shift) & 0x1f), os);
            }
            count++;
            shift -= 5;
        }
        if (doWrite) {
            writeByteAsAscii(b & 0x1f, os);
        }
        return count+1;
    }

    public static void main(String[] args) throws IOException {

        LZ7 lzv = new LZ7();

        File f = new File("c:/tmp/findPpn/418842132.xml");
        //File f = new File("c:/data/ggc/clustering.log");
        FileInputStream fis = new FileInputStream(f);
        byte b[] = new byte[(int)f.length()];
        fis.read(b);
        fis.close();

        //byte original[] = "hallo a hallo b hallo c hallo d hallo e hallo".getBytes();
        //byte original[] = " hallo aaaaa hallo hallo".getBytes();
        //byte original[] = " aaaaa bbbbb bbbbb bbbbbc".getBytes();
        //byte original[] = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes();
        byte original[] = b;

        //lz4.writeSequence("a".getBytes(), 1, 39);
        //lz4.writeSequence("abcbla".getBytes(), 3, 6);

        long t1 = System.currentTimeMillis();
        byte compressed[] = lzv.compress(original);
        System.out.println(new String(compressed));
        System.err.println(System.currentTimeMillis()-t1);
//        FileOutputStream fos = new FileOutputStream(new File("c:/data/out.bin"));
//        fos.write(compressed);
//        fos.close();
        System.err.println(original.length+" : "+compressed.length);
        byte fDecompressed[] = lzv.decompress(compressed);
        //       System.err.println(new String(fDecompressed, "UTF-8"));
        System.err.println(Arrays.equals(original, fDecompressed));

        //System.out.println(new String(Arrays.copyOf(back, len), Util.UTF8));
    }
}