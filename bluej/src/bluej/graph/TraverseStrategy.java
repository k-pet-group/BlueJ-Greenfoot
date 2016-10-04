/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2016  Michael Kolling and John Rosenberg 
 
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

import javafx.scene.input.KeyCode;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.Target;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A strategy to move graph selections with keyboard input.
 * 
 * @author fisker
 */
@OnThread(Tag.FXPlatform)
public interface TraverseStrategy
{
    /**
     * Given a currently selected vertex and a key press, decide which vertex 
     * should be selected next.
     * 
     * @param graph  The graph we're looking at.
     * @param currentVertex  The currently selected vertex.
     * @param key  The key that was pressed.
     * @return     A vertex that should be selected now.
     */
    public Target findNextVertex(Package graph, Target currentVertex, KeyCode key);
}