/*
 * Matcher.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.xhtmlrenderer.css.newmatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.xhtmlrenderer.css.extend.AttributeResolver;
import org.xhtmlrenderer.css.extend.StylesheetFactory;
import org.xhtmlrenderer.css.extend.TreeResolver;
import org.xhtmlrenderer.css.sheet.MediaRule;
import org.xhtmlrenderer.css.sheet.PageRule;
import org.xhtmlrenderer.css.sheet.Ruleset;
import org.xhtmlrenderer.css.sheet.Stylesheet;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.Util;


/**
 * @author Torbjoern Gannholm
 */
public class Matcher {

    Mapper docMapper;
    private org.xhtmlrenderer.css.extend.AttributeResolver _attRes;
    private org.xhtmlrenderer.css.extend.TreeResolver _treeRes;
    private org.xhtmlrenderer.css.extend.StylesheetFactory _styleFactory;

    private java.util.Map _map;

    //handle dynamic
    private Set _hoverElements;
    private Set _activeElements;
    private Set _focusElements;
    private Set _visitElements;
    
    private List _pageRules;
    private List _fontFaceRules;
    
    public Matcher(
            TreeResolver tr, AttributeResolver ar, StylesheetFactory factory, List stylesheets, String medium) {
        newMaps();
        _treeRes = tr;
        _attRes = ar;
        _styleFactory = factory;
        
        _pageRules = new ArrayList();
        _fontFaceRules = new ArrayList();
        docMapper = createDocumentMapper(stylesheets, medium);
    }
    
    public void removeStyle(Object e) {
        _map.remove(e);
    }

    public CascadedStyle getCascadedStyle(Object e, boolean restyle) {
        synchronized (e) {
            Mapper em;
            if (!restyle) {
                em = getMapper(e);
            } else {
                em = matchElement(e);
            }
            return em.getCascadedStyle(e);
        }
    }

    /**
     * May return null.
     * We assume that restyle has already been done by a getCascadedStyle if necessary.
     */
    public CascadedStyle getPECascadedStyle(Object e, String pseudoElement) {
        synchronized (e) {
            Mapper em = getMapper(e);
            return em.getPECascadedStyle(e, pseudoElement);
        }
    }
    
    public PageInfo getPageCascadedStyle(String pageName, String pseudoPage) {
        List props = new ArrayList();
        Map marginBoxes = new HashMap();

        for (Iterator i = _pageRules.iterator(); i.hasNext(); ) {
            PageRule pageRule = (PageRule)i.next();
            
            if (pageRule.applies(pageName, pseudoPage)) {
                props.addAll(pageRule.getRuleset().getPropertyDeclarations());
                marginBoxes.putAll(pageRule.getMarginBoxes());
            }
        }
        
        CascadedStyle style = null;
        if (props.isEmpty()) {
            style = CascadedStyle.emptyCascadedStyle;
        } else {
            style = new CascadedStyle(props.iterator());
        }
        
        return new PageInfo(props, style, marginBoxes);
    }
    
    public List getFontFaceRules() {
        return _fontFaceRules;
    }
    
    public boolean isVisitedStyled(Object e) {
        return _visitElements.contains(e);
    }

    public boolean isHoverStyled(Object e) {
        return _hoverElements.contains(e);
    }

    public boolean isActiveStyled(Object e) {
        return _activeElements.contains(e);
    }

    public boolean isFocusStyled(Object e) {
        return _focusElements.contains(e);
    }

    protected Mapper matchElement(Object e) {
        synchronized (e) {
            Object parent = _treeRes.getParentElement(e);
            Mapper child;
            if (parent != null) {
                Mapper m = getMapper(parent);
                child = m.mapChild(e);
            } else {//has to be document or fragment node
                child = docMapper.mapChild(e);
            }
            return child;
        }
    }

