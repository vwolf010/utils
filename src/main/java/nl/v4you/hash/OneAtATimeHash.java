package nl.v4you.hash;

import java.util.Arrays;

public class OneAtATimeHash {
    byte b[];
    int hash; // defaults to 0

    public OneAtATimeHash(byte b[]) {
        this.b = b;
    }

    public OneAtATimeHash set(byte b[]) {
        this.b = b;
        hash=0;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return Arrays.equals(b, ((OneAtATimeHash) o).b);
    }

    @Override
    public int hashCode() {
        if (hash==0 && b.length>0) {
            hash = calcHash(b);
        }
        return hash;
    }

    public static int calcHash(byte key[]) {
        int hash = 0;

        for (byte b : key) {
            hash += (b & 0xff);
            hash += (hash << 10);
            hash ^= (hash >>> 6);
        }

        hash += (hash << 3);
        hash ^= (hash >>> 11);
        hash += (hash << 15);
        return hash;
    }
}
