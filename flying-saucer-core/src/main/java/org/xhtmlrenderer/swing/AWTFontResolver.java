/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.swing;

import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.FontResolver;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.render.FSFont;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * A Font Resolver creates fonts from the given CSS font names and styles.
 *
 * @author Joshua Marinacci
 */
public class AWTFontResolver implements FontResolver {

    // Maps string to font objects,
    private final Map<String, Font> instance_hash = new HashMap();

    // The family names of available fonts.
    private final Set<String> available_fonts_hash = new HashSet();

    // Overridden font names,
    private final Map<String, Font> overriden_fonts_hash = new HashMap();

    // True if fonts should be created with kerning enabled,
    private boolean kerning_enabled = false;
    
    // True if fonts should be created with ligatures enabled,
    private boolean ligatures_enabled = false;

    /**
     * Constructor for the FontResolverTest object
     */
    public AWTFontResolver() {
        init();
    }

    /**
     * Enables kerning for all new font instances created. If the font has
     * kerning enabled by system default, there is currently no way to disable
     * it. Default is false (don't enable kerning).
     */
    public void setKerning(boolean enabled) {
        kerning_enabled = enabled;
    }

    /**
     * Enables ligatures for all new font instances created. If the font has
     * ligatures enabled by system default, there is currently no way to
     * disable it. Default is false (don't enable ligatures).
     */
    public void setLigatures(boolean enabled) {
        ligatures_enabled = enabled;
    }

    private void init() {
        GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available_fonts = gfx.getAvailableFontFamilyNames();

        instance_hash.clear();
        available_fonts_hash.clear();

        // preload the font map with the font names as keys
        // don't add the actual font objects because that would be a waste of memory
        // we will only add them once we need to use them
        // put empty strings in instead
        for (int i = 0; i < available_fonts.length; i++) {
            available_fonts_hash.add(available_fonts[i]);
        }

        // preload sans, serif, and monospace into the available font hash
        available_fonts_hash.add("Serif");
        available_fonts_hash.add("SansSerif");
        available_fonts_hash.add("Monospaced");
    }

    public void flushCache() {
        init();
    }

    /**
     * Resolves a font given the list of font families. The first family found
     * is returned.
     *
     * @param ctx
     * @param families PARAM
     * @param size     PARAM
     * @param weight   PARAM
     * @param style    PARAM
     * @param variant  PARAM
     * @return Returns
     */
    public FSFont resolveFont(SharedContext ctx, String[] families, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        //Uu.p("familes = ");
        //Uu.p(families);
        // for each font family
        if (families != null) {
            for (int i = 0; i < families.length; i++) {
                Font font = resolveFont(ctx, families[i], size, weight, style, variant);
                if (font != null) {
                    return new AWTFSFont(font);
                }
            }
        }

        // if we get here then no font worked, so just return default sans
        //Uu.p("pulling out: -" + available_fonts_hash.get("SansSerif") + "-");
        String family = "SansSerif";
        if (style == IdentValue.ITALIC) {
            family = "Serif";
        }

        Font fnt = createFont(ctx, family, size, weight, style, variant);
        instance_hash.put(getFontInstanceHashName(ctx, family, size, weight, style, variant), fnt);
        //Uu.p("subbing in base sans : " + fnt);
        return new AWTFSFont(fnt);
    }

