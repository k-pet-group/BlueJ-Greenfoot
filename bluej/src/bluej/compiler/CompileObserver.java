/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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

import java.io.File;

/**
 * Observer interface for classes that are interested in compilation.
 *
 * All events are generated on the compiler thread.
 *
 * @author  Michael Cahill
 * @version $Id: CompileObserver.java 6163 2009-02-19 18:09:55Z polle $
 */
public interface CompileObserver
{
    /**
     * A compilation job has started.
     */
    void startCompile(File[] sources);
    
    /**
     * An error message occurred during compilation
     */
    void errorMessage(String filename, int lineNo, String message);
    
    /**
     * A warning message occurred during compilation
     */
    void warningMessage(String filename, int lineNo, String message);

    /**
     * A Compilation job finished.
     */
    void endCompile(File[] sources, boolean succesful);
}
