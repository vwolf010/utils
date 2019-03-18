package nl.v4you.hash;

import java.util.Arrays;

public class HashTester {

    //https://en.wikipedia.org/wiki/List_of_hash_functions#Non-cryptographic_hash_functions
    //http://burtleburtle.net/bob/hash/doobs.html
    //https://research.neustar.biz/2011/12/29/choosing-a-good-hash-function-part-2/
    //https://softwareengineering.stackexchange.com/questions/49550/which-hashing-algorithm-is-best-for-uniqueness-and-speed

    // from http://burtleburtle.net/bob/hash/hashfaq.html:
    //    for (h=0, i=0; i<len; ++i) h += key[i]; is bad.
    //    for (h=0, i=0; i<len; ++i) h ^= key[i]; is bad.
    //    for (h=0, i=0; i<len; ++i) h *= key[i]; is hilariously bad.
    //    for (h=0, i=0; i<len; ++i) h = (h<<5)^(h>>27)^key[i]; is OK for one-word ASCII text, otherwise it's bad.
    //    for (h=0, i=0; i<len; ++i) h = (h<<4)^(h>>28)^key[i]; is OK for one-word ASCII text, otherwise it's bad.
    //    Jenkins ona-at-a-time is good.
    //    for (h=0, i=0; i<len; ++i) h = tab[(h^key[i])&0xff]; may be good.
    //    for (h=0, i=0; i<len; ++i) h = (h>>8)^tab[(key[i]+h)&0xff]; may be good.
    //    Anything with a modulo prime at the end is probably bad; a decent calcHash function could use a power of two and wouldn't need to rely on the modulo prime to further mix anything.
    //    Universal calcHash functions are usually good.
    //    CRC hashes are usually good.
    //    lookup3.c and checksum.c are some good hashes.
    //    All cryptographic hashes (MD4, MD5, SHA, Snefru, RIPE-MD) are good.

    /*
     * The signed left shift operator "<<" shifts a bit pattern to the left,
     * and the signed right shift operator ">>" shifts a bit pattern to the right.
     * The unsigned right shift operator ">>>" shifts a zero into the leftmost position,
     * while the leftmost position after ">>" depends on sign extension.
     */


//      http://java-performance.info/implementing-world-fastest-java-int-to-int-hash-map/
//    private static final int INT_PHI = 0x9E3779B9;
//    public static int phiMix( final int x ) {
//        final int h = x * INT_PHI;
//        return h ^ (h >> 16);
//    }

    int dx_hack_hash(byte data[]) {
        int hash0 = 0x12a3fe2d;
        int hash1 = 0x37abe8f9;
        for (byte c : data) {
            int hash = hash1 + (hash0 ^ ( (c & 0xff) * 7152373));

            if ((hash & 0x80000000L)!=0)
                hash -= 0x7fffffff;
            hash1 = hash0;
            hash0 = hash;
        }
        return hash0;
    }

    //            16 buckets : 27775.8768 (count=16777216, msecs=1738)
//            32 buckets : 1.73012433436E7 (count=16777216, msecs=1254)
//            64 buckets : 8650622.687399996 (count=16777216, msecs=1204)
//            128 buckets : 4325313.693799997 (count=16777216, msecs=1215)
//            256 buckets : 2162660.9075999986 (count=16777216, msecs=1221)
//            512 buckets : 1081419.3487999993 (count=16777216, msecs=1210)
//            1024 buckets : 540730.2863999993 (count=16777216, msecs=1232)
    int djb2(byte data[]) {
        int hash = 5381;
        for (byte c : data) {
            hash = ((hash << 5) + hash) + (c & 0xff); /* calcHash * 33 + c */
        }
        return hash;
    }

