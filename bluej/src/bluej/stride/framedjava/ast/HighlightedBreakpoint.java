/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.ast;

import javafx.scene.Node;

import threadchecker.OnThread;
import threadchecker.Tag;

public interface HighlightedBreakpoint
{
    @OnThread(Tag.FX)
    public void removeHighlight();

    @OnThread(Tag.FXPlatform)
    Node getNode();

    @OnThread(Tag.FXPlatform)
    double getYOffset();

    @OnThread(Tag.FXPlatform)
    double getYOffsetOfTurnBack();

    @OnThread(Tag.FXPlatform)
    boolean isBreakpointFrame();

    /**
     * Is this execution index in the execution history part
     * of the current loop?  This depends on the state
     * of the <1/3> arrows on the variable state.
     */
    @OnThread(Tag.FXPlatform)
    boolean showExec(int index);
}
