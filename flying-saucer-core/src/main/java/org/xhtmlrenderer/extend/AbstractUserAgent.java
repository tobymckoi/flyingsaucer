/*
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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

package org.xhtmlrenderer.extend;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.imageio.ImageIO;

import org.xhtmlrenderer.css.parser.CSSErrorHandler;
import org.xhtmlrenderer.css.parser.CSSParser;
import org.xhtmlrenderer.css.sheet.Stylesheet;
import org.xhtmlrenderer.dom.Document;
import org.xhtmlrenderer.parser.Parser;
import org.xhtmlrenderer.parser.XHTMLJavaSAXParser;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.DocumentResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.swing.AWTFSImage;
import org.xhtmlrenderer.swing.ImageProgressListener;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;

/**
 * An abstract implementation of UserAgentCallback that implements the common
 * set of features that most user agents will need to implement. This includes
 * the FS Stylesheet parser and optional delegated image loading/caching.
 *
 * @author Torbjoern Gannholm
 * @author Tobias Downer
 */
public abstract class AbstractUserAgent implements UserAgentCallback {

    private static final int DEFAULT_IMAGE_CACHE_SIZE = 16;

    private final Object URL_LOCK = new Object();

    /**
     * a (simple) LRU cache
     */
    protected LinkedHashMap _imageCache;
    protected int _imageCacheCapacity;
    private String _baseURL;
    private String _cachedDefaultBaseURL;

    // The parser for style sheet creation.
    private final CSSParser _cssParser;
    // The parser for DOM creation. This is the Java XML parser by default.
    private Parser _documentParser;

    private boolean deferredImageLoading = false;
    private final List<ImageProgressListener> imageProgressListeners = new ArrayList();
    private final int backgroundImageLoadWorkersCount;
    private ExecutorService imageLoaderThreadPool = null;

    /**
     * Creates a new instance of NaiveUserAgent with a max image cache of 16 images.
     */
    public AbstractUserAgent() {
        this(DEFAULT_IMAGE_CACHE_SIZE);
    }

    /**
     * Creates a new NaiveUserAgent with a cache of a specific size.
     *
     * @param imgCacheSize Number of images to hold in cache before LRU images are released.
     */
    public AbstractUserAgent(final int imgCacheSize) {
        this(imgCacheSize, 5);
    }

    /**
     * Creates a new NaiveUserAgent with a cache of a specific size.
     *
     * @param imgCacheSize Number of images to hold in cache before LRU images are released.
     * @param backgroundImageLoadWorkersCount
     */
    public AbstractUserAgent(final int imgCacheSize,
                          final int backgroundImageLoadWorkersCount) {
        this._imageCacheCapacity = imgCacheSize;
        this.backgroundImageLoadWorkersCount = backgroundImageLoadWorkersCount;

        // note we do *not* override removeEldestEntry() here--users of this class must call shrinkImageCache().
        // that's because we don't know when is a good time to flush the cache
        this._imageCache = new java.util.LinkedHashMap(_imageCacheCapacity, 0.75f, true);

        this._cssParser = new CSSParser(new CSSErrorHandler() {
            public void error(String uri, String message) {
                XRLog.cssParse(Level.WARNING, "(" + uri + ") " + message);
            }
        });
        this._documentParser = new XHTMLJavaSAXParser();
    }

    /**
     * Sets the document parser used by this user agent when document resources
     * are fetched. By default this is an XHTMLJavaSAXParser.
     * 
     * ISSUE: Do we want to expand this to a more general mechanism so that
     *   this agent picks a parser depending on content type, or file
     *   name?
     * 
     * @param parser
     */
    public void setDocumentParser(Parser parser) {
        this._documentParser = parser;
    }

    /**
     * Returns the current document parser.
     * 
     * @return 
     */
    public Parser getDocumentParser() {
        return this._documentParser;
    }

    /**
     * When deferred image loading is enabled, image resources loading is
     * deferred to a background thread and any ImageProgressListener objects
     * registered are notified when the image is finished loading. This should
     * result in the panel displaying the HTML to repaint or layout if the
     * image dimensions has not been specified by CSS.
     * <p>
     * When deferred image loading is disabled, this agent will block until
     * image resources have finished being fully loaded.
     * <p>
     * Deferred image loading is disabled by default.
     * 
     * @param enabled
     */
    public void setDeferredImageLoadingEnabled(boolean enabled) {
        deferredImageLoading = enabled;
    }

    /**
     * Registers an ImageProgressListener that is notified whenever the load
     * progress of a deferred image changes. See
     * 'setDeferredImageLoadingEnabled'.
     * 
     * @param listener
     */
    public void addImageProgressListener(ImageProgressListener listener) {
        synchronized (imageProgressListeners) {
            for (ImageProgressListener l : imageProgressListeners) {
                if (l == listener) {
                    return;
                }
            }
            imageProgressListeners.add(listener);
        }
    }