    //            16 buckets : 19283.7854 (count=16777216, msecs=1684)
//            32 buckets : 2.1673563439799994E7 (count=16777216, msecs=1374)
//            64 buckets : 3.2099184269820005E8 (count=16777216, msecs=1314)
//            128 buckets : 1.6049592312140003E8 (count=16777216, msecs=1262)
//            256 buckets : 8.024796498160003E7 (count=16777216, msecs=1255)
//            512 buckets : 4.0124004309999995E7 (count=16777216, msecs=1279)
//            1024 buckets : 2.0062011216799993E7 (count=16777216, msecs=1277)
    int sdbm(byte data[]) {
        int hash = 0;
        for (byte c : data) {
            hash = (c & 0xff) + (hash << 6) + (hash << 16) - hash;
        }
        return hash;
    }

    //            16 buckets : 15.321599999999998 (count=16777216, msecs=2259)
//            32 buckets : 7.869199999999999 (count=16777216, msecs=1540)
//            64 buckets : 6.892600000000001 (count=16777216, msecs=1501)
//            128 buckets : 11.100999999999999 (count=16777216, msecs=1567)
//            256 buckets : 6.605799999999996 (count=16777216, msecs=1518)
//            512 buckets : 12.976000000000013 (count=16777216, msecs=1473)
//            1024 buckets : 885.9359999999984 (count=16777216, msecs=1535)
    int noise(byte data[]) {
        long noise = 22222;
        for (byte b : data) {
            noise = (noise * 196314165L) + 907633515L + (b & 0xff);
            noise &= 0xffffffff;
        }
        return (int)noise;
    }

    //            16 buckets : 317.37480000000005 (count=16777216, msecs=1729)
//            32 buckets : 1030.6688 (count=16777216, msecs=1222)
//            64 buckets : 1212.4696 (count=16777216, msecs=1215)
//            128 buckets : 1236.4346000000005 (count=16777216, msecs=1200)
//            256 buckets : 6914.336800000003 (count=16777216, msecs=1207)
//            512 buckets : 4150.797599999998 (count=16777216, msecs=1213)
//            1024 buckets : 2972.8383999999996 (count=16777216, msecs=1199)
    int fnv1_hash_32_bit(byte data[]) {
        // https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
        int h = (int)2166136261L; // use this offset for 32-bit calcHash
        for (byte p : data) {
            h = (h * 16777619) ^ (p & 0xff);
        }

        return (int)h;
    }

    //            16 buckets : 317.37480000000005 (count=16777216, msecs=1731)
//            32 buckets : 1030.6688 (count=16777216, msecs=1259)
//            64 buckets : 2126.0940000000005 (count=16777216, msecs=1219)
//            128 buckets : 2585.5484 (count=16777216, msecs=1229)
//            256 buckets : 1672.3192 (count=16777216, msecs=1216)
//            512 buckets : 1650.5792000000017 (count=16777216, msecs=1251)
//            1024 buckets : 1634.0400000000002 (count=16777216, msecs=1215)
    long fnv1_hash_64_bit(byte data[]) {
        // https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
        long h = 1469598103934665603L * 10L + 7L;
        for (byte p : data) {
            h = (h * 1099511628211L) ^ (p & 0xff);
        }
        return h;
    }

    //            16 buckets : 569.7966 (count=16777216, msecs=1653)
//            32 buckets : 4894.002200000001 (count=16777216, msecs=1229)
//            64 buckets : 2821.034199999999 (count=16777216, msecs=1236)
//            128 buckets : 1808.1438000000003 (count=16777216, msecs=1240)
//            256 buckets : 4351.035599999999 (count=16777216, msecs=1202)
//            512 buckets : 2830.4205999999976 (count=16777216, msecs=1215)
//            1024 buckets : 2332.7297999999987 (count=16777216, msecs=1227)
    int fnv1a_hash_32_bit(byte data[])  {
        // https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
        int h = (int)2166136261L;
        for (byte p : data) {
            h = (h ^ (p & 0xff)) * 16777619;
        }
        return h;
    }

