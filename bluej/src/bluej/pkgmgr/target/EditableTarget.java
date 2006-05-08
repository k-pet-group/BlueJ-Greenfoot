package bluej.pkgmgr.target;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import bluej.editor.*;
import bluej.pkgmgr.Package;

/**
 * A target in a package that can be edited as text
 *
 * @author  Michael Cahill
 * @version $Id: EditableTarget.java 4116 2006-05-08 13:18:36Z polle $
 */
public abstract class EditableTarget extends Target
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
     * Ensure that the source file of this target is up-to-date (i.e.
     * that any possible unsaved changes in an open editor window are 
     * saved).
     */
    public void ensureSaved() throws IOException
    {
        if(editor != null) {
            editor.save();
        }
    }
    
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

    /**
     * Return true if this editor has been opened at some point since this project was opened.
     */
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

    /*
     * Called by Editor when a file is changed
     */
    public void modificationEvent(Editor editor) {}

    /*
     * Called by Editor when a file is saved
     */
    public void saveEvent(Editor editor) {}

    /*
     * Called by Editor when a file is closed
     */
    public void closeEvent(Editor editor) {}

    /*
     * Called by Editor when a breakpoint is been set/cleared
     */
    public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
    { return null; }

    /*
     * The "compile" function was invoked in the editor
     */
    public void compile(Editor editor) {}

    // --- end of EditorWatcher interface ---
}
