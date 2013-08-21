/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2012,2013  Poul Henriksen and Michael Kolling 
 
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

import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
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
public class KeyboardManager implements TriggeredKeyListener, FocusListener
{
    private String lastKeyTyped;

    // We don't know how many virtual keys there are or what they are
    // defined as. To allow checking arbitrary keys, we'll allocate an
    // initial array size of 100, but increase it if we see a higher key
    // code.
    private int numKeys = 100;
    private boolean [] keyLatched = new boolean[numKeys];
    private boolean [] keyDown = new boolean[numKeys];

    // The highest named key index
    private int maxNamedKey = 0;
    // The names of keys, for those keys we want to translate
    private String [] keyNames;
    
    private Map<String,Integer> keyCodeMap;
    
    /** Do we think that a numlock key is present? */
    private boolean hasNumLock = true;
    
    /**
     * Constructor for a KeyboardManager. Key events must be delivered
     * from an external source.
     */
    public KeyboardManager()
    {
        keyCodeMap = new HashMap<String,Integer>();
        addAllKeys();
        buildKeyNameArray();        
    }
    
    /**
     * Clear the latched state of keys which were down, but are no longer
     * down.
     */
    public synchronized void clearLatchedKeys()
    {
        for (int i = 0; i < numKeys; i++) {
            keyLatched[i] &= keyDown[i];
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
        addKey("up", KeyEvent.VK_UP);
        addKey("down", KeyEvent.VK_DOWN);
        addKey("left", KeyEvent.VK_LEFT);
        addKey("right", KeyEvent.VK_RIGHT);
        addKey("space", KeyEvent.VK_SPACE);
        addKey("enter", KeyEvent.VK_ENTER);
        addKey("escape", KeyEvent.VK_ESCAPE);
        addKey("f1", KeyEvent.VK_F1);
        addKey("f2", KeyEvent.VK_F2);
        addKey("f3", KeyEvent.VK_F3);
        addKey("f4", KeyEvent.VK_F4);
        addKey("f5", KeyEvent.VK_F5);
        addKey("f6", KeyEvent.VK_F6);
        addKey("f7", KeyEvent.VK_F7);
        addKey("f8", KeyEvent.VK_F8);
        addKey("f9", KeyEvent.VK_F9);
        addKey("f10", KeyEvent.VK_F10);
        addKey("f11", KeyEvent.VK_F11);
        addKey("f12", KeyEvent.VK_F12);
        addKey("backspace", KeyEvent.VK_BACK_SPACE);
        addKey("\'", KeyEvent.VK_QUOTE);
        addKey("shift", KeyEvent.VK_SHIFT);
        addKey("control", KeyEvent.VK_CONTROL);
    }
    
    /**
     * Add a single key into the key code map. Adjust numKeys if necessary
     * so that it can contain the given keycode.
     * 
     * @param keyName   The name of the key to add (Greenfoot name)
     * @param keyCode   The key code of the key to add (Java key code)
     */
    private void addKey(String keyName, int keyCode)
    {
        keyCodeMap.put(keyName, keyCode);
        if (keyCode + 1 > maxNamedKey) {
            maxNamedKey = keyCode + 1;
        }
    }
    
    /**
     * Build the three key arrays: keyLatched, keyDown, and keyNames.
     */
    private void buildKeyNameArray()
    {
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
    }
        
    
    /**
     * Make sure the key arrays are big enough to store information about
     * the given key. Should be called from a synchronized context.
     * 
     * @param keycode  The keycode of the key to check.
     */
    private void checkKeyArrays(int keycode)
    {
        int nsize = keycode + 1;
        if (nsize > numKeys) {
            // we're seeing a new key code, increase array size
            boolean [] newKeyLatched = new boolean[nsize];
            boolean [] newKeyDown = new boolean[nsize];
            for (int i = 0; i < numKeys; i++) {
                newKeyLatched[i] = keyLatched[i];
                newKeyDown[i] = keyDown[i];
            }
            keyLatched = newKeyLatched;
            keyDown = newKeyDown;
            numKeys = nsize;
        }
    }
    
    /**
     * Get the last key pressed, as a String key name identifying the key.
     */
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
    public synchronized boolean isKeyDown(int keycode)
    {
        if (keycode < numKeys) {
            boolean pressed = keyDown[keycode] || keyLatched[keycode];
            keyLatched[keycode] = false;
            return pressed;
        }
        else {
            return false;
        }
    }
    
    /**
     * Check whether a key, identified by name, is currently down
     * (or latched).
     * 
     * @param keyId  The Greenfoot name for the key to check
     * @return       True if the key is down, or was down since it was
     *               last checked; false otherwise.
     */
    public boolean isKeyDown(String keyId)
    {
        Integer code = keyCodeMap.get(keyId.toLowerCase());
        if (code != null) {
            return isKeyDown(code);
        }
        else {
            // If the keyId is a single character, treat the unicode
            // value of that character as a virtual key code. This is
            // something of a hack, but it works for a range of keys.
            if (keyId.codePointCount(0, keyId.length()) == 1) {
                int keyChar = keyId.codePointAt(0);
                keyChar = Character.toUpperCase(keyChar);
                return isKeyDown(keyChar);
            }
            throw new IllegalArgumentException("\"" + keyId + "\" key doesn't exist. "
                    + "Please change the key name while invoking Greenfoot.isKeyDown() method"); 
        }
    }
    
    // ----- KeyListener interface -----
    
    /*
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public synchronized void keyPressed(KeyEvent event)
    {
        int keyCode = event.getKeyCode();
        pressKey(keyCode);
    }

    private void pressKey(int keyCode)
    {
        keyCode = numLockTranslate(keyCode);
        checkKeyArrays(keyCode);
        keyLatched[keyCode] = true;
        keyDown[keyCode] = true;
    }
    
    /*
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public synchronized void keyReleased(KeyEvent event)
    {
        int keyCode = event.getKeyCode();
        releaseKey(keyCode);
    }

    private void releaseKey(int keyCode)
    {
        keyCode = numLockTranslate(keyCode);
        checkKeyArrays(keyCode);
        keyDown[keyCode] = false;
        if (keyCode < maxNamedKey) {
            String keyName = keyNames[keyCode];
            if (keyName != null) {
                lastKeyTyped = keyName;
            }
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
    private int numLockTranslate(int keycode)
    {
        if (keycode >= KeyEvent.VK_NUMPAD0 && keycode <= KeyEvent.VK_NUMPAD9) {
            // At least on linux, we can only get these codes if numlock is on; in that
            // case we want to map to a digit anyway.
            return keycode - KeyEvent.VK_NUMPAD0 + KeyEvent.VK_0;
        }

        // Seems on linux (at least) we can't get the numlock state (get an
        // UnsupportedOperationException). Update: on Java 1.7.0_03 at least,
        // we can now retrieve numlock state on linux.
        boolean numlock = true;
        if (hasNumLock) {
            try {
                numlock = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK);
            }
            catch (UnsupportedOperationException usoe) {
                // Don't try to get numlock status again
                hasNumLock = false;
            }
        }

        if (numlock) {
            // Translate to digit
            if (keycode == KeyEvent.VK_KP_UP) {
                keycode = KeyEvent.VK_8;
            }
            else if (keycode == KeyEvent.VK_KP_DOWN) {
                keycode = KeyEvent.VK_2;
            }
            else if (keycode == KeyEvent.VK_KP_LEFT) {
                keycode = KeyEvent.VK_4;
            }
            else if (keycode == KeyEvent.VK_KP_RIGHT) {
                keycode = KeyEvent.VK_6;
            }
        }
        else {
            // Translate to direction
            if (keycode == KeyEvent.VK_KP_UP) {
                keycode = KeyEvent.VK_UP;
            }
            else if (keycode == KeyEvent.VK_KP_DOWN) {
                keycode = KeyEvent.VK_DOWN;
            }
            else if (keycode == KeyEvent.VK_KP_LEFT) {
                keycode = KeyEvent.VK_LEFT;
            }
            else if (keycode == KeyEvent.VK_KP_RIGHT) {
                keycode = KeyEvent.VK_RIGHT;
            }
        }
        return keycode;
    }
    
    /*
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    @Override
    public synchronized void keyTyped(KeyEvent key)
    {
        char c = key.getKeyChar();
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
            lastKeyTyped = "" + c;
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
        for (int keyCode = 0; keyCode < keyDown.length; keyCode++) {
            keyDown[keyCode] = false;
            keyLatched[keyCode] = false;
        }
    }
}
