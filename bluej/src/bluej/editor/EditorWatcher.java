// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@mip.sdu.dk

package bluej.editor;

/**
 * @version $Id: EditorWatcher.java 2752 2004-07-07 09:39:56Z mik $
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
