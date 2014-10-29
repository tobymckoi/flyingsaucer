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

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.layout.UnbreakableContent.Fragment;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.InlineBox;

/**
 * Calculates how styled runs of text are broken at an advance.
 *
 * @author Tobias Downer
 */
public class NewBreaker {

    /**
     * Given a run of Styleable objects, returns a list of UnbreakableContent
     * objects that describe how each individual elements of the run can be
     * broken at a line break. The order of the returned UnbreakableContent list
     * is appropriate for laying out the text in the order of the run.
     *
     * @param c
     * @param box
     * @return
     */
    public static List<UnbreakableContent> calculateUnbreakables(
                                                LayoutContext c, BlockBox box) {

        List<Styleable> currentRun = box.getInlineContent();
        ArrayList<UnbreakableContent> output = new ArrayList(currentRun.size());

        int first = 0;
        int pos = 0;
        for (Styleable s : currentRun) {

            // If it's not a text component
            if (!s.getStyle().isInline()) {
                // Add the text elements,
                if (pos != first) {
                    output.addAll(calculateUnbreakableWordsInText(
                            c, currentRun.subList(first, pos)));
                }
                // Create an UnbreakableContent (non text type)
                Fragment fragment = new Fragment(s, 0, 0, true, true);
                UnbreakableContent unbreakable = new UnbreakableContent(false, fragment);

                unbreakable.calculateMetrics(c);

                output.add(unbreakable);

                first = pos + 1;
            }

            ++pos;

        }

        if (pos != first) {
            output.addAll(calculateUnbreakableWordsInText(
                    c, currentRun.subList(first, pos)));
        }

        return output;
    }

    /**
     * Given a run of Styleable objects, returns a list of UnbreakableContent
     * objects that describe how each individual elements of the run can be
     * broken at a line break. The order of the returned UnbreakableContent list
     * is appropriate for laying out the text in the order of the run.
     * <p>
     * Basically, each 'UnbreakableContent' object returned represent a single
     * word. Each word may be composed of a series of individual styles.
     *
     * @param c
     * @param currentRun
     * @return
     */
    public static List<UnbreakableContent> calculateUnbreakableWordsInText(
                                 LayoutContext c, List<Styleable> currentRun) {

        // Collate the run into a string we use for line breaks,
        StringBuilder collate = new StringBuilder();
        List<Integer> runIndexes = new ArrayList(currentRun.size());

        for (Styleable styleable : currentRun) {
            // Yes, so append the text to a string and mark the fragment.
            final InlineBox iB = (InlineBox) styleable;
            String fragment;
        // This is a prediction of the content of a dynamic function, used for
            // layout.
            if (iB.isDynamicFunction()) {
                fragment = iB.getContentFunction().getLayoutReplacementText();
            } else {
                fragment = iB.getText();
            }
            collate.append(fragment);
            runIndexes.add(collate.length());
        }

        List<UnbreakableContent> unbreakables = new ArrayList();

        // Apply the break iterator to the string,
        final BreakIterator iter = c.getLineBreaker();
        iter.setText(collate.toString());

        // For each word,
        int wordStart = 0;
        int wordEnd = iter.next();

        // Force at least one unbreakable to be output if there's no string content
        // but there are indexes,
        boolean forceOne = !runIndexes.isEmpty();

        while (forceOne || wordEnd != BreakIterator.DONE) {
            forceOne = false;

            // Create an UnbreakableContent (text type)
            UnbreakableContent unbreakable = new UnbreakableContent(true);

            // Work out all the different boxes that intersect this unbreakable
            int runElementStart = 0;
            for (int i = 0; i < runIndexes.size(); ++i) {
                int runElementEnd = runIndexes.get(i);

                boolean includeFragment;
                if (runElementStart == runElementEnd) {
                    includeFragment
                            = runElementStart == collate.length()
                            || (runElementStart >= wordStart && runElementStart < wordEnd);
                } else {
                    includeFragment = (wordEnd > runElementStart && wordStart < runElementEnd);
                }

                if (includeFragment) {

                    int fs = Math.max(0, wordStart - runElementStart);
                    int fe = Math.min(runElementEnd - runElementStart, wordEnd - runElementStart);
                    boolean startOfBox, endOfBox;

                    Styleable styleable = currentRun.get(i);
                    final InlineBox inlineBox = (InlineBox) currentRun.get(i);

                    // Conditions in which this fragment is the start or end of the
                    // box style.
                    startOfBox = inlineBox.isStartsHere() && (fs == 0);
                    endOfBox = inlineBox.isEndsHere() && (fe == (runElementEnd - runElementStart));

                    // Add each box as a fragment of this unbreakable,
                    Fragment f
                            = new Fragment(styleable, fs, fe, startOfBox, endOfBox);
                    unbreakable.addFragment(f);

                }
                runElementStart = runElementEnd;
            }

            // Calculate the basic metrics (width) of this unbreakable,
            unbreakable.calculateMetrics(c);

            // Add to the unbreakables list,
            unbreakables.add(unbreakable);

            // Go to the next word,
            wordStart = wordEnd;
            wordEnd = iter.next();

        }

        return unbreakables;

    }

