/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.util.List;

/**
 * A class defining the debugger thread primitives needed by BlueJ.
 *
 * Objects of this class can only be constructed 
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: DebuggerThread.java 6215 2009-03-30 13:28:25Z polle $
 */

public abstract class DebuggerThread
{
    public abstract String getName();

    public abstract String getStatus();
    
    public abstract boolean isSuspended();
	public abstract boolean isAtBreakpoint();

    public abstract String getClass(int frameNo);
    public abstract String getClassSourceName(int frameNo);
    public abstract int getLineNumber(int frameNo);
    public abstract boolean isKnownSystemThread();

    public abstract List getStack();
    public abstract List getLocalVariables(int frameNo);
    public abstract boolean varIsObject(int frameNo, int index);
    public abstract DebuggerObject getStackObject(int frameNo, int index);
    public abstract DebuggerObject getCurrentObject(int frameNo);
    public abstract DebuggerClass getCurrentClass(int frameNo);

    public abstract void setSelectedFrame(int frame);
    public abstract int getSelectedFrame();

	public abstract void halt();
	public abstract void cont();
	
    public abstract void step();
    public abstract void stepInto();
}
