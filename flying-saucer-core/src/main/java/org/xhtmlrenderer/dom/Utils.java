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

package org.xhtmlrenderer.dom;

/**
 *
 * @author Tobias Downer
 */
public class Utils {

    /**
     * Equality test with null handling.
     * 
     * @param ob1
     * @param ob2
     * @return 
     */
    public static boolean equals(Object ob1, Object ob2) {
        if (ob1 == ob2) {
            return true;
        }
        else if (ob1 == null && ob2 != null) {
            return false;
        }
        else if (ob1 != null && ob2 == null) {
            return false;
        }
        else {
            return ob1.equals(ob2);
        }
    }
    
    public static void dump(int indent, Node n) {
        
        System.out.print("                                             ".substring(0, indent));

        if (n instanceof CharacterData) {
            CharacterData cd = (CharacterData) n;
            String s = cd.getData();
            s = s.replace("\n", "\\n");
            s = s.replace("\r", "\\r");
            if (n instanceof TextNode) {
                System.out.println("TEXT: " + s);
            }
        }
        else {
        
            System.out.print(n.getNodeName());
            System.out.print(" ");
            System.out.println(n.getAttributes());
        }

        for (Node c : n.getChildNodes()) {
            dump(indent + 2, c);
        }

    }

    public static void checkParents(Node prev, Node cur) {
        if (!equals(prev, cur.getParentNode())) {
            throw new AssertionError();
        }
        for (Node n : cur.getChildNodes()) {
            checkParents(cur, n);
        }
    }

}
