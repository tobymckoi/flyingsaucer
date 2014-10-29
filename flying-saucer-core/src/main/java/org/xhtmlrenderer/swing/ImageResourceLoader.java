package org.xhtmlrenderer.swing;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.StreamResource;
import org.xhtmlrenderer.util.XRLog;


/**
 *
 */
public class ImageResourceLoader {
    public static final RepaintListener NO_OP_REPAINT_LISTENER = new RepaintListener() {
        public void repaintRequested(boolean doLayout) {
            XRLog.general(Level.FINE, "No-op repaint requested");
        }
    };
    private final Map _imageCache;

    private final ImageLoadQueue _loadQueue;

    private final int _imageCacheCapacity;

    private RepaintListener _repaintListener = NO_OP_REPAINT_LISTENER;

    private final boolean _useBackgroundImageLoading;

    public ImageResourceLoader() {
        // FIXME
        this(16);
    }

    public ImageResourceLoader(int cacheSize) {
        this._imageCacheCapacity = cacheSize;
        this._useBackgroundImageLoading = Configuration.isTrue("xr.image.background.loading.enable", false);

        if (_useBackgroundImageLoading) {
            this._loadQueue = new ImageLoadQueue();
            final int workerCount = Configuration.valueAsInt("xr.image.background.workers", 5);
            for (int i = 0; i < workerCount; i++) {
                new ImageLoadWorker(_loadQueue).start();
            }
        } else {
            this._loadQueue = null;
        }

        this._repaintListener = NO_OP_REPAINT_LISTENER;

        // note we do *not* override removeEldestEntry() here--users of this class must call shrinkImageCache().
        // that's because we don't know when is a good time to flush the cache
        this._imageCache = new LinkedHashMap(cacheSize, 0.75f, true);
    }

    public static ImageResource loadImageResourceFromUri(final String uri) {
        if (ImageUtil.isEmbeddedBase64Image(uri)) {
            return loadEmbeddedBase64ImageResource(uri);
        } else {
            StreamResource sr = new StreamResource(uri);
            InputStream is;
            ImageResource ir = null;
            try {
                sr.connect();
                is = sr.bufferedStream();
                try {
                    BufferedImage img = ImageIO.read(is);
                    if (img == null) {
                        throw new IOException("ImageIO.read() returned null");
                    }
                    ir = createImageResource(uri, img);
                } catch (FileNotFoundException e) {
                    XRLog.exception("Can't read image file; image at URI '" + uri + "' not found");
                } catch (IOException e) {
                    XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                } finally {
                    sr.close();
                }
            } catch (IOException e) {
                // couldnt open stream at URI...
                XRLog.exception("Can't open stream for URI '" + uri + "': " + e.getMessage());
            }
            if (ir == null) {
                ir = createImageResource(uri, null);
            }
            return ir;
        }
    }
    
    public static ImageResource loadEmbeddedBase64ImageResource(final String uri) {
        BufferedImage bufferedImage = ImageUtil.loadEmbeddedBase64Image(uri);
        if (bufferedImage != null) {
            FSImage image = AWTFSImage.createImage(bufferedImage);
            return new ImageResource(null, image);
        } else {
            return new ImageResource(null, null);
        }
    }

    public synchronized void shrink() {
        int ovr = _imageCache.size() - _imageCacheCapacity;
        Iterator it = _imageCache.keySet().iterator();
        while (it.hasNext() && ovr-- > 0) {
            it.next();
            it.remove();
        }
    }

    public synchronized void clear() {
        _imageCache.clear();
    }

    public ImageResource get(final String uri) {
        return get(uri, -1, -1);
    }

    public synchronized ImageResource get(final String uri, final int width, final int height) {
        if (ImageUtil.isEmbeddedBase64Image(uri)) {
            ImageResource resource = loadEmbeddedBase64ImageResource(uri);
            if (resource.getImage() != null) {
                resource = new ImageResource(null, resource.getImage().createScaled(width, height));
            }
            return resource;
        } else {
            CacheKey key = new CacheKey(uri);
            ImageResource ir = (ImageResource) _imageCache.get(key);
            if (ir == null) {

                // Not in the cache, so try and load it,
                if (isImmediateLoadUri(uri)) {
                    XRLog.load(Level.FINE, "Load immediate: " + uri);
                    ir = loadImageResourceFromUri(uri);
                    _imageCache.put(key, ir);
                    FSImage awtfsImage = ir.getImage();
                    if (width > -1 && height > -1) {
                        XRLog.load(Level.FINE, this + ", scaling " + uri + " to " + width + ", " + height);
                        ir = new ImageResource(ir.getImageUri(), awtfsImage.createScaled(width, height));
                    }
                } else {
                    XRLog.load(Level.FINE, "Image cache miss, URI not yet loaded, queueing: " + uri);
                    MutableFSImage mfsi = new MutableFSImage(_repaintListener);
                    ir = new ImageResource(uri, mfsi);
                    _imageCache.put(key, ir);
                    _loadQueue.addToQueue(this, uri, mfsi, width, height);
                }

            }
            
            return ir;
        }
    }

    public boolean isImmediateLoadUri(final String uri) {
        return ! _useBackgroundImageLoading || uri.startsWith("jar:file:") || uri.startsWith("file:");
    }

    public synchronized void loaded(final ImageResource ir, final int width, final int height) {
        String imageUri = ir.getImageUri();
        if (imageUri != null) {
            _imageCache.put(new CacheKey(imageUri), ir);
        }
    }

    public static ImageResource createImageResource(final String uri, final BufferedImage img) {
        if (img == null) {
            return new ImageResource(uri, AWTFSImage.createImage(ImageUtil.createTransparentImage(10, 10)));
        } else {
            return new ImageResource(uri, AWTFSImage.createImage(ImageUtil.makeCompatible(img)));
        }
    }

    public void setRepaintListener(final RepaintListener repaintListener) {
        _repaintListener = repaintListener;
    }

    public void stopLoading() {
        if (_loadQueue != null) {
            XRLog.load("By request, clearing pending items from load queue: " + _loadQueue.size());
            _loadQueue.reset();
        }
    }

    private static class CacheKey {
        final String uri;

        public CacheKey(final String uri) {
            this.uri = uri;
        }

        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;

            final CacheKey cacheKey = (CacheKey) o;

            if (!uri.equals(cacheKey.uri)) return false;

            return true;
        }

        public int hashCode() {
            int result = uri.hashCode();
            return result;
        }
    }
}

// from-io-loader
// from-cache-loader
// from-fs-loader
