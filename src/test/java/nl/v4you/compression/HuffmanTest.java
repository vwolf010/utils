package nl.v4you.compression;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
    public void testAsciiTable() {
        Huffman h = new Huffman();
        h.encode("This should be a very good example of an ascii table, meaning no special characters except 0x9, 0xa and 0xd".getBytes());
        assertEquals(5, h.getEncodedSize());
    }
}
