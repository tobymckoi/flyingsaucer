/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 * Copyright (c) 2006 Wisconsin Court System
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
package org.xhtmlrenderer.extend;

import java.awt.Rectangle;

import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.FSFontMetrics;
import org.xhtmlrenderer.render.JustificationInfo;

public interface TextRenderer {
    public void setup(FontContext context);

    public void drawString(OutputDevice outputDevice, String string, float x, float y);
    public void drawString(
            OutputDevice outputDevice, String string, float x, float y, JustificationInfo info);
    
    public void drawGlyphVector(OutputDevice outputDevice, FSGlyphVector vector, float x, float y);
    
    public FSGlyphVector getGlyphVector(OutputDevice outputDevice, FSFont font, String string);
    
    public float[] getGlyphPositions(OutputDevice outputDevice, FSFont font, FSGlyphVector fsGlyphVector);
    public Rectangle getGlyphBounds(OutputDevice outputDevice, FSFont font, FSGlyphVector fsGlyphVector, int index, float x, float y);

    public FSFontMetrics getFSFontMetrics(
            FontContext context, FSFont font, String string );

    public int getWidth(FontContext context, FSFont font, String string);

    /**
     * Returns the logical width of the string when rendered using the given
     * font. When the logical width is added to the y position of the text, you
     * get the position to place text immediately proceeding this string.
     * 
     * @param fontContext
     * @param font
     * @param string
     * @return 
     */
    public float getLogicalGlyphsWidth(FontContext fontContext, FSFont font, String string);

    public void setFontScale(float scale);

    public float getFontScale();

    /**
     * Set the smoothing threashold. This is a font size above which
     * all text will be anti-aliased. Text below this size will not be antialiased. 
     * Set to -1 for no antialiasing. 
     * Set to 0 for all antialising.
     * Else, set to the threshold font size. does not take font scaling
     * into account.
     */
    public void setSmoothingThreshold(float fontsize);

    public int getSmoothingLevel();

    /**
     * @deprecated no-op, will be removed in a future release. Anti-aliasing is now controlled via the smoothing
     * threshhold.
     * @param level no-op
     */
    public void setSmoothingLevel(int level);

    /**
     * Sets whether Fractional Metrics is enabled or not. If this is given
     * 'null' (default) then the configuration default is used.
     * 
     * @param enabled
     */
    public void setFractionalMetrics(Boolean enabled);

    /**
     * Returns the current Fractional Metrics setting.
     * 
     * @return 
     */
    public Boolean getFractionalMetrics();

    /**
     * Sets whether font kerning is enabled by default. If this is given 'null'
     * (default) then the system default is used. Note that font kerning may
     * be enabled by CSS or by system default, in which case this value is
     * ignored.
     * 
     * @param enabled
     */
    public void setKerning(Boolean enabled);

    /**
     * Returns the current font kerning default setting.
     * 
     * @return 
     */
    public Boolean getKerning();

    /**
     * Sets whether font ligatures are enabled by default. If this is given
     * 'null' (default) then the system default is used. Note that font
     * ligatures may be enabled by CSS or by system default, in which case
     * this value is ignored.
     * 
     * @param enabled
     */
    public void setLigatures(Boolean enabled);

    /**
     * Returns the current font ligatures setting.
     * 
     * @return 
     */
    public Boolean getLigatures();

}

