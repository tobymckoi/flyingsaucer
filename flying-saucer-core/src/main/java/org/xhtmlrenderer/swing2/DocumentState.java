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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xhtmlrenderer.dom.Document;
import org.xhtmlrenderer.layout.SharedContext;

/**
 * A DocumentState encapsulates the state of a single document. This
 * object provides observers a way to be notified of the status of a
 * document as it is progressively loaded.
 *
 * @author Tobias Downer
 */
public class DocumentState {

    /**
     * The agent for this document state.
     */
    private final Agent agent;

    /**
     * The SharedContext object for the document currently loaded, or null
     * if no document is loaded.
     */
    private SharedContext sharedContext = null;

    /**
     * The DOM of the document current loaded, or null if no document is
     * loaded.
     */
    private Document dom = null;

    private final Object LOCK = new Object();

    /**
     * The set of listeners.
     */
    private final List<DocumentListener> listeners = new ArrayList(4);

    /**
     * Constructor.
     * 
     * @param agent 
     */
    DocumentState(Agent agent) {
        this.agent = agent;
    }

    /**
     * Returns the SharedContext and Document object for the current document
     * that's loaded, or null if no document currently loaded. If a document is
     * currently loaded and then a new document is loaded after, this object
     * will return the previous document until the new document is ready for
     * rendering.
     * <p>
     * Returns null if no document is currently loaded.
     * 
     * @return 
     */
    public LoadedDocument getLoadedDocument() {
        synchronized (LOCK) {
            if (sharedContext == null) {
                return null;
            }
            return new LoadedDocument(sharedContext, dom);
        }
    }

    /**
     * Adds a listener to this document state.
     * 
     * @param documentListener
     */
    public void addListener(DocumentListener documentListener) {
        synchronized (listeners) {
            for (DocumentListener l : listeners) {
                if (l == documentListener) {
                    return;
                }
            }
            listeners.add(documentListener);
        }
    }

    /**
     * Removes a listener from this document state.
     * @param documentListener
     */
    public void removeListener(DocumentListener documentListener) {
        synchronized (listeners) {
            Iterator<DocumentListener> it = listeners.iterator();
            while (it.hasNext()) {
                DocumentListener l = it.next();
                if (l == documentListener) {
                    it.remove();
                    return;
                }
            }
        }
    }

    /**
     * Attempts to load the document at the given URI address. This can be
     * called safely from any thread. The method returns immediately regardless
     * of whether the URI is valid or not, or the document can be loaded.
     * The loading of the document happens on its own thread.
     * <p>
     * A document is considered loaded when the document content is
     * locally available, the content has been parsed into a DOM, and
     * stylesheet information is downloaded and available locally.
     * <p>
     * When a document has loaded, a 'documentLoaded' event is generated.
     * 
     * @param uri 
     */
    public void loadURI(String uri) {
        
        fire(new StartedDocumentEvent(uri));
        
        agent.dispatchLoadURI(uri, this);
    }

    /**
     * Called by the agent when a document is successfully loaded and parsed,
     * and the shared context will be populated with the stylesheet
     * information, etc. The given 'dom' will contain the completed document
     * object model ready for layout.
     * <p>
     * This call can happen on any thread.
     * 
     * @param ss
     * @param dom
     */
    void documentLoaded(String uri, SharedContext ss, Document dom) {

        synchronized (LOCK) {
            this.sharedContext = ss;
            this.dom = dom;
        }

        // Notify listeners that the document is loaded,
        fire(new LoadedDocumentEvent(uri, ss, dom));
    }

    /**
     * Called by the agent when the progression of an image in this document
     * changes. Typically this will result in the renderer repainting the
     * box(s) that contain the image. Image progress events only occur when
     * deferred image loading is enabled.
     */
    void imageProgress(ImageProgressEvent evt) {
        fire(evt);
    }

    /**
     * Fires a document event to all the listeners.
     * 
     * @param evt 
     */
    private void fire(DocumentEvent evt) {
        // Make a copy of the listeners list,
        DocumentListener[] lls;
        synchronized (listeners) {
            lls = listeners.toArray(new DocumentListener[listeners.size()]);
        }
        // Dispatch from the array list,
        for (DocumentListener listener : lls) {
            listener.notify(evt);
        }
    }

}
