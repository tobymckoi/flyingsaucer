/*
 * DOMTreeResolver.java
 * Copyright (c) 2005 Scott Cytacki
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.xhtmlrenderer.css.extend.lib;

import java.util.ListIterator;

import org.xhtmlrenderer.css.extend.TreeResolver;
import org.xhtmlrenderer.dom.Element;
import org.xhtmlrenderer.dom.Node;
import org.xhtmlrenderer.dom.NodeList;

/**
 * @author scott
 *         <p/>
 *         works for a w3c DOM tree
 */
public class DOMTreeResolver implements TreeResolver {
    public Object getParentElement(Object element) {
        Node parent = ((Element) element).getParentNode();
        if (!(parent instanceof Element)) parent = null;
        return parent;
    }

    public Object getPreviousSiblingElement(Object element) {
        Node sibling = ((Element) element).getPreviousSibling();
        while (sibling != null && !(sibling instanceof Element)) {
            sibling = sibling.getPreviousSibling();
        }
        if (sibling == null || !(sibling instanceof Element)) {
            return null;
        }
        return sibling;
    }

    public String getElementName(Object element) {
        String name = ((Element) element).getLocalName();
        if (name == null) name = ((Element) element).getNodeName();
        return name;
    }

    public boolean isFirstChildElement(Object element) {
        Node parent = ((Element) element).getParentNode();
        for (Node cc : parent.getChildNodes()) {
            if (cc instanceof Element && cc == element) {
                return true;
            }
        }
        return false;
    }

    public boolean isLastChildElement(Object element) {
        Node parent = ((Element) element).getParentNode();
        NodeList children = parent.getChildNodes();
        ListIterator<Node> it = children.listIterator(children.size());
        while (it.hasPrevious()) {
            Node cc = it.previous();
            if (cc instanceof Element && cc == element) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesElement(Object element, String namespaceURI, String name) {
        Element e = (Element)element;
        String localName = e.getLocalName();
        String eName;

        if (localName == null) {
            eName = e.getNodeName();
        } else {
            eName = localName;
        }

        if (namespaceURI != null) {
            return name.equals(localName) && namespaceURI.equals(e.getNamespaceURI());
        } else if (namespaceURI == TreeResolver.NO_NAMESPACE) {
            return name.equals(eName) && e.getNamespaceURI() == null;
        } else /* if (namespaceURI == null) */ {
            return name.equals(eName);
        }
    }
    
    public int getPositionOfElement(Object element) {
        Node parent = ((Element) element).getParentNode();

        int elt_count = 0;
        for (Node n : parent.getChildNodes()) {
            if (n instanceof Element) {
                if (n == element) {
                    return elt_count;
                } else {
                    elt_count++;
                }
            }
        }

        //should not happen
        return -1;
    }
}
