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
package greenfoot.util;

/**
 * A location in integers
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: Location.java 6216 2009-03-30 13:41:07Z polle $
 */
public class Location
    implements Cloneable
{
    private int x;
    private int y;

    public Location()
    {
        x = 0;
        y = 0;
    }

    public Location(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    /**
     * Multiplies the location with the parameters.
     * 
     * @param xMappingScale
     * @param yMappingScale
     */
    public void scale(int xMappingScale, int yMappingScale)
    {
        x *= xMappingScale;
        y *= yMappingScale;
    }

    public Object clone()
    {
        Object o = null;
        try {
            o = super.clone();
        }
        catch (CloneNotSupportedException e) {}
        return o;
    }

    /**
     * Adds the parameters to the location.
     * 
     * @param dx
     * @param dy
     */
    public void add(int dx, int dy)
    {
        x += dx;
        y += dy;
    }

    public String toString()
    {
        String s = super.toString();
        s = s + " (" + getX() + ", " + getY() + ")";
        return s;
    }

}