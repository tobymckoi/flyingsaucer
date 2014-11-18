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
package org.xhtmlrenderer.css.style;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.css.CSSPrimitiveValue;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.style.derived.ModifierIdentValue;

public class FontSizeHelper {
    private static final LinkedHashMap PROPORTIONAL_FONT_SIZES = new LinkedHashMap();
    private static final LinkedHashMap FIXED_FONT_SIZES = new LinkedHashMap();
    
    private static final PropertyValue DEFAULT_SMALLER = new PropertyValue(CSSPrimitiveValue.CSS_EMS, 0.8f, "0.8em");
    private static final PropertyValue DEFAULT_LARGER = new PropertyValue(CSSPrimitiveValue.CSS_EMS, 1.2f, "1.2em");
    
    static {
        // XXX Should come from (or be influenced by) the UA.  These sizes
        // correspond to the Firefox defaults
        PROPORTIONAL_FONT_SIZES.put(IdentValue.XX_SMALL, new PropertyValue(CSSPrimitiveValue.CSS_PX, 9f, "9px"));
        PROPORTIONAL_FONT_SIZES.put(IdentValue.X_SMALL, new PropertyValue(CSSPrimitiveValue.CSS_PX, 10f, "10px"));
        PROPORTIONAL_FONT_SIZES.put(IdentValue.SMALL, new PropertyValue(CSSPrimitiveValue.CSS_PX, 13f, "13px"));
        PROPORTIONAL_FONT_SIZES.put(IdentValue.MEDIUM, new PropertyValue(CSSPrimitiveValue.CSS_PX, 16f, "16px"));
        PROPORTIONAL_FONT_SIZES.put(IdentValue.LARGE, new PropertyValue(CSSPrimitiveValue.CSS_PX, 18f, "18px"));
        PROPORTIONAL_FONT_SIZES.put(IdentValue.X_LARGE, new PropertyValue(CSSPrimitiveValue.CSS_PX, 24f, "24px"));
        PROPORTIONAL_FONT_SIZES.put(IdentValue.XX_LARGE, new PropertyValue(CSSPrimitiveValue.CSS_PX, 32f, "32px"));
        
        FIXED_FONT_SIZES.put(IdentValue.XX_SMALL, new PropertyValue(CSSPrimitiveValue.CSS_PX, 9f, "9px"));
        FIXED_FONT_SIZES.put(IdentValue.X_SMALL, new PropertyValue(CSSPrimitiveValue.CSS_PX, 10f, "10px"));
        FIXED_FONT_SIZES.put(IdentValue.SMALL, new PropertyValue(CSSPrimitiveValue.CSS_PX, 12f, "12px"));
        FIXED_FONT_SIZES.put(IdentValue.MEDIUM, new PropertyValue(CSSPrimitiveValue.CSS_PX, 13f, "13px"));
        FIXED_FONT_SIZES.put(IdentValue.LARGE, new PropertyValue(CSSPrimitiveValue.CSS_PX, 16f, "16px"));
        FIXED_FONT_SIZES.put(IdentValue.X_LARGE, new PropertyValue(CSSPrimitiveValue.CSS_PX, 20f, "20px"));
        FIXED_FONT_SIZES.put(IdentValue.XX_LARGE, new PropertyValue(CSSPrimitiveValue.CSS_PX, 26f, "26px"));        
    }
    
    public static IdentValue getNextSmaller(IdentValue absFontSize, boolean monospace) {
        Map fontSizeMap = monospace ? FIXED_FONT_SIZES : PROPORTIONAL_FONT_SIZES;
        IdentValue prev = null;
        for (Iterator i = fontSizeMap.keySet().iterator(); i.hasNext(); ) {
            IdentValue ident = (IdentValue)i.next();
            if (ident == absFontSize) {
                return prev;
            }
            prev = ident;
        }
        return null;
    }
    
    public static IdentValue getNextLarger(IdentValue absFontSize, boolean monospace) {
        Map fontSizeMap = monospace ? FIXED_FONT_SIZES : PROPORTIONAL_FONT_SIZES;
        for (Iterator i = fontSizeMap.keySet().iterator(); i.hasNext(); ) {
            IdentValue ident = (IdentValue)i.next();
            if (ident == absFontSize && i.hasNext()) {
                return (IdentValue)i.next();
            }
        }
        return null;
    }
    
    public static PropertyValue resolveAbsoluteFontSize(IdentValue fontSize, boolean monospace) {
        if (monospace) {
            return (PropertyValue)FIXED_FONT_SIZES.get(fontSize);
        } else {
            return (PropertyValue)PROPORTIONAL_FONT_SIZES.get(fontSize);
        }
    }
    
