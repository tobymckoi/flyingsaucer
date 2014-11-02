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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.swing.AWTFSImage;
import org.xml.sax.InputSource;

/**
 * @author Administrator
 */
public class ImageResource extends AbstractResource {

    /**
     * Static images used as place holders for images that are loading or
     * not found.
     */
    public final static FSImage NOT_FOUND_IMG;
    public final static FSImage LOADING_IMG;

    static {
        // Create a 10x10 fully blank buffered image,
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
//        g.setComposite(AlphaComposite.Clear);
        g.setColor(new Color(0, 0, 255, 10));
        g.fillRect(0, 0, 8, 8);
        g.dispose();
        FSImage blankImg = AWTFSImage.createImage(img);

        NOT_FOUND_IMG = blankImg;
        LOADING_IMG = blankImg;
    }

    private final String _imageUri;
    private FSImage _img;

    // Lock when setting/accessing the image.
    private final Object IMG_LOCK = new Object();

    /**
     * Constructs this ImageResource with an FSImage.
     *
     * @param uri
     * @param img
     */
    public ImageResource(final String uri, FSImage img) {
        super((InputSource) null);
        _imageUri = uri;
        _img = img;
    }

    public FSImage getImage() {
        synchronized (IMG_LOCK) {
            return _img;
        }
    }

    public boolean isLoaded() {
        synchronized (IMG_LOCK) {
            return (_img != LOADING_IMG);
        }
    }

    /**
     * Blocks the current thread until this image is fully available.
     */
    public void blockUntilLoaded() {
        synchronized (IMG_LOCK) {
            long timeoutStart = System.currentTimeMillis();
            while (!isLoaded()) {
                try {
                    IMG_LOCK.wait(10000);
                }
                catch (InterruptedException ex) {
                    // Ignore?
                }
                // Timeout after 30 seconds waiting on image loading,
                if (System.currentTimeMillis() - timeoutStart > 30000) {
                    throw new RuntimeException(
                                    "Timed out waiting for image to load.");
                }
            }
        }
    }

    public String getImageUri() {
        return _imageUri;
    }

    public boolean hasDimensions(final int width, final int height) {
        FSImage image = getImage();
        if (image != LOADING_IMG) {
            return false;
        }
        if (image instanceof AWTFSImage) {
            AWTFSImage awtfi = (AWTFSImage) image;
            return awtfi.getWidth() == width && awtfi.getHeight() == height;
        } else {
            return false;
        }
    }

    public void setImage(FSImage loadedImage) {
        synchronized (IMG_LOCK) {
            _img = loadedImage;
            IMG_LOCK.notifyAll();
        }
    }

    /**
     * Given the dimensions defined by CSS (if any), returns the dimensions
     * of this image. 'cssWidth' or 'cssHeight' may be -1 if the dimension is
     * not defined.
     *
     * @param dotsPerPixel
     * @param cssWidth
     * @param cssHeight
     * @return
     */
    public Dimension calculateSizeFromCSS(
                            float dotsPerPixel, int cssWidth, int cssHeight) {

        FSImage img = getImage();
        if (cssWidth == -1 && cssHeight == -1) {
            return new Dimension( (int) (img.getWidth() * dotsPerPixel),
                                  (int) (img.getHeight() * dotsPerPixel) );
        }

        int currentWith = img.getWidth();
        int currentHeight = img.getHeight();
        int toWidth = cssWidth;
        int toHeight = cssHeight;

        if (toWidth == -1) {
            toWidth = (int)(currentWith * ((double)toHeight / currentHeight));
        }
        if (toHeight == -1) {
            toHeight = (int)(currentHeight * ((double)toWidth / currentWith));
        }

        return new Dimension(toWidth, toHeight);

    }

}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.6  2009/05/15 16:20:13  pdoubleya
 * ImageResource now tracks the URI for the image that was created and handles mutable images.
 *
 * Revision 1.4  2007/04/11 21:09:06  pdoubleya
 * Remove commented block
 *
 * Revision 1.3  2006/02/02 02:47:36  peterbrant
 * Support non-AWT images
 *
 * Revision 1.2  2005/06/25 17:23:34  tobega
 * first refactoring of UAC: ImageResource
 *
 * Revision 1.1  2005/02/03 20:39:35  pdoubleya
 * Added to CVS.
 *
 *
 */