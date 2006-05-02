package greenfoot;

import greenfoot.core.KeyboardManager;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;


/**
 * This utility class provides methods to control the simulation
 * and interact with the system.
 * 
 * <h3>Key names</h3>
 * 
 * <p>Part of the functionality provided by this class is the ability to
 * retrieve keyboard input. The methods getKey() and isKeyDown() are used
 * for this and they return/understand the following key names:
 * 
 * <ul>
 * <li>"a", "b", .., "z" (alphabetical keys), "0".."9" (digits), most
 *     punctuation marks. getKey() also returns uppercase characters when
 *     appropriate.
 * <li>"up", "down", "left", "right" (the cursor keys)
 * <li>"enter", "space", "tab", "escape"
 * <li>"F1", "F2", .., "F12" (the function keys)
 * </ul>
 * 
 * @author Davin McCall
 * @version 0.6 dev
 * @cvs-version $Id: Greenfoot.java 4063 2006-05-02 10:14:42Z davmac $
 */
public class Greenfoot
{
    private static KeyboardManager keyboardManager = WorldHandler.instance().getKeyboardManager();
    
    /**
     * Get the most recently pressed key, since the last time this method was
     * called. If no key was pressed since this method was last called, it
     * will return null. If more than one key was pressed, this returns only
     * the most recently pressed key.
     * 
     * @return  The name of the most recently pressed key
     */
    public static String getKey()
    {
        return keyboardManager.getKey();
    }
    
    /**
     * Check whether a given key is currently pressed down.
     * 
     * @param keyName  The name of the key to check
     * @return         True if the key is down
     */
    public static boolean isKeyDown(String keyName)
    {
        return keyboardManager.isKeyDown(keyName);
    }
    
    /**
     * Set the delay between simulation steps when the simulation is
     * running.
     *  
     * @param millis  The delay between simulation steps, in milliseconds
     */
    public static void setSimulationDelay(int millis)
    {
        Simulation.getInstance().setDelay(millis);
    }
    
    /**
     * Pause the simulation.
     */
    public static void pauseSimulation()
    {
        Simulation.getInstance().setPaused(true);
    }
    
    /**
     * Run (or resume) the simulation.
     */
    public static void resumeSimulation()
    {
        Simulation.getInstance().setPaused(false);
    }
}
