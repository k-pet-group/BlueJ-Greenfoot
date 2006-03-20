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

import greenfoot.GreenfootImage;
import java.util.ArrayList;

/**
 * A critter is an actor that moves through its world, processing
 * its neighbors in some way and then picking a new location. 
 * Override this class to obtain critters with interesting behavior. 
 * <br />
 * The implementation of this class is testable on the AP CS A and AB exams. 
 */
public class Critter extends Actor
{
   public Critter() {
       setActorImage("images/Critter.gif");
    }
   /**
    * A critter acts by getting a list of its neighbors, processing them, 
    * getting locations to move to, selecting one of them, and moving
    * to the selected location.
    */
   public void act()
   {
      if (getGrid() == null) 
         return;
      ArrayList<Actor> actors = getActors();
      processActors(actors);
      ArrayList<Location> moveLocs = getMoveLocations();
      Location loc = selectMoveLocation(moveLocs);
      makeMove(loc);
   }
   
   /**
    * Get the actors for processing. Implemented to return 
    * the actors that occupy neighboring grid locations. Override this 
    * method for subclasses that look elsewhere for actors to process. 
    * @return a list of actors that are neighbors of this critter
    */
   public ArrayList<Actor> getActors()
   {
      return getGrid().getNeighbors(getLocation());
   }
   
   /**
    * Process the actors. This method is implemented to "eat" (i.e. remove)
    * all actors that are not rocks or critters. Override it in subclasses that process 
    * neighbors in a different way. 
    * @param actors the actors to be processed
    */
   public void processActors(ArrayList<Actor> actors)
   {
      for (Actor a : actors)
      {
         if (!(a instanceof Rock) && !(a instanceof Critter))
            a.removeSelfFromGrid();
      }
   }

   /**
    * Get the possible locations for the next move. Implemented to return
    * the empty neighboring locations. Override this method for subclasses
    * that look elsewhere for move locations. 
    * @return a list of possible locations for the next move
    */
   public ArrayList<Location> getMoveLocations()
   {
      return getGrid().getEmptyNeighborLocations(getLocation());
   }
   
   /**
    * Selects the location for the next move. Implemented to randomly
    * pick one of the possible locations, or to return the 
    * current location if locs has size 0. Override this method for 
    * subclasses that have another mechanism for selecting the 
    * next move location.
    * @param locs the possible locations for the next move
    * @return the location that was selected for the next move.
    */
   public Location selectMoveLocation(ArrayList<Location> locs)
   {
      int n = locs.size();
      if (n == 0) 
         return getLocation();
      int r = (int) (Math.random() * n);
      return locs.get(r); 
   }

   /**
    * Moves this critter to the given location. Implemented to call 
    * moveTo. Override this method for subclasses that want to carry
    * out other actions for moving (for example, turning or leaving
    * traces). 
    * @param loc the location to move to (must be valid)
    */
   public void makeMove(Location loc)
   {
      moveTo(loc);
   }
   
   /**
    * Finds the valid neighbor locations of this critter in different 
    * directions. 
    * @param directions - an array of directions (which are  relative to
    * the current direction)
    * @return a set of valid locations that are neighbors of the 
    * current location in the given directions
    */ 
   public ArrayList<Location> findValidNeighborLocations(int[] directions)
   {
      ArrayList<Location> locs = new ArrayList<Location>();
      Grid gr = getGrid();
      Location loc = getLocation();

      for (int d : directions)
      {
         Location neighborLoc = loc.getNeighborLocation(getDirection() + d);
         if (gr.isValid(neighborLoc))
            locs.add(neighborLoc);
      }      
      return locs;
   }
}
