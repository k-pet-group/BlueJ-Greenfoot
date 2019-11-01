/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2012,2014,2019  Michael Kolling and John Rosenberg
 
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
package bluej.terminal;

import bluej.extensions2.SourceType;
import bluej.pkgmgr.Package;

import java.io.File;

/**
 * A class that holds information about the location of an exception.
 * Effectively, this represents one line in a stack-trace.
 * @author nccb
 *
 */
public class ExceptionSourceLocation
{
    /** The package */
    private Package pkg;
    /** The unqualified class name */
    private String className;
    /** The line number */
    private int lineNumber;
    
    /** The starting position in the document of the bit to be linked */
    private int startPos;
    /** The ending position in the document of the bit to be linked */
    private int endPos;
    
    public ExceptionSourceLocation(int startPos, int endPos,
            Package pkg, String className, int lineNumber)
    {
        this.startPos = startPos;
        this.endPos = endPos;
        this.pkg = pkg;
        this.className = className;
        this.lineNumber = lineNumber;
    }
    
    public int getStart()
    {
        return startPos;
    }
    public int getEnd()
    {
        return endPos;
    }
    
    public void showInEditor()
    {
        String fileName = className.replace('.', '/') + "." + SourceType.Java.toString().toLowerCase();
        
        pkg.exceptionMessage(new File(pkg.getPath(), fileName).getAbsolutePath(), lineNumber);
        
    }
    
    
}