package bluej.pkgmgr;

import java.io.*;
import javax.swing.text.*;

import bluej.editor.Editor;
import bluej.editor.EditorWatcher;
import bluej.utility.Utility;
import bluej.utility.DialogManager;

/**
 * A target in a package that can be edited as text
 *
 * @author  Michael Cahill
 * @version $Id: EditableTarget.java 1018 2001-12-04 05:08:03Z ajp $
 */
public abstract class EditableTarget extends DependentTarget
    implements EditorWatcher
{
    protected Editor editor;

//    private Document document;

    protected EditableTarget(Package pkg, String name)
    {
        super(pkg, name);

/* ajp experimental code
        try {
            BufferedReader br = new BufferedReader(new FileReader(getSourceFile()));
            document = new PlainDocument();

            EditorKit ek = new DefaultEditorKit();

            ek.read(br, document, 0);
        }
        catch(Exception e) {

        } */
    }

    /**
     * @returns a boolean indicating whether this target contains source code
     */
    protected abstract boolean isCode();

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
    protected void open()
    {
        Editor editor = getEditor();

        if(editor == null)
            getPackage().showError("error-open-source");
        else
            editor.setVisible(true);

        /*((AbstractDocument)document).dump(new PrintStream(System.out));*/
    }

    /**
    ** Called to reopen the editor for this target
    **/
    protected void reopen()
    {
        if(editor != null)
            editor.reloadFile();
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
