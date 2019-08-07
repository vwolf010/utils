package nl.v4you.compression;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
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
}
