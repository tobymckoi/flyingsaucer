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

package org.xhtmlrenderer.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import javax.xml.transform.sax.SAXSource;

import org.xhtmlrenderer.dom.Document;
import org.xhtmlrenderer.parser.XHTMLJavaSAXParser;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;
import org.xml.sax.InputSource;

/**
 * An XMLDocumentResource is an implementation of DocumentResource for XML
 * and XHTML formatted documents.
 * 
 * This class provides some loading and parsing functionality (as static
 * methods) using the internal Java SAX parser. This may change in the future
 * because parsing should really happen within the user agent.
 *
 * @author Tobias Downer
 */
public class XMLDocumentResource extends DocumentResource {

    private String uri = null;
    private Document document = null;

    public XMLDocumentResource(final String uri, Document document) {
        this.uri = uri;
        this.document = document;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public Document getDocument() {
        return document;
    }

    // ----- Static Methods -----
    // NOTE: These static methods are pretty ugly. We should only be able to
    //   generate XMLDocumentResource objects within a user agent.

    /**
     * VM Static XHTMLJavaSAXParser. This ultimately should be removed and
     * the parsing should be done in the user agent.
     */
    private static final XHTMLJavaSAXParser javaSAXParser = new XHTMLJavaSAXParser();

    public static XMLDocumentResource load(String uri, SAXSource sax) {
        try {
            long st = System.currentTimeMillis();
            Document document = javaSAXParser.createDocument(sax);
            long end = System.currentTimeMillis();
            XMLDocumentResource docResource =
                                    new XMLDocumentResource(uri, document);
            docResource.setElapsedLoadTime(end - st);
            XRLog.load("Loaded document in ~" + docResource.getElapsedLoadTime() + "ms");
            return docResource;
        }
        catch (IOException ex) {
            throw new XRRuntimeException("Failed to parse XML Document.", ex);
        }
    }

    public static XMLDocumentResource load(String uri, InputSource insource) {
        try {
            long st = System.currentTimeMillis();
            Document document = javaSAXParser.createDocument(insource);
            long end = System.currentTimeMillis();
            XMLDocumentResource docResource =
                                    new XMLDocumentResource(uri, document);
            docResource.setElapsedLoadTime(end - st);
            XRLog.load("Loaded document in ~" + docResource.getElapsedLoadTime() + "ms");
            return docResource;
        }
        catch (IOException ex) {
            throw new XRRuntimeException("Failed to parse XML Document.", ex);
        }
    }

    public static XMLDocumentResource load(String uri, InputStream ins) {
        return load(uri, new InputSource(ins));
    }

    public static XMLDocumentResource load(String uri, Reader reader) {
        return load(uri, new InputSource(reader));
    }

}
