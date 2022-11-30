/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This class holds a location in some source code
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.Any)
public final class SourceLocation
{
    private String classname;
    private String filename;
    private String methodname;
    private int lineNumber;

    /**
     * Construct a SourceLocation instance.
     * @param classname   Name of the class
     * @param filename    Name of the source file, relative to the source root (may be null)
     * @param methodname  Name of the method
     * @param lineNumber  Line number
     */
    public SourceLocation(String classname, String filename, 
                          String methodname, int lineNumber)
    {
        this.classname = classname;
        this.filename = filename;
        this.methodname = methodname;
        this.lineNumber = lineNumber;
    }

    public String getClassName()
    {
        return classname;
    }

    /**
     * Get the name of the source file corresponding to this SourceLocation, if known.
     * @return  The relative path to the source file, or null
     */
    public String getFileName()
    {
        return filename;
    }

    public String getMethodName()
    {
        return methodname;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }
    
    /**
     * Return the location in the format "<class>.<method>"
     */
    public String toString()
    {
        return classname + "." + methodname;
    }
}
