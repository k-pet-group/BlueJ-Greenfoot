/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2012,2013,2015,2018  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.input;

import javafx.scene.input.KeyCode;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Manage keyboard input, to allow Greenfoot programs to poll for
 * keystrokes. Keystrokes will be delivered on the GUI event thread,
 * but may be polled from another thread.
 * 
 * <p>The following key names are recognized:
 * up, down, left, right (cursor keys); enter, space, tab, escape, backspace,
 * F1-F12.
 * 
 * @author Davin McCall
 */
public class KeyboardManager
{
    // The last key typed, returned by Greenfoot.getKey()
    private String lastKeyTyped;

    // The keys which are latched are those which have been pressed and released in this frame
    // but we want to remember that they were pressed briefly, so that Greenfoot.isKeyDown
    // still returns true for their press, in the case that the frame rate is low and the key
    // was pressed within a frame.  Cleared by clearLatchedKeys() after each frame for
    // those keys which are now released.
    private final Set<String> keyLatched = new HashSet<>();
    // Those keys which are actually pressed down right now.
    private final Set<String> keyDown = new HashSet<>();

    /** Do we think that a numlock key is present? */
    private boolean hasNumLock = true;
    
    /**
     * Constructor for a KeyboardManager. Key events must be delivered
     * from an external source.
     */
    public KeyboardManager()
    {        
    }
    
    /**
     * Clear the latched state of keys which were down, but are no longer
     * down.
     */
    @OnThread(Tag.Simulation)
    public synchronized void clearLatchedKeys()
    {
        for (Iterator<String> i = keyLatched.iterator(); i.hasNext(); )
        {
            String keyCode = i.next();
            if (!keyDown.contains(keyCode))
            {
                i.remove();
            }
        }
    }
        
    /**
     * Get the last key pressed, as a String key name identifying the key.
     */
    @OnThread(Tag.Simulation)
    public synchronized String getKey()
    {
        String r = lastKeyTyped;
        lastKeyTyped = null;
        return r;
    }
    
    /**
     * Check whether a key, identified by a key name (String),
     * is currently down (or latched).
     * 
     * @param key     The name of the key to check
     * @return        True if the key is currently down, or was down since
     *                it was last checked; false otherwise.
     */
    @OnThread(Tag.Simulation)
    public synchronized boolean isKeyDown(String key)
    {
        key = key.toLowerCase();
        boolean pressed = keyDown.contains(key) || keyLatched.contains(key);
        // We forget any was-pressed state here; if the frame is long
        // then we don't necessarily want to record the key as held down all
        // frame if the user taps it lightly (e.g. if someone uses a complex
        // act cycle via Greenfoot.delay() and Greenfoot.ask())
        keyLatched.remove(key);
        return pressed;
    }

    /**
     * Notifies that a key has been pressed.
     * @param keyCode The KeyCode from KeyEvent.getCode()
     * @param keyText The text from KeyEvent.getText()
     */
    public synchronized void keyPressed(KeyCode keyCode, String keyText)
    {
        String keyName = getKeyName(keyCode, keyText);
        keyLatched.add(keyName);
        keyDown.add(keyName);
    }

    /**
     * Notifies that a key has been released.
     * @param keyCode The KeyCode from KeyEvent.getCode()
     * @param keyText The text from KeyEvent.getText()
     */
    public synchronized void keyReleased(KeyCode keyCode, String keyText)
    {
        String keyName = getKeyName(keyCode, keyText);
        keyDown.remove(keyName);
        lastKeyTyped = keyName;
    }

    /**
     * Translate the "key pad" directional keys according to the status of numlock,
     * and otherwise translate a KeyCode+text into a Greenfoot key name.
     * 
     * @param keycode  The original keycode
     * @param keyText  The text from the original keyboard event
     * @return   The translated key name
     */
    private String getKeyName(KeyCode keycode, String keyText)
    {
        if (keycode.ordinal() >= KeyCode.NUMPAD0.ordinal() && keycode.ordinal() <= KeyCode.NUMPAD9.ordinal()) {
            // At least on linux, we can only get these codes if numlock is on; in that
            // case we want to map to a digit anyway.
            return "" + (char)('0' + (keycode.ordinal() - KeyCode.NUMPAD0.ordinal()));
        }

        // Seems on linux (at least) we can't get the numlock state (get an
        // UnsupportedOperationException). Update: on Java 1.7.0_03 at least,
        // we can now retrieve numlock state on linux.
        boolean numlock = true;
        if (hasNumLock)
        {
            try
            {
                numlock = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK);
            }
            catch (UnsupportedOperationException usoe)
            {
                // Don't try to get numlock status again
                hasNumLock = false;
            }
        }

        if (numlock)
        {
            // Translate to digit
            if (keycode == KeyCode.KP_UP)
            {
                keycode = KeyCode.DIGIT8;
            }
            else if (keycode == KeyCode.KP_DOWN)
            {
                keycode = KeyCode.DIGIT2;
            }
            else if (keycode == KeyCode.KP_LEFT)
            {
                keycode = KeyCode.DIGIT4;
            }
            else if (keycode == KeyCode.KP_RIGHT)
            {
                keycode = KeyCode.DIGIT6;
            }
        }
        else
        {
            // Translate to direction
            if (keycode == KeyCode.KP_UP)
            {
                keycode = KeyCode.UP;
            }
            else if (keycode == KeyCode.KP_DOWN)
            {
                keycode = KeyCode.DOWN;
            }
            else if (keycode == KeyCode.KP_LEFT)
            {
                keycode = KeyCode.LEFT;
            }
            else if (keycode == KeyCode.KP_RIGHT)
            {
                keycode = KeyCode.RIGHT;
            }
        }

        // Handle the keys where the Greenfoot name doesn't line up with the FX KeyCode.getName():
        switch (keycode)
        {
            case ESCAPE:
                return "escape";
            case BACK_SPACE:
                return "backspace";
            case QUOTE:
                return "\'";
            case CONTROL:
                return "control";
            default:
                break;
        }
        // By default, use the key text lower-cased if present:
        if (!keyText.isEmpty())
        {
            // Fix a few keys which don't have text corresponding to their names:
            switch (keyText)
            {
                case "\r": case "\n":
                    return "enter";
                case "\t":
                    return "tab";
                case "\b":
                    return "backspace";
                case " ":
                    return "space";
                case "\u001B":
                    return "escape";
            }
            
            return keyText.toLowerCase();
        }
        else
        {
            // Otherwise fetch the JavaFX name from the KeyCode and lower-case that:
            return keycode.getName().toLowerCase();
        }
    }

    /**
     * Notifies that a key has been typed.
     * @param keyCode The KeyCode from KeyEvent.getCode()
     * @param keyText The text from KeyEvent.getText()
     */
    public synchronized void keyTyped(KeyCode keyCode, String keyText)
    {
        String keyName = getKeyName(keyCode, keyText);
        if (!keyName.isEmpty() && !keyName.equals("undefined"))
        {
            lastKeyTyped = keyName;
        }
    }

    public void focusGained() { }

    /**
     * If we loose focus, we should treat all keys as not pressed anymore
     */
    public void focusLost()
    {
        releaseAllKeys();
    }

    /**
     * Release all the keys.
     */
    private synchronized void releaseAllKeys()
    {
        keyDown.clear();
        keyLatched.clear();
    }
}
