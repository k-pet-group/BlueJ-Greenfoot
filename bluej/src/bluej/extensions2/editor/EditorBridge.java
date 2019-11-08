/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2.editor;

import bluej.extensions2.SourceType;
import bluej.pkgmgr.target.ClassTarget;

/**
 * This class acts as a bridge between the extensions.editor classes
 * and BlueJ-internal to provide access to methods which
 * shouldn't be documented in the Extensions API Javadoc. By using this class,
 * those methods can be made package-local.
 *
 * This class should be excluded when the Javadoc API documentation is generated.
 *
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
    public static JavaEditor newJavaEditor(ClassTarget aTarget)
    {
        if (aTarget == null || aTarget.getSourceType() != SourceType.Java)
            return null;

        bluej.editor.Editor bjEditor = aTarget.getEditor();
        if (bjEditor == null)
            return null;
        return new JavaEditor(bjEditor.assumeText());
    }
    
    public static bluej.editor.Editor getJavaEditor(JavaEditor editor)
    {
        return editor.getEditor();
    }
}
