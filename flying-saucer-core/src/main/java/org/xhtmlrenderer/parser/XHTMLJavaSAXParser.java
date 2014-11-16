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
import java.util.logging.Level;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.xhtmlrenderer.dom.Document;
import org.xhtmlrenderer.dom.FSSAXHandler;
import org.xhtmlrenderer.resource.FSEntityResolver;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * XHTML parser takes an input stream and parses it into an
 * org.xhtmlrenderer.dom.Document using the internal Java XML parsing API
 * (the SAX parser).
 *
 * @author Patrick Wright
 * @author Tobias Downer
 */
public class XHTMLJavaSAXParser implements Parser {

    private static boolean useConfiguredParser;

    static {
        useConfiguredParser = true;
    }

    /**
     * Given a java.io.Reader containing the text content of the document,
     * produces an org.xhtmlrenderer.dom.Document object representing it.
     * 
     * @param reader
     * @return 
     * @throws java.io.IOException 
     */
    @Override
    public Document createDocument(Reader reader) throws IOException {
        InputSource inputSource = new InputSource(reader);
        return createDocument(inputSource);
    }

    /**
     * Parses and returns a Flying Saucer DOM hierarchy object given an
     * XML SAX InputSource (a reader/input stream reference). This implements
     * its own error handler/entity resolver.
     * 
     * @param ins
     * @return 
     * @throws java.io.IOException 
     */
    public Document createDocument(InputSource ins) throws IOException {

        XMLReader xmlReader = newXMLReader();
        addHandlers(xmlReader);
        setParserFeatures(xmlReader);

        SAXSource input = new SAXSource(xmlReader, ins);

        // Create a flying saucer DOM builder from the SAX input source,
        Document fsDocument = createDocument(input);

        return fsDocument;
    }

    /**
     * Parses and returns a Flying Saucer DOM hierarchy object given a
     * SAX source. The SAXSource should be configured with the handlers (error
     * handles and entity resolvers) required.
     * 
     * @param saxSource
     * @return 
     * @throws java.io.IOException 
     */
    public Document createDocument(SAXSource saxSource) throws IOException {

        // Create a flying saucer DOM builder from the SAX input source,
        InputSource source = saxSource.getInputSource();
        XMLReader xmlreader = saxSource.getXMLReader();
        try {

            // Create the SAX handler,
            FSSAXHandler handler = new FSSAXHandler();
            // Set the content handler in the XML reader,
            xmlreader.setContentHandler(handler);
            // Make sure we receive lexical information (comments)
            xmlreader.setProperty("http://xml.org/sax/properties/lexical-handler",
                                  handler); 
            // Parse using the XMLReader interface,
            xmlreader.parse(source);

            // Fetch the create document and return it,
            Document doc = handler.getDocument();
//            Utils.dump(2, doc);
            return doc;

        }
        catch (SAXException ex) {
            throw new XRRuntimeException(
                    "Can't load the XML resource (using TRaX transformer). " +
                            ex.getMessage(), ex);
        }

    }