    // -----

    private final List<UnbreakableContent> unbreakables;

    private int currentIndex;

    private boolean stayOpen = false;
    private boolean overflowedLine = false;

    /**
     * Constructor over an ordered list of unbreakables.
     *
     * @param unbreakables
     */
    public NewBreaker(List<UnbreakableContent> unbreakables) {
        this.unbreakables = unbreakables;
        currentIndex = 0;
    }

    /**
     * Returns true if the last call to 'nextLine' required that the line was
     * broken before the end of the run was reached, meaning the line should be
     * closed. If returns false, then the end of the run was reached, therefore
     * the line should remain open.
     *
     * @return
     */
    public boolean shouldCloseLine() {
        return !stayOpen;
    }

    /**
     * Returns true if the last call to 'nextLine' produced a line that
     * overflowed the available width boundary. This happens when the part is
     * unbreakable.
     *
     * @return
     */
    public boolean isOverflowedLine() {
        return overflowedLine;
    }

    /**
     * Either returns the next run of text that will fit into the remaining
     * width, the next child BlockBox object, or null indicating the block is
     * finished.
     *
     * @param c
     * @param maxAvailableWidth
     * @param lineStart true if we are laying out the unbreakables in a line
     * starting with an empty line.
     * @param remainingWidth the width (in pixels) to fill.
     * @return
     */
    public BreakerRun nextRun(LayoutContext c, int maxAvailableWidth,
            boolean lineStart, int remainingWidth) {

        // An iterator from the current index,
        ListIterator<UnbreakableContent> iterator
                = unbreakables.listIterator(currentIndex);

        // While there's words to iterate,
        int count = 0;

        overflowedLine = false;

        // Builds a set of style runs,
        StyleRun styleRun = new StyleRun();

        int minWrapPoint = lineStart ? currentIndex + 1 : currentIndex;
        int wrapPoint = currentIndex;

        boolean trimFirst = lineStart;

        while (iterator.hasNext()) {

            UnbreakableContent unbreakable = iterator.next();

            if (!unbreakable.isText()) {
                if (!styleRun.isEmpty()) {
                    // Return the current style run,
                    stayOpen = true;
                    final float styleRunWidth
                            = styleRun.getWorkingWidth(c, maxAvailableWidth);
                    return new BreakerRun(styleRun.getSyleBoxList(true),
                            styleRunWidth,
                            styleRun.trailingWhitespaceWidthDifference);
                } else {
                    // It's empty, so just return the block,
                    ++currentIndex;
                    stayOpen = true;
                    return new BreakerRun(unbreakable.getBlockBox());
                }
            }

            // Note that it's impossible to get to here without the unbreakables in
            // the list all being text content.
            // If this is the start of the line then strip any leading whitespace,
            if (trimFirst) {
                unbreakable = unbreakable.stripLeadingWhitespace();
                trimFirst = false;
            }

            // If it's a 'nowrap' element
            boolean noWrap = false; //unbreakable.isNoWrap();

            // If we can wrap,
            if (!noWrap) {
                wrapPoint = Math.max(wrapPoint, currentIndex);
            } // If no wrap,
            else {
            }

            // Push an unbreakable into the style run,
            styleRun.push(unbreakable);
            ++currentIndex;

            // If we need to break,
            if (wrapPoint >= minWrapPoint
                    && styleRun.getWorkingWidth(c, maxAvailableWidth) > remainingWidth) {

                // Pop off elements until we hit the wrap point. Note that this may
                // pop all the elements if this is not a line start.
                int toPop = (currentIndex - wrapPoint);
                for (int i = 0; i < toPop; ++i) {
                    styleRun.pop();
                    --currentIndex;
                }

                final float styleRunWidth
                        = styleRun.getWorkingWidth(c, maxAvailableWidth);
                if (styleRunWidth > remainingWidth) {
                    overflowedLine = true;
                }

                // The style run represents the area to return,
                stayOpen = false;
                return new BreakerRun(styleRun.getSyleBoxList(false),
                        styleRunWidth,
                        styleRun.trailingWhitespaceWidthDifference);

            }

            ++count;
        }

        // If here, we finished iterating over all the words and they fit into the
        // available width.
        // Return null if we reached the end,
        if (count == 0) {
            stayOpen = true;
            return null;
        }

        // Test if we exceeded the advance,
        final float styleRunWidth = styleRun.getWorkingWidth(c, maxAvailableWidth);
        // Yes!
        if (styleRunWidth > remainingWidth) {
            overflowedLine = true;
        }

        // We didn't reach the end, so return the remaining unbreakables,
        currentIndex = unbreakables.size();

        // 'lineParts' is the unbreakable words that should layout into an open
        // box,
        stayOpen = true;
        return new BreakerRun(styleRun.getSyleBoxList(true),
                styleRunWidth, styleRun.trailingWhitespaceWidthDifference);

    }

