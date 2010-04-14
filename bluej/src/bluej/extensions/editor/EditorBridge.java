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
package bluej.extensions.editor;

import bluej.pkgmgr.target.*;

/**
 * This class acts as a bridge between the extensions.editor classes
 * and BlueJ-internal to provide access to methods which
 * shouldn't be documented in the Extensions API Javadoc. By using this class,
 * those methods can be made package-local.
 *
 * This class should be excluded when the Javadoc API documentation is generated.
 *
 * @version $Id: EditorBridge.java 7337 2010-04-14 14:52:24Z nccb $
 * @author Damiano Bolla, University of Kent at Canterbury, 2004
 */ 
public class EditorBridge
{
    /**
     *  Returns a new Editor for the given ClassTarget.
     *
     * @param  aTarget  Bluej Class Target to retrieve the editor from
     * @return          Proxy editor object or null if it cannot be created
     */
    public static Editor newEditor(ClassTarget aTarget)
    {
        bluej.editor.Editor bjEditor = aTarget.getEditor();
        if (bjEditor == null) 
            return null;
        return new Editor(bjEditor);
    }
    
    public static bluej.editor.Editor getEditor(Editor editor)
    {
        return editor.getEditor();
    }
}
