/*
 This file is part of the BlueJ program.
 Copyright (C) 2014,2016  Michael Kolling and John Rosenberg

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
package bluej.compiler;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Observer interface for classes that are interested in compilation.
 *
 * All events will be received on the event dispatch thread.
 *
 * Note this is no longer an exact copy of CompileObserver.  That class returns void
 * from the compilerMessage method, because none of the CompileObserver callers actually care.
 * The only observer which cares is the data collection observer, which implements
 * FXCompileObserver (this class), so only this class returns something from compilerMessage.
 *
 * @author  Michael Cahill
 */
@OnThread(Tag.FXPlatform)
public interface FXCompileObserver
{
    /**
     * A compilation job has started.
     */
    void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence);
    
    /**
     * An error or warning message occurred during compilation
     * 
     * Returns whether or not the error was shown to the user (for data collection purposes)
     */
    boolean compilerMessage(Diagnostic diagnostic, CompileType type);
    
    /**
     * A Compilation job finished.
     */
    void endCompile(CompileInputFile[] sources, boolean succesful, CompileType type, int compilationSequence);
}