    // -----


    /**
     * The style run manages a series of style boxes added in the order they'd
     * appear in a line.
     */
    private static class StyleRun {

        private final List<UnbreakableContent> lineBuffer = new ArrayList();
        private final List<InlineBoxPart> boxes = new ArrayList();
        private InlineBoxPart top = null;
        private float trailingWhitespaceWidthDifference = 0;

        /**
         * Push into the styled box run.
         *
         * @param unbreakable
         */
        private void pushAsStyleBox(UnbreakableContent unbreakable) {
            List<Fragment> fragments = unbreakable.getFragments();
            for (Fragment f : fragments) {
                if (top != null) {
                    if (!top.sameBox(f.getInlineBox())) {
                        top = new InlineBoxPart(f.getInlineBox());
                        top.concatenateRange(f);
                        top.addToWidth(f.getCalculatedWidth());
                        boxes.add(top);
                    } else {
                        top.concatenateRange(f);
                        top.addToWidth(f.getCalculatedWidth());
                    }
                } else {
                    top = new InlineBoxPart(f.getInlineBox());
                    top.concatenateRange(f);
                    top.addToWidth(f.getCalculatedWidth());
                    boxes.add(top);
                }
            }
        }

        /**
         * Rebuild the style run from the information in lineBuffer.
         */
        private void rebuildStyleRun(boolean open) {
            boxes.clear();
            top = null;
            int sz = lineBuffer.size();
            for (int i = 0; i < sz; ++i) {
                UnbreakableContent t = lineBuffer.get(i);
                if (!open && i == sz - 1) {
                    pushAsStyleBox(t);
                    trailingWhitespaceWidthDifference
                            = -(t.getRawWidth() - t.stripTrailingWhitespace().getRawWidth());
//                  pushAsStyleBox(t.stripTrailingWhitespace());
                } else {
                    pushAsStyleBox(t);
                }
            }
        }

        /**
         * Push a new unbreakable into this style run.
         */
        private void push(UnbreakableContent unbreakable) {
            lineBuffer.add(unbreakable);
            pushAsStyleBox(unbreakable);
        }

        /**
         * Remove the top entry from this style run.
         */
        private void pop() {
            lineBuffer.remove(lineBuffer.size() - 1);
            rebuildStyleRun(true);
        }

        private List<InlineBoxPart> getSyleBoxList(boolean open) {
            rebuildStyleRun(open);
            return Collections.unmodifiableList(boxes);
        }

        public boolean isEmpty() {
            return lineBuffer.isEmpty();
        }

        private boolean containsMoreThanSingle() {
            return lineBuffer.size() > 1;
        }

        private boolean containsSingle() {
            return lineBuffer.size() == 1;
        }

        /**
         * Finds the current working width of this style run including padding/
         * borders and margins.
         *
         * @return
         */
        private int getWorkingWidth(LayoutContext c, int availableWidth) {
            float workingWidth = 0f;
            int sz = lineBuffer.size();
            for (int i = 0; i < sz; ++i) {
                UnbreakableContent text = lineBuffer.get(i);
                // If it's the last entry then strip any trailing whitespaces,
                if (i == sz - 1) {
                    text = text.stripTrailingWhitespace();
                }
                workingWidth += text.getRawWidth();
            }
            // Add any left or right border styles,
            for (InlineBoxPart b : boxes) {
                if (b.includesFirst()) {
                    CalculatedStyle style = b.getStyle();
                    float left_padding = style.getMarginBorderPadding(
                            c, availableWidth, CalculatedStyle.LEFT);
                    workingWidth += left_padding;
                }
                if (b.includesLast()) {
                    CalculatedStyle style = b.getStyle();
                    float right_padding = style.getMarginBorderPadding(
                            c, availableWidth, CalculatedStyle.RIGHT);
                    workingWidth += right_padding;
                }

            }
            return (int) Math.ceil(workingWidth);
        }

