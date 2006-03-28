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
 * 
 * Modified by Poul Henriksen to make it work with Greenfoot
 */

import greenfoot.World;
import greenfoot.Actor;

import java.awt.Color;


/**
 * A bug is an actor that can move and turn. It drops flowers
 * as it moves. Override a bug to draw shapes in the actor world.
 * <br />
 * The implementation of this class is testable on the AP CS A and AB exams. 
 */
public class Bug extends GridActor
{
   /**
    * Constructs a red bug.
    */
   public Bug()
   {
       setGridActorImage("images/Bug.gif");
      setColor(Color.RED);
   }

   /**
    * Constructs a bug of a given color.
    * @param color the color for this bug
    */
   public Bug(Color color)
   {
       setImage("images/Bug.gif");
      setColor(color);
   }
   
   public void act()
   {
      if (canMove())
         move();
      else
         turn();      
   }
   
   /**
    * Makes this bug turn by 45 degrees to the right.
    */
   public void turn()
   {
      setDirection(getDirection() + Location.HALF_RIGHT);
   }   
   
   /**
    * Moves this bug forward. The bug may replace other
    * actors or fall outside the grid. Call canMove to check
    * whether it is safe to move.
    */
   public void move()
   {
      Grid<GridActor> gr = getGrid();
      if (gr == null) 
         return;
      Location loc = getLocation();
      Location next = loc.getNeighborLocation(getDirection());
      if (gr.isValid(next))      
         moveTo(next);
      else
         removeSelfFromGrid();
      Flower flower = new Flower(getColor());
      flower.putSelfInGrid(gr, loc);
   }
   
   /**
    * Tests whether this bug can move forward. The bug must be
    * in a grid, the next forward location must be valid, and it must either
    * be empty or occupied by a flower. 
    * @return true if this bug can move.
    */
   public boolean canMove()
   {
      Grid<GridActor> gr = getGrid();
      if (gr == null) 
         return false;
      Location loc = getLocation();
      Location next = loc.getNeighborLocation(getDirection());
      if (!gr.isValid(next)) {
         return false;
        }
         GridActor neighbor = gr.get(next);
      return (neighbor == null) || (neighbor instanceof Flower);
      // ok to move into empty cell or onto flower
      // not ok to move onto any other actor            
   }
}
