package bluej.utility;

import junit.framework.TestCase;

public class EditDistanceTest extends TestCase
{
    public void testEditDistance()
    {
        assertEquals("geto-get == 1", 1, Utility.editDistance("geto", "get"));
        assertEquals("geto-gteo == 1", 1, Utility.editDistance("geto", "gteo"));
        assertEquals("edge-gteo == 4", 4, Utility.editDistance("edge", "gteo"));
    }
}
