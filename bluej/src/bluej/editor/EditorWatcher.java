package bluej.editor;

/**
** @version $Id: EditorWatcher.java 361 2000-01-14 00:59:47Z mik $
** @author Michael Cahill
** Interface between the editor and the rest of BlueJ
** The editor uses this class
**/
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
     * Called by Editor to change the view displayed by an editor
     * @param viewType	the view to display, should be
     *			one of bluej.editor.Editor.PUBLIC, etc.
     * @return a boolean indicating if the change was allowed
     */
    boolean changeView(Editor editor, int viewType);

    /**
     * Called by Editor when a file is to be compiled
     */
    void compile(Editor editor);

} // end class EditorWatcher
