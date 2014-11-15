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

package org.xhtmlrenderer.parser;

import java.io.IOException;
import java.io.Reader;
import org.xhtmlrenderer.dom.Document;

/**
 * Parses a text document and creates an org.xhtmlrenderer.dom.Document object
 * representing the DOM hierarchy of the source document.
 *
 * @author Tobias Downer
 */
public interface Parser {

    /**
     * Given a java.io.Reader containing the text content of the document,
     * produces an org.xhtmlrenderer.dom.Document object representing it.
     * 
     * @param reader
     * @return 
     * @throws java.io.IOException 
     */
    Document createDocument(Reader reader) throws IOException;

}
