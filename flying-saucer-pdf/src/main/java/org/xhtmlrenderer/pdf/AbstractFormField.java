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
package org.xhtmlrenderer.pdf;

import java.awt.Point;

import org.xhtmlrenderer.css.parser.FSCMYKColor;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.dom.Element;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.util.*;

import com.lowagie.text.pdf.PdfTemplate;

public abstract class AbstractFormField implements ITextReplacedElement {
    protected static final String DEFAULT_CHECKED_STATE = "Yes";
    protected static final String OFF_STATE = "Off"; // required per the spec

    // By default, the field will be font-size * this value
    private static final float FONT_SIZE_ADJUSTMENT = 0.80f;

    private int _x;
    private int _y;
    private int _width;
    private int _height;

    private String _fieldName;

    protected abstract String getFieldType();

    protected int getX() {
        return _x;
    }

    protected void setX(int x) {
        _x = x;
    }

    protected int getY() {
        return _y;
    }

    protected void setY(int y) {
        _y = y;
    }

    protected int getWidth() {
        return _width;
    }

    protected void setWidth(int width) {
        _width = width;
    }

    protected int getHeight() {
        return _height;
    }

    protected void setHeight(int height) {
        _height = height;
    }

    protected String getFieldName(ITextOutputDevice outputDevice, Element e) {
        if (_fieldName == null) {
            String result = e.getAttribute("name");

            if (Util.isNullOrEmpty(result)) {
                _fieldName = getFieldType()
                        + outputDevice.getNextFormFieldIndex();
            } else {
                _fieldName = result;
            }
        }

        return _fieldName;
    }

    protected String getValue(Element e) {
        String result = e.getAttribute("value");

        if (Util.isNullOrEmpty(result)) {
            return DEFAULT_CHECKED_STATE;
        } else {
            return result;
        }
    }

    protected boolean isChecked(Element e) {
        return !Util.isNullOrEmpty(e.getAttribute("checked"));
    }

    protected boolean isReadOnly(Element e) {
        return !Util.isNullOrEmpty(e.getAttribute("readonly"));
    }
    
    protected boolean isSelected(Element e) {
        return Util.isNullOrEmpty(e.getAttribute("selected"));
    }

    public void detach(LayoutContext c) {
    }

    public int getIntrinsicHeight() {
        return getHeight();
    }

    public int getIntrinsicWidth() {
        return getWidth();
    }

    public Point getLocation() {
        return new Point(getX(), getY());
    }

    public boolean isRequiresInteractivePaint() {
        // N/A
        return false;
    }

    public void setLocation(int x, int y) {
        setX(x);
        setY(y);
    }

    protected void initDimensions(LayoutContext c, BlockBox box, int cssWidth,
            int cssHeight) {
        if (cssWidth != -1) {
            setWidth(cssWidth);
        } else {
            if (cssHeight != -1) {
                setWidth(cssHeight);
            } else {
                setWidth((int) (box.getStyle().getFont(c).getSize() * FONT_SIZE_ADJUSTMENT));
            }
        }

        if (cssHeight != -1) {
            setHeight(cssHeight);
        } else {
            if (cssWidth != -1) {
                setHeight(cssWidth);
            } else {
                setHeight((int) (box.getStyle().getFont(c).getSize() * FONT_SIZE_ADJUSTMENT));
            }
        }
    }

    protected String spaces(int count) {
        StringBuffer result = new StringBuffer(count);
        for (int i = 0; i < count; i++) {
            result.append(' ');
        }
        return result.toString();
    }
    
    protected void setStrokeColor(PdfTemplate template, FSColor color)
    {
        if (color instanceof FSRGBColor)
        {
            FSRGBColor rgb = (FSRGBColor)color;
            template.setRGBColorStroke(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
        }
        else if (color instanceof FSCMYKColor)
        {
            FSCMYKColor cmyk = (FSCMYKColor)color;
            template.setCMYKColorStroke(
                    (int)(cmyk.getCyan()*255), (int)(cmyk.getMagenta()*255), 
                    (int)(cmyk.getYellow()*255), (int)(cmyk.getBlack()*255));
        }
    }
    
    protected void setFillColor(PdfTemplate template, FSColor color)
    {
        if (color instanceof FSRGBColor)
        {
            FSRGBColor rgb = (FSRGBColor)color;
            template.setRGBColorFill(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
        }
        else if (color instanceof FSCMYKColor)
        {
            FSCMYKColor cmyk = (FSCMYKColor)color;
            template.setCMYKColorFill(
                    (int)(cmyk.getCyan()*255), (int)(cmyk.getMagenta()*255), 
                    (int)(cmyk.getYellow()*255), (int)(cmyk.getBlack()*255));
        }
    }
}
