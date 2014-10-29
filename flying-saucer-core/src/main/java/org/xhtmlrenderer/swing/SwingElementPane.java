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

package org.xhtmlrenderer.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.RepaintManager;

/**
 *
 * @author Tobias Downer
 */
public class SwingElementPane extends Container {
  
  public SwingElementPane() {
    super();
    setLayout(null);
    setVisible(true);
  }

  @Override
  public void invalidate() {
  }

  @Override
  public void paint(Graphics g) {
    // This object doesn't do the painting,
  }

  @Override
  public void update(Graphics g) {
  }

  @Override
  public void repaint(long tm, int x, int y, int width, int height) {
    super.repaint(tm, x, y, width, height);
  }

  

  /**
   * Paint the component.
   * 
   * @param g
   * @param c
   * @param bounds
   * @param validate 
   */
  public void paintComponent(Graphics g, Component c, Rectangle bounds, boolean validate) {

    if (c.getParent() != this) {
      this.add(c);
    }

    boolean wasDoubleBuffered = false;
    if ((c instanceof JComponent) && ((JComponent)c).isDoubleBuffered()) {
        wasDoubleBuffered = true;
        ((JComponent)c).setDoubleBuffered(false);
    }

    // If the current repaint manager can be temporarily disabled, then we
    // disable it for the 'setBounds' operation so the 'repaint' action doesn't
    // get propagated.
    RepaintManager rm = RepaintManager.currentManager(null);
    DisableableRepaintManager drm = null;
    if (rm instanceof DisableableRepaintManager) {
      drm = (DisableableRepaintManager) rm;
      drm.setDisableMarkDirtyRegions(true);
    }
    try {
      c.setBounds(bounds);
    }
    finally {
      if (drm != null) drm.setDisableMarkDirtyRegions(false);
    }

    if(validate) {
      c.validate();
    }

    Graphics cg = g.create(bounds.x, bounds.y, bounds.width, bounds.height);
    try {
      c.paint(cg);
    }
    finally {
      cg.dispose();
    }

    if (wasDoubleBuffered && (c instanceof JComponent)) {
      ((JComponent)c).setDoubleBuffered(true);
    }

  }
  
}
