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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import org.w3c.dom.Node;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.extend.TextRenderer;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.InlineBox;

/**
 * Contains information about a styled text element that is unbreakable, and the
 * style objects necessary to paint it.
 *
 * @author Tobias Downer
 */
public class UnbreakableContent {

    private final boolean isText;
    private final List<Fragment> fragments;
    private float contentHeight = 0;

    /**
     * Public constructor.
     *
     * @param isText true when this unbreakable content represents a text block.
     */
    public UnbreakableContent(boolean isText) {
        this.isText = isText;
        fragments = new ArrayList();
    }

    public UnbreakableContent(boolean isText, Fragment f) {
        this.isText = isText;
        fragments = new ArrayList(1);
        fragments.add(f);
    }

    private UnbreakableContent(boolean isText, List<Fragment> fragments) {
        this.isText = isText;
        this.fragments = fragments;
    }

    /**
     * Returns the height of this unbreakable content object.
     * @return 
     */
    public float getContentHeight() {
        return contentHeight;
    }

    /**
     * Returns true if this content is text, false if this content represents a
     * parent block (such as an image).
     *
     * @return
     */
    public boolean isText() {
        return isText;
    }

    /**
     * Returns true if this unbreakable contains any break on width fragments.
     * 
     * @return 
     */
    public boolean isWhitespaceBreakOnWidth() {
        for (Fragment f : fragments) {
            CalculatedStyle style = f.getStyle();
            IdentValue whitespace = style.getWhitespace();
            if (whitespace == IdentValue.NORMAL ||
                whitespace == IdentValue.PRE_WRAP ||
                whitespace == IdentValue.PRE_LINE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this unbreakable has a style that requires whitespace
     * to be collapsed. Whitespace collapse is actually preprocessed, but this
     * check is still necessary to determine if leading space should be
     * removed from a line.
     * 
     * @return 
     */
    public boolean isWhitespaceCollapseWhitespace() {
        for (Fragment f : fragments) {
            CalculatedStyle style = f.getStyle();
            IdentValue whitespace = style.getWhitespace();
            if (whitespace == IdentValue.NORMAL ||
                whitespace == IdentValue.NOWRAP ||
                whitespace == IdentValue.PRE_LINE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the fragments in this unbreakable. This is used for inline
     * modification of fragments (such as setting the first-letter of a block
     * with a CSS style).
     * 
     * @param newFragments 
     */
    void updateFragments(List<Fragment> newFragments) {
        this.fragments.clear();
        this.fragments.addAll(newFragments);
    }

    /**
     * Returns the list of all fragments in this object.
     *
     * @return
     */
    public List<Fragment> getFragments() {
        return Collections.unmodifiableList(fragments);
    }

    /**
     * Adds a new fragment to this styled text object.
     *
     * @param fragment
     */
    public void addFragment(Fragment fragment) {
        assert (fragment.isText() == isText);
        fragments.add(fragment);
    }

    /**
     * If it's not text, then it'll be a single fragment with the BlockBox in
     * it.
     *
     * @return
     */
    public BlockBox getBlockBox() {
        if (isText()) {
            throw new IllegalStateException("Not child node");
        }
        return fragments.get(0).getBlockBox();
    }

    /**
     * Calculates the metrics of this styled text.
     *
     * @param c
     * @param parent
     */
    public void calculateMetrics(LayoutContext c, BlockBox parent) {

        // If calculating the metrics for text items,
        if (isText) {

             // We need to calculate the width of this unbreakable with and without
            // leading and/or trailing whitespace,
            // Test of leading whitespace,
            int headNonWhitespace = -1;
            ListIterator<Fragment> iterator = fragments.listIterator();
            while (iterator.hasNext()) {
                Fragment f = iterator.next();
                String mt = f.getMasterText();
                for (int i = f.getStart(); i < f.getEnd(); ++i) {
                    if (mt.charAt(i) != ' ') {
                        headNonWhitespace = i;
                        break;
                    }
                }
                if (headNonWhitespace == -1) {
                    // This fragment is all whitespace,
                    f.setLeadingWhitespace(f.getEnd());
                } else {
                    f.setLeadingWhitespace(headNonWhitespace);
                    break;
                }
            }

            // Trailing whitespace,
            int tailNonWhitespace = -1;
            iterator = fragments.listIterator(fragments.size());
            while (iterator.hasPrevious()) {
                Fragment f = iterator.previous();
                String mt = f.getMasterText();
                for (int i = f.getEnd() - 1; i >= f.getStart(); --i) {
                    if (mt.charAt(i) != ' ') {
                        tailNonWhitespace = i + 1;
                        break;
                    }
                }
                if (tailNonWhitespace == -1) {
                    // This fragment is all whitespace,
                    f.setTrailingWhitespace(f.getStart());
                } else {
                    f.setTrailingWhitespace(tailNonWhitespace);
                    break;
                }
            }

            float maxHeight = 1f;
            for (Fragment f : fragments) {

                float lineHeight = f.getInlineBox().getStyle().getLineHeight(c);
                maxHeight = Math.max(lineHeight, maxHeight);

                TextRenderer textRenderer = c.getTextRenderer();
                FSFont font = f.getStyle().getFSFont(c);

                // The width of the text with the whitespace,
                float fragmentWidth = textRenderer.getLogicalGlyphsWidth(
                        c.getFontContext(), font, f.getFragmentString());
                f.setCalculatedWidth(fragmentWidth);

                if (f.hasLeadingWhitespace()) {
                    // The width without leading whitespace,
                    fragmentWidth = textRenderer.getLogicalGlyphsWidth(
                            c.getFontContext(), font, f.getNoLWhitespaceFragmentString());
                    f.setWithoutLeadingWhitespaceWidth(fragmentWidth);
                }

                if (f.hasTrailingWhitespace()) {
                    // The width without trailing whitespace,
                    fragmentWidth = textRenderer.getLogicalGlyphsWidth(
                            c.getFontContext(), font, f.getNoTWhitespaceFragmentString());
                    f.setWithoutTrailingWhitespaceWidth(fragmentWidth);
                }

            }
            
            contentHeight = maxHeight;

        } // Not a text component,
        else {

            Fragment blockFragment = fragments.get(0);
            BlockBox child = blockFragment.getBlockBox();
            if (!child.getStyle().isNonFlowContent() &&
                    ( child.getStyle().isInlineBlock() ||
                      child.getStyle().isInlineTable() )) {

                child.setContainingBlock(parent);
                child.setContainingLayer(c.getLayer());
                child.calcDimensions(c);

                int childWidth = child.getContentWidth();
                blockFragment.setCalculatedWidth(childWidth);

                contentHeight = child.getHeight();
            }

        }

    }

    /**
     * Returns the calculated width of this unbreakable. Note that the returned
     * width does NOT include padding/margin or border margin information.
     *
     * @return
     */
    public float getRawWidth() {
        float widthTotal = 0f;
        for (Fragment f : fragments) {
            float w = f.getCalculatedWidth();
            widthTotal += w;
        }
        return widthTotal;
    }

    /**
     * Returns an unbreakable styled text object that is the same as this but
     * without any leading whitespace.
     *
     * @return
     */
    public UnbreakableContent stripLeadingWhitespace() {
        // If it doesn't have any leading whitespace then return this object,
        if (!hasLeadingWhitespace()) {
            return this;
        }
        List<Fragment> newList = new ArrayList(fragments.size());
        for (Fragment f : fragments) {
            newList.add(f.withoutLeadingWhitespace());
        }
        return new UnbreakableContent(true, newList);
    }

    /**
     * Returns an unbreakable styled text object that is the same as this but
     * without any trailing whitespace.
     *
     * @return
     */
    public UnbreakableContent stripTrailingWhitespace() {
        // If it doesn't have any trailing whitespace then return this object,
        if (!hasTrailingWhitespace()) {
            return this;
        }
        List<Fragment> newList = new ArrayList(fragments.size());
        for (Fragment f : fragments) {
            newList.add(f.withoutTrailingWhitespace());
        }
        return new UnbreakableContent(true, newList);
    }

    public boolean hasLeadingWhitespace() {
        if (!isText) {
            return false;
        }
        for (Fragment f : fragments) {
            if (f.hasLeadingWhitespace()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTrailingWhitespace() {
        if (!isText) {
            return false;
        }
        for (Fragment f : fragments) {
            if (f.hasTrailingWhitespace()) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if this is text content that ends with a new line character.
     * @return 
     */
    public boolean endsWithNL() {
        if (isText) {
            int sz = fragments.size();
            if (sz > 0) {
                Fragment f = fragments.get(sz - 1);
                return f.getFragmentString().endsWith("\n");
            }
        }
        return false;
    }

    public static class Fragment {

        private final Styleable styleable;
        private final int start;
        private final int end;
        private float calculatedWidth = 0;
        private int leadingWhitespace;
        private int trailingWhitespace;
        private float calculatedNoLeadingWhitespaceWidth;
        private float calculatedNoTrailingWhitespaceWidth;
        private final boolean startOfBox;
        private final boolean endOfBox;

        public Fragment(Styleable styleable, int start, int end,
                boolean startOfBox, boolean endOfBox) {

            this.styleable = styleable;
            this.start = start;
            this.end = end;
            this.leadingWhitespace = start;
            this.trailingWhitespace = end;
            this.startOfBox = startOfBox;
            this.endOfBox = endOfBox;

        }

        public Styleable getStyleable() {
            return this.styleable;
        }

        public InlineBox getInlineBox() {
            return (InlineBox) getStyleable();
        }

        public boolean isText() {
            return (styleable instanceof InlineBox);
        }

        private void setLeadingWhitespace(int index) {
            leadingWhitespace = index;
        }

        private void setTrailingWhitespace(int index) {
            trailingWhitespace = index;
        }

        public CalculatedStyle getStyle() {
            return getStyleable().getStyle();
        }

        public Node getTextNode() {
            return getInlineBox().getTextNode();
        }

        public String getMasterText() {
            return getInlineBox().getText();
        }

        public BlockBox getBlockBox() {
            return (BlockBox) styleable;
        }

        /**
         * Returns true if this fragment is the first fragment produced from the
         * InlineBox.
         *
         * @return
         */
        public boolean isStartOfBox() {
            return startOfBox;
        }

        /**
         * Returns true if this fragment is the last fragment produced from the
         * InlineBox.
         *
         * @return
         */
        public boolean isEndOfBox() {
            return endOfBox;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getLength() {
            return end - start;
        }

        public String getFragmentString() {
            return getMasterText().substring(getStart(), getEnd());
        }

        public void setCalculatedWidth(float width) {
            this.calculatedWidth = width;
        }

        public float getCalculatedWidth() {
            return this.calculatedWidth;
        }

        private String getNoLWhitespaceFragmentString() {
            return getMasterText().substring(leadingWhitespace, getEnd());
        }

        private String getNoTWhitespaceFragmentString() {
            return getMasterText().substring(getStart(), trailingWhitespace);
        }

        private void setWithoutLeadingWhitespaceWidth(float width) {
            this.calculatedNoLeadingWhitespaceWidth = width;
        }

        private void setWithoutTrailingWhitespaceWidth(float width) {
            this.calculatedNoTrailingWhitespaceWidth = width;
        }

        public float getCalculatedNoLeadingWhitespaceWidth() {
            return calculatedNoLeadingWhitespaceWidth;
        }

        public float getCalculatedNoTrailingWhitespaceWidth() {
            return calculatedNoTrailingWhitespaceWidth;
        }

        public int getLeadingWhitespace() {
            return leadingWhitespace;
        }

        public int getTrailingWhitespace() {
            return trailingWhitespace;
        }

        public boolean hasLeadingWhitespace() {
            return leadingWhitespace != start;
        }

        public boolean hasTrailingWhitespace() {
            return trailingWhitespace != end;
        }

        public boolean hasWhitespace() {
            return (hasLeadingWhitespace() || hasTrailingWhitespace());
        }

        private Fragment withoutLeadingWhitespace() {
            if (!hasLeadingWhitespace()) {
                return this;
            }
            Fragment f = new Fragment(styleable, leadingWhitespace, end,
                    startOfBox, endOfBox);
            f.setCalculatedWidth(calculatedNoLeadingWhitespaceWidth);
            if (leadingWhitespace != end && hasTrailingWhitespace()) {
                f.setTrailingWhitespace(trailingWhitespace);
                float widthChange = calculatedWidth - calculatedNoLeadingWhitespaceWidth;
                f.setWithoutTrailingWhitespaceWidth(
                        calculatedNoTrailingWhitespaceWidth - widthChange);
            }
            return f;
        }

        private Fragment withoutTrailingWhitespace() {
            if (!hasTrailingWhitespace()) {
                return this;
            }
            Fragment f = new Fragment(styleable, start, trailingWhitespace,
                    startOfBox, endOfBox);
            f.setCalculatedWidth(calculatedNoTrailingWhitespaceWidth);
            if (start != trailingWhitespace && hasLeadingWhitespace()) {
                f.setLeadingWhitespace(leadingWhitespace);
                float widthChange = calculatedWidth - calculatedNoTrailingWhitespaceWidth;
                f.setWithoutLeadingWhitespaceWidth(
                        calculatedNoLeadingWhitespaceWidth - widthChange);
            }
            return f;
        }

    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Fragment f : fragments) {
            if (first) {
                first = false;
            } else {
                b.append("|");
            }
            if (f.isText()) {
                b.append(f.getFragmentString());
            } else {
                b.append("<>");
            }
        }
        b.append(" (");
        b.append(getRawWidth());
        b.append(" x ");
        b.append(contentHeight);
        b.append(")");
        return b.toString();
    }

}
