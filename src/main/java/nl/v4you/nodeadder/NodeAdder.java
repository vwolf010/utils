package nl.v4you.nodeadder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * Helper class to add dom objects in a fluent api way
 * @author Victor van der Wolf
 *
 * Created on: 9 jan. 2013
 *
 * $Id$
 */
public class NodeAdder {

    final static int MAX_NODES = 50;
    final static String EMPTY_STRING_MSG = "Empty string not allowed";

    int idx=-1;
    Node nodes[] = new Node[MAX_NODES];
    Document doc;

    public NodeAdder(Document doc) {
        if (doc==null)
            throw new NullPointerException();
        this.doc = doc;
        idx=-1;
    }

    public NodeAdder(Document doc, Node node) {
        if (doc==null || node==null)
            throw new NullPointerException();

        this.doc = doc;
        idx=0;
        nodes[0] = node;
    }

    public NodeAdder(Document doc, String tagName) throws NodeAdderException {
        if (doc==null || tagName==null)
            throw new NullPointerException();
        if (tagName.length()==0)
            throw new NodeAdderException(EMPTY_STRING_MSG);
        this.doc = doc;
        idx=0;
        nodes[0] = doc.createElement(tagName);
        doc.appendChild(nodes[0]);
    }

    public NodeAdder(Document doc, String tagName, String ns) throws NodeAdderException {
        if (doc==null || tagName==null)
            throw new NullPointerException();
        if (tagName.length()==0)
            throw new NodeAdderException(EMPTY_STRING_MSG);
        this.doc = doc;
        idx=0;
        nodes[0] = doc.createElementNS(ns, tagName);
        doc.appendChild(nodes[0]);
    }

    public NodeAdder node(String name) throws NodeAdderException {
        if (name==null)
            throw new NullPointerException();
        if (name.length()==0)
            throw new NodeAdderException(EMPTY_STRING_MSG);
        idx++;
        if (idx>=MAX_NODES) {
            throw new NodeAdderException("Too many nodes");
        }
        else {
            if (idx==0) {
                nodes[idx] = doc.createElement(name);
                doc.appendChild(nodes[idx]);
            }
            else {
                nodes[idx] = doc.createElement(name);
                nodes[idx-1].appendChild(nodes[idx]);
            }
        }
        return this;
    }

    /**
     * Example: node("kbmd-events:event", "http://schemas.kb.nl/kbmd-events/v1")
     *
     * @param name Name of the tag
     * @param ns Namespace
     * @return Reference to self for fluent api
     * @throws NodeAdderException
     */
    public NodeAdder node(String name, String ns) throws NodeAdderException {
        if (ns==null || name==null)
            throw new NullPointerException();
        if (ns.length()==0 || name.length()==0)
            throw new NodeAdderException(EMPTY_STRING_MSG);
        idx++;
        if (idx>=MAX_NODES) {
            throw new NodeAdderException("Too many nodes");
        }
        else {
            nodes[idx] = doc.createElementNS(ns, name);
            nodes[idx-1].appendChild(nodes[idx]);
        }
        return this;
    }

    public NodeAdder node(Node newNode) {
        nodes[idx].appendChild(doc.adoptNode(newNode));
        return this;
    }

    public NodeAdder end() throws NodeAdderException {
        idx--;
        if (idx<0) {
            throw new NodeAdderException("Node underflow error");
        }
        return this;
    }

    public NodeAdder attr(String name, String value, String ns) throws NodeAdderException {
        // name should have a value, value can be null or empty
        if (name==null)
            throw new NullPointerException();
        if (name.length()==0)
            throw new NodeAdderException(EMPTY_STRING_MSG);
        if (idx<0) {
            throw new NodeAdderException("Root node not set");
        }
        else {
            Element e = (Element)nodes[idx];
            e.setAttributeNS(ns, name, value);
        }
        return this;
    }

    public NodeAdder attr(String name, String value) throws NodeAdderException {
        // name should have a value, value can be null or empty
        if (name==null)
            throw new NullPointerException();
        if (name.length()==0)
            throw new NodeAdderException(EMPTY_STRING_MSG);
        if (idx<0) {
            throw new NodeAdderException("Root node not set");
        }
        else {
            Element e = (Element)nodes[idx];
            e.setAttribute(name, value);
        }
        return this;
    }
    
    public NodeAdder val(String value) throws NodeAdderException {
        if (idx<0)
            throw new NodeAdderException("Root node not set");

        if (value!=null) {
            Element e = (Element)nodes[idx];
            e.setTextContent(value);
        }
        return this;
    }

    public Node getRoot() {
        return nodes[0];
    }
}