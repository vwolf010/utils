package nl.v4you.memoryfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MemoryFile {
    public static long MAX_SIZE = 4L * 1024L * 1024L * 1024L; // Limit maximum filesize to 4Gb

    class ByteBlock {
        ByteBlock prev = null;
        byte buf[] = null;
        ByteBlock next = null;

        ByteBlock(int size) {
            buf = new byte[size];
        }
    }

    private ByteBlock first = null;
    private ByteBlock cur = null;
    private ByteBlock last = null;

    private long size = 0;
    private long pos = 0;

    private int blk_size = 4096;
    private int blk_mask = 4095;

    public MemoryFile(int blk_size) {
        if (blk_size!=64 && blk_size!=128 && blk_size!=256 && blk_size!=512 && blk_size!=1024 && blk_size!=2048 && blk_size!=4096) {
            throw new RuntimeException("blk_size can be 64, 128, 256, 512, 1024, 2048 or 4096");
        }
        this.blk_size = blk_size;
        this.blk_mask = this.blk_size-1;
    }

    public long getSize() {
        return size;
    }

    public void write(int b) {
        int idx = (int)(pos & blk_mask);
        if (cur==null) {
            first = new ByteBlock(blk_size);
            cur = first;
            last = first;
        }
        else if (idx==0) {
            if (cur.next!=null) {
                cur = cur.next;
            }
            else {
                cur.next = new ByteBlock(blk_size);
                last = cur.next;
                last.prev = cur;
                cur = cur.next;
            }
        }
        cur.buf[idx] = (byte)b;
        pos++;
        if (pos>size) {
            size=pos;
        }
    }

    private int fillBlock(byte[] dum, int start, int len) {
        if (len==0) {
            return 0;
        }
        int idx = (int)(pos & blk_mask);
        if (cur==null) {
            first = new ByteBlock(blk_size);
            cur = first;
            last = first;
        }
        else if (idx==0) {
            if (cur.next!=null) {
                cur = cur.next;
            }
            else {
                cur.next = new ByteBlock(blk_size);
                last = cur.next;
                last.prev = cur;
                cur = cur.next;
            }
        }
        int cnt = blk_size-idx;
        if (cnt>len) {
            cnt=len;
        }
        for (int n=0; n<cnt; n++) {
            cur.buf[idx++] = dum[start++];
        }
        pos += cnt;
        if (pos>size) {
            size=pos;
        }
        return cnt;
    }

    public void write(byte[] dum, int start, int len) {
        int written;
        while ((written=fillBlock(dum, start, len))>0) {
            start += written;
            len -= written;
        }
    }

    public int read() {
        int idx = (int)(pos & blk_mask);
        if (pos>=size) {
            return -1;
        }
        if (cur==null) {
            cur = first;
        }
        else {
            if (idx==0) {
                cur = cur.next;
            }
        }
        pos++;
        return cur.buf[idx] & 0xff;
    }

    public int emptyBlock(byte[] arr, int start, int len) {
        if (len<=0) {
            return 0;
        }
        int cnt = len;
        if (pos+cnt>=size) {
            cnt = (int)(size-pos);
        }
        if (cnt<=0) {
            return 0;
        }
        int idx = (int)(pos & blk_mask);
        int remaining = blk_size-idx;
        if (cnt>remaining) {
            cnt = remaining;
        }
        if (cur==null) {
            cur = first;
        }
        else {
            if (idx==0) {
                cur = cur.next;
            }
        }
        for (int n=0; n<cnt; n++) {
            arr[start++] = cur.buf[idx++];
        }
        pos += cnt;
        return cnt;
    }

    public int read(byte[] dum, int start, int len) {
        int bread;
        int readTotal=0;
        while ((bread=emptyBlock(dum, start, len))>0) {
            start += bread;
            len -= bread;
            readTotal += bread;
        }
        if (readTotal<=0) {
            return -1;
        }
        return readTotal;
    }

    public void reset() {
        ByteBlock x = first;
        while (x!=null) {
            ByteBlock tmp = x.next;
            x.next = null;
            x.prev = null;
            x = tmp;
        }
        first = null;
        cur = null;
        last = null;
        size = 0;
        pos = 0;
    }

    public void rewind() {
        pos = 0;
        cur = null;
    }

    public byte[] getBytes(int offset, int len) {
        if (offset+len>size) {
            throw new RuntimeException("offset+len is bigger than size");
        }
        byte arr[] = new byte[len];
        int idx = 0;
        ByteBlock dum = first;
        while (offset>blk_size) {
            dum = dum.next;
            offset -= blk_size;
        }
        idx = offset;
        int bread = 0;
        while (bread<len) {
            while (idx<blk_size && bread<len) {
                arr[bread++] = dum.buf[idx++];
            }
            dum = dum.next;
            idx = 0;
        }
        return arr;
    }

    public byte[] getBytes(int offset) {
        int len = (int)size-offset;
        return getBytes(offset, (int)len);
    }

    public byte[] getBytes() {
        return getBytes(0, (int)size);
    }

    public void setBytes(byte dum[], int offset, int len) {
        for (int n=offset; n<len; n++) {
            write(dum[n] & 0xff);
        }
    }

    public void setBytes(byte dum[]) {
        setBytes(dum, 0, dum.length);
    }

    public void copyFrom(InputStream is) throws IOException {
        reset();
        byte dum[] = new byte[4096];
        int bread;
        while ((bread=is.read(dum))>0) {
            write(dum, 0, bread);
        }
    }

    public void copyTo(OutputStream os) throws IOException {
        long left = size;
        ByteBlock bb = first;
        long todo = left;
        if (todo>blk_size) {
            todo=blk_size;
        }
        while (left>0) {
            os.write(bb.buf, 0, (int)todo);
            bb = bb.next;
            left -= todo;
            todo = left;
            if (todo>blk_size) {
                todo=blk_size;
            }
        }
    }

    public String toString() {
        if (this.size>200) {
            return new String(getBytes(0,200)+"...");
        }
        return new String(getBytes());
    }
}
