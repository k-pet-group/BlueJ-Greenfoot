package bluej.pkgmgr;

import bluej.editor.Editor;
import bluej.editor.EditorWatcher;
import bluej.utility.Utility;

/** 
 ** @version $Id: EditableTarget.java 114 1999-06-08 04:02:49Z mik $
 ** @author Michael Cahill
 **
 ** A target in a package that can be edited as text
 **/

public abstract class EditableTarget extends Target

	implements EditorWatcher
{
    protected Editor editor;
	
    protected EditableTarget(Package pkg, String name)
    {
	super(pkg, name);
    }

    /**
    ** @returns a boolean indicating whether this target contains source code
    **/
    protected abstract boolean isCode();
	
    /**
    ** @returns the name of the (text) file this target corresponds to.
    **/
    protected abstract String sourceFile();
	
    /**
    ** @return the editor object associated with this target
    **/
    public abstract Editor getEditor();

    /**
    ** @return the current view being shown - one of the Editor constants
    **/
    public abstract int getDisplayedView();
	
    /**
    ** Called to open the editor for this target
    **/
    protected void open()
    {
	// force getting a new editor
	if(editor == null)
	    getEditor();
	else
	    editor.show(getDisplayedView());

	if(editor == null)
	    Utility.showError(pkg.getFrame(),
			      "There is a problem opening the source of\n" +
			      "this class.  Serious trouble!");
    }

    /**
    ** Called to reopen the editor for this target
    **/
    protected void reopen()
    {
	if(editor != null)
	    editor.reloadFile();

	open();
    }
	
    public boolean usesEditor(Editor editor)
    {
	return (this.editor == editor);
    }

    public boolean editorOpen()
    {
	return (editor!=null);
    }

    // --- EditorWatcher interface ---
    // (The EditorWatcher methods are typically redefined in subclasses)

    /**
     ** Called by Editor when a file is changed
     ** @param filename	the name of the file that was modified
     **/
    public void modificationEvent(Editor editor) {}

    /**
     ** Called by Editor when a file is saved
     ** @param editor	the editor object being saved
     **/
    public void saveEvent(Editor editor) {}

    /**
     ** Called by Editor when a file is closed
     ** @param editor	the editor object being closed
     **/
    public void closeEvent(Editor editor) {}
    
    /**
     ** Called by Editor when a breakpoint is been set/cleared
     ** @param filename	the name of the file that was modified
     ** @param lineNo	the line number of the breakpoint
     ** @param set		whether the breakpoint is set (true) or cleared
     **/
    public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
    { return null; }

    /**
     ** Called by Editor to change the view displayed by an editor
     ** @param viewname	the name of the view to display, should be 
     **			one of bluej.editor.Editor.PUBLIC, etc.
     ** @returns a boolean indicating if the change was allowed
     **/
    public boolean changeView(Editor editor, int viewType) { return false; }

    /**
     ** The "compile" function was invoked in the editor
     **/
    public void compile(Editor editor) {}

    // --- end of EditorWatcher interface ---
}
