/**
 ** @author Michael Koelling
 ** @version 1.0
 ** @date 2 January 1997
 **
 ** Short:   A shark in a simulation
 **
 **  This class is used to create shark objects in an environmental simulation.
 **  Sharks can move, propagate and be displayed on screen.  They have to eat
 **  fish.  Sharks look around them (just at the immediate neighbouring 
 **  positions on the planet) for fish - if they find one, they are happy.  If
 **  not, they swim around in the search of fish.  If they do not find fish to
 **  eat within a certain time (defined in the "World" class and returned via 
 **  the routine "sharkStarvation"), they die of starvation.
 **/

import bluej.runtime.Terminal;
import java.util.Random;

public class Shark extends Creature
{
	static final String sign = "D";
	
	int timeToPropagate;
	int timeToFeed;

	/**
	 ** Create a a shark at position (x, y)
	 **/
	public Shark(World world, int x, int y)
	{
		super(world, x, y);
		timeToPropagate = Simulator.random(0, world.sharkPropTime + 3);
		timeToFeed = world.sharkStarveTime - 2;
	}

	/**
	 ** It is time for this shark to act.  The shark swims around, looking
	 ** for fish to eat, propagating from time to time...
	 ** Return false if shark has died.
	 **/
	boolean act()
	{
		if(dead)
			return false;
		if(--timeToFeed < 0)
		{
			die();
			return false;
		}
		
		if(timeToPropagate == 0)
		{
			move(true);
			timeToPropagate = world.sharkPropTime;
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
	private boolean move(boolean propagate, boolean look_fish)
	{
		int dir;
		boolean done = false;
		
		dir = Simulator.random(0, 4);	// start with a random direction
		for(int _try = 0; _try < 4 && !done; _try++)
		{
			switch((dir + _try) % 4)
			{
			case 0:
				done = moveTo(x + 1, y, propagate, look_fish);	// right
				break;

			case 1:
				done = moveTo(x, y + 1, propagate, look_fish);	// down
				break;
				
			case 2:
				done = moveTo(x - 1, y, propagate, look_fish);	// left
				break;
				
			case 3:
				done = moveTo(x, y - 1, propagate, look_fish);	// up
				break;
			}
		}
		
		return done;
	}

	/**
	 ** Try to move somewhere.  Trying random directions.  Just remain
	 ** still if all neighbouring fields are full.
	 **/
	private void move(boolean propagate)
	{
		boolean look_fish = !propagate;
		
		boolean done = move(propagate, look_fish);
		if(!done && look_fish)	// if we didn't find a fish, just try to move
			done = move(propagate, false);
	}
	
	private boolean moveTo(int new_x, int new_y, boolean propagate, boolean look_fish)
	{
		Creature creature;
		
		// cope with wraparound
		new_x = (new_x + world.columns) % world.columns;
		new_y = (new_y + world.rows) % world.rows;
		
		creature = world.lookAt(new_x, new_y);
		
		if(look_fish)
		{
			if((creature != null) && (creature instanceof Fish))
			{
				// eat the fish
				creature.die();
				timeToFeed = world.sharkStarveTime;	// full now!
				world.moveCreature(x, y, new_x, new_y);
				x = new_x;
				y = new_y;
				return true;
			}
		}
		else if(creature == null)	// space is empty
		{
			if(propagate)
				new Shark(world, new_x, new_y);
			else
			{
				world.moveCreature(x, y, new_x, new_y);
				x = new_x;
				y = new_y;
			}
			return true;
		}
				
		return false;
	}
}
