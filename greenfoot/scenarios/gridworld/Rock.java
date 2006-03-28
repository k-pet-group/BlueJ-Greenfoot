/* 
 * AP(r) Computer Science GridWorld Case Study:
 * Copyright(c) 2005-2006 Cay S. Horstmann (http://horstmann.com)
 *
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * @author Cay Horstmann
 */


import java.awt.Color;


import greenfoot.GreenfootImage;

/**
 * A rock is an actor that does nothing. It is commonly used to 
 * block other actors from moving.
 * <br />
 * It has not been decided whether the implementation or API of this class 
 * is testable on the AP CS A and AB exams. 
 */

public class Rock extends GridActor 
{
   private static final Color DEFAULT_COLOR = Color.BLACK;

   /**
    * Constructs a black rock.
    */
   public Rock()
   {
      setGridActorImage("images/Rock.gif");
      setColor(DEFAULT_COLOR);
   }
   
   /**
    * Constructs a rock of a given color.
    * @param color the initial color of this rock
    */
   public Rock(Color color)
   {
       setImage("images/Rock.gif");
      setColor(color);
   }
   
   /**
    * This method overrides the act method in the GridActor class
    * to do nothing.
    */
   public void act()
   {     
   }     
}
