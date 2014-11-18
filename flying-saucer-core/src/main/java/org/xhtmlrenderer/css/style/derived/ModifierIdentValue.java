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

package org.xhtmlrenderer.css.style.derived;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.DerivedValue;

/**
 * A ModifierIdentValue is basically a delegated IdentValue object where this
 * object references the CSSName and the selector Style where this value was
 * defined. This allows us to implement identifiers such as the font-size
 * 'smaller' and 'larger' identifiers that modify the font size of the parent.
 *
 * @author Tobias Downer
 */
public class ModifierIdentValue extends DerivedValue {

    private final CalculatedStyle _style;
    private final IdentValue _identValue;

    public ModifierIdentValue(CalculatedStyle style, CSSName name, PropertyValue value) {
        super(name, value.getPrimitiveType(), value.getCssText(), value.getCssText());

        _style = style;
        _identValue = DerivedValueFactory.getIdentValue(value);
    }

    @Override
    public IdentValue asIdentValue() {
        return _identValue;
    }

    @Override
    public boolean isIdent() {
        // NOTE: NOT USED.
        return true;
    }

    @Override
    public String asString() {
        return _identValue.toString();
    }

    @Override
    public boolean isDependentOnFontSize() {
        // NOTE: NOT USED.
        return false;
    }





    public CalculatedStyle getStyle() {
        return _style;
    }

}
