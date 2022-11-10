package nl.v4you.hash;

import java.util.Arrays;

public class OneAtATimeHash implements Comparable<OneAtATimeHash> {
    private byte b[];
    private int hash; // defaults to 0

    private OneAtATimeHash(byte b[], int hash) {
        this.b = b;
        this.hash = hash;
    }

    public OneAtATimeHash(byte b[]) {
        this.b = b;
        hash = 0;
        hashCode();
    }

    public OneAtATimeHash set(byte b[]) {
        this.b = b;
        hash = 0;
        hashCode();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (((OneAtATimeHash)o).hash != this.hash) {
            return false;
        }
        return Arrays.equals(b, ((OneAtATimeHash) o).b);
    }

    @Override
    public int hashCode() {
        if (hash==0) {
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

        if (key!=null) {
            for (byte b : key) {
                hash += (b & 0xff);
                hash += (hash << 10);
                hash ^= (hash >>> 6);
            }
        }

        hash += (hash << 3);
        hash ^= (hash >>> 11);
        hash += (hash << 15);
        return hash;
    }

    public OneAtATimeHash clone() {
        OneAtATimeHash o = new OneAtATimeHash(Arrays.copyOf(b, b.length), hash);
        return o;
    }
}
