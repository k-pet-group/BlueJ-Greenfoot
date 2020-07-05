/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package threadchecker;

public enum Tag
{
    // FXPlatform is the FX thread, Swing is the EDT
    // FX means either FX thread or a loader thread for FX
    // Things like Thread.run or FXWorker.construct are tagged as Unique
    // Worker is for background tasks not on any of the other threads.
    // Any means that the method is safe to call from any thread (including FX, Swing, and others)

    /**
     * Only for use EITHER on the FX platform thread OR in a loading thread (e.g. Stride editor loading).
     */
    FX,
    /**
     * Only for use on the FX platform thread.
     */
    FXPlatform,
    /**
     * Only for use on the Swing event thread (aka EDT)
     */
    Swing,
    /**
     * Only for use on the Greenfoot Simulation thread in the simulation VM.
     */
    Simulation,
    /**
     * Only for use on a background worker thread.
     */
    Worker,
    /**
     * The thread that handles JDI events from the debugger
     */
    VMEventHandler,
    /**
     * Any thread except VMEventHandler
     */
    NOTVMEventHandler,
    /**
     * May be used on any thread.
     */
    Any;

    /**
     * Checks if this tag on a method is allowed when overriding the given (potentially null) parent method tag.
     * 
     * The rule is pretty simple: if the parent tag is present, it must match this tag.
     * If the parent tag is empty, this tag must be Any (because the untagged parent can be called from any thread, so we can too)
     */
    public boolean canOverride(Tag parent)
    {
        if (parent == null)
            return this == Any;
        else if (parent == FXPlatform && this == FX)
            return true; // FX can override FXPlatform, but not vice versa
        else if (this == Any)
            return true; // Any can override a more-specific parent tag
        else
            return this == parent;
    }

    /**
     * Checks if code tagged with this tag can call a method tagged with the given (potentially null) tag.
     * 
     * The rule is simple: we return false if the destination tag is Unique; or is FX/Swing and does not match this tag.
     * In all other cases we return true.
     */
    public boolean canCall(Tag dest, boolean sameInstance)
    {
        if (dest == null || dest == Any) // Can call dest any from any source
            return true;
        else if (dest == Tag.FX && this == Tag.FXPlatform)
            return true; // FXPlatform can call FX, but not vice versa
        else if (dest == NOTVMEventHandler)
            return this != VMEventHandler;
        else
            return this == dest;
        
    }
}