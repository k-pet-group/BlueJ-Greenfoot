/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2012,2014,2019,2026  Michael Kolling and John Rosenberg

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

import java.io.File;
import java.util.regex.Pattern;

import bluej.pkgmgr.Package;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class that holds information about the location of an exception.
 * Effectively, this represents one line in a stack-trace.
 * @author nccb
 *
 */
public class ExceptionSourceLocation
{
    @OnThread(Tag.Any)
    static final Pattern STACK_TRACE_PATTERN = Pattern.compile("at (\\S+)\\((\\S+\\.(?:java|kt)):(\\d+)\\)");

    /** The package */
    private Package pkg;
    /** The reported source filename, including its extension */
    private String sourceFileName;
    /** The line number */
    private int lineNumber;

    /** The starting position in the document of the bit to be linked */
    private int startPos;
    /** The ending position in the document of the bit to be linked */
    private int endPos;

    /**
     * Create a source link from a frame whose file belongs to the given package.
     *
     * @param startPos first linked character in the terminal line
     * @param endPos exclusive end of the linked text
     * @param pkg package owning the source target
     * @param sourceFileName reported source filename, not the JVM class name
     * @param lineNumber one-based source line
     */
    public ExceptionSourceLocation(int startPos, int endPos,
            Package pkg, String sourceFileName, int lineNumber)
    {
        this.startPos = startPos;
        this.endPos = endPos;
        this.pkg = pkg;
        this.sourceFileName = sourceFileName;
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
        pkg.exceptionMessage(new File(pkg.getPath(), sourceFileName).getAbsolutePath(), lineNumber);

    }


}