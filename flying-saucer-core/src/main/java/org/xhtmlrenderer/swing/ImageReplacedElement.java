/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.swing;

import java.awt.*;
import org.xhtmlrenderer.extend.FSImage;

import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.resource.ImageResource;

/**
 * An ImageReplacedElement is a {@link ReplacedElement} that contains a {@link java.awt.Image}. It's used as a
 * container for images included within XML being rendered. The image contained is immutable.
 */
public class ImageReplacedElement implements ReplacedElement {

    private Point _location = new Point(0, 0);

    protected ImageResource _image;
    protected int _targetWidth;
    protected int _targetHeight;

    protected ImageReplacedElement() {
    }

    /**
     * Creates a new ImageReplacedElement and scales it to the size specified if either width or height has a valid
     * value (values are > -1), otherwise original size is preserved. The idea is that the image was loaded at
     * a certain size (that's the Image instance here) and that at the time we create the ImageReplacedElement
     * we have a target W/H we want to use.
     *
     * @param image An image.
     * @param targetWidth The width we'd like the image to have, in pixels.
     * @param targetHeight The height we'd like the image to have, in pixels.
     */
    public ImageReplacedElement(ImageResource image, int targetWidth, int targetHeight) {
        if (image == null) throw new NullPointerException();
        if (targetWidth < 0 || targetHeight < 0) throw new IllegalStateException();
        _image = image;
        _targetWidth = targetWidth;
        _targetHeight = targetHeight;
    }

    /** {@inheritDoc} */
    public void detach(LayoutContext c) {
        // nothing to do in this case
    }

    /** {@inheritDoc} */
    public int getIntrinsicHeight() {
        return _targetHeight;
    }

    /** {@inheritDoc} */
    public int getIntrinsicWidth() {
        return _targetWidth;
    }

    /** {@inheritDoc} */
    public Point getLocation() {
        return _location;
    }

    /** {@inheritDoc} */
    public boolean isRequiresInteractivePaint() {
        return true;
    }

    /** {@inheritDoc} */
    public void setLocation(int x, int y) {
        _location = new Point(x, y);
    }

    /**
     * Returns the ImageResource for this replacement.
     * @return 
     */
    public ImageResource getImageResource() {
        return _image;
    }

    /**
     * The image we're replacing.
     * @return see desc
     */
    public FSImage getFSImage() {
        return _image.getImage().createScaled(_targetWidth, _targetHeight);
    }

	public int getBaseline() {
		return 0;
	}

	public boolean hasBaseline() {
		return false;
	}
}
