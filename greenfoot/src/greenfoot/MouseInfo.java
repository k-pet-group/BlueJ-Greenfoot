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
 * This class contains information about the current status of the mouse. You
 * can get a MouseInfo object via {@link Greenfoot#getMouseInfo()}.
 * 
 * @see Greenfoot#getMouseInfo()
 * @author Poul Henriksen
 * @version 2.4
 */
public class MouseInfo
{    
    private Actor actor;
    private int button;
    private int x;
    private int y;
    private int clickCount;;
    
    /**
     * Do not create your own MouseInfo objects. Use
     * {@link Greenfoot#getMouseInfo() getMouseInfo()}.
     * 
     * @see Greenfoot#getMouseInfo()
     */
    MouseInfo() {  }
    
    /**
     * Return the current x position of the mouse cursor.
     * 
     * @return the x position in grid coordinates
     */
    public int getX() 
    {
        return x;
    }

    /**
     * Return the current y position of the mouse cursor.
     * 
     * @return the y position in grid coordinates
     */
    public int getY() 
    {
        return y;
    }
    
    /**
     * Return the actor (if any) that the current mouse behaviour is related to.
     * If the mouse was clicked or pressed the actor it was clicked on will be
     * returned. If the mouse was dragged or a drag ended, the actor where the
     * drag started will be returned. If the mouse was moved, it will return the
     * actor that the mouse is currently over.
     * 
     * @return Actor that the current mouse behaviour relates to, or null if
     *         there is no actor related to current behaviour. 
     */
    public Actor getActor()
    {
        return actor;
    }
    
    /**
     * The number of the pressed or clicked button (if any).
     * 
     * @return The button number. Usually 1 is the left button, 2 is the middle
     *         button and 3 is the right button.
     */
    public int getButton() {
        return button;
    }

    /**
     * Return the number of mouse clicks associated with this mouse event.
     * @return The number of times a button has been clicked.
     */
    public int getClickCount() {
        return clickCount;
    }
    
    void setButton(int button)
    {
        this.button = button;
    }

    void setLoc(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    void setActor(Actor actor)
    {
        this.actor = actor;
    }

    void setClickCount(int clickCount)
    {
        this.clickCount = clickCount;
    }    
    
    public String toString() 
    {
        return "MouseInfo. Actor: " + actor + "  Location: (" + x + "," + y + ")  Button: " + button + " Click Count: " + clickCount;
    }
}
