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
 * @author Chris Nevison
 * @author Barbara Wells
 * 
 * 
 * Modified by Poul Henriksen to make it work with Greenfoot
 *
 */

public class BoxBug extends Bug
{
   private int steps;
   private int sideLength;

   public BoxBug(int n)
   {
      
      setImage("images/BoxBug.gif");
      sideLength = n;
   }
   
   public BoxBug()
   {       
      setImage("images/BoxBug.gif");
      sideLength = 4;
   }

   public void act()
   {
      if (steps < sideLength && canMove())
      {
         move();
         steps++;
      }
      else
      {
         turn();
         turn();
         steps = 0;
      }
   }
}