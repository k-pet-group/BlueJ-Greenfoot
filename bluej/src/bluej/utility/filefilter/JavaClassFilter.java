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
package bluej.utility.filefilter;

import java.io.*;

/**
 * A FileFilter that only accepts Java class files.
 * An instance of this class can be used as a parameter for
 * the listFiles method of class File.
 *
 * @author  Axel Schmolitzky
 * @version $Id: JavaClassFilter.java 6215 2009-03-30 13:28:25Z polle $
 */
public class JavaClassFilter implements FileFilter
{
    /**
     * This method only accepts files that are Java class files.
     * Whether a file is a Java class file is determined by the fact that
     * its filename ends with ".class".
     */
    public boolean accept(File pathname)
    {
        return pathname.getName().endsWith(".class");
    }
}
