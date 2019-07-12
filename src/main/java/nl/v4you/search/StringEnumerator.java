package nl.v4you.search;

import nl.v4you.hash.OneAtATimeHash;

import java.nio.charset.Charset;
import java.util.HashMap;

public class StringEnumerator {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private OneAtATimeHash hs = new OneAtATimeHash(null);

    private int count = 0;
    private HashMap<OneAtATimeHash, Integer> map = new HashMap<>();
    private HashMap<Integer, String> rmap = new HashMap<>();

    public int getEnum(String str) {
        byte strAsBytes[] = str.getBytes(UTF8);
        Integer ppnEnum = map.get(hs.set(strAsBytes));
        if (ppnEnum!=null) {
            return ppnEnum;
        }
        else {
            map.put(hs.clone(), count);
            rmap.put(count, str);
            return count++;
        }
    }

    public String getString(int pEnum) {
        return rmap.get(pEnum);
    }
}
