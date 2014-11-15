/*
 * {{{ header & license
 * Copyright (c) 2007 Vianney le Clément
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
package org.xhtmlrenderer.swt;

import java.io.*;
import java.util.Iterator;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.xhtmlrenderer.extend.AbstractUserAgent;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.XRLog;

/**
 * Naive user agent, copy of org.xhtmlrenderer.swing.NaiveUserAgent (but
 * modified for SWT, of course).
 *
 * @author Vianney le Clément
 *
 */
public class NaiveUserAgent extends AbstractUserAgent {

    private final Device _device;

    /**
     * Creates a new instance of NaiveUserAgent
     */
    public NaiveUserAgent(Device device) {
        _device = device;
    }

    public CSSResource getCSSResource(String uri, int origin) {
        return super.getCSSResource(uri, origin);
    }

    public ImageResource getImageResource(String uri) {
        ImageResource ir = null;
        if (ImageUtil.isEmbeddedBase64Image(uri)) {
            ir = loadEmbeddedBase64ImageResource(uri);
        } else {
            uri = resolveURI(uri);
            ir = (ImageResource) _imageCache.get(uri);
            // TODO: check that cached image is still valid
            if (ir == null) {
                InputStream is = resolveAndOpenStream(uri);
                if (is != null) {
                    try {
                        ir = createImageResource(uri, is);
                        if (_imageCache.size() >= _imageCacheCapacity) {
                            // prevent the cache from growing too big
                            ImageResource old = (ImageResource) _imageCache
                                    .remove(_imageCache.keySet().iterator().next());
                            ((SWTFSImage) old.getImage()).getImage().dispose();
                        }
                        _imageCache.put(uri, ir);
                    } catch (SWTException e) {
                        XRLog.exception(
                                "Can't read image file; unexpected problem for URI '"
                                + uri + "'", e);
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // swallow
                        }
                    }
                }
            }
            if (ir == null) {
                ir = new ImageResource(uri, null);
            }
        }
        return ir;
    }
    
    /**
     * Factory method to generate ImageResources from a given Image. May be
     * overridden in subclass.
     *
     * @param uri The URI for the image, resolved to an absolute URI.
     * @param is Stream of the image; may be null (for example, if image could
     * not be loaded).
     *
     * @return An ImageResource containing the image.
     */
    protected ImageResource createImageResource(String uri, InputStream is) {
        return new ImageResource(uri, new SWTFSImage(new Image(_device, is), this, uri));
    }
    
    private ImageResource loadEmbeddedBase64ImageResource(final String uri) {
        byte[] image = ImageUtil.getEmbeddedBase64Image(uri);
        if (image != null) {
            return createImageResource(null, new ByteArrayInputStream(image));
        }
        return new ImageResource(null, null);
    }

    /**
     * If the image cache has more items than the limit specified for this class, the least-recently used will
     * be dropped from cache until it reaches the desired size.
     */
    public void shrinkImageCache() {
        int ovr = _imageCache.size() - _imageCacheCapacity;
        Iterator it = _imageCache.keySet().iterator();
        while (it.hasNext() && ovr-- > 0) {
            ImageResource ir = (ImageResource) it.next();
            ((SWTFSImage) ir.getImage()).getImage().dispose();
            it.remove();
        }
    }

    /**
     * Empties the image cache entirely.
     */
    public void clearImageCache() {
        for (Iterator iter = _imageCache.values().iterator(); iter.hasNext();) {
            ImageResource ir = (ImageResource) iter.next();
            ((SWTFSImage) ir.getImage()).getImage().dispose();
        }
        _imageCache.clear();
    }

}
