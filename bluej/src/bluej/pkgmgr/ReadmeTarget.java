package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.graph.GraphEditor;
import bluej.utility.*;
import bluej.editor.*;

import java.util.Properties;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**
 * A parent package
 *
 * @author  Andrew Patterson
 * @version $Id: ReadmeTarget.java 538 2000-06-12 10:20:59Z ajp $
 */
public class ReadmeTarget extends Target
    implements ActionListener, EditorWatcher
{
    static final int DEF_WIDTH = 60;
    static final int DEF_HEIGHT = 60;

    static final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    static final Color colBorder = Config.getItemColour("colour.target.border");
    static final Color textfg = Config.getItemColour("colour.text.fg");

    protected Editor editor;

    public ReadmeTarget(Package pkg)
    {
        // create the target with an identifier name that cannot be
        // a valid java name
        super(pkg, "@README", 10, 10, DEF_WIDTH, DEF_HEIGHT);
    }

    public void load(Properties props, String prefix) throws NumberFormatException
    {
    }

    public void save(Properties props, String prefix)
    {
    }

    /**
     * @return the name of the (text) file this target corresponds to.
     */
    public File getSourceFile()
    {
        return new File(getPackage().getPath(), "README.TXT");
    }

    /**
     * Copy all the files belonging to this target to a new location.
     * For package targets, this has not yet been implemented.
     *
     * @arg directory The directory to copy into (ending with "/")
     */
    public boolean copyFiles(String directory)
    {
        return true;
    }

    public boolean isResizable()
    {
        return false;
    }

    public boolean isMoveable()
    {
        return false;
    }

    public boolean isSaveable()
    {
        return false;
    }

    Color getBackgroundColour()
    {
        return defaultbg;
    }

    Color getBorderColour()
    {
        return colBorder;
    }

    Color getTextColour()
    {
        return textfg;
    }

    Font getFont()
    {
        return PrefMgr.getStandardFont();
    }

    /**
     ** @return the editor object associated with this target. May be null
     **  if there was a problem opening this editor.
     **/
    public Editor getEditor()
    {
        if(editor == null)
            editor = getPackage().editorManager.openText(getSourceFile().getPath(),
                                                        "README.TXT", this);
        return editor;
    }

    // --- EditorWatcher interface ---

    /**
     * Called by Editor when a file is changed
     */
    public void modificationEvent(Editor editor)
    {
    }

    /**
     * Called by Editor when a file is saved
     * @param editor	the editor object being saved
     */
    public void saveEvent(Editor editor)
    {
    }

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
     *
     * @return  null if there was no problem, or an error string
     */
    public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
    {
        return null;
    }

    /**
     * Called by Editor to change the view displayed by an editor
     * @param viewname	the name of the view to display, should be
     * 		one of bluej.editor.Editor.PUBLIC, etc.
     * @return a boolean indicating if the change was allowed
     */
    public boolean changeView(Editor editor, int viewType)
    {
        return true;
    }


    public void compile(Editor editor)
    {
    }

    // --- end of EditorWatcher interface ---

    public void actionPerformed(ActionEvent e)
    {
    }

    public void draw(Graphics2D g)
    {
        // draw the shadow
        g.setColor(shadowCol);
        g.fillRect(SHAD_SIZE, height, width, SHAD_SIZE);
        g.fillRect(width, HANDLE_SIZE + SHAD_SIZE, SHAD_SIZE, height - HANDLE_SIZE);

        // draw folded paper edge
        int xpoints[] = { 1, width - HANDLE_SIZE, width, width, 1 };
        int ypoints[] = { 1, 1, HANDLE_SIZE + 1, height, height };

        Polygon p = new Polygon(xpoints, ypoints, 5);

        int thickness = ((flags & F_SELECTED) == 0) ? 1 : 2;

        g.setColor(Color.white);
        g.fill(p);
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(thickness));
        g.draw(p);

        g.drawLine(width - HANDLE_SIZE, 1,
                   width - HANDLE_SIZE, HANDLE_SIZE);
        g.drawLine(width - HANDLE_SIZE, HANDLE_SIZE,
                   width - 2, HANDLE_SIZE);

        g.setStroke(new BasicStroke(1));
        g.drawLine(10, HANDLE_SIZE*2 - 10, width - 10, HANDLE_SIZE*2 - 10);
        g.drawLine(10, HANDLE_SIZE*2 - 5, width - 10, HANDLE_SIZE*2 - 5);
        g.drawLine(10, HANDLE_SIZE*2, width - 10, HANDLE_SIZE*2);
        g.drawLine(10, HANDLE_SIZE*2 + 5, width - 10, HANDLE_SIZE*2 + 5);
        g.drawLine(10, HANDLE_SIZE*2 + 10, width - 10, HANDLE_SIZE*2 + 10);

    }

    /**
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     */
    public void doubleClick(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        if(getEditor() == null) {
            getPackage().showError("error-open-readme");

            try {
                getSourceFile().createNewFile();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        else
            getEditor().setVisible(true);
    }

    public void popupMenu(MouseEvent evt, int x, int y, GraphEditor editor)
    {
    }
}
