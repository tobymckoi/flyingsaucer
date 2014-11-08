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
public class AttributeImpl implements Attribute {

    private final String uri;
    private final String localName;
    private final String qName;
    private final String value;

    public AttributeImpl(String uri, String localName, String qName, String value) {
        this.uri = uri;
        this.localName = localName;
        this.qName = qName;
        this.value = value;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public String getKey() {
        return qName;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }

}
