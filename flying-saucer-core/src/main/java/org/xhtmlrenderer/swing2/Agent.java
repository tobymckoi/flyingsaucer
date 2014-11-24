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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import javax.imageio.ImageIO;

import org.xhtmlrenderer.css.parser.CSSErrorHandler;
import org.xhtmlrenderer.css.parser.CSSParser;
import org.xhtmlrenderer.css.sheet.Stylesheet;
import org.xhtmlrenderer.dom.Document;
import org.xhtmlrenderer.dom.Element;
import org.xhtmlrenderer.extend.FontResolver;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.TextRenderer;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.extend.UserInterface;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.parser.Parser;
import org.xhtmlrenderer.parser.XHTMLJavaSAXParser;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.DocumentResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;
import org.xhtmlrenderer.swing.AWTFSImage;
import org.xhtmlrenderer.swing.AWTFontResolver;
import org.xhtmlrenderer.swing.Java2DTextRenderer;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;

/**
 * Manages capabilities provided by the operating environment such as the
 * load resources over a network, caches, cookie stores, etc.
 *
 * @author Tobias Downer
 */
public class Agent {

    /**
     * The thread pool used by the agent to load resources in the background.
     */
    private ExecutorService threadPool;

//    /**
//     * The user agent used to access the underlying resources.
//     */
//    private AbstractUserAgent agentUserAgent;

    /**
     * The shared font resolver.
     */
    private FontResolver agentFontResolver;

    /**
     * True for deferred image loading (when an image is requested it's
     * loaded on a background thread).
     */
    private boolean deferredImageLoading = false;

    /**
     * The image cache maps URI to ImageResource objects.
     */
    private final Map<String, ImageResource> imageCache = new HashMap();
    private final Map<String, List<DocumentState>> statesObservingImageProgress = new HashMap();
    private final Object imageCacheLock = new Object();

    /**
     * Constructor.
     */
    public Agent() {
    }

