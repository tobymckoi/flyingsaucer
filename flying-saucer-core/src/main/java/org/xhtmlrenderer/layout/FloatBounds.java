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

package org.xhtmlrenderer.layout;

import java.awt.Rectangle;

/**
 * An object that represents the margin edge of an area used with the float
 * manager for calculating layout around floated boxes.
 *
 * @author Tobias Downer
 */
public class FloatBounds {

    /**
     * The margin edge of the box.
     */
    private final Rectangle marginEdge;

    /**
     * Constructor.
     * 
     * @param marginEdge
     */
    public FloatBounds(Rectangle marginEdge) {
        this.marginEdge = marginEdge;
    }

    /**
     * The outer margin edge of the box element.
     * 
     * @return 
     */
    public Rectangle getMarginEdge() {
        return marginEdge;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getMarginEdge());
        return b.toString();
    }

}
