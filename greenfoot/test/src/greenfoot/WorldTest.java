package greenfoot;

import junit.framework.TestCase;

public class WorldTest extends TestCase
{
    public void testSetWrapped() {
        World world = new World(10,10,10);
        world.setWrapped(true);
        assertTrue(world.isWrapped());
        world.setWrapped(false);
        assertFalse(world.isWrapped());
    }
}