    Mapper createDocumentMapper(List stylesheets, String medium) {
        TreeMap<String, Selector> sorter = new TreeMap();
        addAllStylesheets(stylesheets, sorter, medium);

        // Create and populate the mapper index,
        MapperIndex mapperIndex = new MapperIndex();
        mapperIndex.populate(sorter.values());

        XRLog.match("Matcher created with " + sorter.size() + " selectors");

        return new Mapper(mapperIndex, sorter.values());
    }
    
    private void addAllStylesheets(List stylesheets, TreeMap<String, Selector> sorter, String medium) {
        int count = 0;
        int pCount = 0;
        for (Iterator i = stylesheets.iterator(); i.hasNext(); ) {
            Stylesheet stylesheet = (Stylesheet)i.next();
            for (Iterator j = stylesheet.getContents().iterator(); j.hasNext(); ) {
                Object obj = (Object)j.next();
                if (obj instanceof Ruleset) {
                    for (Iterator k = ((Ruleset)obj).getFSSelectors().iterator(); k.hasNext(); ) {
                        Selector selector = (Selector)k.next();
                        selector.setPos(++count);
                        sorter.put(selector.getOrder(), selector);
                    }
                } else if (obj instanceof PageRule) {
                    ((PageRule)obj).setPos(++pCount);
                    _pageRules.add(obj);
                } else if (obj instanceof MediaRule) {
                    MediaRule mediaRule = (MediaRule)obj;
                    if (mediaRule.matches(medium)) {
                        for (Iterator k = mediaRule.getContents().iterator(); k.hasNext(); ) {
                            Ruleset ruleset = (Ruleset)k.next();
                            for (Iterator l = ruleset.getFSSelectors().iterator(); l.hasNext(); ) {
                                Selector selector = (Selector)l.next();
                                selector.setPos(++count);
                                sorter.put(selector.getOrder(), selector);
                            }
                        }
                    }
                }
            }
            
            _fontFaceRules.addAll(stylesheet.getFontFaceRules());
        }
        
        Collections.sort(_pageRules, new Comparator() {
            public int compare(Object o1, Object o2) {
                PageRule p1 = (PageRule)o1;
                PageRule p2 = (PageRule)o2;
                
                if (p1.getOrder() - p2.getOrder() < 0) {
                    return -1;
                } else if (p1.getOrder() == p2.getOrder()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
    }

    private void link(Object e, Mapper m) {
        _map.put(e, m);
    }

    private void newMaps() {
        _map = Collections.synchronizedMap(new java.util.HashMap());
        _hoverElements = Collections.synchronizedSet(new java.util.HashSet());
        _activeElements = Collections.synchronizedSet(new java.util.HashSet());
        _focusElements = Collections.synchronizedSet(new java.util.HashSet());
        _visitElements = Collections.synchronizedSet(new java.util.HashSet());
    }

    private Mapper getMapper(Object e) {
        Mapper m = (Mapper) _map.get(e);
        if (m != null) {
            return m;
        }
        m = matchElement(e);
        return m;
    }

    private static java.util.Iterator getMatchedRulesets(final List mappedSelectors) {
        return
                new java.util.Iterator() {
                    java.util.Iterator selectors = mappedSelectors.iterator();

                    public boolean hasNext() {
                        return selectors.hasNext();
                    }

                    public Object next() {
                        if (hasNext()) {
                            return ((Selector) selectors.next()).getRuleset();
                        } else {
                            throw new java.util.NoSuchElementException();
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
    }

    private static java.util.Iterator getSelectedRulesets(java.util.List selectorList) {
        final java.util.List sl = selectorList;
        return
                new java.util.Iterator() {
                    java.util.Iterator selectors = sl.iterator();

                    public boolean hasNext() {
                        return selectors.hasNext();
                    }

                    public Object next() {
                        if (hasNext()) {
                            return ((Selector) selectors.next()).getRuleset();
                        } else {
                            throw new java.util.NoSuchElementException();
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
    }

    private org.xhtmlrenderer.css.sheet.Ruleset getElementStyle(Object e) {
        synchronized (e) {
            if (_attRes == null || _styleFactory == null) {
                return null;
            }
            
            String style = _attRes.getElementStyling(e);
            if (Util.isNullOrEmpty(style)) {
                return null;
            }
            
            return _styleFactory.parseStyleDeclaration(org.xhtmlrenderer.css.sheet.StylesheetInfo.AUTHOR, style);
        }
    }

    private org.xhtmlrenderer.css.sheet.Ruleset getNonCssStyle(Object e) {
        synchronized (e) {
            if (_attRes == null || _styleFactory == null) {
                return null;
            }
            String style = _attRes.getNonCssStyling(e);
            if (Util.isNullOrEmpty(style)) {
                return null;
            }
            return _styleFactory.parseStyleDeclaration(org.xhtmlrenderer.css.sheet.StylesheetInfo.AUTHOR, style);
        }
    }


    /**
     * A Selector with an accompanying index representing its position relative
     * to the original specificity order. Used for chained selectors.
     */
    private static class IndexedSelector implements Comparable<IndexedSelector> {
        private final Integer index;
        private final Selector selector;
        public IndexedSelector(Integer index, Selector selector) {
            this.index = index;
            this.selector = selector;
        }

        @Override
        public int compareTo(IndexedSelector o) {
            return index.compareTo(o.index);
        }

        @Override
        public int hashCode() {
            return index.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final IndexedSelector other = (IndexedSelector) obj;
            return index.equals(other.index);
        }

    }

    /**
     * Mapper represents a local CSS for a Node that is used to match the Node's
     * children.
     *
     * @author Torbjoern Gannholm
     */
    class Mapper {
        private final MapperIndex baseIndex;

        private final List<IndexedSelector> additionalAxes;
        private final List<Selector> discountedAxes;

        private HashMap pseudoSelectors;
        private List mappedSelectors;
        private HashMap children;

        Mapper(MapperIndex mapperIndex, Collection selectors) {
            this.baseIndex = mapperIndex;
            additionalAxes = Collections.EMPTY_LIST;
            discountedAxes = Collections.EMPTY_LIST;
        }

        private Mapper(MapperIndex mapperIndex,
                        List<IndexedSelector> additionalAxes,
                        List<Selector> discountedAxes) {
            this.baseIndex = mapperIndex;
            this.additionalAxes = additionalAxes;
            this.discountedAxes = discountedAxes;
        }

        /**
         * Side effect: creates and stores a Mapper for the element
         *
         * @param e
         * @return The selectors that matched, sorted according to specificity
         *         (more correct: preserves the sort order from Matcher creation)
         */
        Mapper mapChild(Object e) {

            List<Integer> possibleSelectors =
                    baseIndex.getPossibleMatchedSelectors(e, _attRes, _treeRes);

            List<Selector> childDiscountedAxes = null;
            List<IndexedSelector> childAdditionalAxes = null;

            HashMap pseudoSelectors = new HashMap();
            List mappedSelectors = new ArrayList(10);

            // Create a list of selectors including the additional axes and
            // not including any discounted axes,
            List<IndexedSelector> selList = new ArrayList(possibleSelectors.size() + 10);
            int checkFrom = 0;
            // The list of possible selectors,
            for (Integer selectorIndex : possibleSelectors) {
                // Add any additional selectors from before this index,
                for (int i = checkFrom; i < additionalAxes.size(); ++i) {
                    IndexedSelector caxe = additionalAxes.get(i);
                    if (caxe.index < selectorIndex.intValue()) {
                        if (!discountedAxes.contains(caxe.selector)) {
                            selList.add(caxe);
                        }
                        checkFrom = i + 1;
                    }
                    else {
                        break;
                    }
                }
                Selector sel = baseIndex.getSelector(selectorIndex);
                if (!discountedAxes.contains(sel)) {
                    selList.add(new IndexedSelector(selectorIndex, sel));
                }
            }
            for (int i = checkFrom; i < additionalAxes.size(); ++i) {
                IndexedSelector caxe = additionalAxes.get(i);
                Selector sel = caxe.selector;
                if (!discountedAxes.contains(sel)) {
                    selList.add(caxe);
                }
            }

            if (additionalAxes.size() > 1) {
                System.out.println("-----");
            }

            StringBuilder key = new StringBuilder();

            // For each indexed selector,
            for (IndexedSelector indexedSel : selList) {

                Selector sel = indexedSel.selector;

                // This is discounted in the children,
                if (sel.getAxis() == Selector.CHILD_AXIS) {
                    if (childDiscountedAxes == null) {
                        childDiscountedAxes = new ArrayList(discountedAxes);
                    }
                    childDiscountedAxes.add(sel);
                }
                else if (sel.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                }

                // Check for match,
                if (!sel.matches(e, _attRes, _treeRes)) {
                    // No match so loop back,
                    continue;
                }

                //Assumption: if it is a pseudo-element, it does not also have dynamic pseudo-class
                String pseudoElement = sel.getPseudoElement();
                if (pseudoElement != null) {
                    List l = (List) pseudoSelectors.get(pseudoElement);
                    if (l == null) {
                        l = new LinkedList();
                        pseudoSelectors.put(pseudoElement, l);
                    }
                    l.add(sel);
                    key.append(sel.getSelectorID()).append(":");
                    continue;
                }
                if (sel.isPseudoClass(Selector.VISITED_PSEUDOCLASS)) {
                    _visitElements.add(e);
                }
                if (sel.isPseudoClass(Selector.ACTIVE_PSEUDOCLASS)) {
                    _activeElements.add(e);
                }
                if (sel.isPseudoClass(Selector.HOVER_PSEUDOCLASS)) {
                    _hoverElements.add(e);
                }
                if (sel.isPseudoClass(Selector.FOCUS_PSEUDOCLASS)) {
                    _focusElements.add(e);
                }
                if (!sel.matchesDynamic(e, _attRes, _treeRes)) {
                    continue;
                }
                key.append(sel.getSelectorID()).append(":");
                Selector chain = sel.getChainedSelector();
                if (chain == null) {
                    mappedSelectors.add(sel);
                } else if (chain.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                } else {
                    if (childAdditionalAxes == null) {
                        childAdditionalAxes = new ArrayList(additionalAxes);
                    }
                    childAdditionalAxes.add(new IndexedSelector(
                                                    indexedSel.index, chain));
                }

            }

            if (children == null) children = new HashMap();
            Mapper childMapper = (Mapper) children.get(key.toString());
            if (childMapper == null) {
                if (childAdditionalAxes != null) {
                    Collections.sort(childAdditionalAxes);
                }
                childMapper = new Mapper(baseIndex,
                                childAdditionalAxes == null ?
                                        additionalAxes : childAdditionalAxes,
                                childDiscountedAxes == null ?
                                        discountedAxes : childDiscountedAxes);
                childMapper.pseudoSelectors = pseudoSelectors;
                childMapper.mappedSelectors = mappedSelectors;
                children.put(key.toString(), childMapper);
            }
            link(e, childMapper);
            return childMapper;
        }

        CascadedStyle getCascadedStyle(Object e) {
            CascadedStyle result;
            synchronized (e) {
                CascadedStyle cs = null;
                org.xhtmlrenderer.css.sheet.Ruleset elementStyling = getElementStyle(e);
                org.xhtmlrenderer.css.sheet.Ruleset nonCssStyling = getNonCssStyle(e);
                List propList = new LinkedList();
                //specificity 0,0,0,0
                if (nonCssStyling != null) {
                    propList.addAll(nonCssStyling.getPropertyDeclarations());
                }
                //these should have been returned in order of specificity
                for (Iterator i = getMatchedRulesets(mappedSelectors); i.hasNext();) {
                    org.xhtmlrenderer.css.sheet.Ruleset rs = (org.xhtmlrenderer.css.sheet.Ruleset) i.next();
                    propList.addAll(rs.getPropertyDeclarations());
                }
                //specificity 1,0,0,0
                if (elementStyling != null) {
                    propList.addAll(elementStyling.getPropertyDeclarations());
                }
                if (propList.isEmpty())
                    cs = CascadedStyle.emptyCascadedStyle;
                else {
                    cs = new CascadedStyle(propList.iterator());
                }

                result = cs;
            }
            return result;
        }

        /**
         * May return null.
         * We assume that restyle has already been done by a getCascadedStyle if necessary.
         */
        public CascadedStyle getPECascadedStyle(Object e, String pseudoElement) {
            java.util.Iterator si = pseudoSelectors.entrySet().iterator();
            if (!si.hasNext()) {
                return null;
            }
            CascadedStyle cs = null;
            java.util.List pe = (java.util.List) pseudoSelectors.get(pseudoElement);
            if (pe == null) return null;

            java.util.List propList = new java.util.LinkedList();
            for (java.util.Iterator i = getSelectedRulesets(pe); i.hasNext();) {
                org.xhtmlrenderer.css.sheet.Ruleset rs = (org.xhtmlrenderer.css.sheet.Ruleset) i.next();
                propList.addAll(rs.getPropertyDeclarations());
            }
            if (propList.size() == 0)
                cs = CascadedStyle.emptyCascadedStyle;//already internalized
            else {
                cs = new CascadedStyle(propList.iterator());
            }
            return cs;
        }

    }



//    /**
//     * Mapper represents a local CSS for a Node that is used to match the Node's
//     * children.
//     *
//     * @author Torbjoern Gannholm
//     */
//    class Mapper {
//        private final MapperIndex baseIndex;
//
//        private final List axes;
//        private HashMap pseudoSelectors;
//        private List mappedSelectors;
//        private HashMap children;
//
//        Mapper(MapperIndex mapperIndex, Collection selectors) {
//            this.baseIndex = mapperIndex;
//            axes = new ArrayList(selectors);
//        }
//
//        private Mapper(MapperIndex mapperIndex, List axes, boolean nocopy) {
//            this.baseIndex = mapperIndex;
//            if (nocopy) {
//                this.axes = axes;
//            } else {
//                this.axes = new ArrayList(axes);
//            }
//        }
//
//        /**
//         * Side effect: creates and stores a Mapper for the element
//         *
//         * @param e
//         * @return The selectors that matched, sorted according to specificity
//         *         (more correct: preserves the sort order from Matcher creation)
//         */
//        Mapper mapChild(Object e) {
//            //Mapper childMapper = new Mapper();
//            List childAxes = new ArrayList(axes.size() + 10);
//            HashMap pseudoSelectors = new HashMap();
//            List mappedSelectors = new ArrayList(10);
//            StringBuilder key = new StringBuilder();
//            for (int i = 0, size = axes.size(); i < size; i++) {
//                Selector sel = (Selector) axes.get(i);
//                if (sel.getAxis() == Selector.DESCENDANT_AXIS) {
//                    //carry it forward to other descendants
//                    childAxes.add(sel);
//                } else if (sel.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
//                    throw new RuntimeException();
//                }
//                if (!sel.matches(e, _attRes, _treeRes)) {
//                    continue;
//                }
//                //Assumption: if it is a pseudo-element, it does not also have dynamic pseudo-class
//                String pseudoElement = sel.getPseudoElement();
//                if (pseudoElement != null) {
//                    java.util.List l = (java.util.List) pseudoSelectors.get(pseudoElement);
//                    if (l == null) {
//                        l = new java.util.LinkedList();
//                        pseudoSelectors.put(pseudoElement, l);
//                    }
//                    l.add(sel);
//                    key.append(sel.getSelectorID()).append(":");
//                    continue;
//                }
//                if (sel.isPseudoClass(Selector.VISITED_PSEUDOCLASS)) {
//                    _visitElements.add(e);
//                }
//                if (sel.isPseudoClass(Selector.ACTIVE_PSEUDOCLASS)) {
//                    _activeElements.add(e);
//                }
//                if (sel.isPseudoClass(Selector.HOVER_PSEUDOCLASS)) {
//                    _hoverElements.add(e);
//                }
//                if (sel.isPseudoClass(Selector.FOCUS_PSEUDOCLASS)) {
//                    _focusElements.add(e);
//                }
//                if (!sel.matchesDynamic(e, _attRes, _treeRes)) {
//                    continue;
//                }
//                key.append(sel.getSelectorID()).append(":");
//                Selector chain = sel.getChainedSelector();
//                if (chain == null) {
//                    mappedSelectors.add(sel);
//                } else if (chain.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
//                    throw new RuntimeException();
//                } else {
//                    childAxes.add(chain);
//                }
//            }
//            if (children == null) children = new HashMap();
//            Mapper childMapper = (Mapper) children.get(key.toString());
//            if (childMapper == null) {
//                childMapper = new Mapper(baseIndex, childAxes, true);
//                childMapper.pseudoSelectors = pseudoSelectors;
//                childMapper.mappedSelectors = mappedSelectors;
//                children.put(key.toString(), childMapper);
//            }
//            link(e, childMapper);
//            return childMapper;
//        }
//
//        CascadedStyle getCascadedStyle(Object e) {
//            CascadedStyle result;
//            synchronized (e) {
//                CascadedStyle cs = null;
//                org.xhtmlrenderer.css.sheet.Ruleset elementStyling = getElementStyle(e);
//                org.xhtmlrenderer.css.sheet.Ruleset nonCssStyling = getNonCssStyle(e);
//                List propList = new LinkedList();
//                //specificity 0,0,0,0
//                if (nonCssStyling != null) {
//                    propList.addAll(nonCssStyling.getPropertyDeclarations());
//                }
//                //these should have been returned in order of specificity
//                for (Iterator i = getMatchedRulesets(mappedSelectors); i.hasNext();) {
//                    org.xhtmlrenderer.css.sheet.Ruleset rs = (org.xhtmlrenderer.css.sheet.Ruleset) i.next();
//                    propList.addAll(rs.getPropertyDeclarations());
//                }
//                //specificity 1,0,0,0
//                if (elementStyling != null) {
//                    propList.addAll(elementStyling.getPropertyDeclarations());
//                }
//                if (propList.isEmpty())
//                    cs = CascadedStyle.emptyCascadedStyle;
//                else {
//                    cs = new CascadedStyle(propList.iterator());
//                }
//
//                result = cs;
//            }
//            return result;
//        }
//
//        /**
//         * May return null.
//         * We assume that restyle has already been done by a getCascadedStyle if necessary.
//         */
//        public CascadedStyle getPECascadedStyle(Object e, String pseudoElement) {
//            java.util.Iterator si = pseudoSelectors.entrySet().iterator();
//            if (!si.hasNext()) {
//                return null;
//            }
//            CascadedStyle cs = null;
//            java.util.List pe = (java.util.List) pseudoSelectors.get(pseudoElement);
//            if (pe == null) return null;
//
//            java.util.List propList = new java.util.LinkedList();
//            for (java.util.Iterator i = getSelectedRulesets(pe); i.hasNext();) {
//                org.xhtmlrenderer.css.sheet.Ruleset rs = (org.xhtmlrenderer.css.sheet.Ruleset) i.next();
//                propList.addAll(rs.getPropertyDeclarations());
//            }
//            if (propList.size() == 0)
//                cs = CascadedStyle.emptyCascadedStyle;//already internalized
//            else {
//                cs = new CascadedStyle(propList.iterator());
//            }
//            return cs;
//        }
//
//    }

}

