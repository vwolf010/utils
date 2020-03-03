package nl.v4you.compression;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class HuffmanTest {

    @Test
    public void testUncompressed() throws CompressionException {
        Huffman h = new Huffman();
        byte uncompressedBuf[] = "abc".getBytes();
        h.encode(uncompressedBuf, uncompressedBuf.length);
        h.decode();
        byte decodedBuf[] = h.getDecodedBuf();
        assertEquals(uncompressedBuf.length, h.getDecodedSize());
        for (int i=0; i<uncompressedBuf.length; i++) {
            assertEquals(uncompressedBuf[i], decodedBuf[i]);
        }
    }

    @Test
    public void testFullTable() throws CompressionException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        for (int i=0; i<0x100; i++) {
            bao.write(i);
        }
        for (int i=0; i<500; i++) {
            bao.write('a');
        }
        Huffman h = new Huffman();
        byte uncompressedBuf[] = bao.toByteArray();
        h.encode(uncompressedBuf, uncompressedBuf.length);
        h.decode();
        byte decodedBuf[] = h.getDecodedBuf();
        assertEquals(uncompressedBuf.length, h.getDecodedSize());
        for (int i=0; i<h.getDecodedSize(); i++) {
            assertEquals(uncompressedBuf[i], decodedBuf[i]);
        }
    }


    @Test
    public void testPartialTable() throws CompressionException, UnsupportedEncodingException {
        String ts = "";
        for (int i='a'; i<'e'; i++) {
            for (int j=0; j<10; j++) {
                ts += (char) i;
            }
        }
        Huffman h = new Huffman();
        byte uncompressedBuf[] = ts.getBytes("UTF-8");
        h.encode(uncompressedBuf, uncompressedBuf.length);
        h.decode();
        byte decodedBuf[] = h.getDecodedBuf();
        assertEquals(uncompressedBuf.length, h.getDecodedSize());
        for (int i=0; i<h.getDecodedSize(); i++) {
            assertEquals(uncompressedBuf[i], decodedBuf[i]);
        }
    }

    @Test
    public void testUnqEq1() throws CompressionException, UnsupportedEncodingException {
        String ts = "";
        for (int j=0; j<500; j++) {
            ts += 'a';
        }
        Huffman h = new Huffman();
        byte uncompressedBuf[] = ts.getBytes("UTF-8");
        h.encode(uncompressedBuf, uncompressedBuf.length);
        h.decode();
        byte decodedBuf[] = h.getDecodedBuf();
        assertEquals(uncompressedBuf.length, h.getDecodedSize());
        for (int i=0; i<h.getDecodedSize(); i++) {
            assertEquals(uncompressedBuf[i], decodedBuf[i]);
        }
    }

    @Test
    public void testEncodingIdempotent() throws CompressionException, UnsupportedEncodingException {
        Huffman h = new Huffman();
        for (int L=0; L<100; L++) {
            String ts = "";
            for (int i='a'; i<'e'; i++) {
                for (int j=0; j<10; j++) {
                    ts += (char) i;
                }
            }
            byte uncompressedBuf[] = ts.getBytes("UTF-8");
            h.encode(uncompressedBuf, uncompressedBuf.length);
            h.decode();
            byte decodedBuf[] = h.getDecodedBuf();
            assertEquals(uncompressedBuf.length, h.getDecodedSize());
            for (int i=0; i<h.getDecodedSize(); i++) {
                assertEquals(uncompressedBuf[i], decodedBuf[i]);
            }
        }
    }

    @Ignore
    @Test
    public void testX() throws Throwable {
        File f = new File("c:/data/temp/402119347.xml");
        FileInputStream fis = new FileInputStream(f);
        byte buf[] = new byte[(int)f.length()];
        fis.read(buf);
        System.out.println("uncompressed  : "+f.length());

        LZV lzv = new LZV();
        System.out.println("lzv           : "+lzv.compress(buf).length);

        Huffman huff = new Huffman();
        huff.encode(buf, buf.length);
        System.out.println("huffman       : "+huff.getEncodedSize());
        huff.decode();
        assertEquals(buf.length, huff.getDecodedSize());
        byte decodedBuf[] = huff.getDecodedBuf();
        assertEquals(buf.length, huff.getDecodedSize());
        for (int i=0; i<huff.getDecodedSize(); i++) {
            assertEquals(buf[i], decodedBuf[i]);
        }

        byte buf2[] = lzv.compress(buf);
        huff.encode(buf2, buf2.length);
        System.out.println("lzv + huffman : "+huff.getEncodedSize());
        huff.decode();
        decodedBuf = huff.getDecodedBuf();
        byte out[] = lzv.decompress(decodedBuf);
        assertEquals(buf.length, out.length);
        for (int i=0; i<out.length; i++) {
            assertEquals(buf[i], out[i]);
        }
    }
}
