/**
 ** The world - this is where all creatures swim around.
 **
 ** @author Michael Kolling
 ** @version 1.0
 **
 ** This class represents the world.  The world is displyed as a grid on 
 ** screen, but it really is a torus: the top of the screen is connected to 
 ** the bottom, and the left side to the right, and creatures can move across
 ** the edges and reappear on the opposite side.
 ** 
 ** The world is filled with water (sorry, no land on our world).  Only water
 ** animals can exist.
 ** 
 ** The world defines the laws of nature (e.g. in what time intervals 
 ** creatures propagate, when they starve, etc.) and it is the place where
 ** creatures live.  It provides a coordinate system over its surface, and 
 ** each existing creature is always at one of those coordinates.
 **/

import java.util.*;
import bluej.runtime.Terminal;

public class World
{
	final int columns = 78;
	final int rows = 20;
	
	Creature[][] world;			// the world - 2d array
	Vector creatures;			// list of creatures in the world
	int num_sharks;				// number of sharks currently alive
	int num_fish;				// number of fish currently alive
	int fishPropTime = 4;		// time taken for fish to propagate
	int sharkPropTime = 12;		// time take for sharks to propagate
	int sharkStarveTime = 7;		// time taken for sharks to starve

	/**
	 ** Constructor for objects of class World
	 **/
	public World()
	{
		initWorld();
		creatures = new Vector();
		num_sharks = 0;
		num_fish = 0;
	}
	
	/**
	 ** Build the world (empty)
	 **/
	void initWorld()
	{
		world = new Creature[columns][rows];
		setParams(fishPropTime, sharkPropTime, sharkStarveTime);
	}
	
	/**
	 ** Time advances.  Give all creatures the chance to do something.
	 **/
	void timeTick()
	{
		for(int i = 0; i < creatures.size(); /* */)
		{
			Creature creature = (Creature)creatures.elementAt(i);
			
			if(!creature.act())	// if it doesn't act, it's dead
			{
				creatures.removeElementAt(i);
				if(creature instanceof Fish)
					--num_fish;
				else if(creature instanceof Shark)
					--num_sharks;
			}
			else
				++i;
		}
	}
	
	/**
	 ** Show the current state of the world on screen
	 **/
	public void show()
	{
		Terminal.cursorTo (0,0);
		
		for(int y = 0; y < rows; y++)
		{
			for(int x = 0; x < columns; x++)
			{
				if(world[x][y] == null)
					Terminal.print(" ");
				else
					world[x][y].show();
			}
			Terminal.print("\n");
		}
			
		Terminal.cursorTo(0, 22);
		Terminal.print("fish: " + num_fish + "  ");
		Terminal.cursorTo(15, 22);
		Terminal.print("sharks: " + num_sharks + "  ");
		Terminal.flush();	// make sure this makes it onto the screen
	}
	
	/**
	 ** Start life on earth
	 ** Time ends if either a key is pressed or one creature goes extinct.
	 **/
	public void startLife()
	{
		// To start life, we go into a loop in which we advance the time
		// (let all creatures act) and then redisplay the world.
		
		Terminal.showCursor(false);
		
		for(;;)
		{
			timeTick();
			show();
			if(Terminal.askChar())
			{
				Terminal.getChar();	// clear it
				break;
			}
			if((num_sharks == 0) || (num_fish == 0))
				break;
		}
		
		Terminal.showCursor(true);
	}
	
	/**
	 **
	 **/
	public void setParams(int fish_prop_time, int shark_prop_time, int shark_starve_time)
	{
		this.fishPropTime = fishPropTime;
		this.sharkPropTime = sharkPropTime;
		this.sharkStarveTime = sharkStarveTime;
		Terminal.cursorTo(0, 23);
		Terminal.print("fish prop: " + fishPropTime);
		Terminal.print("   shark prop: " + sharkPropTime);
		Terminal.print("   shark stavation time: " + sharkStarveTime);
		Terminal.print("  (press key to stop)");
		Terminal.flush();	// make sure this makes it onto the screen
	}
	
	/**
	 ** Place a creature at position (x,y)
	 **/
	void newCreature(Creature c, int x, int y)
	{
		world[x][y] = c;
		creatures.addElement(c);
		if(c instanceof Fish)
			++num_fish;
		else if(c instanceof Shark)
			++num_sharks;
	}
	
	/**
	 ** Move a creature to position (new_x,new_y)
	 **/
	void moveCreature(int old_x, int old_y, int new_x, int new_y)
	{
		world[new_x][new_y] = world[old_x][old_y];
		world[old_x][old_y] = null;
	}
	
	/**
	 ** Remove the creature at position (x,y)
	 **/
	void removeCreature(int x, int y)
	{
		world[x][y] = null;
	}
	
	Creature lookAt(int x, int y)
	{
		return world[x][y];
	}
}
