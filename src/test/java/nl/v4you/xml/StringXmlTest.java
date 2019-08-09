package nl.v4you.xml;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class StringXmlTest {

    @Test
    public void testWriteHeaderWhenNoContent() {
        StringXml s = new StringXml();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", s.toString());
    }

    @Test
    public void testWriteSimpleNode() throws StringXmlException {
        StringXml s = new StringXml();
        s.node("n1").val("value").end();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<n1>value</n1>", s.toString());
    }

    @Test
    public void testUseDefaultNameSpace() throws StringXmlException {
        StringXml s = new StringXml();
        s.addNs("", "http://ns1#").node("n1").ns("").val("value").end();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<n1 xmlns=\"http://ns1#\">value</n1>", s.toString());
    }

    @Test
    public void testUseNameSpace() throws StringXmlException {
        StringXml s = new StringXml();
        s.addNs("ns1", "http://ns1#").node("n1").ns("ns1").val("value").end();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<n1 xmlns:ns1=\"http://ns1#\">value</n1>", s.toString());
    }

    @Test
    public void testOmitXmlHeader() throws StringXmlException {
        StringXml s = new StringXml();
        s.omitHeader(true).node("n1").val("value").end();
        assertEquals("<n1>value</n1>", s.toString());
    }

    @Test
    public void testEscape() throws StringXmlException {
        StringXml s = new StringXml();
        s.omitHeader(true).node("n1").val("&<>'\"\u0100").end();
        assertEquals("<n1>&amp;&lt;&gt;&apos;&quot;&#x100;</n1>", s.toString());

        s = new StringXml();
        s.omitHeader(true).node("n1").attr("a1", "&<>'\"").end();
        assertEquals("<n1 a1=\"&amp;&lt;&gt;&apos;&quot;\"/>", s.toString());
    }

    @Ignore
    @Test
    public void testEscapeSurrogatePair() throws StringXmlException {
        StringXml s = new StringXml();
        s.omitHeader(true).node("n1").val(""+Character.toChars(10437)[0]+""+Character.toChars(10437)[1]).end();
        assertEquals("<n1>&#x28c5;</n1>", s.toString());

        s = new StringXml();
        s.omitHeader(true).node("n1").attr("a1", "\u28C5").end();
        assertEquals("<n1 a1=\"&#x28c5;\"/>", s.toString());
    }

    @Test
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

    @Test
    public void testCompact() {
        StringXml s = new StringXml();
        s.compact(true).indent("##").node("n1").node("n2").node("n3").end().end().end();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><n1><n2><n3/></n2></n1>", s.toString());
    }

    @Test(expected = StringXmlException.class)
    public void testIllegalCharacter() throws StringXmlException {
        StringXml s = new StringXml();
        s.node("n1").val(""+(char)0x1).end().toString();
    }
}