    public static PropertyValue getDefaultRelativeFontSize(IdentValue fontSize) {
        if (fontSize == IdentValue.LARGER) {
            return DEFAULT_LARGER;
        } else if (fontSize == IdentValue.SMALLER) {
            return DEFAULT_SMALLER;
        } else {
            return null;
        }
    }
    
    public static boolean isMonospace(String[] fontFamilies) {
        for (int i = 0; i < fontFamilies.length; i++) {
            if (fontFamilies[i].equals("monospace")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the PX size of the given fixed font identifier.
     * 
     * @param fontSize
     * @param monospace
     * @return 
     */
    public static float getFixedFontPXSize(IdentValue fontSize, boolean monospace) {
        PropertyValue pvalue = resolveAbsoluteFontSize(fontSize, monospace);
        return pvalue.getFloatValue();
    }

    /**
     * Resolves a 'larger' or 'smaller' font size modifier and returns the
     * absolute size of the font. This works by walking back through the parent
     * style hierarchy until a none modifier font-size is found. This is used
     * as a base font on which the modifier chain is applied.
     * <p>
     * If the base font-size is an identity (eg. 'x-small', 'large', etc) then
     * the modifiers adjust the resulting font size based on the identity map.
     * Otherwise, it falls back to adjusting the base font size by x1.2 and
     * x0.8 for 'larger' and 'smaller' respectively.
     * 
     * @param ctx
     * @param modifierValue
     * @param isMonospaceFont
     * @return 
     */
    public static float resolveFontSizeModifier(CssContext ctx,
                ModifierIdentValue modifierValue, boolean isMonospaceFont) {

        // Either 'larger' or 'smaller',
        List<IdentValue> modifiersList = new ArrayList(4);
        modifiersList.add(modifierValue.asIdentValue());

        CalculatedStyle parentStyle = modifierValue.getStyle().getParent();

        CalculatedStyle baseStyle = null;
        FSDerivedValue baseFontSize = null;
        
        while (parentStyle != null) {
            FSDerivedValue parentFontSize = parentStyle.valueByName(CSSName.FONT_SIZE);
            if (parentFontSize == null) {
                break;
            }
            if (!(parentFontSize instanceof ModifierIdentValue)) {
                baseFontSize = parentFontSize;
                baseStyle = parentStyle;
                break;
            }
            ModifierIdentValue parentModifier = (ModifierIdentValue) parentFontSize;
            modifiersList.add(parentModifier.asIdentValue());

            // Go back to parent,
            parentStyle = parentModifier.getStyle().getParent();
        }

        // So now 'modifiersList' contains the list of modifiers to apply to
        // a base size.

        if (baseStyle != null && baseFontSize instanceof IdentValue) {

            IdentValue psize = (IdentValue) baseFontSize;
            
            // Apply modifiers,
            Iterator<IdentValue> modifiersIterator = modifiersList.iterator();
            while (psize != null && modifiersIterator.hasNext()) {
                IdentValue mod = modifiersIterator.next();
                if (mod == IdentValue.LARGER) {
                    psize = FontSizeHelper.getNextLarger(psize, isMonospaceFont);
                }
                else if (mod == IdentValue.SMALLER) {
                    psize = FontSizeHelper.getNextSmaller(psize, isMonospaceFont);
                }
            }

            if (psize != null) {
                float sizePx = FontSizeHelper.getFixedFontPXSize(psize, isMonospaceFont);
                return sizePx * ctx.getDotsPerPixel();
            }

        }
        
        // If no base size or base size is not ident value,
        Iterator<IdentValue> modifiersIterator = modifiersList.iterator();

        // We set base size to a default value if necessary,
        float sizePx;
        if (baseStyle != null && baseFontSize instanceof IdentValue) {
            sizePx = FontSizeHelper.getFixedFontPXSize(
                    (IdentValue) baseFontSize, isMonospaceFont) * ctx.getDotsPerPixel();
        }
        else if (baseStyle == null) {
            IdentValue iv = modifiersIterator.next();
            sizePx = (iv == IdentValue.LARGER) ?
                                ctx.getDotsPerPixel() * 18f :
                                ctx.getDotsPerPixel() * 13f;
        }
        else {
            // So resolve to a font size,
            sizePx = baseStyle.getFloatPropertyProportionalTo(
                                                CSSName.FONT_SIZE, 0, ctx);
        }
        // Apply modifiers,
        while (modifiersIterator.hasNext()) {
            IdentValue mod = modifiersIterator.next();
            if (mod == IdentValue.LARGER) {
                sizePx = sizePx * 1.2f;
            }
            else if (mod == IdentValue.SMALLER) {
                sizePx = sizePx * 0.8f;
            }
        }

        return sizePx;
        
    }

}
