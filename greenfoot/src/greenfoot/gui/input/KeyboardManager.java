/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2012,2013,2015  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.event.TriggeredKeyListener;
import javafx.scene.input.KeyCode;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private String lastKeyTyped;

    private final EnumSet<KeyCode> keyLatched = EnumSet.noneOf(KeyCode.class);
    private final EnumSet<KeyCode> keyDown = EnumSet.noneOf(KeyCode.class);

    // This maps key names to codes, and is used during Greenfoot.isKeyDown()
    // so that we can take the key the user asked for and check if it is pressed or not.
    // Every possible key is in this map:
    private final Map<String, KeyCode> keyCodeMap = new HashMap<>();
    
    // This maps from key code to String name, and is used for processing ahead of
    // Greenfoot.getKey() to work out the name of the last pressed key.
    private final Map<KeyCode, String> keyNames = new HashMap<>();


    /** Do we think that a numlock key is present? */
    private boolean hasNumLock = true;
    
    /**
     * Constructor for a KeyboardManager. Key events must be delivered
     * from an external source.
     */
    public KeyboardManager()
    {
        addAllKeys();
        buildKeyNameArray();        
    }
    
    /**
     * Clear the latched state of keys which were down, but are no longer
     * down.
     */
    @OnThread(Tag.Any)
    public synchronized void clearLatchedKeys()
    {
        for (KeyCode keyCode : KeyCode.values())
        {
            if (!keyDown.contains(keyCode))
            {
                keyLatched.remove(keyCode);
            }
        }
    }
    
    /**
     * Add all the keys which Greenfoot will directly support into the
     * key code map (excluding keys with single-character names identifying
     * the key, such as "a", "b" .. "z" and "0" .. "9", and other keys whose
     * unicode value maps to their key code.
     */
    private void addAllKeys()
    {
        // For most keys, the Greenfoot name lines up with the lower-cased FX name:
        for (KeyCode keyCode : KeyCode.values())
        {
            addKey(keyCode.getName().toLowerCase(), keyCode);
        }
        
        // And the ones where the Greenfoot name doesn't line up with the FX name:
        addKey("escape", KeyCode.ESCAPE);
        addKey("backspace", KeyCode.BACK_SPACE);
        addKey("\'", KeyCode.QUOTE);
        addKey("control", KeyCode.CONTROL);
    }
    
    /**
     * Add a single key into the key code map. Adjust numKeys if necessary
     * so that it can contain the given keycode.
     * 
     * @param keyName   The name of the key to add (Greenfoot name)
     * @param keyCode   The key code of the key to add (Java key code)
     */
    private void addKey(String keyName, KeyCode keyCode)
    {
        keyCodeMap.put(keyName, keyCode);
    }
    
    /**
     * Build the three key arrays: keyLatched, keyDown, and keyNames.
     */
    private void buildKeyNameArray()
    {
        /* TODO commented out, pending a fix for the way we deal with key typed:
        keyNames = new String[maxNamedKey];
        Iterator<String> keyNamesIterator = keyCodeMap.keySet().iterator();
        while (keyNamesIterator.hasNext()) {
             String keyName = keyNamesIterator.next();
             int keyCode = keyCodeMap.get(keyName);
             keyNames[keyCode] = keyName;
        }
        
        // remove from the keyNames table keys which will generate
        // keyTyped events.
        keyNames[KeyEvent.VK_SPACE] = null;
        keyNames[KeyEvent.VK_ENTER] = null;
        keyNames[KeyEvent.VK_ESCAPE] = null;
        keyNames[KeyEvent.VK_TAB] = null;
        keyNames[KeyEvent.VK_BACK_SPACE] = null;
        keyNames[KeyEvent.VK_QUOTE] = null;
        */
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
     * Check whether a key, identified by a virtual key code (int),
     * is currently down (or latched).
     * 
     * @param keycode   The key code of the key to check
     * @return        True if the key is currently down, or was down since
     *                it was last checked; false otherwise.
     */
    @OnThread(Tag.Simulation)
    public synchronized boolean isKeyDown(KeyCode keycode)
    {
        boolean pressed = keyDown.contains(keycode) || keyLatched.contains(keycode);
        keyLatched.remove(keycode);
        return pressed;
    }
    
    /**
     * Check whether a key, identified by name, is currently down
     * (or latched).
     * 
     * @param keyId  The Greenfoot name for the key to check
     * @return       True if the key is down, or was down since it was
     *               last checked; false otherwise.
     */
    @OnThread(Tag.Simulation)
    public boolean isKeyDown(String keyId)
    {
        KeyCode code = keyCodeMap.get(keyId.toLowerCase());
        if (code != null)
        {
            return isKeyDown(code);
        }
        else
        {
            // If the keyId is a single character, look for the keycode corresponding to that
            // character.
            if (keyId.codePointCount(0, keyId.length()) == 1)
            {
                KeyCode keyCode = KeyCode.getKeyCode(keyId);
                if (keyCode != null)
                {
                    return isKeyDown(keyCode);
                }
            }
            throw new IllegalArgumentException("\"" + keyId + "\" key doesn't exist. "
                    + "Please change the key name while invoking Greenfoot.isKeyDown() method"); 
        }
    }
    
    public synchronized void pressKey(KeyCode keyCode)
    {
        keyCode = numLockTranslate(keyCode);
        keyLatched.add(keyCode);
        keyDown.add(keyCode);
    }
    

    public synchronized void releaseKey(KeyCode keyCode)
    {
        keyCode = numLockTranslate(keyCode);
        keyDown.remove(keyCode);
        String keyName = keyNames.get(keyCode);
        if (keyName != null) {
            lastKeyTyped = keyName;
        }
    }

    public void listeningStarted(Object obj)
    {       
    }

    public void listeningEnded()
    {
        releaseAllKeys();
    }

    /**
     * Translate the "key pad" directional keys according to the status of numlock.
     * 
     * @param keycode  The original keycode
     * @return   The translated keycode
     */
    private KeyCode numLockTranslate(KeyCode keycode)
    {
        if (keycode.ordinal() >= KeyCode.NUMPAD0.ordinal() && keycode.ordinal() <= KeyCode.NUMPAD9.ordinal()) {
            // At least on linux, we can only get these codes if numlock is on; in that
            // case we want to map to a digit anyway.
            return KeyCode.values()[keycode.ordinal() - KeyCode.NUMPAD0.ordinal() + KeyCode.DIGIT0.ordinal()];
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
        return keycode;
    }
    
    public synchronized void keyTyped(String key)
    {
        char c = key.charAt(0);
        if (c == '\n' || c == '\r') {
            lastKeyTyped = "enter";
        }
        else if (c == '\t') {
            lastKeyTyped = "tab";
        }
        else if (c == '\b') {
            lastKeyTyped = "backspace";
        }
        else if (c == ' ') {
            lastKeyTyped = "space";
        }
        else if (c == 27) {
            lastKeyTyped = "escape";
        }
        else {
            lastKeyTyped = key;
        }
    }

    public void focusGained(FocusEvent e) { }

    /**
     * If we loose focus, we should treat all keys as not pressed anymore
     */
    public void focusLost(FocusEvent e)
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
