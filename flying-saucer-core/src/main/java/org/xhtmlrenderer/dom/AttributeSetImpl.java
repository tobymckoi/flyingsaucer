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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Tobias Downer
 */
public class AttributeSetImpl implements AttributeSet {

    private List<Attribute> attributes = null;

    @Override
    public Collection<Attribute> getAttributes() {
        if (attributes == null) {
            return Collections.EMPTY_SET;
        }
        return Collections.unmodifiableCollection(attributes);
    }

    @Override
    public boolean hasAttribute(String key) {
        if (attributes == null) {
            return false;
        }
        for (Attribute attr : attributes) {
            if (attr.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAttributeNS(String namespaceURI, String key) {
        if (attributes == null) {
            return false;
        }
        for (Attribute attr : attributes) {
            if (Utils.equals(namespaceURI, attr.getUri()) && attr.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getValue(String key) {
        if (attributes != null) {
            for (Attribute attr : attributes) {
                if (attr.getKey().equals(key)) {
                    return attr.getValue();
                }
            }
        }
        return "";
    }

    public String getValueNS(String namespaceURI, String key) {
        if (attributes != null) {
            for (Attribute attr : attributes) {
                if (Utils.equals(namespaceURI, attr.getUri()) && attr.getKey().equals(key)) {
                    return attr.getValue();
                }
            }
        }
        return "";
    }

    @Override
    public Attribute getAttribute(String key) {
        if (attributes != null) {
            for (Attribute attr : attributes) {
                if (attr.getKey().equals(key)) {
                    return attr;
                }
            }
        }
        return null;
    }

    @Override
    public Attribute setAttribute(String key, String value) {
        return fsAddAttribute(new AttributeImpl(null, null, key, value));
    }

    @Override
    public Attribute setAttributeNS(String namespaceUri, String key, String value) {
        String localName = key.substring(key.indexOf("-") + 1);
        return fsAddAttribute(new AttributeImpl(namespaceUri, localName, key, value));
    }


    public boolean isEmpty() {
        return attributes == null || attributes.isEmpty();
    }


    @Override
    public String toString() {
        return (attributes == null) ? "[]" : attributes.toString();
    }

    public Attribute fsAddAttribute(Attribute attribute) {
        if (attributes == null) {
            attributes = new ArrayList(8);
        }
        attributes.add(attribute);
        return attribute;
    }

}