        public String toString() {
            return boxes.toString();
        }

    }

    /**
     * An InlineBoxPart represents a sub-section of an 'InlineBox' that is used
     * to represent a run of text in a line. The InlineBoxPart can be used to
     * produce InlineText objects for layout of lines.
     */
    public static class InlineBoxPart {

        private final InlineBox inlineBox;
        private boolean includesFirst;
        private boolean includesLast;
        private int start;
        private int end;
        private float width = 0;

        public InlineBoxPart(InlineBox inlineBox) {
            this.inlineBox = inlineBox;
            this.start = -1;
            this.end = -1;
            this.includesFirst = false;
            this.includesLast = false;
        }

        public InlineBox getInlineBox() {
            return inlineBox;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getTrimmedWhitespaceEnd() {
            // Returns the end index of this box part minus the whitespace.
            String masterText = getInlineBox().getText().substring(start, end);
            int spaceCount = 0;
            for (int i = masterText.length() - 1; i >= 0; --i) {
                if (masterText.charAt(i) == ' ') {
                    ++spaceCount;
                } else {
                    break;
                }
            }
            return end - spaceCount;
        }

        /**
         * Returns true if this text contains text.
         *
         * @return
         */
        public boolean hasTextContent() {
            return (start < end);
        }

        public CalculatedStyle getStyle() {
            return inlineBox.getStyle();
        }

        public boolean includesFirst() {
            return includesFirst;
        }

        public boolean includesLast() {
            return includesLast;
        }

        public boolean sameBox(InlineBox inlineBox) {
            return this.inlineBox == inlineBox;
        }

        public float getWidth() {
            return width;
        }

        /**
         * Concatenates the given inlineBox bounds into the range represented by
         * the fragment.
         */
        private void concatenateRange(Fragment f) {
            this.includesFirst |= f.isStartOfBox();
            this.includesLast |= f.isEndOfBox();
            if (start == -1 || end == -1) {
                start = f.getStart();
                end = f.getEnd();
            } else {
                int s = f.getStart();
                int e = f.getEnd();
                if (s < start) {
                    start = s;
                }
                if (e > end) {
                    end = e;
                }
            }
        }

        private void addToWidth(float widthToAdd) {
            this.width += widthToAdd;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            if (includesFirst) {
                b.append("(S)");
            }
            b.append("'");
            b.append(inlineBox.getText().substring(start, end));
            b.append("'");
            if (includesLast) {
                b.append("(E)");
            }
            return b.toString();
        }

    }

    /**
     * A BreakerRun is either a run of text of potentially different styles that
     * fits within a given advance, or a single BlockBox component that
     * represents a more complex child layout.
     */
    public static class BreakerRun {

        private final boolean isText;
        private final BlockBox blockBoxChild;
        private final List<InlineBoxPart> textRun;
        private final float calculatedWidth;
        private final float trailingWhitespaceWidthDifference;

        public BreakerRun(BlockBox child) {
            isText = false;
            this.blockBoxChild = child;
            this.textRun = null;
            this.calculatedWidth = Float.NaN;
            this.trailingWhitespaceWidthDifference = 0f;
        }

        public BreakerRun(List<InlineBoxPart> textRun,
                float calculatedWidth,
                float trailingWhitespaceWidthDifference) {
            isText = true;
            this.textRun = textRun;
            this.calculatedWidth = calculatedWidth;
            this.trailingWhitespaceWidthDifference = trailingWhitespaceWidthDifference;
            this.blockBoxChild = null;
        }

        public boolean isText() {
            return isText;
        }

        /**
         * The difference between the width of this text run with trailing
         * whitespace and without it.
         *
         * @return
         */
        public float getTrailingWhitespaceWidthDifference() {
            return trailingWhitespaceWidthDifference;
        }

        /**
         * Returns the width minus any trailing whitespace glyphs.
         *
         * @return
         */
        public float getWorkingWidth() {
            return calculatedWidth;
        }

        public BlockBox getChildNode() {
            return blockBoxChild;
        }

        public List<InlineBoxPart> getTextRun() {
            return textRun;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            if (isText) {
                b.append("TEXT:");
                b.append(getTextRun());
            } else {
                b.append("BOX:");
                b.append(getChildNode());
            }
            return b.toString();
        }

    }

}
