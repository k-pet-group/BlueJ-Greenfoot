package bluej.pkgmgr.target;

import java.awt.Rectangle;
import java.io.File;
import java.util.Properties;

import bluej.editor.*;
import bluej.pkgmgr.Package;

/**
 * A target in a package that can be edited as text
 *
 * @author  Michael Cahill
 * @version $Id: EditableTarget.java 2642 2004-06-21 14:53:23Z polle $
 */
public abstract class EditableTarget extends DependentTarget
    implements EditorWatcher
{
    protected Editor editor;
    protected Rectangle editorBounds;

    protected EditableTarget(Package pkg, String name)
    {
        super(pkg, name);
    }

    /**
     * @returns the name of the (text) file this target corresponds to.
     */
    protected abstract File getSourceFile();

    /**
     * @return the editor object associated with this target
     */
    public abstract Editor getEditor();

    /**
     * Called to open the editor for this target
     */
    public void open()
    {
        Editor editor = getEditor();

        if(editor == null)
            getPackage().showError("error-open-source");
        else
            editor.setVisible(true);
    }

    /**
     *
     */
    protected void close()
    {
        getEditor().close();
    }

    public boolean usesEditor(Editor editor)
    {
        return (this.editor == editor);
    }

    public boolean editorOpen()
    {
        return (editor!=null);
    }
    
    public void load(Properties props, String prefix) throws NumberFormatException
    {
        super.load(props, prefix);
        if(props.getProperty(prefix + ".editor.x") != null) {
	        editorBounds = new Rectangle(Integer.parseInt(props.getProperty(prefix + ".editor.x")),
	                Integer.parseInt(props.getProperty(prefix + ".editor.y")), 
	                Integer.parseInt(props.getProperty(prefix + ".editor.width")),
	                Integer.parseInt(props.getProperty(prefix + ".editor.height")));
        }
    }

    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);
        if (editor != null) {
            editorBounds = editor.getBounds();            
        } 
        if(editorBounds!=null) {
            props.put(prefix + ".editor.x", String.valueOf((int) editorBounds.getX()));
            props.put(prefix + ".editor.y", String.valueOf((int) editorBounds.getY()));
            props.put(prefix + ".editor.width", String.valueOf((int) editorBounds.getWidth()));
            props.put(prefix + ".editor.height", String.valueOf((int) editorBounds.getHeight()));
        }
    }
    
    // --- EditorWatcher interface ---
    // (The EditorWatcher methods are typically redefined in subclasses)

    /**
     * Called by Editor when a file is changed
     * @param filename	the name of the file that was modified
     */
    public void modificationEvent(Editor editor) {}

    /**
     * Called by Editor when a file is saved
     * @param editor	the editor object being saved
     */
    public void saveEvent(Editor editor) {}

    /**
     * Called by Editor when a file is closed
     * @param editor	the editor object being closed
     */
    public void closeEvent(Editor editor) {}

    /**
     * Called by Editor when a breakpoint is been set/cleared
     * @param filename	the name of the file that was modified
     * @param lineNo	the line number of the breakpoint
     * @param set	whether the breakpoint is set (true) or cleared
     */
    public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
    { return null; }

    /**
     * The "compile" function was invoked in the editor
     */
    public void compile(Editor editor) {}

    // --- end of EditorWatcher interface ---
}
