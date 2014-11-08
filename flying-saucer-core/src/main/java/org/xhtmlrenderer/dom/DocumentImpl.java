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

/**
 *
 * @author Tobias Downer
 */
public class DocumentImpl extends ElementImpl implements Document {

    DocumentImpl() {
        super(null, null, null, null);
    }

    @Override
    public Document getOwnerDocument() {
        return this;
    }

    @Override
    public String getNodeName() {
        return "#document";
    }

    @Override
    public Element getDocumentElement() {
        for (Node n : getChildNodes()) {
            if (n instanceof Element) {
                return (Element) n;
            }
        }
        throw new IllegalStateException("Document doesn't have base element");
    }

    @Override
    public Element getElementById(String idName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TextNode createTextNode(String text) {
        return new TextNodeImpl(this, text);
    }

    @Override
    public Element createElement(String tagName) {
        return new ElementImpl(this, null, null, tagName);
    }

    @Override
    public Element createElementNS(String namespaceUri, String qName) {
        int delim = qName.indexOf("-");
        if (delim == -1) {
            delim = -1;
        }
        return new ElementImpl(this, namespaceUri,
                                        qName.substring(delim + 1), qName);
    }
    
}
