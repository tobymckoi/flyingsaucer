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
package org.xhtmlrenderer.render;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.css.CSSPrimitiveValue;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.style.BackgroundPosition;
import org.xhtmlrenderer.css.style.BackgroundSize;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.xhtmlrenderer.css.style.derived.LengthValue;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.Uu;

/**
 * An abstract implementation of an {@link OutputDevice}.  It provides complete
 * implementations for many <code>OutputDevice</code> methods.
 */
public abstract class AbstractOutputDevice implements OutputDevice {

    private FontSpecification _fontSpec;

    protected abstract void drawLine(int x1, int y1, int x2, int y2);
    
    public void drawText(RenderingContext c, InlineText inlineText) {
        InlineLayoutBox iB = inlineText.getParent();
        String text = inlineText.getSubstring();

        if (text != null && text.length() > 0) {
            setColor(iB.getStyle().getColor());
            setFont(iB.getStyle().getFSFont(c));
            setFontSpecification(iB.getStyle().getFontSpecification());
            if (inlineText.getParent().getStyle().isTextJustify()) {
                JustificationInfo info = inlineText.getParent().getLineBox().getJustificationInfo();
                if (info != null) {
                    c.getTextRenderer().drawString(
                            c.getOutputDevice(),
                            text,
                            iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline(),
                            info);
                } else {
                    c.getTextRenderer().drawString(
                            c.getOutputDevice(),
                            text,
                            iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline());
                }
            } else {
                c.getTextRenderer().drawString(
                        c.getOutputDevice(),
                        text,
                        iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline());
            }
        }

        if (c.debugDrawFontMetrics()) {
            drawFontMetrics(c, inlineText);
        }
    }

    private void drawFontMetrics(RenderingContext c, InlineText inlineText) {
        InlineLayoutBox iB = inlineText.getParent();
        String text = inlineText.getSubstring();

        setColor(new FSRGBColor(0xFF, 0x33, 0xFF));

        FSFontMetrics fm = iB.getStyle().getFSFontMetrics(null);
        int width = c.getTextRenderer().getWidth(
                c.getFontContext(),
                iB.getStyle().getFSFont(c), text);
        int x = iB.getAbsX() + inlineText.getX();
        int y = iB.getAbsY() + iB.getBaseline();

        drawLine(x, y, x + width, y);

        y += (int) Math.ceil(fm.getDescent());
        drawLine(x, y, x + width, y);

        y -= (int) Math.ceil(fm.getDescent());
        y -= (int) Math.ceil(fm.getAscent());
        drawLine(x, y, x + width, y);
    }

    public void drawTextDecoration(
            RenderingContext c, InlineLayoutBox iB, TextDecoration decoration) {
        setColor(iB.getStyle().getColor());

        Rectangle edge = iB.getContentAreaEdge(iB.getAbsX(), iB.getAbsY(), c);

        fillRect(edge.x, iB.getAbsY() + decoration.getOffset(),
                    edge.width, decoration.getThickness());
    }

    public void drawTextDecoration(RenderingContext c, LineBox lineBox) {
        setColor(lineBox.getStyle().getColor());
        Box parent = lineBox.getParent();
        List decorations = lineBox.getTextDecorations();
        for (Iterator i = decorations.iterator(); i.hasNext(); ) {
            TextDecoration textDecoration = (TextDecoration)i.next();
            if (parent.getStyle().isIdent(
                    CSSName.FS_TEXT_DECORATION_EXTENT, IdentValue.BLOCK)) {
                fillRect(
                    lineBox.getAbsX(),
                    lineBox.getAbsY() + textDecoration.getOffset(),
                    parent.getAbsX() + parent.getTx() + parent.getContentWidth() - lineBox.getAbsX(),
                    textDecoration.getThickness());
            } else {
                fillRect(
                    lineBox.getAbsX(), lineBox.getAbsY() + textDecoration.getOffset(),
                    lineBox.getContentWidth(),
                    textDecoration.getThickness());
            }
        }
    }

