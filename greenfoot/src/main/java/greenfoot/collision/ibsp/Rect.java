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
package greenfoot.collision.ibsp;

final public class Rect
{
    private int x, y, width, height;
    
    public Rect(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public void copyFrom(Rect other)
    {
        this.x = other.x;
        this.y = other.y;
        this.width = other.width;
        this.height = other.height;
    }
    
    public String toString()
    {
        return ("rect (" + x + "," + y + ")-(" + (x + width) + "," + (y + height) + ")");
    }
    
    public int getX()
    {
        return x;
    }
    
    public int getMiddleX()
    {
        return x + width / 2;
    }
    
    public int getRight()
    {
        return x + width;
    }
    
    public int getY()
    {
        return y;
    }
        
    public int getMiddleY()
    {
        return y + height / 2;
    }
    
    public int getTop()
    {
        return y + height;
    }

    public int getWidth()
    {
        return width;
    }
    
    public int getHeight()
    {
        return height;
    }
    
    public boolean contains(Rect other)
    {
        return (x <= other.x &&
                y <= other.y &&
                getTop() >= other.getTop() &&
                getRight() >= other.getRight());
    }

    public static Rect getIntersection(Rect a, Rect b)
    {
        int a_x = a.getX();
        int a_r = a.getRight();
        int a_y = a.getY();
        int a_t = a.getTop();
        
        int b_x = b.getX();
        int b_r = b.getRight();
        int b_y = b.getY();
        int b_t = b.getTop();
        
        // Calculate intersection
        int i_x = Math.max(a_x, b_x);
        int i_r = Math.min(a_r, b_r);
        int i_y = Math.max(a_y, b_y);
        int i_t = Math.min(a_t, b_t);
        if (i_x >= i_r || i_y >= i_t) {
            return null;
        }
        else {
            return new Rect(i_x, i_y, i_r - i_x, i_t - i_y);
        }
    }
    
    public static boolean equals(Rect a, Rect b)
    {
        return a.x == b.x && a.y == b.y &&
            a.width == b.width && a.height == b.height;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    public boolean intersects(Rect otherBounds)
    {        
        if (otherBounds.x >= getRight()) {
            return false;
        } else if (x >= otherBounds.getRight()) {
            return false;
        }         
        if (otherBounds.y >= getTop()) {
            return false;
        } else if (y >= otherBounds.getTop()) {
            return false;
        }
        return true;
    }
}
