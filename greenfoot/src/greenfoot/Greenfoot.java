package greenfoot;

import java.io.IOException;
import java.util.Random;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import greenfoot.core.KeyboardManager;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.sound.SoundPlayer;


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
 * @version 1.0
 * @cvs-version $Id: Greenfoot.java 4679 2006-10-30 17:06:20Z polle $
 */
public class Greenfoot
{
    private static KeyboardManager keyboardManager = WorldHandler.getInstance().getKeyboardManager();
    
    private static Random randomGenerator = new Random();
    
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
     * This method will delay the current execution by the time specified
     * by the Greenfoot environment (the speed slider).
     * 
     * @see #setSimulationSpeed(int)
     */
    public void delay()
    {
        WorldHandler.getInstance().repaint();
        Simulation.getInstance().sleep();
    }
    
    /**
     * Set the speed of the simulation execution.
     *  
     * @param speed  The new speed. the value must be in the range (1..100)
     */
    public static void setSimulationSpeed(int speed)
    {
        Simulation.getInstance().setSpeed(speed);
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
    
    /**
     * Return a random number between 0 (inclusive) and limit (exclusive).
     */
    public static int getRandomNumber(int limit) {
        return randomGenerator.nextInt(limit);
    }
    
    public static void playSound(final String soundFile)
    {
        try {
            SoundPlayer.getInstance().play(soundFile);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (LineUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
