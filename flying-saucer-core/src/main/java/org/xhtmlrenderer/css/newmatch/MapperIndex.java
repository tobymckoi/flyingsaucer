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

package org.xhtmlrenderer.css.newmatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.xhtmlrenderer.css.extend.AttributeResolver;
import org.xhtmlrenderer.css.extend.TreeResolver;

/**
 * A MapperIndex is a supplementary object for a Matcher.Mapper that provides
 * indexes for common selector queries. It is able to quickly find selectors
 * that match against a tag name, a 'class' attribute value or an 'id'
 * attribute value. Selectors with more complex matching conditions are
 * also indexed to be matched separately.
 * <p>
 * The intention of this object is to speed up element to selector mapping
 * operations. Once populated, this index can be shared between mapping
 * objects to provide the base query mechanism.
 *
 * @author Tobias Downer
 */
class MapperIndex {

    /**
     * The list of all selectors in specificity order.
     */
    private List<Selector> selectorOrder;

    /**
     * Class index maps from a class attribute name to all selectors that have
     * the class name as a condition.
     */
    private final List<NamedIndex> classIndex = new ArrayList();

    /**
     * ID index maps from an id attribute name to all selectors that have the
     * id name as a condition.
     */
    private final List<NamedIndex> idIndex = new ArrayList();

    /**
     * Tag index maps from a tag name to all selectors that match against the
     * given tag.
     */
    private final List<NamedIndex> tagIndex = new ArrayList();

    /**
     * Index of selectors with no tag name (matches all).
     */
    private final Set<Integer> noTagIndex = new HashSet();

    /**
     * Index of selectors with no conditions (matches all).
     */
    private final Set<Integer> noConditionsIndex = new HashSet();

    /**
     * All selectors that aren't matched against any of the above indexes.
     */
    private final Set<Integer> miscIndex = new HashSet();

