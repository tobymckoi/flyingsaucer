/*
 * {{{ header & license
 * Copyright (c) 2007 Vianney le Clément
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
package org.xhtmlrenderer.extend;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

public interface FSCanvas {
    Rectangle getFixedRectangle();

    int getX();

    int getY();

    /**
     * Paints the given AWT/Swing component at the given bounds on the graphics
     * object. This may also register the component onto the pane. If 'validate'
     * is true then the component is also validated.
     * 
     * @param g
     * @param c
     * @param bounds
     * @param validate 
     */
    void paintSwingReplacedComponent(Graphics g, Component c, Rectangle bounds, boolean validate);

}
