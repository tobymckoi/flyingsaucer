/*
 * Copyright (C) 2014 Tobias Downer.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package org.xhtmlrenderer.dom;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * A SAX handler that forms a Flying Saucer DOM object hierarchy given
 * information provided to this object from the SAX methods implemented.
 *
 * @author Tobias Downer
 */
public class FSSAXHandler implements ContentHandler, LexicalHandler {

    private final DocumentImpl document;

    private Locator locator;

    private final List<ElementImpl> elementStack = new ArrayList();
    private final List<String> dtdStack = new ArrayList(3);

    final StringBuilder curTopText = new StringBuilder();
    private boolean currentTextInc = false;

    private static final boolean PRINT_DEBUG = false;
    

    /**
     * Constructor.
     */
    public FSSAXHandler() {
        this.document = new DocumentImpl();
    }

    /**
     * Returns the DocumentImpl formed by this SAX handler.
     * 
     * @return 
     */
    public DocumentImpl getDocument() {
        return document;
    }

    /**
     * Adds node to the current top of the document stack.
     * 
     * @param node 
     */
    private void addToTop(Node node) {
        ElementImpl cur = elementStack.get(elementStack.size() - 1);
        cur.appendChild(node);
    }

    /**
     * Push a DTD.
     */
    private void pushDTD(String name) {
        dtdStack.add(name);
    }

    /**
     * Pop a DTD.
     */
    private void popDTD() {
        dtdStack.remove(dtdStack.size() - 1);
    }

    /**
     * If currently in a DTD, returns true.
     * @param locator 
     */
    private boolean isInDTD() {
        return !dtdStack.isEmpty();
    }

    // -----

    @Override
    public void setDocumentLocator(Locator locator) {
        if (PRINT_DEBUG) System.out.println("setDocumentLocator(" + locator + ")");
        this.locator = locator;
    }

    @Override
    public void startDocument() throws SAXException {
        if (PRINT_DEBUG) System.out.println("startDocument");
        elementStack.add(document);
    }

    @Override
    public void endDocument() throws SAXException {
        if (PRINT_DEBUG) System.out.println("endDocument");
        addTextNode();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (PRINT_DEBUG) System.out.println("startPrefixMapping(" + prefix + ", " + uri + ")");
        addTextNode();
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (PRINT_DEBUG) System.out.println("endPrefixMapping(" + prefix + ")");
        addTextNode();
    }

    @Override
    public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) throws SAXException {
        if (PRINT_DEBUG) System.out.println("startElement(" + uri + ", " + localName + ", " + qName + ", " + atts + ")");
        addTextNode();
        ElementImpl element = new ElementImpl(document, uri, localName, qName);
        addToTop(element);
        elementStack.add(element);

        int sz = atts.getLength();
        for (int i = 0; i < sz; ++i) {
            String attType = atts.getType(i);
            if ( attType.equals("CDATA") || attType.equals("ID") ||
                 attType.equals("IDREF") || attType.equals("IDREFS") ) {

                String attLocalName = atts.getLocalName(i);
                String attValue = atts.getValue(i);
                String attQName = atts.getQName(i);
                String attUri = atts.getURI(i);

                if (PRINT_DEBUG) System.out.println("  attribute(" + attUri + ", " + attLocalName + ", " + attQName + ", " + attType + ", " + attValue + ")");

                element.fsAddAttribute(new AttributeImpl(
                                    attUri, attLocalName, attQName, attValue));

            }
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (PRINT_DEBUG) System.out.println("endElement(" + uri + ", " + localName + ", " + qName + ")");
        addTextNode();
        elementStack.remove(elementStack.size() - 1);
    }

    private void addTextNode() {
        if (currentTextInc) {
            addToTop(document.createTextNode(curTopText.toString()));

            // Clear the top text string,
            curTopText.setLength(0);
            currentTextInc = false;
        }
    }

    private void textInput(StringBuilder text, char[] ch, int start, int length) throws SAXException {
        // Append the character sequence,
        text.append(ch, start, length);
    }

    private void textInput(StringBuilder text, String string) throws SAXException {
        // Append the character sequence,
        text.append(string);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (PRINT_DEBUG) {
            String str = String.copyValueOf(ch, start, length);
            System.out.println("characters(" + str + ")");
        }
        textInput(curTopText, ch, start, length);
        currentTextInc = true;
    }

    public void characters(String string) throws SAXException {
        textInput(curTopText, string);
        currentTextInc = true;
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (PRINT_DEBUG) {
            String str = String.copyValueOf(ch, start, length);
            System.out.println("ignorableWhitespace(" + str + ")");
        }
        textInput(curTopText, ch, start, length);
        currentTextInc = true;
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (PRINT_DEBUG) System.out.println("processingInstruction(" + target + ", " + data + ")");
        addTextNode();
        addToTop(new ProcessingInstructionImpl(document, target, data));
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        if (PRINT_DEBUG) System.out.println("skippedEntity(" + name + ")");
        addTextNode();
    }



    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        if (PRINT_DEBUG) System.out.println("startDTD(" + name + ", " + publicId + ", " + systemId + ")");
        addTextNode();
        pushDTD(name);
    }

    @Override
    public void endDTD() throws SAXException {
        if (PRINT_DEBUG) System.out.println("endDTD()");
        addTextNode();
        popDTD();
    }

    @Override
    public void startEntity(String name) throws SAXException {
        if (PRINT_DEBUG) System.out.println("startEntity(" + name + ")");
        addTextNode();
    }

    @Override
    public void endEntity(String name) throws SAXException {
        if (PRINT_DEBUG) System.out.println("endEntity(" + name + ")");
        addTextNode();
    }

    @Override
    public void startCDATA() throws SAXException {
        if (PRINT_DEBUG) System.out.println("startCDATA()");
        addTextNode();
    }

    @Override
    public void endCDATA() throws SAXException {
        if (PRINT_DEBUG) System.out.println("endCDATA()");
        addTextNode();
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        if (!isInDTD()) {
            if (PRINT_DEBUG) {
                String str = String.copyValueOf(ch, start, length);
                System.out.println("COMMENT(" + str + ")");
            }
            addTextNode();
            addToTop(new CommentDataImpl(
                            document, String.copyValueOf(ch, start, length)));
        }
    }

    public void comment(String string) throws SAXException {
        if (!isInDTD()) {
            addTextNode();
            addToTop(new CommentDataImpl(document, string));
        }
    }

}
