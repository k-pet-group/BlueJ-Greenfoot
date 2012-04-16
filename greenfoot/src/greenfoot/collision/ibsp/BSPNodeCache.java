/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.collision.ibsp;

/**
 * A cache for BSP nodes, allowing object re-use. Might help reduce garbage collection
 * impact.
 * 
 * @author Davin McCall
 */
public class BSPNodeCache
{
    private static final int CACHE_SIZE = 1000;
    
    private static BSPNode [] cache = new BSPNode[CACHE_SIZE];
    private static int tail = 0;
    private static int size = 0;
    
    public static BSPNode getBSPNode()
    {
        if (size == 0) {
            return new BSPNode(new Rect(0,0,0,0), 0, 0);
        }
        else {
            int ppos = tail - size;
            if (ppos < 0) {
                ppos += CACHE_SIZE;
            }

            BSPNode node = cache[ppos];
            node.setParent(null);
            size--;
            return node;
        }
    }
    
    public static void returnNode(BSPNode node)
    {
        node.blankNode();
        cache[tail++] = node;
        if (tail == CACHE_SIZE) {
            tail = 0;
        }
        size = Math.min(size + 1, CACHE_SIZE);
        
        if (node.getLeft() != null || node.getRight() != null) {
            throw new RuntimeException("HHHHH!");
        }
        
    }
}
