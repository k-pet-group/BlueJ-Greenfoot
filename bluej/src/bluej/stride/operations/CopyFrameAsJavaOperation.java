/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2020 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.operations;

import java.util.Arrays;
import java.util.List;

import bluej.Config;
import bluej.stride.framedjava.frames.GreenfootFrameUtil;
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;

public class CopyFrameAsJavaOperation extends FrameOperation
{
    public CopyFrameAsJavaOperation(InteractionManager editor)
    {
        super(editor, "COPY-JAVA", Combine.ALL);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    protected void execute(List<Frame> frames)
    {
        GreenfootFrameUtil.doCopyAsJava(frames);
    }

    @Override
    public List<ItemLabel> getLabels()
    {
        return Arrays.asList(l(Config.getString("frame.operation.copy.as.java"), MenuItemOrder.COPY));
    }
}
