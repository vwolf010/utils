package nl.v4you.memoryfile;

import java.io.IOException;
import java.io.OutputStream;

public class MemoryFileOutputStream extends OutputStream {
    MemoryFile mf = null;

    public MemoryFileOutputStream(MemoryFile mf) {
        this.mf = mf;
        mf.reset();
    }

    @Override
    public void flush() throws IOException {
        super.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public void write(byte buf[], int start, int len) throws IOException {
        mf.write(buf, start, len);
    }

    @Override
    public void write(byte buf[]) throws IOException {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(int arg0) throws IOException {
        mf.write(arg0);
    }
}