    /**
     * Removes an ImageProgressListener previously added by a call to
     * 'addImageProgressListener'.
     * 
     * @param listener
     */
    public void removeImageProgressListener(ImageProgressListener listener) {
        synchronized (imageProgressListeners) {
            Iterator<ImageProgressListener> it = imageProgressListeners.iterator();
            while (it.hasNext()) {
                if (it.next() == listener) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Notifies that the image resource load has completed and the image has
     * been set to its loaded version.
     * 
     * @param imageResource 
     */
    private void fireImageProgressCompleted(ImageResource imageResource) {
        ImageProgressListener[] listeners;
        synchronized (imageProgressListeners) {
            listeners = imageProgressListeners.toArray(
                    new ImageProgressListener[imageProgressListeners.size()]);
        }
        for (ImageProgressListener l : listeners) {
            l.imageCompleted(imageResource, false);
        }
    }

    /**
     * If the image cache has more items than the limit specified for this class, the least-recently used will
     * be dropped from cache until it reaches the desired size.
     */
    public void shrinkImageCache() {
        int ovr = _imageCache.size() - _imageCacheCapacity;
        Iterator it = _imageCache.keySet().iterator();
        while (it.hasNext() && ovr-- > 0) {
            it.next();
            it.remove();
        }
    }

    /**
     * Empties the image cache entirely.
     */
    public void clearImageCache() {
        _imageCache.clear();
    }

    /**
     * Blocks while loading an image. Throws IOException if loading the image
     * failed because the uri is invalid, the file was not found, or the image
     * could not be decoded.
     * 
     * @param uri
     * @return 
     * @throws java.io.IOException
     */
    public BufferedImage loadImage(String uri) throws IOException {
        InputStream is = resolveAndOpenStream(uri);
        if (is == null) {
            throw new IOException("Failed to open stream");
        }
        try {
            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                throw new IOException("ImageIO.read() returned null");
            }
            return img;
        } finally {
            is.close();
        }
    }

    /**
     * A Runnable that loads the given image resource in the background.
     */
    protected class LoadImageRunnable implements Runnable {
        private final ImageResource imageResource;

        public LoadImageRunnable(ImageResource imageResource) {
            this.imageResource = imageResource;
        }

        @Override
        public void run() {
            String uri = imageResource.getImageUri();
            try {
                BufferedImage img = loadImage(uri);
                imageResource.setImage(AWTFSImage.createImage(img));
            } catch (FileNotFoundException e) {
                XRLog.exception("Can't read image file; image at URI '" + uri + "' not found");
                imageResource.setImage(ImageResource.NOT_FOUND_IMG);
            } catch (IOException e) {
                XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                imageResource.setImage(ImageResource.NOT_FOUND_IMG);
            }
            finally {
                fireImageProgressCompleted(imageResource);
            }
        }
    }

    /**
     * Dispatches the loading of the given image to the background image
     * loader thread.
     * 
     * @param ir
     */
    public void loadInBackground(ImageResource ir) {
        if (imageLoaderThreadPool == null) {
            imageLoaderThreadPool =
                Executors.newFixedThreadPool(backgroundImageLoadWorkersCount,
                    new ThreadFactory() {
                        private final AtomicInteger threadNumber = new AtomicInteger(1);
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r);
                            t.setName("Image Loader-" + threadNumber.getAndIncrement());
                            t.setDaemon(true);
                            return t;
                        }
                    }
            );
        }
        // Dispatch the image load operation to the thread pool,
        imageLoaderThreadPool.execute(new LoadImageRunnable(ir));
    }

    /**
     * Converts an InputStream to a Reader given a content type and uri string.
     * 
     * @param uri
     * @param contentType
     * @param ins
     * @return
     * @throws IOException 
     */
    protected Reader inputStreamToReader(
                String uri, String contentType, InputStream ins)
                                                        throws IOException {

        // Decode any parameters in 'content-type'
        Map<String, String> contentParams = new HashMap(4);
        String[] paramsSplit = contentType.split("\\;");
        for (int i = 1; i < paramsSplit.length; ++i) {
            String param = paramsSplit[i];
            int delim = param.indexOf("=");
            if (delim != -1) {
                contentParams.put(param.substring(0, delim).trim().toLowerCase(Locale.ENGLISH),
                                  param.substring(delim + 1).trim());
            }
        }

        String charset = contentParams.get("charset");
        // If no charset given then default to utf-8,
        if (charset == null) {
            charset = "utf-8";
        }

        return new InputStreamReader(ins, charset);
    }

