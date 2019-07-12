package nl.v4you.search;

import junit.framework.TestCase;
import nl.v4you.hash.OneAtATimeHash;

public class HashTest extends TestCase {
    public void testOneAtATimeComparable() {
        OneAtATimeHash a = new OneAtATimeHash(null);
        OneAtATimeHash b = new OneAtATimeHash(null);
        assertEquals(0, a.compareTo(b));

        a.set(null);
        b.set("a".getBytes());
        assertEquals(-1, a.compareTo(b));
        assertEquals(1, b.compareTo(a));

        a.set("abcd".getBytes());
        b.set("abcd".getBytes());
        assertEquals(0, a.compareTo(b));

        a.set("aaa".getBytes());
        a.set("aab".getBytes());
        assertEquals(-1, a.compareTo(b));
        assertEquals(1, b.compareTo(a));

        a.set("aaa".getBytes());
        b.set("aaaa".getBytes());
        assertEquals(-1, a.compareTo(b));
        assertEquals(1, b.compareTo(a));
    }
}
