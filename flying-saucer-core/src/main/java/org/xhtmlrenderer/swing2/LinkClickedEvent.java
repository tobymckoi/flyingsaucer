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

package org.xhtmlrenderer.swing2;

import org.xhtmlrenderer.dom.Element;

/**
 * An event signifying an interactive click on the given &lt;A&gt; link which
 * has the given href content.
 *
 * @author Tobias Downer
 */
public class LinkClickedEvent extends DocumentEvent {

    private final Element element;
    private final String uri;

    public LinkClickedEvent(Element element, String uri) {
        this.element = element;
        this.uri = uri;
    }

    public Element getElement() {
        return element;
    }

    public String getUri() {
        return uri;
    }

}
