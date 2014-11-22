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
package org.xhtmlrenderer.layout;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.render.RenderingContext;
import org.xhtmlrenderer.util.XRRuntimeException;

public class BoxRangeHelper {

    private final LinkedList<BoxRangeData> _clipRegionStack = new LinkedList();
    
    private final OutputDevice _outputDevice;
    private final List<BoxRangeData> _rangeList;
    
    private int _rangeIndex = 0;
    private BoxRangeData _current = null;
    
    public BoxRangeHelper(OutputDevice outputDevice, List<BoxRangeData> rangeList) {
        _outputDevice = outputDevice;
        _rangeList = rangeList;
        
        if (rangeList.size() > 0) {
            _current = rangeList.get(0);
        }
    }
    
    public void checkFinished() {
        if (_clipRegionStack.size() != 0) {
            throw new XRRuntimeException("internal error");
        }
    }
    
    public void pushClipRegion(RenderingContext c, int contentIndex) {
        Shape preClip = (Shape) _outputDevice.getClip();
        List<Rectangle> clips = new ArrayList(4);
        while (_current != null && _current.getRange().getStart() == contentIndex) {

            Rectangle clipEdge = _current.getBox().getChildrenClipEdge(c);

            _current.setClip(preClip);
            _clipRegionStack.add(_current);

            clips.add(clipEdge);

            if (_rangeIndex == _rangeList.size() - 1) {
                _current = null;
            } else {
                _current = _rangeList.get(++_rangeIndex);
            }
        }
        
        // Clip against a union of the clip edges found,
        if (!clips.isEmpty()) {
            Iterator<Rectangle> iterator = clips.iterator();
            Rectangle first = iterator.next();
            if (!iterator.hasNext()) {
                // This is the most common case, just a single clip.
                _outputDevice.clip(first);
            }
            else {
                // Otherwise we create a union of all the clips in this range
                // and clip against that rectangle.
                Rectangle aggregateClip = new Rectangle(first);
                while (iterator.hasNext()) {
                    aggregateClip = aggregateClip.union(iterator.next());
                }
                _outputDevice.clip(aggregateClip);
            }
        }

    }
    
    public void popClipRegions(RenderingContext c, int contentIndex) {
        if (_clipRegionStack.isEmpty()) {
            return;
        }
        // Make sure to pop all the regions we pushed for the given content
        // index.
        BoxRangeData data = _clipRegionStack.getLast();
        if (data.getRange().getEnd() == contentIndex) {
            int contentStart = data.getRange().getStart();
            
            while (_clipRegionStack.size() > 0) {
                data = _clipRegionStack.getLast();
                if (data.getRange().getStart() == contentStart) {
                    _outputDevice.setClip(data.getClip());
                    _clipRegionStack.removeLast();
                } else {
                    break;
                }
            }
        }
    }
}

