/**
 ** @author Michael Koelling
 ** @version 1.0
 ** @date 2 January 1997
 **
 ** A fish in a simulation.
 **
 **  This class is used to create fish objects in an environmental simulation.
 **  Fish can move, propagate and be displayed on screen.  They have a 
 **  position.
 **
 **  Fish swim around randomly to a neighbouring position that is free.  A fish
 **  has to move to live (it must find plankton!).  If all four directions 
 **  around the fish are blocked, it cannot move and dies.
 **/

import bluej.runtime.Terminal;
import java.util.Random;

public class Fish extends Creature
{
	static final String sign = "+";
	
	int timeToPropagate;

	/**
	 ** Create a a fish at position (x, y)
	 **/
	public Fish(World world, int x, int y)
	{
		super(world, x, y);
		timeToPropagate = Simulator.random(0, world.fishPropTime + 2);
	}

	/**
	 ** It is time for this fish to act.  The fish swims around, 
	 ** propagating from time to time, hoping not to get eaten.
	 **/
	boolean act()
	{
		if(dead)
			return false;
		if(timeToPropagate == 0)
		{
			move(true);
			timeToPropagate = world.fishPropTime;
		}
		else
		{
			move(false);
			--timeToPropagate;
		}
		return true;
	}
	
	/**
	 ** Show this creature on screen.  It is assumed that the screen
	 ** cursor is at the position where it is to be shown.  No positioning
	 ** takes place, just shows the creatures signature.
	 **/
	void show()
	{
		Terminal.print(sign);
	}
	
	/**
	 ** Try to move somewhere.  Trying random directions.  Just remain
	 ** still if all neighbouring fields are full.
	 **/
	private void move(boolean propagate)
	{
		int dir;
		boolean done = false;
		
		dir = Simulator.random(0, 4);	// start with a random direction
		for(int _try = 0; (_try < 4) && !done; _try++)
		{
			switch((dir + _try) % 4)
			{
			case 0:
				done = moveTo(x + 1, y, propagate);	// right
				break;
				
			case 1:
				done = moveTo(x, y + 1, propagate);	// down
				break;
				
			case 2:
				done = moveTo(x - 1, y, propagate);	// left
				break;
				
			case 3:
				done = moveTo(x, y - 1, propagate);	// up
				break;
			}
		}
		
		if(!done)		// a fish that cannot move dies
			die();
	}
	
	private boolean moveTo(int new_x, int new_y, boolean propagate)
	{
		Fish child;
		
		// cope with wraparound
		new_x = (new_x + world.columns) % world.columns;
		new_y = (new_y + world.rows) % world.rows;
		
		if(world.lookAt(new_x, new_y) != null)	// already occupied
			return false;
		
		// otherwise, the space is free
		if(propagate)
			new Fish(world, new_x, new_y);
		else
		{
			world.moveCreature(x, y, new_x, new_y);
			x = new_x;
			y = new_y;
		}
		
		return true;
	}
}
