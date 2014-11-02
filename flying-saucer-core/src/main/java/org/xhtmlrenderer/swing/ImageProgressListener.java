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

package org.xhtmlrenderer.swing;

import org.xhtmlrenderer.resource.ImageResource;

/**
 * A listener that is registered with NativeUserAgent that gets notified
 * whenever the load progress of a deferred image changes.
 * <p>
 * All notification calls happen on the worker thread that dealt with the
 * image decoding. If this is used to update a Swing element, the event
 * handling code must be dispatched on the AWT dispatcher thread.
 *
 * @author Tobias Downer
 */
public interface ImageProgressListener {

    /**
     * Notifies that the given image ImageResource has completed being
     * loaded into memory. The 'resized' boolean is true when the guessed
     * dimensions of the image are different than the final size.
     * <p>
     * NOTE: Any event handling code must be dispatched on the AWT event
     *   dispatcher thread if it causes a change in the UI.
     * 
     * @param imageResource
     * @param resized
     */
    public void imageCompleted(ImageResource imageResource, boolean resized);

}
