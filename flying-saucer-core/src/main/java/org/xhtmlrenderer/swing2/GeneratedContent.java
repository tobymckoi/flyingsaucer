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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;

import org.xhtmlrenderer.dom.Document;
import org.xhtmlrenderer.parser.Parser;
import org.xhtmlrenderer.resource.DocumentResource;
import org.xhtmlrenderer.util.GeneralUtil;

/**
 * Generated XHTML content that's produced for errors, syntax errors, etc.
 *
 * @author Tobias Downer
 */
public class GeneratedContent {

    private static void appendHTMLHeader(StringBuilder b) {
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        b.append("<!DOCTYPE html\n");
        b.append("    PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n");
        b.append("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
        b.append("\n");
        b.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
    }

    /**
     * Given a 'content' string, parses as a document and returns the
     * generated document resource object. Used to create dynamic content
     * from the engine.
     * 
     * @param uri
     * @param content
     * @return
     * @throws IOException 
     */
    static DocumentResource generateDocumentFromContent(
                        Parser documentParser,
                        String uri, String content) throws IOException {

        Document document = documentParser.createDocument(new StringReader(content));
        return new DocumentResource(uri, document);

    }

    /**
     * Generates an HTML page reporting that the local file doesn't exist.
     * 
     * @param fileName
     * @return 
     */
    static DocumentResource fileNotExistsDocument(
                            Parser documentParser,
                            String uri, File fileName) throws IOException {
        StringBuilder b = new StringBuilder();

        String titleMsg = MessageFormat.format("{0} Not Found", fileName);

        appendHTMLHeader(b);
        b.append("<head><title>");
        b.append(GeneralUtil.escapeHTML(titleMsg));
        b.append("</title></head>");
        b.append("<body>");
        b.append("<h2>");
        b.append(GeneralUtil.escapeHTML(titleMsg));
        b.append("</h2>");
        b.append("<p>The file is not found on this computer.</p>");
        b.append("</body>");
        b.append("</html>");

        return generateDocumentFromContent(documentParser, uri, b.toString());
    }

    /**
     * Generates an HTML page listing the files in the directory.
     */
    static DocumentResource localDirectoryDocument(
                            Parser documentParser,
                            String uri, File directory) throws IOException {

        StringBuilder b = new StringBuilder();

        String titleMsg = MessageFormat.format("Index of {0}", directory);

        appendHTMLHeader(b);
        b.append("<head>\n");
        b.append("<title>\n");
        b.append(GeneralUtil.escapeHTML(titleMsg));
        b.append("</title>\n");
        b.append("<style>\n");
        b.append("body {  }\n");
        b.append("h2 { border-bottom: 1px solid #909090; padding-bottom: 18px; }\n");
        b.append("li { list-style-type: none; }\n");
        b.append("a { text-decoration: none; }\n");
        b.append("</style>\n");
        b.append("</head>\n");
        b.append("<body>\n");
        b.append("<h2>");
        b.append(GeneralUtil.escapeHTML(titleMsg));
        b.append("</h2>\n");

        File[] files = directory.listFiles();
        b.append("<ul>\n");
        File parentFile = directory.getParentFile();
        if (parentFile != null) {
            String link = parentFile.toURI().toString();
            b.append("<li>");        
            b.append("<a href='");
            b.append(link);
            b.append("'>");
            b.append("..");
            b.append("</a>");
            b.append("</li>\n");
        }
        if (files != null) {
            for (File f : files) {
                if (!f.isHidden()) {
                    b.append("<li>");
                    String fname = f.getName();
                    String name;
                    String link;
                    if (f.isDirectory()) {
                        name = fname + "/";
                        link = f.toURI().toString();
                    }
                    else {
                        name = fname;
                        link = f.toURI().toString();
                    }
                    b.append("<a href='");
                    b.append(link);
                    b.append("'>");
                    b.append(GeneralUtil.escapeHTML(name));
                    b.append("</a>");

                    b.append("</li>\n");
                }
            }

        }
        else {
            b.append("<li><i>No files to list.</i></li>");
        }
        b.append("</ul>\n");

        b.append("</body>\n");
        b.append("</html>\n");
        
        return generateDocumentFromContent(documentParser, uri, b.toString());
    }




}
