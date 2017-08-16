/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2016  Michael Kolling and John Rosenberg
 
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
package bluej.collect;

import java.io.File;

import bluej.compiler.Diagnostic;

/**
 * Wraps a Diagnostic with two extra pieces of information:
 *
 * - Whether the message was shown to the user (previously, only the first message was shown;
 *   now it is all the messages unless there is an internal problem showing it)
 *
 * - What the original file name was (.stride file for Stride)
 */
// package-visible
public class DiagnosticWithShown
{
    // The wrapped Diagnostic
    private final Diagnostic diagnostic;
    // Was the error shown to the user already?
    private final boolean shownToUser;
    // File name of original file (.stride file for Stride, .java file for Java)
    private final File userFileName;

    /**
     *
     * @param diagnostic
     * @param shownToUser Will now usually be true (unless editor cannot be found, or similar) , since we switched to red underlines for errors.
     */
    public DiagnosticWithShown(Diagnostic diagnostic, boolean shownToUser, File userFileName)
    {
        this.diagnostic = diagnostic;
        this.shownToUser = shownToUser;
        this.userFileName = userFileName;
    }
    public Diagnostic getDiagnostic()
    {
        return diagnostic;
    }
    public boolean wasShownToUser()
    {
        return shownToUser;
    }
    public File getUserFileName()
    {
        return userFileName;
    }
}