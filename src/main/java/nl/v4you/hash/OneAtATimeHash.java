package nl.v4you.hash;

import java.util.Arrays;

public class OneAtATimeHash implements Comparable<OneAtATimeHash> {
    private byte b[];
    private int hash; // defaults to 0

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

    @Override
    public int compareTo(OneAtATimeHash o) {
        if (o==null) {
            throw new NullPointerException();
        }
        if (b==null) {
            if (o.b==null) return 0;
            return -1;
        }
        if (o.b==null) {
            return 1;
        }
        int l = b.length;
        if (o.b.length<l) l=o.b.length;
        for (int n=0; n<l; n++) {
            if (b[n]==o.b[n]) continue;
            return b[n]-o.b[n];
        }
        if (b.length<o.b.length) return -1;
        if (b.length>o.b.length) return 1;
        return 0;
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

    public OneAtATimeHash clone() {
        OneAtATimeHash o = new OneAtATimeHash(Arrays.copyOf(b, b.length));
        o.hash = hash;
        return o;
    }
}
