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

import org.xhtmlrenderer.render.Box;

/**
 * An object that describes how to process a box that contains resources that
 * changes properties of the box over time.
 *
 * @author Tobias Downer
 */
public class BoxLoadInfo {

    /**
     * The box associated.
     */
    private final Box box;

    /**
     * What to do with this box once the resource status changes.
     */
    private final byte statusOperation;

    /**
     * Constructor.
     *
     * @param box
     * @param statusOperation
     */
    public BoxLoadInfo(Box box, byte statusOperation) {
        this.box = box;
        this.statusOperation = statusOperation;
    }

    /**
     * The box associated.
     *
     * @return
     */
    public Box getBox() {
        return box;
    }

    /**
     * Returns the operation to perform when the status of this box changes.
     *
     * @return
     */
    public byte getStatusOperation() {
        return statusOperation;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(box.toString());
        b.append(" - ");
        b.append(statusOperation == STATUS_REPAINT ? "REPAINT" : "RELAYOUT");
        return b.toString();
    }

    public static final byte STATUS_REPAINT  = 1;
    public static final byte STATUS_RELAYOUT = 2;

}
