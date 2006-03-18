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

import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import greenfoot.GreenfootImage;

import java.util.HashMap;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;

public class Actor extends GreenfootObject
{
 /* private Grid<Actor> grid;
   private Location location;*/
   private int direction;
   private Color color;
    private HashMap tintedImages = new HashMap();

   /**
    * Constructs a blue actor that is facing north.
    */
   public Actor()
   {
      setImage("images/Actor.gif");
      direction = Location.NORTH;
      color  = Color.BLUE;
   }

   /**
    * Override this method to define the action of this actor.
    * By default, an actor acts by flipping its direction.
    */
   public void act() 
   { 
      setDirection(getDirection() + Location.HALF_CIRCLE); 
   }
   
   /**
    * Gets the location of this actor
    * <br>Precondition: This actor is in a grid
    * @return this actor's location
    */
   public Location getLocation()
   {
      return new Location(getY(), getX()); //location;
   }
   
   /**
    * Gets the grid in which this actor is located
    * @return the grid, or null if this actor is not contained in a grid
    */
   public Grid<Actor> getGrid()
   {
      return (Grid<Actor>) getWorld(); //grid;
   }
   
   /**
    * Puts this actor into a grid. If there is another actor at the given location, 
    * it is removed.
    * <br>Precondition: This actor is not in a grid
    * @param gr the grid into which this actor should be placed
    * @param loc the location into which the actor should be placed (must be valid)
    */
   public void putSelfInGrid(Grid<Actor> gr, Location loc)
   {
       getWorld().addObject(this);
       this.setLocation(loc.col(), loc.row());
      /*if (grid != null)
         throw new IllegalStateException("This actor is already in a grid.");
      
      Actor actor = gr.get(loc);
      if (actor != null) 
         actor.removeSelfFromGrid();
      gr.put(loc, this);
      grid = gr;
      location = loc;*/
   }
   
   /**
    * Removes an actor from a grid. 
    * <br>Precondition: This actor has been put in a grid.
    */
   public void removeSelfFromGrid()
   {
       if(getWorld() != null) {
           getWorld().removeObject(this);
        }
     /* if (grid == null)
         throw new IllegalStateException("This actor is not in a grid.");
      if (grid.get(location) != this)
         throw new IllegalStateException("The grid contains a different actor at location " + location + ".");
      
      grid.remove(location);
      grid = null;
      location = null;*/
   }
   
   /**
    * Moves this actor to a new location. If there is another actor 
    * at the given location, it is removed.
    * <br>Precondition: (1) This actor has been put in a grid
    * (2) the new location is valid
    * @param newLocation the new location
    */
   public void moveTo(Location newLocation)
   {
       if(getX() == newLocation.col() && getY() == newLocation.row()) {
           return;
        }
        Location location = new Location(getY(), getX());
       getGrid().remove(location);
      Actor other = getGrid().get(newLocation);
      if (other != null) 
         other.removeSelfFromGrid();
      location = newLocation;
      getGrid().put(location, this);
    /*   if(getX() == newLocation.col() && getY() == newLocation.row()) {
           return;
        }
       Actor other = (Actor) getOneObjectAt(newLocation.col(),newLocation.row(),null);
       if(other != null) {
           System.out.println("Removing in moveTo: " + other);
           other.removeSelfFromGrid();
       }
       super.setLocation(newLocation.col(), newLocation.row());*/
    /*  if (grid == null)
         throw new IllegalStateException("This actor is not in a grid.");
      if (grid.get(location) != this)
         throw new IllegalStateException("The grid contains a different actor at location " + location + ".");
      if (!grid.isValid(newLocation))
         throw new IllegalArgumentException("Location " + newLocation + " is not valid.");
      
      if (newLocation.equals(location)) 
         return;
      grid.remove(location);
      Actor other = grid.get(newLocation);
      if (other != null) 
         other.removeSelfFromGrid();
      location = newLocation;
      grid.put(location, this);*/
   }
   
   /**
    * Gets the current direction of this actor.
    * @return the direction in { 0, 1, ..., 359 }
    */
   public int getDirection()
   {
      return direction;
   }

   /**
    * Sets the current direction of this actor. The direction
    * will be converted to a value in { 0, 1, ..., 359 }
    * @param newDirection the new direction 
    */
   public void setDirection(int newDirection)
   {
      direction = newDirection % Location.FULL_CIRCLE;
      if (direction < 0)
         direction += Location.FULL_CIRCLE;
         
      super.setRotation(direction);
   }
   
   /**
    * Gets the color of this actor.
    * @return the color
    */
   public Color getColor()
   {
      return color;
   }
  
   /**
    * Sets the color of this actor.
    * @param newColor the new color
    */
   public void setColor(Color newColor)
   {
      color = newColor;
   }
   
    public GreenfootImage getImage()
    {
        if(color != null) {
            return tinted();
        }
        else {
            return super.getImage();
        }
    }

    
    
    private GreenfootImage tinted()
    {
        
        GreenfootImage tinted = (GreenfootImage) tintedImages.get(color);
        if (tinted == null) // not cached, need new filter for color
        {
           FilteredImageSource src = new FilteredImageSource(super.getImage().getAWTImage().getSource(), new TintFilter(color));
           tinted = new GreenfootImage(Toolkit.getDefaultToolkit().createImage(src));
           // Cache tinted image in map by color, we're likely to need it again.
           tintedImages.put(color, tinted);
        }
        return tinted;
    }

    
      
   public String toString()
   {
      return getClass().getName() 
         + "[location=" + "(" +getX()+ "," + getY() + ")" 
         + ",direction=" + direction
         + ",color=" + color + "]";
   } 
    
    /**
     * An image filter class that tints colors based on the tint provided to the
     * constructor (the color of an object).
     */
    private static class TintFilter extends RGBImageFilter
    {
       private int tintR, tintG, tintB;

       /** Constructs an image filter for tinting colors in an image. * */
       public TintFilter(Color color)
       {
          canFilterIndexColorModel = true;
          int rgb = color.getRGB();
          tintR = (rgb >> 16) & 0xff;
          tintG = (rgb >> 8) & 0xff;
          tintB = rgb & 0xff;
       }

       public int filterRGB(int x, int y, int argb)
       {
          // Separate pixel into its RGB coomponents.
          int alpha = (argb >> 24) & 0xff;
          int red = (argb >> 16) & 0xff;
          int green = (argb >> 8) & 0xff;
          int blue = argb & 0xff;

          // Use NTSC/PAL algorithm to convert RGB to grayscale.
          int lum = (int) (0.2989 * red + 0.5866 * green + 0.1144 * blue);

          // Interpolate along spectrum black->white with tint at midpoint
          double scale = Math.abs((lum - 128) / 128.0); // absolute distance
                                                          // from midpt
          int edge = lum < 128 ? 0 : 255; // going towards white or black?
          red = tintR + (int) ((edge - tintR) * scale); // scale from midpt to
                                                          // edge
          green = tintG + (int) ((edge - tintG) * scale);
          blue = tintB + (int) ((edge - tintB) * scale);
          return (alpha << 24) | (red << 16) | (green << 8) | blue;
       }

    }

   
}