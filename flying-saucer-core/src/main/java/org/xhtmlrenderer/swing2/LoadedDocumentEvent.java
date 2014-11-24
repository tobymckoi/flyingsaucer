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

import org.xhtmlrenderer.dom.Document;
import org.xhtmlrenderer.layout.SharedContext;

/**
 * An event that notifies observers that a document was successfully
 * loaded.
 *
 * @author Tobias Downer
 */
public class LoadedDocumentEvent extends DocumentEvent {

    /**
     * The URI of the document loaded.
     */
    private final String uri;

    /**
     * The SharedContext object representing the loaded state of the
     * document.
     */
    private final SharedContext sharedState;

    /**
     * The Document.
     */
    private final Document document;

    /**
     * Creates the document event.
     * 
     * @param sharedState
     * @param document 
     */
    public LoadedDocumentEvent(String uri,
                            SharedContext sharedState, Document document) {
        this.uri = uri;
        this.sharedState = sharedState;
        this.document = document;
    }

    /**
     * Returns the URI of the document loaded.
     * 
     * @return 
     */
    public String getUri() {
        return uri;
    }

    /**
     * The shared context of the document loaded.
     * 
     * @return 
     */
    public SharedContext getSharedState() {
        return sharedState;
    }

    /**
     * The DOM.
     * 
     * @return 
     */
    public Document getDocument() {
        return document;
    }

}
