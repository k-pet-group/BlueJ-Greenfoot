/**
 ** Class Simulator - controls the world and creates time
 **/

import java.util.Random;

public class Simulator
{
	public World earth;
	
	private static Random randomiser = new Random();
	
	public static int random(int min, int max)
	{
		return min + Math.abs(randomiser.nextInt() % (max - min));
	}

	/**
	 ** Constructor for the Simulator
	 **/
	public Simulator()
	{
	}

	/**
	 ** Create the world and all the creatures on it.  Everything is
	 ** created and shown on screen, but time does not yet exist.  To
	 ** start the life on earth, you have to create time, too...
	 **/
	public void createWorld()
	{
		earth = new World();
		createSharks(10);
		createFish(150);
		earth.show();
	}

	/**
	 ** Start life on earth.
	 **/
	public void createTime()
	{
		earth.startLife();
	}
	
	private void createSharks(int numSharks)
	{
		int w = earth.columns;
		int h = earth.rows;
		
		while(numSharks > 0)
		{
			int x = random(0, w);
			int y = random(0, h);
			
			if(earth.lookAt(x, y) == null)
			{
				new Shark(earth, x, y);
				--numSharks;
			}
		}
	}
	
	private void createFish(int numFish)
	{
		int w = earth.columns;
		int h = earth.rows;
		
		while(numFish > 0)
		{
			int x = random(0, w);
			int y = random(0, h);
			
			if(earth.lookAt(x, y) == null)
			{
				new Fish(earth, x, y);
				--numFish;
			}
		}
	}
}
