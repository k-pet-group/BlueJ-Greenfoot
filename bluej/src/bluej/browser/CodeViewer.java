package bluej.browser;

import javax.swing.*; 
import javax.swing.border.*; 
import java.awt.*; 

import bluej.editor.moe.MoeEditorManager;
import bluej.editor.moe.MoeEditor;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;
import bluej.pkgmgr.ClassTarget;
import bluej.views.View;
import bluej.views.ViewFilter;
import bluej.views.EditorPrintWriter;
import bluej.utility.ClasspathSearcher;

/**
 * A read only (Moe) text editor for showing views of a class selected
 * in the library browser.  Defaults to a public view of a class.
 * 
 * @author $Author: mik $
 * @version $Id: CodeViewer.java 36 1999-04-27 04:04:54Z mik $
 */
public class CodeViewer implements EditorWatcher {
	private MoeEditorManager edMgr = new MoeEditorManager(false);
		
	private Editor editor = null;
	private ClassTarget theClass = null;
	private String className = null;
	private boolean hasSource = true;
	
	/**
	 * Creates a new CodeViewer.  Does nothing.
	 */
  public CodeViewer() {}

	/**
	 * Return the editor used by the code viewer, 
	 * or null if the editor hasn't been created yet.
	 * 
	 * @return the editor used by the code viewer.
	 */
  public Editor getEditor() {
	return editor;
  }
  
	/**
	 * Spawn an independent Moe Editor window for the specified file.
	 * As this editor is to be used in read only mode, don't register
	 * a watcher for it.  Set the editor to show the public view of the class.
	 * 
	 * @param theClass the ClassTarget representing the class to view
	 * @param className the name of the class in package notation (i.e., a.b.c)
	 * @param isCompiled true if <code>fileName</code> is compiled
	 */
	public void openClass(ClassTarget theClass, String className, boolean isCompiled) {
		this.theClass = theClass;
		this.className = className;
		String fileName = theClass.sourceFile();
		editor = edMgr.openClass(null, 
					 "Library Browser - " + fileName, 
					 this,
					 isCompiled,
					 null);
		showView(editor, Editor.PUBLIC);
		
	}

	/**
	 * Open a class that has no corresponding source file.
	 * 
	 * @param theClass the ClassTarget representing the class to view
	 * @param className the name of the class in package notation (i.e., a.b.c)
	 * @param isCompiled true if <code>fileName</code> is compiled
	 */
	public void openClassWithNoSource(ClassTarget theClass, String className, boolean isCompiled) {
		hasSource = false;
		openClass(theClass, className, isCompiled);
	}
	
	/**
	 * Called when the source of the editor has been changed.  Unimplemented because
	 * the editor used is read only.  Part of the EditorWatcher interface.
	 * 
	 * @param editor the editor with the modified contents.
	 */
	public void modificationEvent(Editor editor) {}

	/**
	 * Called when the file open in the editor is saved.  Unimplemented because
	 * the editor used is read only.  Part of the EditorWatcher interface.  
	 * 
	 * @param editor the editor with the file to be saved.
	 **/
	public void saveEvent(Editor editor) {}

	/**
	 * Called when the file open in the editor is closed.  Unimplemented because
	 * the CodeViewer has no interest in the file when it is closed as it has not 
	 * been modified.  Part of the EditorWatcher interface.  
	 * 
	 * @param editor the editor with the file to be closed.
	 **/
	public void closeEvent(Editor editor) {}

	/**
	 * Called by Editor when a breakpoint is been set/cleared.  Unimplemented 
	 * because setting of breakpoints is not allowed in the read only editor.  
	 * Part of the EditorWatcher interface.  
	 * 
	 * @arg lineNo the line number of the breakpoint
	 * @arg set	whether the breakpoint is set (true) or cleared
	 */
	public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
        { return null; }

	/**
	 ** Called by Editor to change the view displayed by an editor
	 ** @arg viewType	the view to display, should be
	 **			one of bluej.editor.Editor.PUBLIC, etc.
	 ** @returns a boolean indicating if the change was allowed
	 **/
	public boolean changeView(Editor editor, int viewType) {
		showView(editor, viewType);
		return true; 
	}

	/**
	 * Called by Editor when a file is to be compiled.  Unimplemented because compilation is not allowed in the read only editor.
	 * Part of the EditorWatcher interface.  
	 **/
	public void compile(Editor editor) {}
	
	/**
	 * Handles the changing of views within the open editor.
	 * Stops view changing when no source is available (only allowed PUBLIC view).
	 * 
	 * @param editor the editor with the file to change the view of
	 * @param viewType the type of view requested.
	 */
	private void showView(Editor editor, int viewType) { 
		editor.setReadOnly(false);
		if (!hasSource)
			viewType = Editor.PUBLIC;
		else if (viewType == Editor.IMPLEMENTATION) {
			editor.showFile(theClass.sourceFile(), true, null);
		} else {
		
			try {
				editor.clear();
				View view = new View(Class.forName(className), new ClasspathSearcher(System.getProperty("java.class.path")));
				int filterType = 0;
				if(viewType == Editor.PUBLIC)
				    filterType = ViewFilter.PUBLIC;
				else if(viewType == Editor.PACKAGE)
				    filterType = ViewFilter.PACKAGE;
				else if(viewType == Editor.INHERITED)
				    filterType = ViewFilter.PROTECTED;
						
				ViewFilter filter= (filterType != 0) ? new ViewFilter(filterType) : null;
				view.print(new EditorPrintWriter(editor), filter);
				editor.show(viewType);
			} catch (ClassNotFoundException cnfe) {
				cnfe.printStackTrace();
			}
		}
		editor.setReadOnly(true);
    }
}
