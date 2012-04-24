/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2012  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot;

import java.util.Random;

import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.sound.MicLevelGrabber;
import greenfoot.sound.Sound;
import greenfoot.sound.SoundFactory;


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
 * <li>"enter", "space", "tab", "escape", "backspace", "shift", "control"
 * <li>"F1", "F2", .., "F12" (the function keys)
 * </ul>
 * 
 * @author Davin McCall
 * @version 2.4
 */
public class Greenfoot
{

    private static Random randomGenerator = new Random();

    /**
     * Sets the World to run to the one given.
     * This World will now be the main World that Greenfoot runs with on the
     * next act.
     *
     * @param world The World to switch running to, cannot be null.
     */
    public static void setWorld(World world)
    {
        if ( world == null ) {
            throw new NullPointerException("The given world cannot be null.");
        }

        WorldHandler.getInstance().setWorld( world );
    }

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
        return WorldHandler.getInstance().getKeyboardManager().getKey();
    }
    
    /**
     * Check whether a given key is currently pressed down.
     * 
     * @param keyName  The name of the key to check
     * @return         True if the key is down
     */
    public static boolean isKeyDown(String keyName)
    {
        return WorldHandler.getInstance().getKeyboardManager().isKeyDown(keyName);
    }
    
    /**
     * Delay the current execution by a number of time steps. 
     * The size of one time step is defined by the Greenfoot environment (the speed slider).
     * 
     * @see #setSpeed(int)
     */
    public static void delay(int time)
    {
        for(int i=0; i < time; i++) {
            WorldHandler.getInstance().repaint();
            Simulation.getInstance().sleep();
        }
    }
    
    /**
     * Set the speed of the execution.
     *  
     * @param speed  The new speed. the value must be in the range (1..100)
     */
    public static void setSpeed(int speed)
    {
        Simulation.getInstance().setSpeed(speed);
    }
    
    /**
     * Pause the execution.
     */
    public static void stop()
    {
        Simulation.getInstance().setPaused(true);
    }
    
    /**
     * Run (or resume) the execution.
     */
    public static void start()
    {
        Simulation.getInstance().setPaused(false);
    }
    
    /**
     * Return a random number between 0 (inclusive) and limit (exclusive).
     */
    public static int getRandomNumber(int limit)
    {
        return randomGenerator.nextInt(limit);
    }

    /**
     * Play sound from a file. The following formats are supported: AIFF, AU and
     * WAV.
     * <p>
     * The file name may be an absolute path, a base name for a file located in
     * the project directory or in the sounds directory of the project
     * directory.
     * 
     * @param soundFile Typically the name of a file in the sounds directory in
     *            the project directory.
     * @throws IllegalArgumentException If the sound can not be loaded.
     */
    public static void playSound(final String soundFile)
    {
        Sound sound = SoundFactory.getInstance().createSound(soundFile, false);

        if( sound != null) {
            sound.play();
        }
    }


    /**
     * True if the mouse has been pressed (changed from a non-pressed state to
     * being pressed) on the given object. If the parameter is an Actor the
     * method will only return true if the mouse has been pressed on the given
     * actor. If there are several actors at the same place, only the top most
     * actor will receive the press. 
     * If the parameter is a World then true will be returned if the mouse was
     * pressed on the world background. If the parameter is null,
     * then true will be returned for any mouse press, independent of the target 
     * pressed on.
     * 
     * @param obj Typically one of Actor, World or null
     * @return True if the mouse has been pressed as explained above
     */
    public static boolean mousePressed(Object obj)
    {
        return WorldHandler.getInstance().getMouseManager().isMousePressed(obj);
    }

    /**
     * True if the mouse has been clicked (pressed and released) on the given
     * object. If the parameter is an Actor the method will only return true if
     * the mouse has been clicked on the given actor. If there are several
     * actors at the same place, only the top most actor will receive the click.
     * If the parameter is a World then true will be returned if the mouse was
     * clicked on the world background. If the parameter is null,
     * then true will be returned for any click, independent of the target 
     * clicked on.
     * 
     * @param obj Typically one of Actor, World or null
     * @return True if the mouse has been clicked as explained above
     */
    public static boolean mouseClicked(Object obj)
    {
        return WorldHandler.getInstance().getMouseManager().isMouseClicked(obj);
    }

    /**
     * True if the mouse is currently being dragged on the given object. The mouse is
     * considered to be dragged on an object if the drag started on that
     * object - even if the mouse has since been moved outside of that object.
     * <p>
     * If the parameter is an Actor the method will only return true if the drag
     * started on the given actor. If there are several actors at the same
     * place, only the top most actor will receive the drag. 
     * If the parameter is a World then true will be returned if the drag action
     * was started on the world background. If the parameter is null,
     * then true will be returned for any drag action, independent of the target 
     * clicked on.
     * 
     * @param obj Typically one of Actor, World or null
     * @return True if the mouse has been dragged as explained above
     */
    public static boolean mouseDragged(Object obj)
    {
        return WorldHandler.getInstance().getMouseManager().isMouseDragged(obj);
    }

    /**
     * True if a mouse drag has ended. This happens when the mouse has been
     * dragged and the mouse button released.
     * <p>
     * If the parameter is an Actor the method will only return true if the drag
     * started on the given actor. If there are several actors at the same
     * place, only the top most actor will receive the drag. 
     * If the parameter is a World then true will be returned if the drag action
     * was started on the world background. If the parameter is null,
     * then true will be returned for any drag action, independent of the target 
     * clicked on.
     * 
     * @param obj
     *            Typically one of Actor, World or null
     * @return True if the mouse has been dragged as explained above
     */
    public static boolean mouseDragEnded(Object obj)
    {
        return WorldHandler.getInstance().getMouseManager().isMouseDragEnded(obj);
    }

    /**
     * True if the mouse has been moved on the given object. The mouse is
     * considered to be moved on an object if the mouse pointer is above
     * that object.
     * <p>
     * If the parameter is an Actor the method will only return true if the move
     * is on the given actor. If there are several actors at the same
     * place, only the top most actor will receive the move. 
     * If the parameter is a World then true will be returned if the move
     * was on the world background. If the parameter is null,
     * then true will be returned for any move, independent of the target 
     * under the move location.
     * 
     * @param obj Typically one of Actor, World or null
     * @return True if the mouse has been moved as explained above
     */
    public static boolean mouseMoved(Object obj)
    {
        return WorldHandler.getInstance().getMouseManager().isMouseMoved(obj);
    }

    /**
     * Return a mouse info object with information about the state of the
     * mouse.
     * 
     * @return The info about the current state of the mouse, or null if the mouse
     *         cursor is outside the world boundary (unless being dragged).
     */
    public static MouseInfo getMouseInfo()
    {
        return WorldHandler.getInstance().getMouseManager().getMouseInfo();
    }
    
    /**
     * Get the microphone input level. This level is an approximation of the loudness
     * any noise that is currently being received by the microphone.
     * 
     * @return The microphone input level (between 0 and 100, inclusive).
     */
    public static int getMicLevel()
    {
        return MicLevelGrabber.getInstance().getLevel();
    }
}
