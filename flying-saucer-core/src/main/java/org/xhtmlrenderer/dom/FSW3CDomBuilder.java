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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.sax.SAXSource;

import org.xhtmlrenderer.util.XRRuntimeException;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * An implementation of FSDomBuilder that uses a SAX source to build into a
 * Flying Saucer DOM.
 *
 * @author Tobias Downer
 */
public class FSW3CDomBuilder implements FSDomBuilder {

    private final SAXSource sax;

    public FSW3CDomBuilder(SAXSource sax) {
        this.sax = sax;
    }

    @Override
    public Document build() {

        DocumentImpl doc = new DocumentImpl();

        InputSource source = sax.getInputSource();
        XMLReader xmlreader = sax.getXMLReader();
        try {
            FSSaxHandler handler = new FSSaxHandler(doc);
            xmlreader.setContentHandler(handler);
            xmlreader.setProperty("http://xml.org/sax/properties/lexical-handler",
                                  handler); 
            xmlreader.parse(source);
        }
        catch (IOException ex) {
            throw new XRRuntimeException(
                    "Can't load the XML resource (using TRaX transformer). " +
                    ex.getMessage(), ex);
        }
        catch (SAXException ex) {
            throw new XRRuntimeException(
                    "Can't load the XML resource (using TRaX transformer). " +
                            ex.getMessage(), ex);
        }
        
//        Utils.dump(2, doc);
        
        return doc;
    }

    /**
     * The SAX Content Handler that populates our DOM.
     */
    private static class FSSaxHandler implements ContentHandler, LexicalHandler {

        private final DocumentImpl document;

        private Locator locator;

        private final List<ElementImpl> elementStack = new ArrayList();

        final StringBuilder curTopText = new StringBuilder();
        private boolean currentTextInc = false;

        

        public FSSaxHandler(DocumentImpl document) {
            this.document = document;
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


        // -----

        @Override
        public void setDocumentLocator(Locator locator) {
//            System.out.println("setDocumentLocator(" + locator + ")");
            this.locator = locator;
        }

        @Override
        public void startDocument() throws SAXException {
//            System.out.println("startDocument");
            elementStack.add(document);
        }

        @Override
        public void endDocument() throws SAXException {
            addTextNode();
//            System.out.println("endDocument");
//            Utils.checkParents(null, document);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
//            System.out.println("startPrefixMapping(" + prefix + ", " + uri + ")");
            addTextNode();
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
//            System.out.println("endPrefixMapping(" + prefix + ")");
            addTextNode();
        }

        @Override
        public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) throws SAXException {
//            System.out.println("startElement(" + uri + ", " + localName + ", " + qName + ", " + atts + ")");
            addTextNode();
            ElementImpl element = new ElementImpl(document, uri, localName, qName);
            addToTop(element);
            elementStack.add(element);

            int sz = atts.getLength();
            for (int i = 0; i < sz; ++i) {
                String attLocalName = atts.getLocalName(i);
                String attType = atts.getType(i);
                String attValue = atts.getValue(i);
                String attQName = atts.getQName(i);
                String attUri = atts.getURI(i);
                
                element.fsAddAttribute(new AttributeImpl(attUri, attLocalName, attQName, attValue));
            }

        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
//            System.out.println("endElement(" + uri + ", " + localName + ", " + qName + ")");
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

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
//            String str = String.copyValueOf(ch, start, length);
//            System.out.println("characters(" + str + ")");
            textInput(curTopText, ch, start, length);
            currentTextInc = true;
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
//            String str = String.copyValueOf(ch, start, length);
//            System.out.println("ignorableWhitespace(" + str + ")");
            textInput(curTopText, ch, start, length);
            currentTextInc = true;
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            addTextNode();
//            System.out.println("processingInstruction(" + target + ", " + data + ")");
            addToTop(new ProcessingInstructionImpl(document, target, data));
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
            addTextNode();
//            System.out.println("skippedEntity(" + name + ")");
        }



        @Override
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            addTextNode();
        }

        @Override
        public void endDTD() throws SAXException {
            addTextNode();
        }

        @Override
        public void startEntity(String name) throws SAXException {
            addTextNode();
//            System.out.println("startEntity(" + name + ")");
        }

        @Override
        public void endEntity(String name) throws SAXException {
            addTextNode();
//            System.out.println("endEntity(" + name + ")");
        }

        @Override
        public void startCDATA() throws SAXException {
            addTextNode();
//            System.out.println("startCDATA()");
        }

        @Override
        public void endCDATA() throws SAXException {
            addTextNode();
//            System.out.println("endCDATA()");
        }

        @Override
        public void comment(char[] ch, int start, int length) throws SAXException {
            addTextNode();
            addToTop(new CommentDataImpl(
                            document, String.copyValueOf(ch, start, length)));

//            String str = String.copyValueOf(ch, start, length);
//            System.out.println("COMMENT(" + str + ")");
        }

    }

}
