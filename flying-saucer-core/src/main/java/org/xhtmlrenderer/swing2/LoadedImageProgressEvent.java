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

import org.xhtmlrenderer.resource.ImageResource;

/**
 * An event that notifies observers that an image has completed loading.
 * After this event is generated, no further events are generated for this
 * image resource.
 *
 * @author Tobias Downer
 */
public class LoadedImageProgressEvent extends ImageProgressEvent {

    public LoadedImageProgressEvent(ImageResource imageResource) {
        super(imageResource);
    }
    
}
