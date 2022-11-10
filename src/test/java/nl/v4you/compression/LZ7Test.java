package nl.v4you.compression;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.StandardCharsets;

@RunWith(JUnit4.class)
public class LZ7Test {

    @Test
    public void testRepeatSingleChar() throws CompressionException {
        LZ7 lz7 = new LZ7();
        String s = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        byte[] a = s.getBytes(StandardCharsets.UTF_8);
        byte b[] = lz7.decompress(lz7.compress(a));
        Assert.assertArrayEquals(a, b);
    }

    @Test
    public void testRepeatSameWord() throws CompressionException {
        LZ7 lz7 = new LZ7();
        String s = "hallo hallo hallo hallo hallo hallo hallo";
        byte[] a = s.getBytes(StandardCharsets.UTF_8);
        byte b[] = lz7.decompress(lz7.compress(a));
        Assert.assertArrayEquals(a, b);
    }

    @Test
    public void testRepeatSameWordBrokenByUniqueStrings() throws CompressionException {
        LZ7 lz7 = new LZ7();
        String s = "hallo a hallo b hallo c hallo d hallo e hallo";
        byte[] a = s.getBytes(StandardCharsets.UTF_8);
        byte b[] = lz7.decompress(lz7.compress(a));
        Assert.assertArrayEquals(a, b);
    }

    @Test
    public void testNoCompression() throws CompressionException {
        LZ7 lz7 = new LZ7();
        String s = "abcdefghijklmnopqrstuvwxyz";
        byte[] a = s.getBytes(StandardCharsets.UTF_8);
        byte b[] = lz7.decompress(lz7.compress(a));
        Assert.assertArrayEquals(a, b);
    }
}
