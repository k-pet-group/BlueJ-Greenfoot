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
 * 
 * Representation of a circle.
 * 
 * @author Poul Henriksen
 *
 */
public class Circle
{
    private int x;
    private int y;
    private int radius;
    
    public Circle(int x, int y, int r) {
        if(r < 0 ) {
            throw new IllegalArgumentException("Radius must be larger than -1. It was: " + r);
        }
        this.setX(x);
        this.setY(y);
        setRadius(r);
    }
    
    public Circle() {
        
    }

    public boolean equals(Object other)
    {
        if (other instanceof Circle) {
            Circle oCircle = (Circle) other;
            return oCircle.x == x && oCircle.y == y && oCircle.radius == radius;
        }
        return false;
    }
    
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + x;
        result = 37 * result + y;
        result = 37 * result + radius;
        return result;
    }
    
    public double getVolume()
    {
        return Math.PI * radius * radius;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public int getX()
    {
        return x;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    public int getY()
    {
        return y;
    }

    public void setRadius(int radius)
    {
        this.radius = radius;
    }

    public int getRadius()
    {
        return radius;
    }

    /**
     * Checks if this circles intersects the other circle.
     */
    public boolean intersects(Circle other)
    {
        int r1 = getRadius();
        int r2 = other.getRadius();
                    
        int dx = getX() - other.getX();
        int dy = getY() - other.getY();
        int dxSq = dx * dx;
        int dySq = dy * dy;
        
        int  circleDistSq = (dxSq + dySq);
        
        if( (r1 + r2) * (r1 + r2) >= circleDistSq) {
            return true;
        } else {
            return false;
        }            
    }

    /**
     * Calculates the circle that bounds this circle and the other.
     * 
     * @return An new circle bounding this and other.
     */
 /*   public Circle merge(Circle other)
    {
        Circle mCircle = new Circle();
        mCircle.merge(this, other);
       if(true)
           return mCircle;
    
        
        int dx = getX() - other.getX();
        int dy = getY() - other.getY();
        int dxSq = dx * dx;
        int dySq = dy * dy;        
        int circleDistSq = (dxSq + dySq);
        int r2 = (getRadius() - other.getRadius());
        

        Circle newCircle = null;
        //check if r1 encloses r2
        if( r2*r2 >= circleDistSq) {
            if(getRadius() < other.getRadius()) {
                newCircle= new Circle(other.getX(), other.getY(), other.getRadius());
            } else {
                newCircle = new Circle(x, y, radius);
            } 
        } else {        
            double circleDist =  Math.sqrt(circleDistSq);
            double r =  (circleDist + getRadius() + other.getRadius()) / 2.;
            
            newCircle = new Circle(getX(), getY(), (int) Math.ceil(r));
            if(circleDist > 0) {
                double f = ((r - getRadius()) / circleDist);                
                
                newCircle.setX(newCircle.getX() - ((int) Math.ceil(f * dx)));
                newCircle.setY(newCircle.getY() - ((int) Math.ceil(f * dy)));
            }            
        }
        System.out.println(newCircle);
        System.out.println(mCircle);
        System.out.println("----------");
        return newCircle;
    }    
    */
    /**
     * Calculates the circle that bounds this circle and the other.
     * 
     */
    public void merge(Circle one, Circle two)
    {
        int dx = one.getX() - two.getX();
        int dy = one.getY() - two.getY();
        int dxSq = dx * dx;
        int dySq = dy * dy;        
        int circleDistSq = (dxSq + dySq);
        int r2 = (one.getRadius() - two.getRadius());
        
        //check if r1 encloses r2
        if( r2*r2 >= circleDistSq) {
            if(one.getRadius() < two.getRadius()) {
                setRadius(two.getRadius());
                setX(two.getX());
                setY(two.getY());
                //biggest = new Circle(two.getX(), two.getY(), two.getRadius());
            } else {
                setRadius(one.getRadius());
                setX(one.getX());
                setY(one.getY());
                //biggest = new Circle(x, y, radius);
            } 
        }
        else {
            double circleDist = Math.sqrt(circleDistSq);
            double r = (circleDist + one.getRadius() + two.getRadius()) / 2.;
            // Circle newCircle = new Circle(getX(), getY(), (int)
            // Math.ceil(r));
            setRadius((int) Math.ceil(r));
            if (circleDist > 0) {
                double f = ((r - one.getRadius()) / circleDist);
                setX(one.getX() - ((int) Math.ceil(f * dx)));
                setY(one.getY() - ((int) Math.ceil(f * dy)));
            } else {
                setX(one.getX());
                setY(one.getY());
            }
        }
    }
    
    public String toString() {
        return "(" + x + "," + y + ") [" + radius +"]" + super.toString();
    }
}
