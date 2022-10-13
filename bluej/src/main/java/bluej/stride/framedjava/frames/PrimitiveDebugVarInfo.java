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
package bluej.stride.framedjava.frames;

import javafx.scene.Node;
import javafx.scene.control.Label;
import threadchecker.OnThread;
import threadchecker.Tag;

public class PrimitiveDebugVarInfo implements DebugVarInfo
{
    private final String value;

    @OnThread(Tag.Any)
    public PrimitiveDebugVarInfo(String value)
    {
        this.value = value;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public Node getDisplay(DebugVarInfo prev)
    {
        Label l = new Label(value);
        if (prev != null && !getInternalValueString().equals(prev.getInternalValueString()))
        {
            l.setStyle("-fx-font-weight: bold;-fx-underline:true;");
        }
        return l;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public String getInternalValueString()
    {
        return value;
    }

}