    /**
     * Creates a font with the given specifications.
     *
     * @param ctx
     * @param fontFamily
     * @param size      PARAM
     * @param weight    PARAM
     * @param style     PARAM
     * @param variant   PARAM
     * @return Returns
     */
    protected Font createFont(SharedContext ctx, String fontFamily, float size, IdentValue weight, IdentValue style, IdentValue variant) {

        Object tattr_weight = TextAttribute.WEIGHT_REGULAR;
        Object tattr_posture = TextAttribute.POSTURE_REGULAR;

        if (weight != null) {
            if (weight == IdentValue.BOLD) {
                tattr_weight = TextAttribute.WEIGHT_BOLD;
            }
            else if (weight == IdentValue.FONT_WEIGHT_100) {
                tattr_weight = TextAttribute.WEIGHT_EXTRA_LIGHT;
            }
            else if (weight == IdentValue.FONT_WEIGHT_200) {
                tattr_weight = TextAttribute.WEIGHT_LIGHT;
            }
            else if (weight == IdentValue.FONT_WEIGHT_300) {
                tattr_weight = TextAttribute.WEIGHT_DEMILIGHT;
            }
            else if (weight == IdentValue.FONT_WEIGHT_400) {
                tattr_weight = TextAttribute.WEIGHT_REGULAR;  // 1f;
            }
            else if (weight == IdentValue.FONT_WEIGHT_500) {
                tattr_weight = TextAttribute.WEIGHT_SEMIBOLD;
            }
            else if (weight == IdentValue.FONT_WEIGHT_600) {
                tattr_weight = TextAttribute.WEIGHT_MEDIUM;
            }
            else if (weight == IdentValue.FONT_WEIGHT_700) {
                tattr_weight = TextAttribute.WEIGHT_BOLD;     // 2f
            }
            else if (weight == IdentValue.FONT_WEIGHT_800) {
                tattr_weight = TextAttribute.WEIGHT_HEAVY;
            }
            else if (weight == IdentValue.FONT_WEIGHT_900) {
                tattr_weight = TextAttribute.WEIGHT_EXTRABOLD;
            }
        }

        if (style != null && (style == IdentValue.ITALIC || style == IdentValue.OBLIQUE)) {
            tattr_posture = TextAttribute.POSTURE_OBLIQUE;
        }

        // scale vs font scale value too
        size *= ctx.getTextRenderer().getFontScale();

        if (variant != null) {
            if (variant == IdentValue.SMALL_CAPS) {
                size *= 0.6f;
            }
        }

        Map<TextAttribute, Object> fontAttrs = new HashMap();
        fontAttrs.put(TextAttribute.FAMILY, fontFamily);
        fontAttrs.put(TextAttribute.SIZE, size);
        fontAttrs.put(TextAttribute.WEIGHT, tattr_weight);
        fontAttrs.put(TextAttribute.POSTURE, tattr_posture);
        if (kerning_enabled) {
            fontAttrs.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        }
        if (ligatures_enabled) {
            fontAttrs.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        }

        Font overrideFont = overriden_fonts_hash.get(fontFamily);
        if (overrideFont != null) {
            return overrideFont.deriveFont(fontAttrs);
        }
        else {
            return Font.getFont(fontAttrs);
        }

    }

    /**
     * Resolves the CSS description of a font to a Font object.
     *
     * @param ctx
     * @param font    PARAM
     * @param size    PARAM
     * @param weight  PARAM
     * @param style   PARAM
     * @param variant PARAM
     * @return Returns
     */
    protected Font resolveFont(SharedContext ctx, String font, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        //Uu.p("here");
        // strip off the "s if they are there
        if (font.startsWith("\"")) {
            font = font.substring(1);
        }
        if (font.endsWith("\"")) {
            font = font.substring(0, font.length() - 1);
        }

        //Uu.p("final font = " + font);
        // normalize the font name
        if (font.equals("serif")) {
            font = "Serif";
        }
        if (font.equals("sans-serif")) {
            font = "SansSerif";
        }
        if (font.equals("monospace")) {
            font = "Monospaced";
        }

        if (font.equals("Serif") && style == IdentValue.OBLIQUE) font = "SansSerif";
        if (font.equals("SansSerif") && style == IdentValue.ITALIC) font = "Serif";

        // assemble a font instance hash name
        String font_instance_name = getFontInstanceHashName(ctx, font, size, weight, style, variant);
        //Uu.p("looking for font: " + font_instance_name);
        // check if the font instance exists in the hash table
        if (instance_hash.containsKey(font_instance_name)) {
            // if so then return it
            return (Font) instance_hash.get(font_instance_name);
        }

        //Uu.p("font lookup failed for: " + font_instance_name);
        //Uu.p("searching for : " + font + " " + size + " " + weight + " " + style + " " + variant);


        // if not then
        //  does the font exist
        if (!available_fonts_hash.contains(font)) {
            return null;
        }

        // now that we have a root font, we need to create the correct version of it
        Font fnt = createFont(ctx, font, size, weight, style, variant);

        // add the font to the hash so we don't have to do this again
        instance_hash.put(font_instance_name, fnt);
        return fnt;

    }

    /**
     * This allows the user to replace one font family with another.
     */
    public void setFontMapping(final String name, final Font font) {
        overriden_fonts_hash.put(name, font);
        available_fonts_hash.add(name);
    }

