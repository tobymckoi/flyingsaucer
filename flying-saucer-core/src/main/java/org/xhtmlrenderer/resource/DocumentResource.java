/*
 * Copyright (C) 2004-2014 Who?
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
package org.xhtmlrenderer.resource;


import org.xhtmlrenderer.dom.Document;

/**
 * Represents a org.xhtmlrenderer.dom.Document resource object in Flying
 * Saucer. A Document is the DOM tree of a parsed XML/XHTML/HTML text
 * document.
 * 
 * Use XMLDocumentResource to represent an XML document resource. Use
 * HTMLDocumentResource for HTML document resources.
 * 
 * @author Tobias Downer
 */
public abstract class DocumentResource extends AbstractResource {

    public abstract Document getDocument();

}