    /**
     * Returns true if the conditions list contains any class and id
     * check conditions.
     *
     * @param conditions
     * @return
     */
    private boolean containsClassOrIdCondition(List<Condition> conditions) {
        for (Condition condition : conditions) {
            if (Condition.isClassCondition(condition) ||
                Condition.isIdCondition(condition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Populates the index with the given Selector collection. The collection
     * must be ordered by specificity.
     * <p>
     * This will typically only be called on the root set of selectors and this
     * object passed between children elements.
     *
     * @param values
     */
    void populate(Collection<Selector> values) {

        // Copy to an array list,
        selectorOrder = new ArrayList(values);

        int index = 0;

        for (Selector selector : selectorOrder) {

            // Populate tag name,
            String tagName = selector.getName();
            List<Condition> conditions = selector.getConditions();
            Selector sibling = selector.getSiblingSelector();
            int axes = selector.getAxis();

            // Axes must be descendant and selector must have no sibling,
            // Either no conditions a single class or id condition is eligible
            // for index.
            boolean conditionsContainsIdOrClass =
                    conditions != null && containsClassOrIdCondition(conditions);
            boolean eligible =
                    (axes == Selector.DESCENDANT_AXIS && sibling == null) &&
                    (conditions == null || conditionsContainsIdOrClass);

            // If eligible,
            if (eligible) {

                // Index by tag,
                if (tagName != null) {
                    tagIndex.add(new NamedIndex(index, tagName));
                }
                else {
                    noTagIndex.add(index);
                }

                // Index by condition,
                if (conditionsContainsIdOrClass) {
                    for (Condition c : conditions) {
                        if (Condition.isClassCondition(c)) {
                            classIndex.add(new NamedIndex(index,
                                        Condition.getClassNameOfCondition(c)));
                        }
                        else if (Condition.isIdCondition(c)) {
                            idIndex.add(new NamedIndex(index,
                                        Condition.getIDNameOfCondition(c)));
                        }
                    }
                }
                else {
                    noConditionsIndex.add(index);
                }

            }
            // Not eligible so add selector to outside index,
            else {

                miscIndex.add(index);

            }

            ++index;
        }

        // Sort the named indexes for easy searching,
        Collections.sort(classIndex);
        Collections.sort(idIndex);
        Collections.sort(tagIndex);

        // Indexes populated!

    }

    /**
     * Resolves an index value to a Selector object, using the specificity
     * lookup.
     *
     * @param selectorIndex
     * @return
     */
    Selector getSelector(Integer selectorIndex) {
        return selectorOrder.get(selectorIndex.intValue());
    }

//    private List<Integer> DEBUGoutput = null;

    /**
     * Returns a set of integers representing all the Selectors that *may*
     * match against the given element. This queries the information from the
     * indexes. The integers in the set can be resolved to their respective
     * Selector object by calling the 'getSelector' method.
     */
    List<Integer> getPossibleMatchedSelectors(Object e,
                AttributeResolver attrResolver, TreeResolver treeResolver) {

// [NOTE: This is approximately how fast the previous algorithm worked]
//        if (true) {
//            if (DEBUGoutput == null) {
//                DEBUGoutput = new ArrayList(selectorOrder.size());
//                for (int i = 0; i < selectorOrder.size(); ++i) {
//                    DEBUGoutput.add(i);
//                }
//            }
//            return DEBUGoutput;
//        }

        // Pull out information from the element,
        String classAttribute = attrResolver.getClass(e);
        String idAttribute = attrResolver.getID(e);
        String tagName = treeResolver.getElementName(e);

        if (classAttribute != null && classAttribute.trim().length() == 0) {
            classAttribute = null;
        }
        if (idAttribute != null && idAttribute.trim().length() == 0) {
            idAttribute = null;
        }

        // Union all the matched id and classes provided,

        // classIdSet = set of
        //   'noIdOrClassCondition' or 'id attribute' or 'class attribute'

        Set<Integer> classIdSet = noConditionsIndex;
        if (idAttribute != null) {
            classIdSet = unionQuerySet(classIdSet, idIndex, idAttribute);
        }
        if (classAttribute != null) {
            String[] classes = classAttribute.split(" ");
            for (String className : classes) {
                className = className.trim();
                if (!className.equals("")) {
                    classIdSet = unionQuerySet(classIdSet, classIndex, className);
                }
            }
        }

        // finalSet = set of
        //   (classIdSet intersect noTags) union (classIdSet intersect 'tag name')

        Set<Integer> fSet1 = intersectSet(classIdSet, noTagIndex);
        Set<Integer> fSet2 = intersectQuerySet(classIdSet, tagIndex, tagName);
        Set<Integer> finalSet = unionSet(fSet1, fSet2);

        // Add misc selectors and return,
        List<Integer> out = unionAndSort(finalSet, miscIndex);

        return out;

    }

    /**
     * Unions the given set with the index and returns a collection that's
     * sorted by the integer value.
     */
    private List<Integer> unionAndSort(
                                    Set<Integer> set, Set<Integer> indexList) {

        // If set is null by this point, there must be something bad with a
        // selector?
        if (set == null) {
            throw new IllegalStateException("set == null");
        }

        List<Integer> outList = new ArrayList(set.size() + indexList.size());

        // Add all from the index,
        for (Integer indexInteger : indexList) {
            // Don't add to the list if it's in the set,
            if (!set.contains(indexInteger)) {
                outList.add(indexInteger);
            }
        }
        // Add all from the set,
        outList.addAll(set);

        // Sort the list and return it,
        Collections.sort(outList);
        return outList;

    }

    /**
     * Returns the intersection of the given sets.
     */
    private Set<Integer> intersectSet(Set<Integer> set1, Set<Integer> set2) {

        if (set1 == null) {
            return set2;
        }
        else {
            if (set2 == null) {
                return set1;
            }
            Set<Integer> outSet = new HashSet();
            Set<Integer> smaller, larger;
            if (set1.size() < set2.size()) {
                smaller = set1;
                larger = set2;
            }
            else {
                smaller = set2;
                larger = set1;
            }
            for (Integer i : smaller) {
                if (larger.contains(i)) {
                    outSet.add(i);
                }
            }
            return outSet;
        }

    }

    /**
     * Returns a set of integer that unions the given set with the match names
     * from the given index.
     */
    private Set<Integer> unionSet(Set<Integer> set1, Set<Integer> set2) {
        Set<Integer> outSet = new HashSet(set1);
        outSet.addAll(set2);
        return outSet;
    }

    /**
     * Returns the intersection of the given set with the index.
     */
    private Set<Integer> intersectSet(Set<Integer> set, List<Index> indexList) {
        Set<Integer> outSet = new HashSet();
        // No input set,
        if (set == null) {
            for (Index i : indexList) {
                outSet.add(i.getIndex());
            }
        }
        // Otherwise, find the intersection,
        else {
            for (Index i : indexList) {
                int namedIndexInteger = i.getIndex();
                if (set.contains(namedIndexInteger)) {
                    outSet.add(i.getIndex());
                }
            }
        }
        return outSet;
    }

    /**
     * Performs a binary search on the given index. Returns the index of the
     * first element that matches the name.
     */
    private int binarySearch(List<NamedIndex> index, String name) {
        int low = 0;
        int high = index.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            NamedIndex midVal = index.get(mid);
            int cmp = midVal.getName().compareTo(name);

            if (cmp < 0) {
                low = mid + 1;
            }
            else if (cmp > 0) {
                high = mid - 1;
            }
            else {
                if (low == high) {
                    return low;
                }
                high = mid;
            }
        }

        // Not found,
        return -(low + 1);
    }

    /**
     * Returns a set of integer that unions the given set with the match names
     * from the given index.
     */
    private Set<Integer> unionQuerySet(Set<Integer> set,
                                        List<NamedIndex> index, String name) {

        // Binary search to find the first entry that matches the given name
        int i = binarySearch(index, name);
        if (i >= 0) {

            // Make a new hash set and add all the matched items,
            Set<Integer> outSet = new HashSet(set);
            int size = index.size();

            while (i < size) {
                NamedIndex ni = index.get(i);
                if (ni.getName().equals(name)) {

                    int namedIndexInteger = ni.getIndex();
                    outSet.add(namedIndexInteger);

                }
                else {
                    break;
                }
                ++i;
            }

            return outSet;

        }
        else {

            // Not found, so return the input set (nothing to union with),
            return set;

        }

    }

    /**
     * Returns a set of integers that match names from the given index.
     */
    private Set<Integer> intersectQuerySet(
                Collection<Integer> set, List<NamedIndex> index, String name) {

        // Binary search to find the first entry that matches the given name
        int i = binarySearch(index, name);
        if (i >= 0) {

            // Make a new hash set and add all the matched items,
            Set<Integer> outSet = new HashSet(128);
            int size = index.size();

            while (i < size) {
                NamedIndex ni = index.get(i);
                if (ni.getName().equals(name)) {

                    int namedIndexInteger = ni.getIndex();

                    // If no input set then add the named index integer to the
                    // output set,
                    if (set == null) {
                        outSet.add(namedIndexInteger);
                    }
                    // Otherwise, only add it to the output set if it's also
                    // in the input set,
                    else {
                        if (set.contains(namedIndexInteger)) {
                            outSet.add(namedIndexInteger);
                        }
                    }

                }
                else {
                    break;
                }
                ++i;
            }

            return outSet;

        }
        else {

            // Not found, so return empty set,
            return Collections.EMPTY_SET;

        }

    }





    // -----

    /**
     * An indexed selector.
     */
    private static class Index {

        // Specificity order index,
        private final int index;

        public Index(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public String toString() {
            return "[" + index + "] SELECTOR ";
        }

    }

    /**
     * An indexed selector with a named string associated.
     */
    private static class NamedIndex extends Index implements Comparable<NamedIndex> {

        // The named item,
        private final String name;

        public NamedIndex(int index, String name) {
            super(index);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        // Comparison is based on the name of this index entry,
        @Override
        public int compareTo(NamedIndex o) {
            return name.compareTo(o.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NamedIndex other = (NamedIndex) obj;
            return name.equals(other.name);
        }

        @Override
        public String toString() {
            return super.toString() + " Name: " + name;
        }

    }

}