    /**
     * Gets the fontInstanceHashName attribute of the FontResolverTest object
     *
     * @param ctx
     * @param name    PARAM
     * @param size    PARAM
     * @param weight  PARAM
     * @param style   PARAM
     * @param variant PARAM @return The fontInstanceHashName value
     * @return 
     */
    protected static String getFontInstanceHashName(SharedContext ctx, String name, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        return name + "-" + (size * ctx.getTextRenderer().getFontScale()) + "-" + weight + "-" + style + "-" + variant;
    }

    @Override
    public FSFont resolveFont(SharedContext renderingContext, FontSpecification spec) {
        return resolveFont(renderingContext, spec.families, spec.size, spec.fontWeight, spec.fontStyle, spec.variant);
    }
}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.3  2009/05/09 14:44:18  pdoubleya
 * FindBugs: redundant call to someObject.toString() and new String("")
 *
 * Revision 1.2  2009/05/08 12:22:26  pdoubleya
 * Merge Vianney's SWT branch to trunk. Passes regress.verify and browser still works :).
 *
 * Revision 1.1.4.2  2008/05/28 09:21:15  vianney
 * updated SWT port to latest HEAD
 *
 * Revision 1.3  2008/01/22 21:25:40  pdoubleya
 * Fix: fonts not being keyed properly in font cache when a scaling factor was applied to the text renderer; scaled font size now used as part of the key.
 *
 * Revision 1.2  2007/02/07 16:33:29  peterbrant
 * Initial commit of rewritten table support and associated refactorings
 *
 * Revision 1.1  2006/02/01 01:30:15  peterbrant
 * Initial commit of PDF work
 *
 * Revision 1.2  2005/10/27 00:08:51  tobega
 * Sorted out Context into RenderingContext and LayoutContext
 *
 * Revision 1.1  2005/06/22 23:48:40  tobega
 * Refactored the css package to allow a clean separation from the core.
 *
 * Revision 1.17  2005/06/16 07:24:48  tobega
 * Fixed background image bug.
 * Caching images in browser.
 * Enhanced LinkListener.
 * Some house-cleaning, playing with Idea's code inspection utility.
 *
 * Revision 1.16  2005/06/03 22:04:10  tobega
 * Now handles oblique fonts somewhat and does a better job of italic
 *
 * Revision 1.15  2005/05/29 16:38:59  tobega
 * Handling of ex values should now be working well. Handling of em values improved. Is it correct?
 * Also started defining dividing responsibilities between Context and RenderingContext.
 *
 * Revision 1.14  2005/03/24 23:19:11  pdoubleya
 * Cleaned up DPI calculations for font size (Kevin).
 *
 * Revision 1.13  2005/02/02 11:32:29  pdoubleya
 * Fixed error in font-weight; now checks for 700, 800, 900 or BOLD.
 *
 * Revision 1.12  2005/01/29 20:21:10  pdoubleya
 * Clean/reformat code. Removed commented blocks, checked copyright.
 *
 * Revision 1.11  2005/01/24 22:46:45  pdoubleya
 * Added support for ident-checks using IdentValue instead of string comparisons.
 *
 * Revision 1.10  2005/01/05 01:10:13  tobega
 * Went wild with code analysis tool. removed unused stuff. Lucky we have CVS...
 *
 * Revision 1.9  2004/12/29 10:39:26  tobega
 * Separated current state Context into LayoutContext and the rest into SharedContext.
 *
 * Revision 1.8  2004/12/12 03:32:55  tobega
 * Renamed x and u to avoid confusing IDE. But that got cvs in a twist. See if this does it
 *
 * Revision 1.7  2004/12/12 02:56:59  tobega
 * Making progress
 *
 * Revision 1.6  2004/11/18 02:58:06  joshy
 * collapsed the font resolver and font resolver test into one class, and removed
 * the other
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.5  2004/11/12 02:23:56  joshy
 * added new APIs for rendering context, xhtmlpanel, and graphics2drenderer.
 * initial support for font mapping additions
 *
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.4  2004/11/08 21:18:20  joshy
 * preliminary small-caps implementation
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.3  2004/10/23 13:03:45  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards except for common packages (java.io, java.util, etc)
 * Added CVS log comments at bottom.
 *
 *
 */

