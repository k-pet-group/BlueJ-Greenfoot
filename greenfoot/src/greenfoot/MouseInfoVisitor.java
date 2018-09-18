/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot;


import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * To get access to package private methods in MouseInfo.
 * 
 * @author Poul Henriksen
 *
 */
@OnThread(Tag.Any)
public class MouseInfoVisitor
{
    public static void setActor(MouseInfo info, Actor actor)
    {
        info.setActor(actor);
    }    

    /**
     * Set the event location for a MouseInfo.
     * @param info   the mouseinfo object
     * @param x      the x-coordinate (in world cells)
     * @param y      the y-coordinate (in world cells)
     * @param px     the x-coordinate (in pixels)
     * @param py     the y-coordinate (in pixels)
     */
    public static void setLoc(MouseInfo info, int x, int y, int px, int py)
    {
        info.setLoc(x, y, px, py);
    }

    public static void setButton(MouseInfo info, int button)
    {
        info.setButton(button);
    }    
    
    public static MouseInfo newMouseInfo()
    {
        return new MouseInfo();
    }

    public static void setClickCount(MouseInfo mouseInfo, int clickCount)
    {
        mouseInfo.setClickCount(clickCount);
    }
    
    /**
     * Get the x-coordinate in pixels from a MouseInfo object.
     */
    public static int getPx(MouseInfo info)
    {
        return info.getPx();
    }
    
    /**
     * Get the y-coordinate in pixels from a MouseInfo object.
     */
    public static int getPy(MouseInfo info)
    {
        return info.getPy();
    }
}
