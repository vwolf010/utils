package nl.v4you.compression;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class HuffmanTest {

    @Test
    public void testUncompressed() throws CompressionException {
        Huffman h = new Huffman();
        byte uncompressedBuf[] = "abcd".getBytes();
        h.encode(uncompressedBuf);
        h.decode();
        byte decodedBuf[] = h.getDecodedBuf();
        assertEquals(uncompressedBuf.length, h.getDecodedSize());
        for (int i=0; i<uncompressedBuf.length; i++) {
            assertEquals(uncompressedBuf[i], decodedBuf[i]);
        }
    }

    @Test
    public void testAsciiTable() throws CompressionException, UnsupportedEncodingException {
        String ts = "\t\n\r";
        for (int i=0x20; i<0x7F; i++) {
            for (int j=0; j<10; j++) {
                ts += (char) i;
            }
        }
        Huffman h = new Huffman();
        byte uncompressedBuf[] = ts.getBytes("UTF-8");
        h.encode(uncompressedBuf);
        h.decode();
        byte decodedBuf[] = h.getDecodedBuf();
        assertEquals(uncompressedBuf.length, h.getDecodedSize());
        for (int i=0; i<uncompressedBuf.length; i++) {
            assertEquals(uncompressedBuf[i], decodedBuf[i]);
        }
    }
}
