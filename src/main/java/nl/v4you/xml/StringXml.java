package nl.v4you.xml;

import java.util.HashMap;
import java.util.regex.Pattern;

public class StringXml {
    class Node {
        String name;
        boolean isFlushed;
        boolean hasChildren;
        boolean isEmpty;
    }

    HashMap<String, String> nameSpaces = new HashMap<>();
    static Pattern FIND_ESCAPE_CHAR = Pattern.compile("[\"']<>&");

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final int STACK_MAX = 50;
    private boolean omitXmlHeader = false;
    private String indent = "   ";
    private boolean isCompact = false;
    private StringBuilder sb = null;
    private Node stack[] = new Node[STACK_MAX];
    private int ptr = 0;

    public StringXml addNs(String name, String value) {
        nameSpaces.put(name, value);
        return this;
    }

    public StringXml omitHeader(boolean omitXmlHeader) {
        this.omitXmlHeader = omitXmlHeader;
        return this;
    }

    public StringXml indent(String indent) {
        if (indent==null) {
            this.indent="";
        }
        else {
            this.indent = indent;
        }
        return this;
    }

    public StringXml compact(boolean isCompact) {
        this.isCompact = isCompact;
        return this;
    }

    StringBuilder addEntity(StringBuilder sb, char arr[], int idx, String entity) {
        if (sb==null) {
            sb = new StringBuilder();
            for (int n=0; n<idx; n++) {
                sb.append(arr[n]);
            }
        }
        sb.append(entity);
        return sb;
    }

    String escape(String str) throws StringXmlException {
        //[#x1-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF] /* any Unicode character, excluding the surrogate blocks, FFFE, and FFFF. */
        //[#x1-#x8] | [#xB-#xC] | [#xE-#x1F] | [#x7F-#x84] | [#x86-#x9F]
        StringBuilder sb=null;
        char arr[] = str.toCharArray();
        for (int i=0; i<arr.length; i++) {
            char ch = arr[i];
            if (ch>=0x20 && ch<=0x7E) {
                if (ch=='&') sb = addEntity(sb, arr, i, "&amp;"); // encode &amp; first!!!
                else if (ch=='<') sb = addEntity(sb, arr, i, "&lt;");
                else if (ch=='>') sb = addEntity(sb, arr, i, "&gt;");
                else if (ch=='"') sb = addEntity(sb, arr, i, "&quot;");
                else if (ch=='\'') sb = addEntity(sb, arr, i, "&apos;");
                else if (sb!=null) sb.append(ch);
                continue;
            }
            else if (ch==0x9) sb = addEntity(sb, arr, i, "&#9;");
            else if (ch==0xA) sb = addEntity(sb, arr, i, "&#xA;");
            else if (ch==0xD) sb = addEntity(sb, arr, i, "&#xD;");
            else if (Character.isSurrogate(ch)) {
                sb = addEntity(sb, arr, i, "&#x" + Integer.toString(Character.codePointAt(arr, i), 16) + ";");
                i++;
            }
            else if (ch>0x7F) sb = addEntity(sb, arr, i, "&#x" + Integer.toString(ch, 16) + ";");
            else {
                throw new StringXmlException("Illegal character detected! "+ch);
            }
        }
        if (sb!=null) {
            return sb.toString();
        }
        return str;
    }

    public StringXml node(String name) {
        if (ptr==STACK_MAX) {
            throw new IllegalStateException("Maximum stack size is "+STACK_MAX);
        }
        if (FIND_ESCAPE_CHAR.matcher(name).find()) {
            throw new IllegalStateException("Forbidden char detected");
        }
        Node n = new Node();
        n.name = name;
        n.isEmpty=true;
        n.isFlushed=false;
        n.hasChildren=false;
        init();
        if (ptr>0) {
            stack[ptr-1].isEmpty=false;
            stack[ptr-1].hasChildren=true;
            flush(true);
            if (!isCompact) {
                if (indent.length() != 0) {
                    for (int i = 0; i < ptr; i++) {
                        sb.append(indent);
                    }
                }
            }
        }
        sb.append("<"+name);
        stack[ptr++]=n;
        return this;
    }

    StringXml ns(String name) throws StringXmlException {
        if (name==null || name.length()==0) {
            attr("xmlns", nameSpaces.get(name));
        }
        else {
            attr("xmlns:"+name, nameSpaces.get(name));
        }
        return this;
    }

    StringXml attr(String name, String value) throws StringXmlException {
        if (FIND_ESCAPE_CHAR.matcher(name).find()) {
            throw new IllegalStateException("Forbidden char detected");
        }
        sb.append(" "+name+"=\""+ escape(value) + "\"");
        return(this);
    }

    void init() {
        if (sb==null) {
            sb = new StringBuilder();
            if (!omitXmlHeader) {
                sb.append(XML_HEADER);
                if (!isCompact) {
                    sb.append('\n');
                }
            }
        }
    }

    void flush(boolean nl) {
        if (!stack[ptr-1].isFlushed) {
            sb.append(">");
            if (nl && !isCompact) {
                sb.append('\n');
            }
            stack[ptr-1].isFlushed=true;
        }
    }

    StringXml val(String value) throws StringXmlException {
        flush(false);
        sb.append(escape(value));
        stack[ptr-1].isEmpty=false;
        return(this);
    }

    public StringXml end() {
        if (!stack[ptr-1].isEmpty) {
            if (stack[ptr-1].hasChildren && !isCompact) {
                sb.append('\n');
            }
            sb.append("</" + stack[ptr - 1].name + ">");
        }
        else {
            sb.append("/>");
        }
        ptr--;
        return this;
    }

    @Override
    public String toString() {
        init();
        return sb.toString();
    }
}
