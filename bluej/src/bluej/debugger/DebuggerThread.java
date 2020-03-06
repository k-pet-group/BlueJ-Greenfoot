/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.debugger;

import bluej.utility.javafx.FXPlatformSupplier;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * A class defining the debugger thread primitives needed by BlueJ.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Andrew Patterson
 */
public abstract class DebuggerThread
{
    public abstract String getName();

    @OnThread(Tag.VMEventHandler)
    public abstract String getStatus();

    @OnThread(Tag.VMEventHandler)
    public abstract boolean isSuspended();
    @OnThread(Tag.VMEventHandler)
    public abstract boolean isAtBreakpoint();

    @OnThread(Tag.VMEventHandler)
    public abstract String getClass(int frameNo);
    @OnThread(Tag.VMEventHandler)
    public abstract String getClassSourceName(int frameNo);
    @OnThread(Tag.VMEventHandler)
    public abstract int getLineNumber(int frameNo);
    public abstract boolean isKnownSystemThread();

    /**
     * Get the current execution of the stack. This is only reliable if the
     * thread is currently halted.
     */
    @OnThread(Tag.VMEventHandler)
    public abstract List<SourceLocation> getStack();

    @OnThread(Tag.VMEventHandler)
    public abstract List<FXPlatformSupplier<VarDisplayInfo>> getLocalVariables(int frameNo);
    @OnThread(Tag.VMEventHandler)
    public abstract boolean varIsObject(int frameNo, int index);
    @OnThread(Tag.VMEventHandler)
    public abstract FXPlatformSupplier<DebuggerObject> getStackObject(int frameNo, int index);
    /**
     * Gets the stack object, but without fetching its type, allowing it to be done from the VMEventHandler thread.
     */
    @OnThread(Tag.VMEventHandler)
    public abstract DebuggerObject getStackObjectUntyped(int frameNo, int index);
    
    /**
     * Return the current instance object of some frame on this thread.
     * The returned object may represent the null reference if the frame
     * is for a static method.
     */
    @OnThread(Tag.VMEventHandler)
    public abstract DebuggerObject getCurrentObject(int frameNo);
    
    /**
     * Return the current class of this thread (may return null if the
     * class cannot be determined).
     */
    @OnThread(Tag.VMEventHandler)
    public abstract DebuggerClass getCurrentClass(int frameNo);

    public abstract void setSelectedFrame(int frame);
    public abstract int getSelectedFrame();

    @OnThread(Tag.VMEventHandler)
    public abstract void halt();
    @OnThread(Tag.VMEventHandler)
    public abstract void cont();

    /**
     * Step to the next line in the current method. This is only valid when the thread is
     * suspended. It is safe to call this from a DebuggerListener.
     */
    @OnThread(Tag.VMEventHandler)
    public abstract void step();

    /**
     * Step to the next executed line (which might be in a called method). This is only valid when the
     * thread is suspended. It is safe to call this from a DebuggerListener.
     */
    @OnThread(Tag.VMEventHandler)
    public abstract void stepInto();
    
    @OnThread(Tag.Any)
    public abstract boolean sameThread(DebuggerThread thread);
}
