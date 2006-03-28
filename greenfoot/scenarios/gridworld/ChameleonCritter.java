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
 * @author Chris Nevison
 * @author Barbara Wells
 * @author Cay Horstmann
 */


import greenfoot.GreenfootImage;
import java.util.ArrayList;

/**
 * A chameleon critter takes on the color of neighboring actors as it moves
 * through the grid. 
 * <br />
 * The implementation of this class is testable on the AP CS A and AB exams. 
 */
public class ChameleonCritter extends Critter
{
    public ChameleonCritter() {  
        setGridActorImage("images/ChameleonCritter.gif");
    }

   /**
    * Randomly selects a neighbor and changes this critter's
    * color to be the same as that neighbor's. If there are no
    * neighbors, no action is taken.
    */
   public void processGridActors(ArrayList<GridActor> actors)
   {
      int n = actors.size();
      if (n == 0) return;
      int r = (int) (Math.random() * n);      
      
      GridActor other = actors.get(r);
      setColor(other.getColor());
   }
   
   public void makeMove(Location loc)
   {
      setDirection(getLocation().directionToward(loc));
      super.makeMove(loc);
   }
}
