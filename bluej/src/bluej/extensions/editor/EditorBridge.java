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
 * @version $Id: EditorBridge.java 3004 2004-09-15 14:15:00Z mik $
 * @author Damiano Bolla, University of Kent at Canterbury, 2004
 */ 
public class EditorBridge
{
    /**
     *  Returns a new Editor for the given ClassTarget
     *
     * @param  aTarget  Bluej Class Target to retrieve the editor from
     * @return          Proxy editor object or null if it cannot be created
     */
    public static Editor newEditor(ClassTarget aTarget)
    {
        bluej.editor.Editor bjEditor = aTarget.getEditor(false);
        if ( bjEditor == null ) return null;
        return new Editor(bjEditor);
    }
}