    //            16 buckets : 569.7966 (count=16777216, msecs=1756)
//            32 buckets : 4894.002200000001 (count=16777216, msecs=1235)
//            64 buckets : 3915.022599999999 (count=16777216, msecs=1214)
//            128 buckets : 3334.776599999999 (count=16777216, msecs=1215)
//            256 buckets : 2251.2257999999983 (count=16777216, msecs=1210)
//            512 buckets : 2217.8302000000003 (count=16777216, msecs=1282)
//            1024 buckets : 1997.2751999999991 (count=16777216, msecs=1220)
    int fnv1a_hash_64_bit(byte data[])  {
        // https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
        long h = 1469598103934665603L * 10L + 7L;
        for (byte p : data) {
            h = (h ^ (p & 0xff)) * 1099511628211L;
        }
        return (int)h;
    }

    //            16 buckets : 19283.7854 (count=16777216, msecs=1941)
//            32 buckets : 2.1673563439799994E7 (count=16777216, msecs=1364)
//            64 buckets : 1.0836785033800008E7 (count=16777216, msecs=1360)
//            128 buckets : 5418395.1734 (count=16777216, msecs=1265)
//            256 buckets : 2709214.8413999993 (count=16777216, msecs=1333)
//            512 buckets : 1354617.0132000002 (count=16777216, msecs=1318)
//            1024 buckets : 677324.0501999989 (count=16777216, msecs=1244)
    int javaString(byte value[]) {
        int h = 0;
        if (h == 0 && value.length > 0) {
            for (int i = 0; i < value.length; i++) {
                h = 31 * h + (value[i] & 0xff);
            }
        }
        return h;
    }

    //            16 buckets : 639.7676 (count=16777216, msecs=1690)
//            32 buckets : 1238.6321999999998 (count=16777216, msecs=1288)
//            64 buckets : 1439.2332 (count=16777216, msecs=1241)
//            128 buckets : 1430.7613999999999 (count=16777216, msecs=1229)
//            256 buckets : 1596.3868000000002 (count=16777216, msecs=1234)
//            512 buckets : 1643.6902000000002 (count=16777216, msecs=1234)
//            1024 buckets : 1676.2823999999976 (count=16777216, msecs=1229)
    int murmur3_32(byte data[], int seed) {

        int offset = 0;
        int length = data.length;
        int hash = seed;
        final int nblocks = length >> 2;

        // body
        for (int i = 0; i < nblocks; i++) {
            int i_4 = i << 2;
            int k = (data[offset + i_4] & 0xff)
                    | ((data[offset + i_4 + 1] & 0xff) << 8)
                    | ((data[offset + i_4 + 2] & 0xff) << 16)
                    | ((data[offset + i_4 + 3] & 0xff) << 24);

            // mix functions
            k *= 0xcc9e2d51;
            k = Integer.rotateLeft(k, 15);
            k *= 0x1b873593;
            hash ^= k;
            hash = Integer.rotateLeft(hash, 13) * 5 + 0xe6546b64;
        }

        // tail
        int idx = nblocks << 2;
        int k1 = 0;
        switch (length - idx) {
            case 3:
                k1 ^= data[offset + idx + 2] << 16;
            case 2:
                k1 ^= data[offset + idx + 1] << 8;
            case 1:
                k1 ^= data[offset + idx];

                // mix functions
                k1 *= 0xcc9e2d51;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= 0x1b873593;
                hash ^= k1;
        }

        // finalization
        hash ^= length;
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);

        return hash;
    }

    //            16 buckets : 706.3706000000001 (count=16777216, msecs=1706)