    public static final XMLReader newXMLReader() {
        XMLReader xmlReader = null;
        String xmlReaderClass = Configuration.valueFor("xr.load.xml-reader");
        
        //TODO: if it doesn't find the parser, note that in a static boolean--otherwise
        // you get exceptions on every load
        try {
            if (xmlReaderClass != null &&
                    !xmlReaderClass.toLowerCase().equals("default") &&
                    useConfiguredParser) {
                try {
                    Class.forName(xmlReaderClass);
                } catch (Exception ex) {
                    useConfiguredParser = false;
                    XRLog.load(Level.WARNING,
                            "The XMLReader class you specified as a configuration property " +
                            "could not be found. Class.forName() failed on "
                            + xmlReaderClass + ". Please check classpath. Use value 'default' in " +
                            "FS configuration if necessary. Will now try JDK default.");
                }
                if (useConfiguredParser) {
                    xmlReader = XMLReaderFactory.createXMLReader(xmlReaderClass);
                }
            }
        } catch (Exception ex) {
            XRLog.load(Level.WARNING,
                    "Could not instantiate custom XMLReader class for XML parsing: "
                    + xmlReaderClass + ". Please check classpath. Use value 'default' in " +
                    "FS configuration if necessary. Will now try JDK default.", ex);
        }
        if (xmlReader == null) {
            try {
                // JDK default
                // HACK: if
                /*CHECK: does this code do anything?
                if (System.getProperty("org.xml.sax.driver") == null) {
                    String newDefault = "org.apache.crimson.parser.XMLReaderImpl";
                    XRLog.load(Level.WARNING,
                            "No value for system property 'org.xml.sax.driver'.");
                }
                */
                xmlReader = XMLReaderFactory.createXMLReader();
                xmlReaderClass = "{JDK default}";
            } catch (Exception ex) {
                XRLog.general(ex.getMessage());
            }
        }
        if (xmlReader == null) {
            try {
                XRLog.load(Level.WARNING, "falling back on the default parser");
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                xmlReader = parser.getXMLReader();
                xmlReaderClass = "SAXParserFactory default";
            } catch (Exception ex) {
                XRLog.general(ex.getMessage());
            }
        }
        if (xmlReader == null) {
            throw new XRRuntimeException("Could not instantiate any SAX 2 parser, including JDK default. " +
                    "The name of the class to use should have been read from the org.xml.sax.driver System " +
                    "property, which is set to: "/*CHECK: is this meaningful? + System.getProperty("org.xml.sax.driver")*/);
        }
        XRLog.load("SAX XMLReader in use (parser): " + xmlReader.getClass().getName());
        return xmlReader;
    }

    /**
     * Adds the default EntityResolver and ErrorHandler for the SAX parser.
     */
    private static void addHandlers(XMLReader xmlReader) {
        try {
            // add our own entity resolver
            xmlReader.setEntityResolver(FSEntityResolver.instance());
            xmlReader.setErrorHandler(new ErrorHandler() {

                public void error(SAXParseException ex) {
                    XRLog.load(ex.getMessage());
                }

                public void fatalError(SAXParseException ex) {
                    XRLog.load(ex.getMessage());
                }

                public void warning(SAXParseException ex) {
                    XRLog.load(ex.getMessage());
                }
            });
        } catch (Exception ex) {
            throw new XRRuntimeException("Failed on configuring SAX parser/XMLReader.", ex);
        }
    }

    /**
     * Sets all standard features for SAX parser, using values from Configuration.
     */
    private static void setParserFeatures(XMLReader xmlReader) {
        try {        // perf: validation off
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            // perf: namespaces
            xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
        } catch (SAXException s) {
            // nothing to do--some parsers will not allow setting features
            XRLog.load(Level.WARNING, "Could not set validation/namespace features for XML parser," +
                    "exception thrown.", s);
        }
        if (Configuration.isFalse("xr.load.configure-features", false)) {
            XRLog.load(Level.FINE, "SAX Parser: by request, not changing any parser features.");
            return;
        }

        // perf: validation off
        setFeature(xmlReader, "http://xml.org/sax/features/validation", "xr.load.validation");

        // mem: intern strings
        setFeature(xmlReader, "http://xml.org/sax/features/string-interning", "xr.load.string-interning");

        // perf: namespaces
        setFeature(xmlReader, "http://xml.org/sax/features/namespaces", "xr.load.namespaces");
        setFeature(xmlReader, "http://xml.org/sax/features/namespace-prefixes", "xr.load.namespace-prefixes");
    }

    /**
     * Attempts to set requested feature on the parser; logs exception if not supported
     * or not recognized.
     */
    private static void setFeature(XMLReader xmlReader, String featureUri, String configName) {
        try {
            xmlReader.setFeature(featureUri, Configuration.isTrue(configName, false));

            XRLog.load(Level.FINE, "SAX Parser feature: " +
                    featureUri.substring(featureUri.lastIndexOf("/")) +
                    " set to " +
                    xmlReader.getFeature(featureUri));
        } catch (SAXNotSupportedException ex) {
            XRLog.load(Level.WARNING, "SAX feature not supported on this XMLReader: " + featureUri);
        } catch (SAXNotRecognizedException ex) {
            XRLog.load(Level.WARNING, "SAX feature not recognized on this XMLReader: " + featureUri +
                    ". Feature may be properly named, but not recognized by this parser.");
        }
    }

}
