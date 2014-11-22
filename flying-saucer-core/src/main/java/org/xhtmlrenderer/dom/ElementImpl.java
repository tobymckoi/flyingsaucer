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
public class ElementImpl extends NodeImpl implements Element {

    private final String uri;
    private final String localName;
    private final String qName;

    private AttributeSetImpl attributes = null;
    private NodeListImpl children = null;

    ElementImpl(DocumentImpl document, String uri, String localName, String qName) {
        super(document);
        this.uri = uri;
        this.localName = localName;
        this.qName = qName;
    }

    @Override
    public String getNodeName() {
        return qName;
    }

    @Override
    public String getNamespaceURI() {
        return uri;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public String getTagName() {
        return getNodeName();
    }

    @Override
    public AttributeSet getAttributes() {
        return fsGetAttributes();
    }

    @Override
    public String getTextContent() {
        return fsGetTextContent(new StringBuilder()).toString();
    }

    @Override
    public NodeList getChildNodes() {
        if (children == null) {
            return NodeListImpl.EMPTY;
        }
        return children;
    }

    @Override
    public String getAttribute(String key) {
        if (attributes == null) {
            return "";
        }
        return getAttributes().getValue(key);
    }

    @Override
    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }

    @Override
    public String getAttributeNS(String namespaceURI, String key) {
        if (attributes == null) {
            return "";
        }
        return getAttributes().getValueNS(namespaceURI, key);
    }

    @Override
    public ElementSet getElementsByTagName(String tagName) {
        ElementSetImpl elementList = new ElementSetImpl();
        fsPopulateElementsByTagName(elementList, tagName);
        return elementList;
    }

    @Override
    public Element appendChild(Node child) {
        if (children == null) {
            children = new NodeListImpl();
        }
        children.fsAddNode(child);
        ((NodeImpl) child).fsSetParent(this);
        return this;
    }

    AttributeSetImpl fsGetAttributes() {
        if (attributes == null) {
            attributes = new AttributeSetImpl();
        }
        return attributes;
    }

    public Attribute fsAddAttribute(Attribute attribute) {
        return fsGetAttributes().fsAddAttribute(attribute);
    }

    private void fsPopulateElementsByTagName(ElementSetImpl elements, String tagName) {
        for (Node n : getChildNodes()) {
            if (n instanceof Element) {
                ElementImpl e = (ElementImpl) n;
                if (e.getTagName().equals(tagName)) {
                    elements.add(e);
                }
                e.fsPopulateElementsByTagName(elements, tagName);
            }
        }
    }

    private StringBuilder fsGetTextContent(StringBuilder b) {
        for (Node n : getChildNodes()) {
            if (n instanceof Element) {
                ElementImpl e = (ElementImpl) n;
                e.fsGetTextContent(b);
            }
            else if (n instanceof CharacterData) {
                CharacterData cd = (CharacterData) n;
                b.append(cd.getData());
            }
        }
        return b;
    }

}
