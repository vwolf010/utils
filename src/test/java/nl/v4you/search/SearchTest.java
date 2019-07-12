package nl.v4you.search;

import junit.framework.TestCase;

public class SearchTest extends TestCase {

    public void testTriGram() {
        TriGram tg = new TriGram();
        tg.store("abcde", 123);
        tg.store("zzzzz", 124);
        tg.store("turks fruit", 125);
        tg.store("turks fruit", 126);

        int r[];

        r = tg.search("abcde", 1.f);
        assertEquals(1, r.length);
        assertEquals(123, r[0]);

        r = tg.search("zzzz", 1.f);
        assertEquals(0, r.length);

        r = tg.search("zzzzzz", 1.f);
        assertEquals(0, r.length);

        r = tg.search("zzzz", 0.5f);
        assertEquals(1, r.length);

        r = tg.search("zzz", 0.5f);
        assertEquals(0, r.length);

        r = tg.search("zzzzzz", 0.8f);
        assertEquals(1, r.length);

        r = tg.search("zzzzzzz", 0.8f);
        assertEquals(0, r.length);

        r = tg.search("turks fruit", 0.8f);
        assertEquals(2, r.length);
    }

    public void testStringEnumerator() {
        StringEnumerator se = new StringEnumerator();

        int car = se.getEnum("Car");
        int train = se.getEnum("Train");
        int boat = se.getEnum("Boat");
        assertEquals(car, se.getEnum("Car"));
        assertEquals(train, se.getEnum("Train"));
        assertEquals(boat, se.getEnum("Boat"));
        assertEquals("Car", se.getString(car));
        assertEquals("Train", se.getString(train));
        assertEquals("Boat", se.getString(boat));
    }
}
