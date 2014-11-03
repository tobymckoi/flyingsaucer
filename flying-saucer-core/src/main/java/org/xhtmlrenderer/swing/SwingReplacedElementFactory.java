/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.DefaultFormSubmissionListener;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.simple.extend.XhtmlForm;
import org.xhtmlrenderer.simple.extend.form.FormField;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.resource.ImageResource;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import org.xhtmlrenderer.layout.BoxLoadInfo;

/**
 * A ReplacedElementFactory where Elements are replaced by Swing components.
 */
public class SwingReplacedElementFactory implements ReplacedElementFactory {

    /**
     * Cache of XhtmlForms keyed by Element.
     */
    protected LinkedHashMap forms;

    private FormSubmissionListener formSubmissionListener;


    public SwingReplacedElementFactory() {
        this.formSubmissionListener = new DefaultFormSubmissionListener();
    }

    /**
     * {@inheritDoc}
     */
    public ReplacedElement createReplacedElement(
            LayoutContext context,
            BlockBox box,
            UserAgentCallback uac,
            int cssWidth,
            int cssHeight
    ) {
        Element e = box.getElement();

        if (e == null) {
            return null;
        }

        if (context.getNamespaceHandler().isImageElement(e)) {

            ImageReplacedElement replacedImage = replaceImage(
                                        uac, context, e, cssWidth, cssHeight);

            // Get the image resource,
            ImageResource ir = replacedImage.getImageResource();

            // If the image is already loaded, we don't need to relayout,
            boolean isLoaded = ir.isLoaded();

            // Register the Box with the image, so when the image loads we
            // know what to repaint.
            boolean unknownDimensions = (cssWidth == -1 || cssHeight == -1);
            // Need to relayout if the image isn't loaded and has unknown
            // dimensions. Otherwise we repaint when the image changes if
            // loaded or known dimensions.
            byte loadOp = !isLoaded && unknownDimensions ?
                                BoxLoadInfo.STATUS_RELAYOUT :
                                BoxLoadInfo.STATUS_REPAINT;
            BoxLoadInfo boxLoadInfo = new BoxLoadInfo(box, loadOp);
            context.registerBoxWithResource(boxLoadInfo, ir.getImageUri());

            return replacedImage;

        } else {
            //form components
            Element parentForm = getParentForm(e, context);
            //parentForm may be null! No problem! Assume action is this document and method is get.
            XhtmlForm form = getForm(parentForm);
            if (form == null) {
                form = new XhtmlForm(uac, parentForm, formSubmissionListener);
                addForm(parentForm, form);
            }

            FormField formField = form.addComponent(e, context, box);
            if (formField == null) {
                return null;
            }

            JComponent cc = formField.getComponent();

            if (cc == null) {
                return new EmptyReplacedElement(0, 0);
            }

            SwingReplacedElement result = new SwingReplacedElement(cc);
            result.setIntrinsicSize(formField.getIntrinsicSize());

            return result;
        }
    }

    /**
     * Handles replacement of image elements in the document. May return the same ReplacedElement for a given image
     * on multiple calls. Image will be automatically scaled to cssWidth and cssHeight assuming these are non-zero
     * positive values. The element is assume to have a src attribute (e.g. it's an <img> element)
     *
     * @param uac       Used to retrieve images on demand from some source.
     * @param context
     * @param elem      The element with the image reference
     * @param cssWidth  Target width of the image
     * @param cssHeight Target height of the image @return A ReplacedElement for the image; will not be null.
     * @return
     */
    protected ImageReplacedElement replaceImage(UserAgentCallback uac, LayoutContext context, Element elem, int cssWidth, int cssHeight) {
        ImageReplacedElement re = null;
        String imageSrc = context.getNamespaceHandler().getImageSourceURI(elem);
        
        if (imageSrc == null || imageSrc.length() == 0) {
            XRLog.layout(Level.WARNING, "No source provided for img element.");
            re = newIrreplaceableImageElement(cssWidth, cssHeight);
        } else if (ImageUtil.isEmbeddedBase64Image(imageSrc)) {
            BufferedImage image = ImageUtil.loadEmbeddedBase64Image(imageSrc);
            if (image != null) {
                re = new ImageReplacedElement(
                          new ImageResource(imageSrc, AWTFSImage.createImage(image))
                          , cssWidth, cssHeight);
            }
        } else {
            // Instantiate,
            String ruri = uac.resolveURI(imageSrc);
            ImageResource imageResource = uac.getImageResource(ruri);
            Dimension dim = imageResource.calculateSizeFromCSS(
                                context.getDotsPerPixel(), cssWidth, cssHeight);
            re = new ImageReplacedElement(imageResource, dim.width, dim.height);

        }
        return re;
    }

