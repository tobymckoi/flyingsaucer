/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 * Copyright (c) 2009 Patrick Wright
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.swing;

import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class AWTFSImage implements FSImage {
    private static final FSImage NULL_FS_IMAGE = new NullImage();

    public static FSImage createImage(Image img) {
        if (img == null) {
            return NULL_FS_IMAGE;
        } else {
            BufferedImage bimg;
            if (img instanceof BufferedImage) {
                bimg = ImageUtil.makeCompatible((BufferedImage) img);
            } else {
                bimg = ImageUtil.convertToBufferedImage(img, BufferedImage.TYPE_INT_ARGB);
            }
            return new NewAWTFSImage(bimg);
        }
    }

    protected AWTFSImage() {
    }

    public abstract BufferedImage getImage();


    static class NewAWTFSImage extends AWTFSImage {
        private final BufferedImage img;
        private final int targetWidth;
        private final int targetHeight;

        private NewAWTFSImage(BufferedImage img, int targetWidth, int targetHeight) {
            this.img = img;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
        }

        public NewAWTFSImage(BufferedImage img) {
            this(img, img.getWidth(), img.getHeight());
        }

        public int getWidth() {
            return targetWidth;
        }

        public int getHeight() {
            return targetHeight;
        }

        public BufferedImage getImage() {
            return img;
        }

        public FSImage createScaled(int width, int height) {
            if (width > 0 || height > 0) {
                int currentWith = getWidth();
                int currentHeight = getHeight();
                int toWidth = width;
                int toHeight = height;

                if (toWidth == -1) {
                    toWidth = (int)(currentWith * ((double)toHeight / currentHeight));
                }

                if (toHeight == -1) {
                    toHeight = (int)(currentHeight * ((double)toWidth / currentWith));
                }

                if (currentWith != toWidth || currentHeight != toHeight) {
                    return new NewAWTFSImage(img, toWidth, toHeight);
                }
            }
            return this;
        }
    }

    private static class NullImage extends AWTFSImage {
        private static final BufferedImage EMPTY_IMAGE = ImageUtil.createTransparentImage(1, 1);

        public int getWidth() {
            return 0;
        }

        public int getHeight() {
            return 0;
        }

        public FSImage createScaled(int width, int height) {
            return this;
        }

        public BufferedImage getImage() {
            return EMPTY_IMAGE;
        }
    }
}
