/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of f
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.vmcomm;

/**
 * A command or event from the server VM to the debug VM, such
 * as keyboard/mouse event, Run, Reset, etc
 */
public class Command
{
    // These are the constants passed in the shared memory between processes,
    // hence they cannot be enums.  They are not persisted anywhere, so can
    // be changed at will (as long as they don't overlap).

    /*
     * Key events.  Followed by one integer which is the key code
     * (using the JavaFX KeyCode enum's ordinal method), then the rest
     * are integer codepoints from the string of the key text.
     */
    public static final int KEY_DOWN = 1;
    public static final int KEY_UP = 2;
    public static final int KEY_TYPED = 3;

    /*
     * Mouse events.  Followed by four integers:
     * X pos, Y pos, button index, click count
     */
    public static final int MOUSE_CLICKED = 11;
    public static final int MOUSE_PRESSED = 12;
    public static final int MOUSE_DRAGGED = 13;
    public static final int MOUSE_RELEASED = 14;
    public static final int MOUSE_MOVED = 15;
    public static final int MOUSE_EXITED = 16;

    /*
     * Commands or requests.  Unless otherwise specified,
     * followed by no integers.
     */
    public static final int COMMAND_RUN = 21;
    // Followed by drag-ID, X, Y:
    public static final int COMMAND_CONTINUE_DRAG = 22;
    // Followed by drag-ID:
    public static final int COMMAND_END_DRAG = 23;
    public static final int COMMAND_PAUSE = 24;
    public static final int COMMAND_ACT = 25;
    public static final int COMMAND_INSTANTIATE_WORLD = 26;
    // Followed by one integer per character in String answer.
    public static final int COMMAND_ANSWERED = 27;
    // Followed by an integer count of key size, then that many integer codepoints,
    // Then same again for value.  If value count is -1,
    // that means value is null (and thus was removed)
    public static final int COMMAND_PROPERTY_CHANGED = 28;
    // Discard the world, but don't make a new one
    public static final int COMMAND_DISCARD_WORLD = 29;
    public static final int COMMAND_SET_SPEED = 30;

    public static final int COMMAND_WORLD_FOCUS_GAINED = 40;
    public static final int COMMAND_WORLD_FOCUS_LOST = 41;
    
    
    // Commands are assigned a stricly increasing ID:
    private static int nextCommandSequence = 1;

    public final int commandSequence;
    public final int commandType;
    public final int[] extraInfo;

    /**
     * Construct a command of the given type, and with any number of additional parameters.
     */
    public Command(int commandType, int... extraInfo)
    {
        this.commandSequence = nextCommandSequence++;
        this.commandType = commandType;
        this.extraInfo = extraInfo;
    }
    
    /**
     * Check if an event is a key event.
     */
    public static boolean isKeyEvent(int event)
    {
        return event >= KEY_DOWN && event <= KEY_TYPED;
    }

    /**
     * Check if an even is a mouse event.
     */
    public static boolean isMouseEvent(int event)
    {
        return event >= MOUSE_CLICKED && event <= MOUSE_EXITED;
    }
}
