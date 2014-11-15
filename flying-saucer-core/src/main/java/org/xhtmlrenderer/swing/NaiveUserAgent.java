/*
 * NaiveUserAgent.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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
 *
 */
package org.xhtmlrenderer.swing;

import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.extend.AbstractUserAgent;
import org.xhtmlrenderer.extend.UserAgentCallback;

/**
 * <p>NaiveUserAgent is a simple implementation of {@link UserAgentCallback} which places no restrictions on what
 * XML, CSS or images are loaded, and reports visited links without any filtering. The most straightforward process
 * available in the JDK is used to load the resources in question--either using java.io or java.net classes.
 *
 * <p>The NaiveUserAgent has a small cache for images,
 * the size of which (number of images) can be passed as a constructor argument. There is no automatic cleaning of
 * the cache; call {@link #shrinkImageCache()} to remove the least-accessed elements--for example, you might do this
 * when a new document is about to be loaded. The NaiveUserAgent is also a DocumentListener; if registered with a
 * source of document events (like the panel hierarchy), it will respond to the
 * {@link org.xhtmlrenderer.event.DocumentListener#documentStarted()} call and attempt to shrink its cache.
 *
 * <p>This class is meant as a starting point--it will work out of the box, but you should really implement your
 * own, tuned to your application's needs.
 *
 * @author Torbjoern Gannholm
 */
public class NaiveUserAgent extends AbstractUserAgent implements DocumentListener {

    /**
     * Creates a new instance of NaiveUserAgent with a max image cache of 16 images.
     */
    public NaiveUserAgent() {
        super();
    }

    /**
     * Creates a new NaiveUserAgent with a cache of a specific size.
     *
     * @param imgCacheSize Number of images to hold in cache before LRU images are released.
     */
    public NaiveUserAgent(final int imgCacheSize) {
        super(imgCacheSize);
    }

    /**
     * Creates a new NaiveUserAgent with a cache of a specific size.
     *
     * @param imgCacheSize Number of images to hold in cache before LRU images are released.
     * @param backgroundImageLoadWorkersCount
     */
    public NaiveUserAgent(final int imgCacheSize,
                          final int backgroundImageLoadWorkersCount) {
        super(imgCacheSize, backgroundImageLoadWorkersCount);
    }

    @Override
    public void documentStarted() {
        shrinkImageCache();
    }

    @Override
    public void documentLoaded() { /* ignore*/ }

    @Override
    public void onLayoutException(Throwable t) { /* ignore*/ }

    @Override
    public void onRenderException(Throwable t) { /* ignore*/ }
}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.40  2009/05/15 16:20:10  pdoubleya
 * ImageResource now tracks the URI for the image that was created and handles mutable images.
 *
 * Revision 1.39  2009/04/12 11:16:51  pdoubleya
 * Remove proposed patch for URLs that are incorrectly handled on Windows; need a more reliable solution.
 *
 * Revision 1.38  2008/04/30 23:14:18  peterbrant
 * Do a better job of cleaning up open file streams (patch by Christophe Marchand)
 *
 * Revision 1.37  2007/11/23 07:03:30  pdoubleya
 * Applied patch from N. Barozzi to allow either toolkit or buffered images to be used, see https://xhtmlrenderer.dev.java.net/servlets/ReadMsg?list=dev&msgNo=3847
 *
 * Revision 1.36  2007/10/31 23:14:43  peterbrant
 * Add rudimentary support for @font-face rules
 *
 * Revision 1.35  2007/06/20 12:24:31  pdoubleya
 * Fix bug in shrink cache, trying to modify iterator without using safe remove().
 *
 * Revision 1.34  2007/06/19 21:25:41  pdoubleya
 * Cleanup for caching in NUA, making it more suitable to use as a reusable UAC. NUA is also now a document listener and uses this to try and trim its cache down. PanelManager and iTextUA are now NUA subclasses.
 *
 * Revision 1.33  2007/05/20 23:25:33  peterbrant
 * Various code cleanups (e.g. remove unused imports)
 *
 * Patch from Sean Bright
 *
 * Revision 1.32  2007/05/09 21:52:06  pdoubleya
 * Fix for rendering problems introduced by removing GraphicsUtil class. Use Image instead of BufferedImage in most cases, convert to AWT image if necessary. Not complete, requires cleanup.
 *
 * Revision 1.31  2007/05/05 21:08:27  pdoubleya
 * Changed image-related interfaces (FSImage, ImageUtil, scaling) to all use BufferedImage, since there were no Image-specific APIs we depended on, and we have more control over what we do with BIs as compared to Is.
 *
 * Revision 1.30  2007/05/05 18:05:21  pdoubleya
 * Remove references to GraphicsUtil and the class itself, no longer needed
 *
 * Revision 1.29  2007/04/10 20:46:02  pdoubleya
 * Fix, was not closing XML source stream when done
 *
 * Revision 1.28  2007/02/07 16:33:31  peterbrant
 * Initial commit of rewritten table support and associated refactorings
 *
 * Revision 1.27  2006/06/28 13:46:59  peterbrant
 * ImageIO.read() can apparently return sometimes null instead of throwing an exception when processing an invalid image
 *
 * Revision 1.26  2006/04/27 13:28:48  tobega
 * Handle situations without base url and no file access gracefully
 *
 * Revision 1.25  2006/04/25 00:23:20  peterbrant
 * Fixes from Mike Curtis
 *
 * Revision 1.23  2006/04/08 08:21:24  tobega
 * relative urls and linked stylesheets
 *
 * Revision 1.22  2006/02/02 02:47:33  peterbrant
 * Support non-AWT images
 *
 * Revision 1.21  2005/10/25 19:40:38  tobega
 * Suggestion from user to use File.toURI.toURL instead of File.toURL because the latter is buggy
 *
 * Revision 1.20  2005/10/09 09:40:27  tobega
 * Use current directory as default base URL
 *
 * Revision 1.19  2005/08/11 01:35:37  joshy
 * removed debugging
 * updated stylesheet to use right aligns
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.17  2005/06/25 19:27:47  tobega
 * UAC now supplies Resources
 *
 * Revision 1.16  2005/06/25 17:23:35  tobega
 * first refactoring of UAC: ImageResource
 *
 * Revision 1.15  2005/06/21 17:52:10  joshy
 * new hover code
 * removed some debug statements
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.14  2005/06/20 23:45:56  joshy
 * hack to fix the mangled background images on osx
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.13  2005/06/20 17:26:45  joshy
 * debugging for image issues
 * font scale stuff
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.12  2005/06/15 11:57:18  tobega
 * Making Browser a better model application with UserAgentCallback
 *
 * Revision 1.11  2005/06/15 11:53:47  tobega
 * Changed UserAgentCallback to getInputStream instead of getReader. Fixed up some consequences of previous change.
 *
 * Revision 1.10  2005/06/13 06:50:16  tobega
 * Fixed a bug in table content resolution.
 * Various "tweaks" in other stuff.
 *
 * Revision 1.9  2005/06/03 00:29:49  tobega
 * fixed potential bug
 *
 * Revision 1.8  2005/06/01 21:36:44  tobega
 * Got image scaling working, and did some refactoring along the way
 *
 * Revision 1.7  2005/03/28 14:24:22  pdoubleya
 * Remove stack trace on loading images.
 *
 * Revision 1.6  2005/02/02 12:14:01  pdoubleya
 * Clean, format, buffer reader.
 *
 *
 */
