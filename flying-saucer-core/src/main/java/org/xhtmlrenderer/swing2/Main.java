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

package org.xhtmlrenderer.swing2;

import java.awt.*;
import javax.swing.*;

/**
 * 
 *
 * @author Tobias Downer
 */
public class Main {

    public static void runMain() {

        JFrame frame = new JFrame("Testing Panel");
        frame.setPreferredSize(new Dimension(1024, 768));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Container c = frame.getContentPane();
        c.setLayout(new BorderLayout());

        DocumentPanel docPanel = new DocumentPanel();
        JScrollPane scrollyDocPanel = new JScrollPane(docPanel);
        c.add(scrollyDocPanel);

        frame.pack();
        frame.setVisible(true);

        // -----

        Agent agent = new Agent();
        agent.init();

        DocumentState state = agent.createDocumentState();
        state.loadURI("http://en.wikipedia.org/wiki/Nausicaa");

        docPanel.setDocumentState(state);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                runMain();
            }
        });
    }

}
