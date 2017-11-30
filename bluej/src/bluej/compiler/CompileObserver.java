/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2016  Michael Kolling and John Rosenberg
 
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

import java.io.File;

/**
 * Observer interface for classes that are interested in compilation.
 *
 * All events are generated on the compiler thread.
 *
 * @author  Michael Cahill
 */
public interface CompileObserver
{
    /**
     * A compilation job has started.
     */
    @OnThread(Tag.Any)
    void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence);
    
    /**
     * An error or warning message occurred during compilation
     */
    @OnThread(Tag.Any)
    void compilerMessage(Diagnostic diagnostic, CompileType type);
    
    /**
     * A Compilation job finished.
     */
    @OnThread(Tag.Any)
    void endCompile(CompileInputFile[] sources, boolean succesful, CompileType type, int compilationSequence);
}
