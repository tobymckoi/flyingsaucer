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

package org.xhtmlrenderer.extend;

import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.FSFontMetrics;

/**
 * Interface for accessing information about the text renderer.
 *
 * @author Tobias Downer
 */
public interface TextSystemAccessor {

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

    public float getFontScale();

    public int getSmoothingLevel();

    /**
     * Returns the current Fractional Metrics setting.
     * 
     * @return 
     */
    public Boolean getFractionalMetrics();

    /**
     * Returns the current font kerning default setting.
     * 
     * @return 
     */
    public Boolean getKerning();

    /**
     * Returns the current font ligatures setting.
     * 
     * @return 
     */
    public Boolean getLigatures();

}
