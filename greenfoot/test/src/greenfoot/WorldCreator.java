package greenfoot;

import greenfoot.core.WorldHandler;

public class WorldCreator
{
    public static World createWorld(int width, int height, int cellSize) {
        World world = new World(width, height, cellSize) {};
        WorldHandler.initialise();
        WorldHandler.getInstance().setWorld(world);
        return world;
    }
}