    public void drawDebugOutline(RenderingContext c, Box box, FSColor color) {
        setColor(color);
        Rectangle rect = box.getMarginEdge(box.getAbsX(), box.getAbsY(), c, 0, 0);
        rect.height -= 1;
        rect.width -= 1;
        drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    public void paintCollapsedBorder(
            RenderingContext c, BorderPropertySet border, Rectangle bounds, int side) {
        BorderPainter.paint(bounds, side, border, c, 0, false);
    }

    public void paintBorder(RenderingContext c, Box box) {
        if (! box.getStyle().isVisible()) {
            return;
        }

        Rectangle borderBounds = box.getPaintingBorderEdge(c);

        BorderPainter.paint(borderBounds, box.getBorderSides(), box.getBorder(c), c, 0, true);
    }

    public void paintBorder(RenderingContext c, CalculatedStyle style, Rectangle edge, int sides) {
        BorderPainter.paint(edge, sides, style.getBorder(c), c, 0, true);
    }

    private ImageResource getBackgroundImage(RenderingContext c, CalculatedStyle style) {
        if (! style.isIdent(CSSName.BACKGROUND_IMAGE, IdentValue.NONE)) {
            String uri = style.getStringProperty(CSSName.BACKGROUND_IMAGE);
            try {
                return c.getUac().getImageResource(uri);
            } catch (Exception ex) {
                ex.printStackTrace();
                Uu.p(ex);
            }
        }
        return null;
    }

    public void paintBackground(
            RenderingContext c, CalculatedStyle style,
            Rectangle bounds, Rectangle bgImageContainer, BorderPropertySet border) {
        paintBackground0(c, style, bounds, bgImageContainer, border);
    }

    public void paintBackground(RenderingContext c, Box box) {
        if (! box.getStyle().isVisible()) {
            return;
        }

        Rectangle backgroundBounds = box.getPaintingBorderEdge(c);
        BorderPropertySet border = box.getStyle().getBorder(c);
        paintBackground0(c, box.getStyle(), backgroundBounds, backgroundBounds, border);
    }

    private void paintBackground0(
            RenderingContext c, CalculatedStyle style,
            Rectangle backgroundBounds, Rectangle bgImageContainer,
            BorderPropertySet border) {
        if (!Configuration.isTrue("xr.renderer.draw.backgrounds", true)) {
            return;
        }

        FSColor backgroundColor = style.getBackgroundColor();
        ImageResource backgroundImage = getBackgroundImage(c, style);

        // If the image width or height is zero, then there's nothing to draw.
        // Also prevents infinte loop when trying to tile an image with zero size.
        if (backgroundImage == null) {
            backgroundImage = null;
        }
        else {
            FSImage fsImage = backgroundImage.getImage();
            if (fsImage.getWidth() == 0 || fsImage.getHeight() == 0) {
                backgroundImage = null;
            }
        }

        if ( (backgroundColor == null || backgroundColor == FSRGBColor.TRANSPARENT) &&
                backgroundImage == null) {
            return;
        }
        
        Shape borderBounds = BorderPainter.generateBorderBounds(backgroundBounds, border, false);

        if (backgroundColor != null && backgroundColor != FSRGBColor.TRANSPARENT) {
            setColor(backgroundColor);
            fill(borderBounds);
        }

        if (backgroundImage != null) {

            Shape oldclip = getClip();
            clip(borderBounds);

            FSImage fsImage = backgroundImage.getImage();

            Rectangle localBGImageContainer = bgImageContainer;
            if (style.isFixedBackground()) {
                localBGImageContainer = c.getViewportRectangle();
            }

            int xoff = localBGImageContainer.x;
            int yoff = localBGImageContainer.y;

            if (border != null) {
                xoff += (int)border.left();
                yoff += (int)border.top();
            }

            fsImage = scaleBackgroundImage(c, style,
                                           localBGImageContainer, fsImage);
            if (fsImage.getWidth() < 1) {
                fsImage = fsImage.createScaled(1, fsImage.getHeight());
            }
            if (fsImage.getHeight() < 1) {
                fsImage = fsImage.createScaled(fsImage.getWidth(), 1);
            }

            float imageWidth = fsImage.getWidth();
            float imageHeight = fsImage.getHeight();

            BackgroundPosition position = style.getBackgroundPosition();
            xoff += calcOffset(
                    c, style, position.getHorizontal(), localBGImageContainer.width, imageWidth);
            yoff += calcOffset(
                    c, style, position.getVertical(), localBGImageContainer.height, imageHeight);

            boolean hrepeat = style.isHorizontalBackgroundRepeat();
            boolean vrepeat = style.isVerticalBackgroundRepeat();

            if (! hrepeat && ! vrepeat) {
                Rectangle imageBounds = new Rectangle(xoff, yoff, (int)imageWidth, (int)imageHeight);
                if (imageBounds.intersects(backgroundBounds)) {
                    drawImage(fsImage, xoff, yoff);
                }
            } else if (hrepeat && vrepeat) {
                paintTiles(
                        fsImage,
                        adjustTo(backgroundBounds.x, xoff, (int)imageWidth),
                        adjustTo(backgroundBounds.y, yoff, (int)imageHeight),
                        backgroundBounds.x + backgroundBounds.width,
                        backgroundBounds.y + backgroundBounds.height);
            } else if (hrepeat) {
                xoff = adjustTo(backgroundBounds.x, xoff, (int)imageWidth);
                Rectangle imageBounds = new Rectangle(xoff, yoff, (int)imageWidth, (int)imageHeight);
                if (imageBounds.intersects(backgroundBounds)) {
                    paintHorizontalBand(
                            fsImage,
                            xoff,
                            yoff,
                            backgroundBounds.x + backgroundBounds.width);
                }
            } else if (vrepeat) {
                yoff = adjustTo(backgroundBounds.y, yoff, (int)imageHeight);
                Rectangle imageBounds = new Rectangle(xoff, yoff, (int)imageWidth, (int)imageHeight);
                if (imageBounds.intersects(backgroundBounds)) {
                    paintVerticalBand(
                            fsImage,
                            xoff,
                            yoff,
                            backgroundBounds.y + backgroundBounds.height);
                }
            }

            setClip(oldclip);

        }
    }

    private int adjustTo(int target, int current, int imageDim) {
        int result = current;
        if (result > target) {
            while (result > target) {
                result -= imageDim;
            }
        } else if (result < target) {
            while (result < target) {
                result += imageDim;
            }
            if (result != target) {
                result -= imageDim;
            }
        }
        return result;
    }

    private void paintTiles(FSImage image, int left, int top, int right, int bottom) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = left; x < right; x+= width) {
            for (int y = top; y < bottom; y+= height) {
                drawImage(image, x, y);
            }
        }
    }

    private void paintVerticalBand(FSImage image, int left, int top, int bottom) {
        int height = image.getHeight();

        for (int y = top; y < bottom; y+= height) {
            drawImage(image, left, y);
        }
    }

    private void paintHorizontalBand(FSImage image, int left, int top, int right) {
        int width = image.getWidth();

        for (int x = left; x < right; x+= width) {
            drawImage(image, x, top);
        }
    }

    private int calcOffset(CssContext c, CalculatedStyle style, PropertyValue value, float boundsDim, float imageDim) {
        if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE) {
            float percent = value.getFloatValue() / 100.0f;
            return Math.round(boundsDim*percent - imageDim*percent);
        } else { /* it's a <length> */
            return (int)LengthValue.calcFloatProportionalValue(
                    style,
                    CSSName.BACKGROUND_POSITION,
                    value.getCssText(),
                    value.getFloatValue(),
                    value.getPrimitiveType(),
                    0,
                    c);
        }
    }

    /**
     * Returns a Dimension that is the absolute image size of the given image
     * with the cssWidth and cssHeight size properties applied to it. The
     * CSS properties may be -1 indicating the size is not specified in which
     * case the returned scale is deduced using the process below.
     * <p>
     * In the case no width/height property is provided, the native image is
     * scaled by dots per pixel. In the case where only one dimension is
     * available, the dimension that's not available is scaled by the same
     * factor as the available dimension scale.
     * 
     * @param img
     * @param dotsPerPixel
     * @param cssWidth
     * @param cssHeight
     * @return 
     */
    public static Dimension calculateAbsoluteImageSize(
                FSImage img, float dotsPerPixel, int cssWidth, int cssHeight) {

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

    /**
     * Given an image and width/height property, returns a scaled version of
     * the image. The width and/or height property may be -1 indicating the
     * size is not specified.
     * <p>
     * In the case no width/height property is provided the native image is
     * scaled by dots per pixel. In the case where only one dimension is
     * available, the dimension that's not available is scaled by the same
     * factor as the available dimensions scale.
     * 
     * @param c
     * @param img
     * @param width
     * @param height
     * @return 
     */
    public static FSImage resolveImageScale(
                            CssContext c, FSImage img, int width, int height) {

        Dimension dim = calculateAbsoluteImageSize(
                                    img, c.getDotsPerPixel(), width, height);
        return img.createScaled(dim.width, dim.height);

    }

    private FSImage scaleBackgroundImage(CssContext c, CalculatedStyle style, Rectangle backgroundContainer, FSImage image) {
        BackgroundSize backgroundSize = style.getBackgroundSize();

        if (! backgroundSize.isBothAuto()) {
            if (backgroundSize.isCover() || backgroundSize.isContain()) {
                int testHeight = (int)((double)image.getHeight() * backgroundContainer.width / image.getWidth());
                if (backgroundSize.isContain()) {
                    if (testHeight > backgroundContainer.height) {
                        image = resolveImageScale(c, image, -1, backgroundContainer.height);
                    } else {
                        image = resolveImageScale(c, image, backgroundContainer.width, -1);
                    }
                } else if (backgroundSize.isCover()) {
                    if (testHeight > backgroundContainer.height) {
                        image = resolveImageScale(c, image, backgroundContainer.width, -1);
                    } else {
                        image = resolveImageScale(c, image, -1, backgroundContainer.height);
                    }
                }
            } else {
                int scaledWidth = calcBackgroundSizeLength(c, style, backgroundSize.getWidth(), backgroundContainer.width);
                int scaledHeight = calcBackgroundSizeLength(c, style, backgroundSize.getHeight(), backgroundContainer.height);

                image = resolveImageScale(c, image, scaledWidth, scaledHeight);
            }
        }
        else {
            // Scale by dots per pixel,
            float dpp = c.getDotsPerPixel();
            int bgWidth = image.getWidth();
            int bgHeight = image.getHeight();
            if (bgWidth >= 0 && bgHeight >= 0) {
                image = image.createScaled(
                              (int) (bgWidth * dpp), (int) (bgHeight * dpp));
            }
        }
        return image;
    }

    private int calcBackgroundSizeLength(CssContext c, CalculatedStyle style, PropertyValue value, float boundsDim) {
        if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) { // 'auto'
            return -1;
        } else if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE) {
            float percent = value.getFloatValue() / 100.0f;
            return Math.round(boundsDim*percent);
        } else {
            return (int)LengthValue.calcFloatProportionalValue(
                    style,
                    CSSName.BACKGROUND_SIZE,
                    value.getCssText(),
                    value.getFloatValue(),
                    value.getPrimitiveType(),
                    0,
                    c);
        }
    }

    /**
     * Gets the FontSpecification for this AbstractOutputDevice.
     *
     * @return current FontSpecification.
     */
    public FontSpecification getFontSpecification() {
	return _fontSpec;
    }

    /**
     * Sets the FontSpecification for this AbstractOutputDevice.
     *
     * @param fs current FontSpecification.
     */
    public void setFontSpecification(FontSpecification fs) {
	_fontSpec = fs;
    }
}
