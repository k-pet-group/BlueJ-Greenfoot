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

import greenfoot.World;
import greenfoot.Actor;


import java.awt.Color;


/**
 * Some actors drop flowers as they move. Flowers wilt by turning
 * black over time.
 * <br />
 * It has not been decided whether the implementation or API of this class 
 * is testable on the AP CS A and AB exams. 
 */

public class Flower extends GridActor
{
   private static final Color DEFAULT_COLOR = Color.PINK;
   private static final double WILT_FACTOR = 0.05; 
      // lose 5% of color value in each step  
   
   /**
    * Constructs a pink flower.
    */
   public Flower()
   {
       
      setImage("images/Flower.gif");
      setColor(DEFAULT_COLOR);
   }
   
   /**
    * Constructs a flower of a given color.
    * @param color the initial color of this flower
    */
   public Flower(Color color)
   {
       setImage("images/Flower.gif");
      setColor(color);
   }

   /**
    * When a flower acts, it "wilts", gradually losing its color.
    */
   public void act()
   {
      Color c = getColor();
      int red = (int) (c.getRed() * (1 - WILT_FACTOR));
      int green = (int) (c.getGreen() * (1 - WILT_FACTOR));
      int blue = (int) (c.getBlue() * (1 - WILT_FACTOR));
      
      setColor(new Color(red, green, blue));
   }
}