    /**
     * Initializes this agent.
     */
    public void init() {
        if (threadPool == null) {
            // NOTE: This creates Daemon threads. Fixed thread pool with 4 threads.
            threadPool = Executors.newFixedThreadPool(4, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = Executors.defaultThreadFactory().newThread(r);
                    thread.setDaemon(true);
                    return thread;
                }
            });
            agentFontResolver = new AWTFontResolver();
        }
    }

    /**
     * Creates a blank DocumentState object.
     * 
     * @return 
     */
    public DocumentState createDocumentState() {
        return new DocumentState(this);
    }

    /**
     * Creates a new document parser. Note that the call to this can
     * come from any thread so the parser itself and what this method
     * does needs to be thread safe.
     * 
     * @return 
     */
    public Parser createDocumentParser() {
        return new XHTMLJavaSAXParser();
    }

    /**
     * Creates a new CSS parser. Note that the call to this can
     * come from any thread so the parser itself and what this method
     * does needs to be thread safe.
     * 
     * @return 
     */
    public CSSParser createCSSParser() {
        CSSErrorHandler cssErrorHandler = new CSSErrorHandler() {
            @Override
            public void error(String uri, String message) {
                // TODO: Put this into an error log,
//                System.err.println("CSS ERROR in '" + uri + "'");
//                System.err.println("    " + message);
            }
        };
        return new CSSParser(cssErrorHandler);
    }

    /**
     * Dispatches a loadURI action and populates the given document state
     * object when the necessary resources are locally available.
     * 
     * @param uri
     * @param state
     */
    void dispatchLoadURI(final String uri, final DocumentState state) {
        // Submit the runnable to dispatch the load URI command,
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    loadURI(uri, state);
                }
                catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        });
    }

    /**
     * Loads a document from the given URI and populates the DocumentState
     * object with the resource information loaded.
     * 
     * @param uri
     * @param state 
     */
    private void loadURI(String uri, final DocumentState state) {

        Parser documentParser = createDocumentParser();
        CSSParser cssParser = createCSSParser();

        UserAgentCallback uac =
                    new AgentResourceLoader(state, cssParser, documentParser);
        FontResolver fr = agentFontResolver;
        ReplacedElementFactory ref = createReplacedElementFactory();

        // NOTE: We should not be exposing a renderer component here. The
        //   design here needs a bit of a refactor.
        //   The TextRenderer should be created during the render cycle. We
        //   need an object here that allows us to query the font context
        //   for text metric calculations.
        TextRenderer tr = new Java2DTextRenderer();
        // PENDING: Make this configurable,
        float dpi = 106f;

        // PENDING; We need to support interactivety in a slightly different
        //   way.
        UserInterface ui = new UserInterface() {
            @Override
            public boolean isHover(Element e) {
                return false;
            }
            @Override
            public boolean isActive(Element e) {
                return false;
            }
            @Override
            public boolean isFocus(Element e) {
                return false;
            }
        };

        // The namespace handler,
        XhtmlNamespaceHandler nsh = new XhtmlNamespaceHandler();

        // Create a shared context object,
        SharedContext ss = new SharedContext(uac, fr, ref, tr, dpi);

        // Load the document,
        DocumentResource docResource = uac.getDocumentResource(uri);
        Document doc = docResource.getDocument();

        // Load the document into the shared context we created,
        ss.reset();
        ss.setBaseURL(uri);
        ss.setNamespaceHandler(nsh);
        ss.getCss().setDocumentContext(ss, ss.getNamespaceHandler(), doc, ui);

        

        // Done,
        state.documentLoaded(uri, ss, doc);

    }

    private ReplacedElementFactory createReplacedElementFactory() {
        return new SwingReplacedElementFactory();
    }

    /**
     * Gets an InputStream for the resource identified. This may load
     * the resource from over a network or fetch it from a local cache.
     *
     * @param uri PARAM
     * @return The stylesheet value
     */
    private InputStream createStreamForURI(String uri) {
        InputStream is = null;
        try {
            is = new URL(uri).openStream();
        }
        catch (java.net.MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        }
        catch (java.io.FileNotFoundException e) {
            XRLog.exception("item at URI " + uri + " not found");
        }
        catch (java.io.IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        }
        return is;
    }

    /**
     * Gets a Reader for the resource identified.
     * 
     * @param uri
     * @return 
     */
    private Reader createReaderForURI(String uri) {
        Reader reader = null;
        try {
            URL url = new URL(uri);
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();

            String contentType = urlConnection.getContentType();

            return inputStreamToReader(
                            uri, contentType, urlConnection.getInputStream());

        }
        catch (java.net.MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        }
        catch (java.io.FileNotFoundException e) {
            XRLog.exception("item at URI " + uri + " not found");
        }
        catch (java.io.IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        }
        return reader;
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
     * Blocks while loading an image. Throws IOException if loading the image
     * failed because the uri is invalid, the file was not found, or the image
     * could not be decoded.
     * 
     * @param uri
     * @return 
     * @throws java.io.IOException
     */
    public BufferedImage loadImage(String uri) throws IOException {
        InputStream is = createStreamForURI(uri);
        if (is == null) {
            throw new IOException("Failed to open stream");
        }
        try {
            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                throw new IOException("ImageIO.read() returned null");
            }
            return img;
        }
        finally {
            is.close();
        }
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
     * Adds the given DocumentState to a list attached to the given URI that's
     * notified of image progression on the given uri.
     * 
     * @param uri
     * @param state 
     */
    private void addStateToNotifyOfImageProgress(String uri, DocumentState state) {
        synchronized (imageCacheLock) {
            List<DocumentState> docStates = statesObservingImageProgress.get(uri);
            if (docStates == null) {
                docStates = new ArrayList();
            }
            for (DocumentState s : docStates) {
                if (s == state) {
                    return;
                }
            }
            docStates.add(state);
        }
    }

    /**
     * Notifies all the document states observing the image of the image
     * progress event.
     * 
     * @param imageResource 
     */
    private void notifyImageProgress(ImageProgressEvent evt, boolean complete) {
        DocumentState[] docStatesArr = null;
        synchronized (imageCacheLock) {
            List<DocumentState> docStates =
                                statesObservingImageProgress.get(evt.getURI());
            if (docStates != null) {
                docStatesArr = docStates.toArray(new DocumentState[docStates.size()]);
            }
            if (complete) {
                statesObservingImageProgress.remove(evt.getURI());
            }
        }
        // Notify all the document states of the image progress,
        if (docStatesArr != null) {
            for (DocumentState dstate : docStatesArr) {
                dstate.imageProgress(evt);
            }
        }
    }

    /**
     * A Runnable that loads the given image resource in the background.
     */
    private class LoadImageRunnable implements Runnable {

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
            }
            catch (FileNotFoundException e) {
                XRLog.exception("Can't read image file; image at URI '" + uri + "' not found");
                imageResource.setImage(ImageResource.NOT_FOUND_IMG);
            }
            catch (IOException e) {
                XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                imageResource.setImage(ImageResource.NOT_FOUND_IMG);
            }
            finally {
                // Notify the document states that the image loading has
                // completed.
                ImageProgressEvent evt =
                                    new LoadedImageProgressEvent(imageResource);
                notifyImageProgress(evt, true);
            }
        }

    }

    /**
     * Loads an image in the background. Generates an image loaded event
     * when completed.
     * 
     * @param ir 
     */
    private void loadImageInBackground(ImageResource ir) {
        // Dispatch the image load operation to the thread pool,
        threadPool.execute(new LoadImageRunnable(ir));
    }

    /**
     * Creates an image resource for the given URI.
     * 
     * @param docState
     * @param uri
     * @return 
     */
    public ImageResource createImageResourceForURI(
                                        DocumentState docState, String uri) {

        synchronized (imageCacheLock) {

            ImageResource ir = (ImageResource) imageCache.get(uri);

            // If the image is loading (deferred) then put the document state
            // into the notification list for this uri,
            if (ir != null && ir == ImageResource.LOADING_IMG) {
                addStateToNotifyOfImageProgress(uri, docState);
            }

            //TODO: check that cached image is still valid
            if (ir == null) {

                // Deferred image loading,
                if (deferredImageLoading) {

                    // Deferred image resource,
                    ir = new ImageResource(uri, ImageResource.LOADING_IMG);

                    // Loads the given image resource in the background,
                    loadImageInBackground(ir);

                }
                // Do not defer image loading,
                else {
                    try {

                        BufferedImage bufferedImage = loadImage(uri);
                        ir = createImageResource(uri, bufferedImage);

                    }
                    catch (FileNotFoundException e) {
                        XRLog.exception("Can't read image file; image at URI '" + uri + "' not found");
                    }
                    catch (IOException e) {
                        XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                    }                
                }

                if (ir == null) {
                    ir = new ImageResource(uri, ImageResource.NOT_FOUND_IMG);
                }
                imageCache.put(uri, ir);

            }

            // Return the image resource,
            return ir;

        }
        
    }

    // -----

    /**
     * The UserAgent used by this object to load resources. Every loading
     * document receives a new instance of this object.
     */
    private class AgentResourceLoader implements UserAgentCallback {

        // The document state
        private final DocumentState docState;
        // The parser for style sheet creation.
        private final CSSParser cssParser;
        // The parser for DOM creation. This is the Java XML parser by
        // default.
        private final Parser documentParser;

        /**
         * The URL set for this user agent.
         */
        private String url;


        /**
         * Creates the user agent with the given parsers.
         * 
         * @param cssParser
         * @param documentParser 
         */
        public AgentResourceLoader(DocumentState docState,
                                CSSParser cssParser, Parser documentParser) {
            this.docState = docState;
            this.cssParser = cssParser;
            this.documentParser = documentParser;
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
        public Stylesheet parseStylesheet(
                        Reader reader, String uri, int origin) throws IOException {
            return cssParser.parseStylesheet(uri, origin, reader);
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
            return documentParser.createDocument(reader);
        }

        @Override
        public ImageResource getImageResource(String uri) {

            ImageResource ir;
            if (ImageUtil.isEmbeddedBase64Image(uri)) {
                BufferedImage image = ImageUtil.loadEmbeddedBase64Image(uri);
                ir = createImageResource(null, image);
            }
            else {
                uri = resolveURI(uri);
                return createImageResourceForURI(docState, uri);
            }
            return ir;

        }

        @Override
        public CSSResource getCSSResource(String uri, int origin) {
            Reader cssReader = createReaderForURI(resolveURI(uri));
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

            }
            catch (IOException ex) {
                XRLog.cssParse(Level.WARNING,
                        "Couldn't parse stylesheet at URI " + uri + ": " + ex.getMessage(), ex);
                ex.printStackTrace();
                // Return an empty style sheet if it failed to load,
                return new CSSResource(uri, new Stylesheet(uri, origin));
            }
            finally {
                try {
                    cssReader.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }

        }

        @Override
        public DocumentResource getDocumentResource(String uri) {

            Reader inputReader = createReaderForURI(resolveURI(uri));
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

            }
            catch (IOException ex) {
                throw new XRRuntimeException("Failed to parse Document.", ex);
            }
            finally {
                try {
                    inputReader.close();
                }
                catch (IOException e) {
                    // swallow
                }
            }

        }

        @Override
        public byte[] getBinaryResource(String uri) {
            InputStream is = createStreamForURI(resolveURI(uri));
            if (is == null) {
                return null;
            }
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
            }
            catch (IOException e) {
                return null;
            }
            finally {
                if (is != null) {
                    try {
                        is.close();
                    }
                    catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

        @Override
        public boolean isVisited(String uri) {
            // PENDING:
            return false;
        }

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
                            String cachedDefaultBaseURL = new File(".").toURI().toURL().toExternalForm();
                            baseURL = cachedDefaultBaseURL;
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

        @Override
        public void setBaseURL(String url) {
            this.url = url;
        }

        @Override
        public String getBaseURL() {
            return url;
        }

    }

}
