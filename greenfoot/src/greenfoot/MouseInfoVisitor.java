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




/**
 * To get access to package private methods in MouseInfo.
 * 
 * @author Poul Henriksen
 *
 */
public class MouseInfoVisitor
{
    public static void setActor(MouseInfo info, Actor actor) {
        info.setActor(actor);
    }    

    public static void setLoc(MouseInfo info, int x, int y) {
        info.setLoc(x, y);
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
}
