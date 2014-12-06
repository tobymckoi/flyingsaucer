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


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

/**
 * 
 *
 * @author Tobias Downer
 */
public class Main {

    private static class TestBrowser extends JPanel {

        private final Agent agent;
        private final DocumentState state;
        private DocumentPanel docPanel;
        private JTextField urlField;

        private final List<String> history = new ArrayList();
        private int historyIndex = 0;

        public TestBrowser(Agent agent) {
            this.agent = agent;
            this.state = this.agent.createDocumentState();
        }

        public DocumentPanel getDocumentPanel() {
            return docPanel;
        }

        public static void checkEventDispatchThread() {
            if (!SwingUtilities.isEventDispatchThread()) {
               throw new AssertionError("Not dispatcher thread");
            }
        }

        public void init() {

            setLayout(new BorderLayout());

            docPanel = new DocumentPanel();
            docPanel.setDocumentState(state);
            JScrollPane scrollyDocPanel = new JScrollPane(docPanel);

            JLabel urlLabel = new JLabel("URL: ");
            urlField = new JTextField(120);

            // Scale up the interface font size,
            Font labelFont = urlLabel.getFont();
            urlLabel.setFont(labelFont.deriveFont(labelFont.getSize2D() * 1.3f));
            Font textFont = urlField.getFont();
            urlField.setFont(textFont.deriveFont(textFont.getSize2D() * 1.3f));
            urlField.addActionListener(urlActionHandler);

            JButton backButton = new JButton("<");
            JButton forwardButton = new JButton(">");
            JButton reloadButton = new JButton("R");

            JPanel buttonsBar = new JPanel();
            buttonsBar.setLayout(new GridLayout(1, 3, 4, 0));
            buttonsBar.setBorder(new EmptyBorder(4, 8, 4, 0));
            buttonsBar.add(backButton);
            buttonsBar.add(forwardButton);
            buttonsBar.add(reloadButton);

            JPanel controlBar = new JPanel();
            controlBar.setBorder(new EmptyBorder(4, 8, 4, 8));
            controlBar.setLayout(new BorderLayout());
            controlBar.add(urlLabel, BorderLayout.WEST);
            controlBar.add(urlField, BorderLayout.CENTER);

            JPanel topBar = new JPanel();
            topBar.setLayout(new BorderLayout());
            topBar.add(buttonsBar, BorderLayout.WEST);
            topBar.add(controlBar, BorderLayout.CENTER);

            add(scrollyDocPanel, BorderLayout.CENTER);
            add(topBar, BorderLayout.NORTH);

            // Add listener that listens for LinkClickEvents and change the
            // page accordingly.
            state.addListener(new DocumentListener() {
                @Override
                public void notify(final DocumentEvent evt) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (evt instanceof LoadedDocumentEvent) {
                                // When a document is loaded, scroll the panel to the
                                // top,
                                docPanel.scrollToTop();
                            }
                            if (evt instanceof LinkClickedEvent) {
                                LinkClickedEvent linkEvent = (LinkClickedEvent) evt;
                                String uri = linkEvent.getUri();
                                goToPage(uri);
                            }
                        }
                    });
                }
            });

            backButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    goBackPage();
                }
            });

            forwardButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    goForwardPage();
                }
            });

            reloadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    goReloadPage();
                }
            });

        }

        public void goToPage(String uri) {
            checkEventDispatchThread();
            urlField.setText(uri);
            state.loadURI(uri);
            if (historyIndex < history.size()) {
                for (int i = history.size() - 1; i >= historyIndex; --i) {
                    history.remove(i);
                }
            }
            history.add(uri);
            historyIndex = history.size();
        }

        public void goBackPage() {
            checkEventDispatchThread();
            if (historyIndex > 0) {
                --historyIndex;
                String uri = history.get(historyIndex - 1);
                urlField.setText(uri);
                state.loadURI(uri);
            }
        }

        public void goForwardPage() {
            checkEventDispatchThread();
            if (historyIndex < history.size()) {
                String uri = history.get(historyIndex);
                ++historyIndex;
                urlField.setText(uri);
                state.loadURI(uri);
            }
        }

        public void goReloadPage() {
            checkEventDispatchThread();
            if (historyIndex > 0) {
                String uri = history.get(historyIndex - 1);
                urlField.setText(uri);
                state.loadURI(uri);
            }
        }

        private ActionListener urlActionHandler = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String urlString = urlField.getText();
                goToPage(urlString);
            }
        };

        public void setInitialFocus() {
            urlField.requestFocusInWindow();
        }

    }

    public static void runMain() {

        try {
            // Set the system look and feel,
            UIManager.setLookAndFeel(
                            UIManager.getSystemLookAndFeelClassName());
        } 
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        Agent agent = new Agent();
        agent.init();

        JFrame frame = new JFrame("Testing Panel");
        frame.setPreferredSize(new Dimension(1024, 768));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Container c = frame.getContentPane();
        c.setLayout(new BorderLayout());

        TestBrowser testBrowser = new TestBrowser(agent);
        testBrowser.init();
        c.add(testBrowser);

        frame.pack();
        frame.setVisible(true);

        // -----

        File currentDir = new File(".");
        String uri;
        try {
            uri = currentDir.getCanonicalFile().toURI().toString();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        testBrowser.goToPage(uri);
        testBrowser.setInitialFocus();

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
