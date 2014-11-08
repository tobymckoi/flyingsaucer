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

import java.util.Set;

/**
 *
 * @author Tobias Downer
 */
public interface AttributeSet {

    public Set<Attribute> entrySet();

    public boolean hasAttribute(String key);

    public boolean hasAttributeNS(String namespaceUri, String key);

    public String getValue(String key);

    public String getValueNS(String namespaceUri, String key);

    public Attribute getAttribute(String key);

    public Attribute setAttribute(String key, String value);
    
    public Attribute setAttributeNS(String namespaceUri, String key, String value);

}
