/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Who?
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
 * }}}
 */
package org.xhtmlrenderer.resource;

import java.io.InputStream;
import java.io.Reader;
import javax.xml.transform.sax.SAXSource;

import org.xhtmlrenderer.dom.Document;
import org.xhtmlrenderer.parser.XHTMLJavaSAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * This is a class for supporting legacy XMLResource code. This delegates
 * to a XMLDocumentResource object.
 * <p>
 * For future code, please use XMLDocumentResource instead.
 *
 * @author Patrick Wright
 * @deprecated use XMLDocumentResource instead.
 */

public class XMLResource extends AbstractResource {

    private final DocumentResource delegate;

    private XMLResource(DocumentResource delegate) {
        this.delegate = delegate;
    }

    // ------ Delegate -----

    public Document getDocument() {
        return delegate.getDocument();
    }

    @Override
    public long getResourceLoadTimeStamp() {
        return delegate.getResourceLoadTimeStamp();
    }

    @Override
    public long getElapsedLoadTime() {
        return delegate.getElapsedLoadTime();
    }

    @Override
    void setElapsedLoadTime(long elapsedLoadTime) {
        delegate.setElapsedLoadTime(elapsedLoadTime);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    // ----- Public Static Methods -----

    /**
     * @deprecated use DocumentResource.load instead.
     */
    public static XMLResource load(InputStream stream) {
        return new XMLResource(XMLDocumentResource.load(null, stream));
    }

    /**
     * @deprecated use DocumentResource.load instead.
     */
    public static XMLResource load(InputSource source) {
        return new XMLResource(XMLDocumentResource.load(null, source));
    }

    /**
     * @deprecated use DocumentResource.load instead.
     */
    public static XMLResource load(Reader reader) {
        return new XMLResource(XMLDocumentResource.load(null, new InputSource(reader)));
    }

    /**
     * @deprecated use DocumentResource.load instead.
     */
    public static XMLResource load(SAXSource source) {
        return new XMLResource(XMLDocumentResource.load(null, source));
    }

    /**
     * @deprecated use DocumentResource.newXMLReader instead.
     */
    public static final XMLReader newXMLReader() {
        return XHTMLJavaSAXParser.newXMLReader();
    }

}
