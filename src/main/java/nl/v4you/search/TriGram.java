package nl.v4you.search;

import nl.v4you.hash.OneAtATimeHash;

import java.nio.charset.Charset;
import java.util.*;

public class TriGram {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int ID = 0;
    private static final int TOT_CNT = 1;

    private OneAtATimeHash hs = new OneAtATimeHash(null);
    private Map<String, TriGramNode> map = new TreeMap<>();

    private char triGramArray[] = new char[3];

    public TriGram() {}

    private class StringData {
        int id;
        int triGramTotal;

        StringData(int id, int triGramTotal) {
            this.id = id;
            this.triGramTotal = triGramTotal;
        }
    }

    private class TriGramNode implements Comparable<TriGramNode> {
        private Set<StringData> ids = new HashSet<>();

        @Override
        public int compareTo(TriGramNode o) {
            return ids.size() - o.ids.size();
        }
    }

    public void store(String str, int id) {
        if (str.length() < 3) {
            return;
        }
        int triGramTotal = str.length() - 2;
        StringData sd = new StringData(id, triGramTotal);
        char chars[] = str.toCharArray();
        for (int n=0; n<chars.length-2; n++) {
            triGramArray[0] = chars[n];
            triGramArray[1] = chars[n+1];
            triGramArray[2] = chars[n+2];
            //byte tgStr[] = new String(triGramArray).getBytes(UTF8);
            String tgStr = new String(triGramArray);
            TriGramNode tg = map.get(tgStr);
            if (tg==null) {
                tg = new TriGramNode();
                map.put(tgStr, tg);
            }
            tg.ids.add(sd);
        }
    }

    public int[] search(String str, float match) {
        if (str.length()<3) {
            return new int[0];
        }
        char chars[] = str.toCharArray();
        int triGramsCount = chars.length - 2;
        int maxDev = Math.round(triGramsCount * (1.f-match));
        int processedCount = 0;
        LinkedList<TriGramNode> tgList = new LinkedList<>();
        for (int n=0; n<chars.length-2; n++) {
            triGramArray[0] = chars[n];
            triGramArray[1] = chars[n+1];
            triGramArray[2] = chars[n+2];
            //TriGramNode tg = map.get(hs.set(new String(triGramArray).getBytes(UTF8)));
            TriGramNode tg = map.get(new String(triGramArray));
            if (tg!=null) {
                tgList.add(tg);
            }
            else {
                processedCount++;
            }
        }
        if (processedCount>maxDev) {
            return new int[0];
        }
        int pcp[] = new int[8];
        int pcpLen = 0;
        Collections.sort(tgList);
        for (TriGramNode tg : tgList) {
            for (int i=0; i<pcpLen; i++) {
                int base = i << 1;
                int cnt = (pcp[base+TOT_CNT] & 0xffff);
                for (StringData sd : tg.ids) {
                    if (sd.id == pcp[base+ID]) {
                        cnt++;
                        pcp[base+TOT_CNT] = (pcp[base+TOT_CNT] & 0xffff0000) | cnt;
                        break;
                    }
                }
                if ((processedCount-cnt)>maxDev) {
                    pcpLen--;
                    if (i!=pcpLen) {
                        int baseLast = pcpLen << 1;
                        pcp[base + ID] = pcp[baseLast + ID];
                        pcp[base + TOT_CNT] = pcp[baseLast + TOT_CNT];
                        i--;
                    }
                }
            }
            if (processedCount <= maxDev) {
                loop1:
                for (StringData id : tg.ids) {
                    if (id.triGramTotal <= (triGramsCount - processedCount + maxDev)) {
                        for (int i = 0; i < pcpLen; i++) {
                            if (pcp[(i << 1) + ID] == id.id)
                                continue loop1;
                        }
                        if (pcpLen == (pcp.length >> 1)) {
                            // we must resize the array
                            int newLen = (int) (((pcp.length >> 1) + 1) * 1.25f) << 1;
                            pcp = Arrays.copyOf(pcp, newLen);
                        }
                        pcp[(pcpLen << 1) + ID] = id.id;
                        pcp[(pcpLen << 1) + TOT_CNT] = (id.triGramTotal<<16) | 1;
                        pcpLen++;
                    }
                }
            }
            processedCount++;
        }
        for (int i=0; i<pcpLen; i++) {
            int base = i << 1;
            int tot = (pcp[base+ TOT_CNT] >>> 16) & 0xffff;
            int cnt = pcp[base+ TOT_CNT] & 0xffff;
            int missed1 = triGramsCount - cnt;
            int missed2 = tot - cnt;
            if (missed2 < 0) missed2 *= -1;
            if (missed1 > maxDev || missed2 > maxDev) {
                pcpLen--;
                if (i!=pcpLen) {
                    int baseLast = pcpLen << 1;
                    pcp[base + ID] = pcp[baseLast + ID];
                    pcp[base + TOT_CNT] = pcp[baseLast + TOT_CNT];
                    i--;
                }
            }
        }

        int result[] = new int[pcpLen];
        for (int i=0; i<pcpLen; i++) {
            result[i] = pcp[i<<1];
        }
        return result;
    }
}
