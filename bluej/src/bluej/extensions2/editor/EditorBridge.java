/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2019,2021  Michael Kolling and John Rosenberg
 
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

import bluej.editor.stride.FrameEditor;
import bluej.extensions2.BClass;
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
     *  Returns a new JavaEditor for the given ClassTarget.
     *
     * @param  aTarget  Bluej Class Target to retrieve the editor from
     * @return          Proxy editor object or null if it cannot be created
     */
    public static JavaEditor newJavaEditor(BClass bClass, ClassTarget aTarget)
    {
        if (aTarget == null || aTarget.getSourceType() != SourceType.Java)
            return null;

        bluej.editor.Editor bjEditor = aTarget.getEditor();
        if (bjEditor == null)
            return null;
        return new JavaEditor(bClass, bjEditor.assumeText());
    }

    /**
     * Gets a new JavaEditor instance for the given class, but only if the editor is already open.
     * @return Null if there is a problem or the editor is not open.
     */
    public static JavaEditor newJavaEditorIfOpen(BClass bClass, ClassTarget aTarget)
    {
        if (aTarget == null || aTarget.getSourceType() != SourceType.Java)
            return null;

        bluej.editor.Editor bjEditor = aTarget.getEditorIfOpen();
        if (bjEditor == null)
            return null;
        return new JavaEditor(bClass, bjEditor.assumeText());
    }

    /**
     * Returns a new StrideEditor for the given ClassTarget.
     *
     * @param  aTarget  Bluej Class Target to retrieve the editor from
     * @return          Proxy editor object or null if it cannot be created
     */
    public static StrideEditor newStrideEditor(BClass bClass, ClassTarget aTarget)
    {
        if (aTarget == null || aTarget.getSourceType() != SourceType.Stride)
            return null;

        bluej.editor.Editor bjEditor = aTarget.getEditor();
        if (bjEditor == null || (!(bjEditor instanceof FrameEditor)))
            return null;
        return new StrideEditor(bClass, (FrameEditor)bjEditor);
    }

    public static StrideEditor newStrideEditorIfOpen(BClass bClass, ClassTarget aTarget)
    {
        if (aTarget == null || aTarget.getSourceType() != SourceType.Stride)
            return null;

        bluej.editor.Editor bjEditor = aTarget.getEditorIfOpen();
        if (bjEditor == null || (!(bjEditor instanceof FrameEditor)))
            return null;
        return new StrideEditor(bClass, (FrameEditor)bjEditor);
    }
    
    public static bluej.editor.Editor getJavaEditor(JavaEditor editor)
    {
        return editor.getEditor();
    }
}
