package greenfoot;

/**
 * Various utility methods for greenfoot scenarios.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: Utilities.java 3238 2004-12-14 18:43:54Z polle $
 */
public class Utilities
{

    /**
     * Pauses for one time step and repaints.
     */
    public static void delay()
    {
        Simulation sim = Simulation.getInstance();
        if(sim != null) {
            sim.delay();
            repaint();
        }
    }

    /**
     * Request a repaint of the world.
     *  
     */
    public static void repaint()
    {
        Simulation sim = Simulation.getInstance();
        if(sim != null) {
            Simulation.getInstance().getWorldHandler().repaint();
        }
    }
}