    /**
     * Gets a Reader for the resource identified.
     * 
     * @param uri
     * @return 
     */
    protected Reader resolveAndOpenReader(String uri) {
        Reader reader = null;
        uri = resolveURI(uri);
        try {
            URL url = new URL(uri);
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();

            String contentType = urlConnection.getContentType();

            return inputStreamToReader(
                            uri, contentType, urlConnection.getInputStream());

        } catch (java.net.MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        } catch (java.io.FileNotFoundException e) {
            XRLog.exception("item at URI " + uri + " not found");
        } catch (java.io.IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        }
        return reader;
    }

    /**
     * Gets an InputStream for the resource identified.
     *
     * @param uri PARAM
     * @return The stylesheet value
     */
    //TOdO:implement this with nio.
    protected InputStream resolveAndOpenStream(String uri) {
        InputStream is = null;
        uri = resolveURI(uri);
        try {
            is = new URL(uri).openStream();
        } catch (java.net.MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        } catch (java.io.FileNotFoundException e) {
            XRLog.exception("item at URI " + uri + " not found");
        } catch (java.io.IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        }
        return is;
    }

    /**
     * Parses and returns style sheet given a Reader containing the style
     * sheet text content, a URI representing the location of the style sheet
     * (for naming purposes only), and the style sheet origin value.
     * 
     * @param reader
     * @param uri
     * @param origin
     * @return 
     */
    public synchronized Stylesheet parseStylesheet(
                    Reader reader, String uri, int origin) throws IOException {
        return _cssParser.parseStylesheet(uri, origin, reader);
    }

    /**
     * Parses and returns a document given a Reader containing the document
     * text content, and a URI representing the location of the document
     * (for naming purposes only).
     * 
     * @param reader
     * @param uri
     * @return 
     * @throws java.io.IOException
     */
    public Document parseDocument(Reader reader, String uri) throws IOException {
        return _documentParser.createDocument(reader);
    }

    /**
     * Retrieves the image located at the given URI. It's assumed the URI does point to an image--the URI will
     * be accessed (using java.io or java.net), opened, read and then passed into the JDK image-parsing routines.
     * The result is packed up into an ImageResource for later consumption.
     *
     * @param uri Location of the image source.
     * @return An ImageResource containing the image.
     */
    @Override
    public ImageResource getImageResource(String uri) {
        ImageResource ir;
        if (ImageUtil.isEmbeddedBase64Image(uri)) {
            BufferedImage image = ImageUtil.loadEmbeddedBase64Image(uri);
            ir = createImageResource(null, image);
        } else {
            uri = resolveURI(uri);
            ir = (ImageResource) _imageCache.get(uri);
            //TODO: check that cached image is still valid
            if (ir == null) {
                // Deferred image loading,
                if (deferredImageLoading) {

                    // Deferred image resource,
                    ir = new ImageResource(uri, ImageResource.LOADING_IMG);

                    // Loads the given image resource in the background,
                    loadInBackground(ir);

                }
                // Do not defer image loading,
                else {
                    try {

                        BufferedImage bufferedImage = loadImage(uri);
                        ir = createImageResource(uri, bufferedImage);

                    } catch (FileNotFoundException e) {
                        XRLog.exception("Can't read image file; image at URI '" + uri + "' not found");
                    } catch (IOException e) {
                        XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                    }                
                }

                if (ir == null) {
                    ir = new ImageResource(uri, ImageResource.NOT_FOUND_IMG);
                }
                _imageCache.put(uri, ir);

            }

        }
        return ir;
    }

    /**
     * Factory method to generate ImageResources from a given Image. May be overridden in subclass. 
     *
     * @param uri The URI for the image, resolved to an absolute URI.
     * @param img The image to package; may be null (for example, if image could not be loaded).
     *
     * @return An ImageResource containing the image.
     */
    protected ImageResource createImageResource(String uri, Image img) {
        return new ImageResource(uri, AWTFSImage.createImage(img));
    }

