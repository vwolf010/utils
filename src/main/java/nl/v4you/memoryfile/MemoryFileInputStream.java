package nl.v4you.memoryfile;

import java.io.InputStream;

public class MemoryFileInputStream extends InputStream {
    MemoryFile mf = null;

    public MemoryFileInputStream(MemoryFile mf)  {
        this.mf = mf;
        mf.rewind();
    }

    @Override
    public int read(byte[] b, int off, int len)  {
        return mf.read(b, off, len);
    }

    @Override
    public int read()  {
        return mf.read();
    }
}
