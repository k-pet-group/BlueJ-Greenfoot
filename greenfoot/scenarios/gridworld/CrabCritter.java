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

import java.util.ArrayList;
import java.awt.Color;


import greenfoot.GreenfootImage;

/**
 * A crab critter looks at a limited set of neighbors when it eats and moves. 
 * <br />
 * This class is not tested on the AP CS A and AB exams. 
 */
public class CrabCritter extends Critter
{
   public CrabCritter()
   {       
       setGridActorImage("images/CrabCritter.gif");
       
      setColor(Color.RED);
   }

   /**
    * A crab gets the actors in the three cells immediately in front,
    * to its front-right and to its front-left
    * @return a list of actors occupying these locations
    */
   public ArrayList<GridActor> getGridActors()
   {
      ArrayList<GridActor> actors = new ArrayList<GridActor>();
      int[] dirs = { Location.AHEAD, Location.HALF_LEFT, Location.HALF_RIGHT };
      for (Location loc : findValidNeighborLocations(dirs))
      {
         GridActor a = getGrid().get(loc);
         if(a != null) 
            actors.add(a);         
      }

      return actors;
   }
   
   /**
    * @return list of empty locations immediately to the right and to the left
    */
   public ArrayList<Location> getMoveLocations()
   {
      ArrayList<Location> locs = new ArrayList<Location>();
      int[] dirs = { Location.LEFT, Location.RIGHT };
      for (Location loc : findValidNeighborLocations(dirs))
         if (getGrid().get(loc) == null)      
            locs.add(loc);

      return locs;
   }
      
   public void makeMove(Location loc) 
   {
      if (loc.equals(getLocation())) 
      {
         double r = Math.random();
         int angle;
         if (r < 0.5) 
            angle = Location.LEFT;
         else 
            angle = Location.RIGHT;
         setDirection(getDirection() + angle);
      }
      else
         super.makeMove(loc);
   }
}
