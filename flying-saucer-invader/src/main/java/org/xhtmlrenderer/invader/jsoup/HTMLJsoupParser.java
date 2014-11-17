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

package org.xhtmlrenderer.invader.jsoup;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;

import org.xhtmlrenderer.dom.Document;
import org.xhtmlrenderer.dom.FSSAXHandler;
import org.xhtmlrenderer.dom.Utils;
import org.xhtmlrenderer.util.XRRuntimeException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A parser that creates a DOM Document using the HTML parser in the jsoup
 * library.
 *
 * @author Tobias Downer
 */
public class HTMLJsoupParser implements org.xhtmlrenderer.parser.Parser {

    @Override
    public Document createDocument(Reader reader) throws IOException {

        // Jsoup parses the HTML content as a string.
        // NOTE: This will prevent us from implementing true incremental
        //   layout.

        StringBuilder b = new StringBuilder(4096);
        char[] charBuf = new char[4096];
        while (true) {
            int readCount = reader.read(charBuf, 0, charBuf.length);
            if (readCount == -1) {
                break;
            }
            b.append(charBuf, 0, readCount);
        }

        // The string,
        String htmlString = b.toString();

        // This is the parsed HTML in the jsoup DOM hierarchy. We want to
        // transform it into the Flying Saucer DOM.
        org.jsoup.nodes.Document jsoup = Jsoup.parse(htmlString);

        // What we do is create a SAX handler object that builds into the
        // Flying Saucer DOM. We can then recursively walk through the jsoup
        // tree and call the necessary methods in the SAX interface to handle
        // the code.

        FSSAXHandler handler = new FSSAXHandler();

//        handler.setDocumentLocator(locator);

        try {
            handler.startDocument();

            List<Node> docNodes = jsoup.childNodes();

            for (Node n : docNodes) {
                if (n instanceof Element) {
                    pushElement((Element) n, handler);
                }
                else if (n instanceof XmlDeclaration) {
                    XmlDeclaration decl = (XmlDeclaration) n;
                    String declString = decl.getWholeDeclaration();
                }
                else if (n instanceof Comment) {
                    // Handle processing instruction.
                    // ISSUE: This seems a bit hacky. Jsoup puts the data in
                    //   the comment.
                    Comment comment = (Comment) n;
                    String commentText = comment.getData();
                    if (commentText.startsWith("?xml-")) {
                        // Trim the head and trailing '?' characters,
                        String t = commentText.substring(
                                                1, commentText.length() - 1);
                        // Split at the first whitespace,
                        int delim = t.indexOf(" ");
                        // Send as processing instruction,
                        handler.processingInstruction(t.substring(0, delim),
                                                      t.substring(delim + 1));
                    }
                }
                else {
//                    System.out.println("Unknown: " + n.getClass());
//                    System.out.println("Val: " + n);
                }
            }
            
            handler.endDocument();
        }
        catch (SAXException ex) {
            throw new XRRuntimeException(
                    "Unable to convert jsoup DOM. " +
                            ex.getMessage(), ex);
        }

        return handler.getDocument();
        
    }

    /**
     * Push an element out to the SAX handler.
     * 
     * @param el
     * @param handler 
     */
    private void pushElement(Element el, FSSAXHandler handler) throws SAXException {

        // Insert the Element and the attributes,
        String jsoupTagName = el.tagName();
        
        // Does Jsoup support namespaces?
        final String uri = "";
        final String localName = jsoupTagName;
        final String qName = jsoupTagName;

        AttributesImpl atts = new AttributesImpl();
        for (Attribute att : el.attributes()) {
            String jsoupKey = att.getKey();
            String jsoupValue = att.getValue();
            String attType = jsoupKey.equals("id") ? "ID" : "CDATA";
            String attUri = "";

            atts.addAttribute(attUri, jsoupKey, jsoupKey, attType, jsoupValue);
        }

        // Start the element,
        handler.startElement(uri, localName, qName, atts);

        // Handle text and data nodes,
        List<Node> nodeList = el.childNodes();
        for (Node n : nodeList) {
            if (n instanceof Comment) {
                Comment comment = (Comment) n;
                handler.comment(comment.getData());
            }
            else if (n instanceof TextNode) {
                TextNode text = (TextNode) n;
                handler.characters(text.getWholeText());
            }
            else if (n instanceof DataNode) {
                DataNode data = (DataNode) n;
                handler.characters(data.getWholeData());
            }
            else if (n instanceof Element) {
                // Recurse,
                pushElement((Element) n, handler);
            }
            else {
                throw new RuntimeException(
                                "Unknown jsoup node type: " + n.getClass());
            }
        }

        // End the element,
        handler.endElement(uri, localName, qName);

    }

    /**
     * For testing.
     */
    public static void main(String[] args) {
        try {
            URL url = new URL("http://www.gnu.org/licenses/license-list.html");

            URLConnection c = url.openConnection();
            c.connect();
            
            HTMLJsoupParser p = new HTMLJsoupParser();

            Document document = p.createDocument(
                        new InputStreamReader(c.getInputStream(), "utf-8"));

            System.out.println("FINISHING WITH RESULT: " + document);
            
            Utils.dump(0, document);

        }
        catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

}
