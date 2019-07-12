package nl.v4you.xml;

import junit.framework.TestCase;

public class StringXmlTest extends TestCase {

    public void testWriteHeaderWhenNoContent() {
        StringXml s = new StringXml();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", s.toString());
    }


    public void testWriteSimpleNode() throws StringXmlException {
        StringXml s = new StringXml();
        s.node("n1").val("value").end();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<n1>value</n1>", s.toString());
    }

    public void testUseDefaultNameSpace() throws StringXmlException {
        StringXml s = new StringXml();
        s.addNs("", "http://ns1#").node("n1").ns("").val("value").end();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<n1 xmlns=\"http://ns1#\">value</n1>", s.toString());
    }

    public void testUseNameSpace() throws StringXmlException {
        StringXml s = new StringXml();
        s.addNs("ns1", "http://ns1#").node("n1").ns("ns1").val("value").end();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<n1 xmlns:ns1=\"http://ns1#\">value</n1>", s.toString());
    }

    public void testOmitXmlHeader() throws StringXmlException {
        StringXml s = new StringXml();
        s.omitHeader(true).node("n1").val("value").end();
        assertEquals("<n1>value</n1>", s.toString());
    }

    public void testEscape() throws StringXmlException {
        StringXml s = new StringXml();
        s.omitHeader(true).node("n1").val("&<>'\"").end();
        assertEquals("<n1>&amp;&lt;&gt;&apos;&quot;</n1>", s.toString());

        s = new StringXml();
        s.omitHeader(true).node("n1").attr("a1", "&<>'\"").end();
        assertEquals("<n1 a1=\"&amp;&lt;&gt;&apos;&quot;\"/>", s.toString());
    }

    public void testIndent() {
        StringXml s = new StringXml();
        s.omitHeader(true).node("n1").node("n2").end().end();
        assertEquals("<n1>\n" +
                "   <n2/>\n" +
                "</n1>", s.toString());

        s = new StringXml();
        s.omitHeader(true).indent("##").node("n1").node("n2").node("n3").end().end().end();
        assertEquals("<n1>\n" +
                "##<n2>\n" +
                "####<n3/>\n" +
                "</n2>\n" +
                "</n1>", s.toString());
    }

    public void testCompact() {
        StringXml s = new StringXml();
        s.compact(true).indent("##").node("n1").node("n2").node("n3").end().end().end();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><n1><n2><n3/></n2></n1>", s.toString());
    }
}