    /**
     * Returns a ReplacedElement for some element in the stream which should be replaceable, but is not. This might
     * be the case for an element like img, where the source isn't provided.
     *
     * @param cssWidth  Target width for the element.
     * @param cssHeight Target height for the element
     * @return A ReplacedElement to substitute for one that can't be generated.
     */
    protected ImageReplacedElement newIrreplaceableImageElement(int cssWidth, int cssHeight) {
        BufferedImage missingImage;
        ImageReplacedElement mre;

        // TODO: we can come up with something better; not sure if we should use Alt text, how text should size, etc.
        missingImage = ImageUtil.createCompatibleBufferedImage(cssWidth, cssHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = missingImage.createGraphics();
        g.setColor(Color.BLACK);
        g.setBackground(Color.WHITE);
        g.setFont(new Font("Serif", Font.PLAIN, 12));
        g.drawString("Missing", 0, 12);
        g.dispose();
        ImageResource imageResource =
                new ImageResource("?", AWTFSImage.createImage(missingImage));
        mre = new ImageReplacedElement(imageResource, cssWidth, cssHeight);

        return mre;
    }

    /**
     * Adds a form to a local cache for quick lookup.
     *
     * @param e The element under which the form is keyed (e.g. "<form>" in HTML)
     * @param f The form element being stored.
     */
    protected void addForm(Element e, XhtmlForm f) {
        if (forms == null) {
            forms = new LinkedHashMap();
        }
        forms.put(e, f);
    }

    /**
     * Returns the XhtmlForm associated with an Element in cache, or null if not found.
     *
     * @param e The Element to which the form is keyed
     * @return The form, or null if not found.
     */
    protected XhtmlForm getForm(Element e) {
        if (forms == null) {
            return null;
        }
        return (XhtmlForm) forms.get(e);
    }

    /**
     * @param e
     */
    protected Element getParentForm(Element e, LayoutContext context) {
        Node node = e;

        do {
            node = node.getParentNode();
        } while (node.getNodeType() == Node.ELEMENT_NODE &&
                !context.getNamespaceHandler().isFormElement((Element) node));

        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        return (Element) node;
    }

    /**
     * Clears out any references to elements or items created by this factory so far.
     */
    public void reset() {
        forms = null;
        //imageComponents = null;
    }

    public void remove(Element e) {
        if (forms != null) {
            forms.remove(e);
        }
    }

    public void setFormSubmissionListener(FormSubmissionListener fsl) {
        this.formSubmissionListener = fsl;
    }

    private static class CacheKey {
        final Element elem;
        final String uri;
        final int width;
        final int height;

        public CacheKey(final Element elem, final String uri, final int width, final int height) {
            this.uri = uri;
            this.width = width;
            this.height = height;
            this.elem = elem;
        }

        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;

            final CacheKey cacheKey = (CacheKey) o;

            if (height != cacheKey.height) return false;
            if (width != cacheKey.width) return false;
            if (!elem.equals(cacheKey.elem)) return false;
            if (!uri.equals(cacheKey.uri)) return false;

            return true;
        }

        public int hashCode() {
            int result = elem.hashCode();
            result = 31 * result + uri.hashCode();
            result = 31 * result + width;
            result = 31 * result + height;
            return result;
        }
    }

}
