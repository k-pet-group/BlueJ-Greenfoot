/**
 ** @author Michael Koelling
 ** @version 1.0
 **
 ** A creature in a simulation - superclass for specific creatures.
 **
 **  This class serves as an abstract superclass for all creatures on our
 **  world.  It defines the common characteristics to all creatures.  They
 **  are these: each creature
 **
 **	- has a position on the world
 **	- can act (do something - what it does is up to the creature itself)
 **	- can die
 **	- can be shown on screen at its position
 **/

import java.util.Random;
 
public abstract class Creature
{
	int x;
	int y;
	World world;
	boolean dead;

	/**
	 ** Create a a creature at position (x,y)
	 **/
	Creature(World world, int x, int y)
	{
		this.world = world;
		this.x = x;
		this.y = y;
		this.dead = false;
		world.newCreature(this, x, y);
	}
	
	/**
	 ** It is time for this creature to act.  It is up to the creature
	 ** what exactly it does.  It can move, multiply, or do any other 
	 ** thing it can think of...
	 ** If the creature acted, it returns true.  If it returns false it
	 ** is dead.
	 **/
	abstract boolean act();
	
	/**
	 ** Die.  Be dead.  Do not continue living.
	 **/
	void die()
	{
		// Just remove creature from map and mark it as being dead. This
		// will cause it to be removed from the list of creatures later.
		world.removeCreature(x, y);
		dead = true;
	}
	
	/**
	 ** Show this creature on screen.  It is assumed that the screen
	 ** cursor is at the position where it is to be shown.  No positioning
	 ** takes place, just shows the creatures signature.
	 **/
	abstract void show();
}
