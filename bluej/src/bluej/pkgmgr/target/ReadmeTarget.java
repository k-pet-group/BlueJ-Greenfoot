package bluej.pkgmgr.target;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Properties;

import javax.swing.*;
import javax.swing.JPopupMenu;

import bluej.Config;
import bluej.editor.*;
import bluej.graph.GraphEditor;
import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;

/**
 * A parent package
 *
 * @author  Andrew Patterson
 * @version $Id: ReadmeTarget.java 2488 2004-04-06 09:42:07Z fisker $
 */
public class ReadmeTarget extends Target
    implements ActionListener, EditorWatcher
{
    static final int WIDTH = 40;
    static final int HEIGHT = 50;
    static final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    static final Color colBorder = Config.getItemColour("colour.target.border");
    static final Color textfg = Config.getItemColour("colour.text.fg");
    static String openStr = Config.getString("pkgmgr.packagemenu.open");
    static String removeStr = Config.getString("pkgmgr.packagemenu.remove");
    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");
    
    public static final String README_ID = "@README";

    protected Editor editor;


    public ReadmeTarget(Package pkg)
    {
        // create the target with an identifier name that cannot be
        // a valid java name
        super(pkg, README_ID);
        
        setPos(10, 10);
        setSize(WIDTH, HEIGHT);
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
        return new File(getPackage().getPath(), Package.readmeName);
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
    
    public boolean isHandle(){
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
            editor = Package.editorManager.openText(
                                                 getSourceFile().getPath(),
                                                 Package.readmeName, this);
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

    public void compile(Editor editor)
    {
    }

    // --- end of EditorWatcher interface ---

    public void actionPerformed(ActionEvent e)
    {
    }


    private void openEditor()
    {
       // try to open it and if not there, create it
       if(getEditor() == null) {
           try {
               getSourceFile().createNewFile();
           }
           catch (IOException ioe) {
               ioe.printStackTrace();
           }
       }
    
       // now try again to open it
       if(getEditor() != null)
           getEditor().setVisible(true);
    }

    /**
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     */
    public void doubleClick(MouseEvent evt, GraphEditor editor)
    {
       openEditor();
    }

    public void popupMenu(int x, int y, GraphEditor editor)
    {
        JPopupMenu menu = createMenu(null);
        if (menu != null){
            editor.add(menu);
            menu.show(editor, x, y);
        }
    }
    
    /**
     * Construct a popup menu which displays all our parent packages.
     */
    private JPopupMenu createMenu(Class cl)
    {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item;
           
        Action openAction = new OpenAction(openStr);

        item = menu.add(openAction);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
        return menu;
       }

	/* (non-Javadoc)
	 * @see bluej.editor.EditorWatcher#generateDoc()
	 */
	public void generateDoc() 
    {
	}
    
    private class OpenAction extends AbstractAction
    {

        public OpenAction(String menu)
        {
            super(menu);
        }

        public void actionPerformed(ActionEvent e)
        {
            openEditor();
        }
    }
    
    public void remove(){
        // The user is not permitted to remove the readmefile
   }
}
