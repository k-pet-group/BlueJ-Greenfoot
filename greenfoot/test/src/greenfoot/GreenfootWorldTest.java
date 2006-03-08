package greenfoot;

import junit.framework.TestCase;

public class GreenfootWorldTest extends TestCase
{
    public void testSetWrapped() {
        GreenfootWorld world = new GreenfootWorld(10,10,10);
        world.setWrapped(true);
        assertTrue(world.isWrapped());
        world.setWrapped(false);
        assertFalse(world.isWrapped());
    }
}