    /**
     * Retrieves the CSS located at the given URI.  It's assumed the URI does point to a CSS file--the URI will
     * be accessed (using java.io or java.net), opened, read and then passed into the CSS parser.
     * The result is packed up into an CSSResource for later consumption.
     *
     * @param uri Location of the CSS source.
     * @return A CSSResource containing the parsed CSS.
     */
    @Override
    public CSSResource getCSSResource(String uri, int origin) {
        Reader cssReader = resolveAndOpenReader(uri);
        if (cssReader == null) {
            return null;
        }
        try {

            // Timing metrics,
            long st = System.currentTimeMillis();
            Stylesheet stylesheet = parseStylesheet(cssReader, uri, origin);
            long end = System.currentTimeMillis();

            CSSResource cssResource = new CSSResource(uri, stylesheet);

            cssResource.setElapsedLoadTime(end - st);
            XRLog.load("Loaded stylesheet in ~" + cssResource.getElapsedLoadTime() + "ms");

            return cssResource;

        } catch (IOException ex) {
            XRLog.cssParse(Level.WARNING,
                    "Couldn't parse stylesheet at URI " + uri + ": " + ex.getMessage(), ex);
            ex.printStackTrace();
            // Return an empty style sheet if it failed to load,
            return new CSSResource(uri, new Stylesheet(uri, origin));
        } finally {
            try {
                cssReader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Retrieves the document located at the given URI. It's assumed the URI does point to a XML--the URI will
     * be accessed (using java.io or java.net), opened, read and then passed into the XML parser (XMLReader)
     * configured for Flying Saucer. The result is packed up into an DocumentResource for later consumption.
     *
     * @param uri Location of the document source.
     * @return An DocumentResource containing the image.
     */
    @Override
    public DocumentResource getDocumentResource(String uri) {
        Reader inputReader = resolveAndOpenReader(uri);
        if (inputReader == null) {
            return null;
        }
        try {

            // Timing metrics,
            long st = System.currentTimeMillis();
            // Parse the document,
            Document document = parseDocument(inputReader, uri);
            long end = System.currentTimeMillis();

            // Return it as a document resource,
            DocumentResource docResource = new DocumentResource(uri, document);

            docResource.setElapsedLoadTime(end - st);
            XRLog.load("Loaded document in ~" + docResource.getElapsedLoadTime() + "ms");

            return docResource;

        } catch (IOException ex) {
            throw new XRRuntimeException("Failed to parse Document.", ex);
        } finally {
            try {
                inputReader.close();
            } catch (IOException e) {
                // swallow
            }
        }
    }

    public DocumentResource getDocumentResource(
                            String uri, String contentType, InputStream ins) {

        try {

            // Timing metrics,
            long st = System.currentTimeMillis();
            Reader r = inputStreamToReader(uri, contentType, ins);
            // Parse the document,
            Document document = parseDocument(r, uri);
            long end = System.currentTimeMillis();

            // Return it as a document resource,
            DocumentResource docResource = new DocumentResource(uri, document);

            docResource.setElapsedLoadTime(end - st);
            XRLog.load("Loaded document in ~" + docResource.getElapsedLoadTime() + "ms");

            return docResource;

        } catch (IOException ex) {
            throw new XRRuntimeException("Failed to parse Document.", ex);
        } finally {
            try {
                ins.close();
            } catch (IOException e) {
                // swallow
            }
        }

    }
    

    @Override
    public byte[] getBinaryResource(String uri) {
        InputStream is = resolveAndOpenStream(uri);
        if (is==null) return null;
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buf = new byte[10240];
            int i;
            while ((i = is.read(buf)) != -1) {
                result.write(buf, 0, i);
            }
            is.close();
            is = null;

            return result.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Returns true if the given URI was visited, meaning it was requested at some point since initialization.
     *
     * @param uri A URI which might have been visited.
     * @return Always false; visits are not tracked in the NaiveUserAgent.
     */
    @Override
    public boolean isVisited(String uri) {
        return false;
    }

    /**
     * URL relative to which URIs are resolved.
     *
     * @param url A URI which anchors other, possibly relative URIs.
     */
    @Override
    public void setBaseURL(String url) {
        synchronized (URL_LOCK) {
            _baseURL = url;
        }
    }

    /**
     * Resolves the URI; if absolute, leaves as is, if relative, returns an absolute URI based on the baseUrl for
     * the agent.
     *
     * @param uri A URI, possibly relative.
     *
     * @return A URI as String, resolved, or null if there was an exception (for example if the URI is malformed).
     */
    @Override
    public String resolveURI(String uri) {
        if (uri == null) return null;

        String baseURL = getBaseURL();

        String ret = null;
        // test if the URI is valid; if not, try to assign the base url as its parent
        try {
            URI result = new URI(uri);
            if (!result.isAbsolute()) {
                // If the baseURL hasn't been set then resolve against the
                // local directory by default.
                if (baseURL == null) {
                    try {
                        synchronized (URL_LOCK) {
                            if (_cachedDefaultBaseURL == null) {
                                _cachedDefaultBaseURL = new File(".").toURI().toURL().toExternalForm();
                            }
                            baseURL = _cachedDefaultBaseURL;
                        }
                    }
                    catch (IOException e) {
                        throw new RuntimeException("Unable to resolve base URL.", e);
                    }
                }
                XRLog.load(uri + " is not a URL; may be relative. Testing using parent URL " + baseURL);
                result = new URI(baseURL).resolve(result);
            }
            ret = result.toString();
        } catch (URISyntaxException e) {
            XRLog.exception("The default NaiveUserAgent cannot resolve the URL " + uri + " with base URL " + baseURL);
        }
        return ret;
    }

    /**
     * Returns the current baseUrl for this class.
     */
    @Override
    public String getBaseURL() {
        synchronized (URL_LOCK) {
            return _baseURL;
        }
    }

}
