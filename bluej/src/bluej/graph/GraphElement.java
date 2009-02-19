/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.graph;

import java.awt.event.MouseEvent;

/**
 * An element in a Graph
 * @author fisker
 * 
 */
public abstract class GraphElement {
    
    /**
     * Remove this element from the graph.
     */
    abstract public void remove();
    
    /**
     * Subtypes of Graph elements must override this if it want GraphEditor
     * to be able to locate them. Only classes that can not be selected and that
     * doesn't have a popupmenu can made due with the default behavior.
     * @return
     */
    abstract public boolean contains(int x, int y);
    
    /**
     * A double click was done on this element.
     */
    public void doubleClick(MouseEvent evt) {}

    /**
     * Post the context menu for this target.
     */
    abstract public void popupMenu(int x, int y, GraphEditor graphEditor);
}