//            32 buckets : 1395.0214 (count=16777216, msecs=1286)
//            64 buckets : 1267.2987999999998 (count=16777216, msecs=1240)
//            128 buckets : 1219.2802000000001 (count=16777216, msecs=1267)
//            256 buckets : 1342.138000000001 (count=16777216, msecs=1257)
//            512 buckets : 1440.2985999999996 (count=16777216, msecs=1262)
//            1024 buckets : 1568.998000000003 (count=16777216, msecs=1248)
    int jenkins_one_at_a_time_hash(byte key[]) {
        int hash = 0;

        for (byte b : key) {
            hash += (b & 0xff); // laatste 9 bits (overflow beinvloed bit 9)
            hash += (hash << 10); // eerste 22 bits
            hash ^= (hash >>> 6); // alle bits
        }

        hash += (hash << 3);
        hash ^= (hash >>> 11);
        hash += (hash << 15);
        return hash;
    }

    //            16 buckets : 1735.2531999999999 (count=16777216, msecs=1742)
//            32 buckets : 2136.4312 (count=16777216, msecs=1288)
//            64 buckets : 1953.4558000000002 (count=16777216, msecs=1275)
//            128 buckets : 1920.2468000000006 (count=16777216, msecs=1294)
//            256 buckets : 1885.841400000001 (count=16777216, msecs=1310)
//            512 buckets : 2016.2949999999983 (count=16777216, msecs=1275)
//            1024 buckets : 2059.4268000000025 (count=16777216, msecs=1277)
    static int superFastHash(byte data[]) {
        int hash = 0;
        int n;

        /* Main loop */
        for (n=0; (n+3)<data.length; n+=4) {
            int w1 = ((data[n] & 0xff)<<8) | (data[n+1] & 0xff);
            int w2 = ((data[n+2] & 0xff)<<8) | (data[n+3] & 0xff);

            hash += w1;
            int tmp = (w2 << 11) ^ hash;
            hash = (hash << 16) ^ tmp;
            hash += hash >>> 11;
        }

        /* Handle end cases */
        switch (data.length & 3) {
            case 3:
                hash += ((data[n] & 0xff)<<8) | (data[n+1] & 0xff);
                hash ^= hash << 16;
                hash ^= data[n+2] << 18;
                hash += hash >>> 11;
            case 2:
                hash += ((data[n] & 0xff)<<8) | (data[n+1] & 0xff);
                hash ^= hash << 11;
                hash += hash >>> 17;
            case 1:
                hash += data[n]; // yes, this is supposed to be a (signed char)
                hash ^= hash << 10;
                hash += hash >>> 1;
        }

        /* Force "avalanching" of final 127 bits */
        hash ^= hash << 3;
        hash += hash >>> 5;
        hash ^= hash << 4;
        hash += hash >>> 17;
        hash ^= hash << 25;
        hash += hash >>> 6;

        return hash;
    }

    int bsdChecksum(byte[] input) {
        int check = 0;
        for (byte b: input) {
            check = (check >>> 1) | ((check & 0x1) << 7);
            check += b & 0xff;
        }
        return check & 0xff;
    }

    void testPerByte(int buckets) {
        int mask = buckets-1;
        int c[] = new int[buckets];
        Arrays.fill(c, 0);
        int count=256*256*256;
        long t1 = System.currentTimeMillis();
        for (int i=0; i<count; i++) {
            String s = "/" + Integer.toString(i & 0xff, 16) +
                    "/" + Integer.toString((i>>>8) & 0xff, 16) +
                    "/" + Integer.toString((i>>>16) & 0xff, 16) +
                    "/" + Integer.toString((i>>>24) & 0xff, 16);
            byte b[] = s.getBytes();
            //byte b[] = (""+(i*17)).getBytes();
            //byte b[] = {0,0,0,0};
            //b[0] = (byte)((i>>>24)&0xff);
            //b[1] = (byte)((i>>>16)&0xff);
            //b[2] = (byte)((i>>>8)&0xff);
            //b[3] = (byte)(i&0xff);

            //int calcHash=i;
            //int calcHash=s.hashCode();
            //int hash=jenkins_one_at_a_time_hash(b);
            //int hash=bsdChecksum(b);
            //int calcHash=murmur3_32(b, 0);
            //int calcHash=javaStringBizar(b);
            //int hash=javaString(b);
            //int calcHash=fnv1_hash_32_bit(b);
            int hash=fnv1a_hash_32_bit(b);
            //int calcHash=dx_hack_hash(b);
            //int calcHash=fnv1_hash_64_bit(b);
            //int calcHash=fnv1a_hash_64_bit(b);
            //int calcHash=djb2(b);
            //int calcHash=sdbm(b);
            //int calcHash=superFastHash(b);
            //int calcHash=noise(b);
            int bucket = hash & mask;
            c[bucket]++;
        }
        long t2 = System.currentTimeMillis() - t1;
        double expected = count/buckets;
        double error = 0;
        for (int i=0; i<buckets; i++) {
            double e = (c[i]-expected)/100.0;
            error += e*e;
        }
        System.err.println(buckets+" buckets : "+error+" (count="+count+", msecs="+t2+")");
    }

    void testPerBit(int bit) {
        int mask = 1 << bit;
        int c = 0;
        int count=256*256*256;
        long t1 = System.currentTimeMillis();
        for (int i=0; i<count; i++) {
            String s = "/" + Integer.toString(i & 0xff, 16) +
                    "/" + Integer.toString((i>>>8) & 0xff, 16) +
                    "/" + Integer.toString((i>>>16) & 0xff, 16) +
                    "/" + Integer.toString((i>>>24) & 0xff, 16);
            byte b[] = s.getBytes();
            //byte b[] = (""+(i*17)).getBytes();
            //byte b[] = {0,0,0,0};
            //b[0] = (byte)((i>>>24)&0xff);
            //b[1] = (byte)((i>>>16)&0xff);
            //b[2] = (byte)((i>>>8)&0xff);
            //b[3] = (byte)(i&0xff);

            //int calcHash=i;
            //int calcHash=s.hashCode();
            //int hash=jenkins_one_at_a_time_hash(b);
            //int calcHash=murmur3_32(b, 0);
            //int calcHash=javaStringBizar(b);
            //int calcHash=javaString(b);
            //int calcHash=fnv1_hash_32_bit(b);
            //int calcHash=fnv1a_hash_32_bit(b);
            //int calcHash=dx_hack_hash(b);
            //int calcHash=fnv1_hash_64_bit(b);
            //int calcHash=fnv1a_hash_64_bit(b);
            //int calcHash=djb2(b);
            //int calcHash=sdbm(b);
            //int calcHash=superFastHash(b);
            //int calcHash=noise(b);
            int hash=bsdChecksum(b);
            c += (hash & mask) >>> bit;
        }
        long t2 = System.currentTimeMillis() - t1;
        double expected = count/2;
        double e = (c-expected)/100.0;
        double error = e*e;
        System.err.println("bit "+bit+" : "+error+" (count="+count+", msecs="+t2+")");
    }

    void go() {
        //System.out.println(jenkins_one_at_a_time_hash("The quick brown fox jumped over the lazy dog.".getBytes()));
        testPerByte(16);
        testPerByte(32);
        testPerByte(64);
        testPerByte(128);
        testPerByte(256);
        testPerByte(512);
        testPerByte(1024);
        testPerByte(2048);
        testPerByte(4096);
//        testPerBit(13);
//        testPerBit(14);
//        testPerBit(15);
//        testPerBit(16);
//        testPerBit(17);
//        testPerBit(18);
//        testPerBit(19);
//        testPerBit(20);
//        testPerBit(21);
//        testPerBit(22);
//        testPerBit(23);
//        testPerBit(24);
//        testPerBit(25);
//        testPerBit(26);
//        testPerBit(27);
//        testPerBit(28);
//        testPerBit(29);
//        testPerBit(30);
//        testPerBit(31);
    }

    public static void main(String[] args) throws Exception {
        System.err.println("go");
        HashTester x = new HashTester();
        x.go();
        System.err.println("done");
    }
}
