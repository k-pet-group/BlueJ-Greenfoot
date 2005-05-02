//Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor;

/**
 * @version $Id: EditorWatcher.java 3357 2005-05-02 03:23:33Z davmac $
 * @author Michael Kolling
 * Interface between the editor and the rest of BlueJ
 * The editor uses this class
 */
public interface EditorWatcher
{
    /**
     * Called by Editor when a file is changed
     */
    void modificationEvent(Editor editor);

    /**
     * Called by Editor when a file is saved
     */
    void saveEvent(Editor editor);

    /**
     * Called by Editor when it is closed
     */
    void closeEvent(Editor editor);

    /**
     * Called by Editor when a breakpoint is been set/cleared
     * @param lineNo		the line number of the breakpoint
     * @param set		whether the breakpoint is set (true) or cleared
     * @return             An error message or null if okay.
     */
    String breakpointToggleEvent(Editor editor, int lineNo, 
                                 boolean set);

    /**
     * Called by Editor when a file is to be compiled
     */
    void compile(Editor editor);
    
    /**
     * Called by Editor when documentation is to be compiled
     */
    void generateDoc();

} // end class EditorWatcher
