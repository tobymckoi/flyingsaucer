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
public abstract class NodeImpl implements Node {

    private final DocumentImpl document;
    private Node parentNode = null;

    NodeImpl(DocumentImpl document) {
        this.document = document;
    }

    @Override
    public Document getOwnerDocument() {
        return document;
    }

    @Override
    public abstract String getNodeName();

    @Override
    public Node getParentNode() {
        return parentNode;
    }

    @Override
    public boolean hasAttributes() {
        return false;
    }

    @Override
    public String getAttribute(String key) {
        return "";
    }

    @Override
    public NodeList getChildNodes() {
        return NodeListImpl.EMPTY;
    }

    @Override
    public AttributeSet getAttributes() {
        return null;
    }

    @Override
    public Node getNextSibling() {
        if (getParentNode() != null) {
            NodeList nodes = getParentNode().getChildNodes();
            int sz = nodes.size();
            for (int i = 0; i < sz - 1; ++i) {
                if (nodes.get(i) == this) {
                    return nodes.get(i + 1);
                }
            }
        }
        return null;
    }

    @Override
    public Node getPreviousSibling() {
        if (getParentNode() != null) {
            NodeList nodes = getParentNode().getChildNodes();
            for (int i = nodes.size() - 1; i > 0; --i) {
                if (nodes.get(i) == this) {
                    return nodes.get(i - 1);
                }
            }
        }
        return null;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getNamespaceURI() {
        return null;
    }

    @Override
    public String getAttributeNS(String namespaceURI, String key) {
        return "";
    }

    // -----
    
    void fsSetParent(Node parent) {
        this.parentNode = parent;
    }

}
