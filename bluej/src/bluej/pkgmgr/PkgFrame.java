package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.debugger.ObjectBench;
import bluej.graph.Graph;
import bluej.graph.GraphEditor;
import bluej.utility.Utility;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.File;
import java.util.Hashtable;

/**
 ** Frame displaying a single BlueJ package.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: PkgFrame.java 429 2000-04-21 00:52:00Z mik $
 **/
public abstract class PkgFrame extends JFrame

implements ActionListener, ItemListener
{
    public static final String AppTitle = "BlueJ version " + bluej.Main.BLUEJ_VERSION;
    static final String noTitle = Config.getString("pkgmgr.noTitle");

    // static final Color bgColor = Config.getItemColour("colour.background");
    public Font PkgMgrFont = PrefMgr.getStandardFont();
    // Internal strings
    static final String chooseUsesFrom = Config.getString("pkgmgr.chooseUsesFrom");
    static final String chooseInhFrom = Config.getString("pkgmgr.chooseInhFrom");
    static final String chooseArrow = Config.getString("pkgmgr.chooseArrow");
    static final String packageSaved = Config.getString("pkgmgr.packageSaved");
    static final String errCloseAlert =  Config.getString("pkgmgr.error.close.title");
    static final String errCloseText =  Config.getString("pkgmgr.error.close.text");
    static final String newpkgTitle =  Config.getString("pkgmgr.newPkg.title");
    static final String createLabel =  Config.getString("pkgmgr.newPkg.buttonLabel");

    static int DEFAULT_WIDTH = 512;
    static int DEFAULT_HEIGHT = 450;

    static PackageChooser pkgChooser = null;	// chooser for packages only
    static JFileChooser fileChooser = null;	// chooser for all files/dirs
    JScrollPane classScroller = null;
    JScrollPane objScroller = null;

    Package pkg;
    GraphEditor editor;
    JPanel mainPanel;
    JLabel statusbar = new JLabel();

    JMenuBar menubar = null;

    public PkgFrame() {
    }

    protected abstract void handleAction(AWTEvent evt);

    public ObjectBench getObjectBench() {
        return null;
    }

    /**
     * Return the package shown by this frame.
     */
    public Package getPackage()
    {
        return pkg;
    }


    public void itemStateChanged(ItemEvent evt)
    {
        handleAction(evt);
    }

    public void actionPerformed(ActionEvent evt)
    {
        handleAction(evt);
    }

    public void resetDependencyButtons() {}

    private void createNewClass() {}


    /**
     *  Get (via a dialogue from the user) and return a new package name.
     *  If cancelled or an invalid name was specified, return null.
     */
    protected String getFileNameDialog(String title, String buttonLabel)
    {
        JFileChooser newChooser = getFileChooser(false);
        newChooser.setDialogTitle(title);

        int result = newChooser.showDialog(this, buttonLabel);

	if (result == JFileChooser.APPROVE_OPTION)
	    return newChooser.getSelectedFile().getPath();
	else if (result == JFileChooser.CANCEL_OPTION)
	    return null;
	else {
	    DialogManager.showError(this, "error-no-name");
	    return null;
	}
    }

    protected void doNewProject()
    {
        String newname = getFileNameDialog(newpkgTitle, createLabel);
        Project.createNewProject(newname);
    }

    /**
     * return a file chooser for choosing any directory (default behaviour)
     */
    private JFileChooser getFileChooser(boolean directoryOnly)
    {
        if(fileChooser == null) {
            fileChooser = new JFileChooser(
                           Config.getPropString("bluej.defaultProjectPath",
                                                "."));
            fileChooser.setFileView(new PackageFileView());
        }
        if (directoryOnly)
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        else
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        return fileChooser;
    }

    /**
     * Return a BlueJ package chooser, i.e. a file chooser which
     * recognises BlueJ packages and treats them differently.
     */
    private PackageChooser getPackageChooser() {
        if(pkgChooser == null)
            pkgChooser = new PackageChooser(
                           Config.getPropString("bluej.defaultProjectPath",
                                                "."));
        return pkgChooser;
    }


    /**
     * Open a dialog that asks for a package to open. If the dialog is
     * cancelled by the user, null is returned. Otherwise the result is
     * the name of an existing directory (either plain or a BlueJ package).
     */
    public String openPackageDialog() {
        PackageChooser chooser = getPackageChooser();

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String dirname = chooser.getSelectedFile().getPath();
            if(dirname != null) {
                // remove any trailing slashes
                int len = dirname.length();
                while((len > 0) && (dirname.charAt(len - 1) == File.separatorChar))
                    --len;
                dirname = dirname.substring(0, len);
            }
            return dirname;
        }

        return null;
    }

    public void doOpenPackage(String pkgname)
    {
        if(pkg.getDirName() == noTitle) {
            classScroller = new JScrollPane(editor);
            mainPanel.add(classScroller, "Center");
            invalidate();
            validate();
        }

        pkg.load(pkgname);
        //mik:Main.addPackage(pkg);
        setWindowTitle();
        enableFunctions(true);
        editor.setGraph(pkg);
        editor.repaint();
    }

    protected void enableFunctions(boolean enable) {}

    /**
     * Set the window title to show the current package name.
     */
    protected void setWindowTitle() {
        if (pkg.getName().equals(Package.noPackage))
            setTitle("BlueJ" + " - " + pkg.getDirName());
        else
            setTitle("BlueJ" + " - " + pkg.getName());
    }

    public void setStatus(String status) {
        if (statusbar != null)
            statusbar.setText(status);
    }

    public void clearStatus() {
        if (statusbar != null)
            statusbar.setText(" ");
    }

    /**
     * Set the frames cursor to a WAIT_CURSOR while system is busy
     */
    public void setWaitCursor(boolean wait)
    {
        if(wait)
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        else
            setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Removes current package details and reverts the frame to
     * similar to opening with no existing package.
     */
    public void removePackage() {
        closePackage();
        //mik: remove pkg from project and add new package
        pkg = new Package(pkg.getProject(), noTitle, this);
        editor = new GraphEditor(pkg,this);
        enableFunctions(false);
        repaint();
    }

    /**
     * Closes the current package.
     */
    public void closePackage() {
        if(classScroller != null)
            mainPanel.remove(classScroller);
        if (pkg != null) {
            pkg.removeLocalClassLoader();
            pkg.removeRemoteClassLoader();
            pkg.closeAllEditors();
            editor = null;

            // remove package from list of open packages
            //mik:Main.removePackage(pkg);

            pkg = null;

        }
    }

    /**
     * Commands - for lookup from events
     */
    Hashtable actions = new Hashtable();	// mapping from event source -> action

    static final int PROJ_COMMAND = 1000;
    static final int PROJ_NEW = PROJ_COMMAND;
    static final int PROJ_OPEN = PROJ_NEW + 1;
    static final int PROJ_CLOSE = PROJ_OPEN + 1;
    static final int PROJ_SAVE = PROJ_CLOSE + 1;
    static final int PROJ_SAVEAS = PROJ_SAVE + 1;
    static final int PROJ_IMPORTCLASS = PROJ_SAVEAS + 1;
    static final int PROJ_PRINT = PROJ_IMPORTCLASS + 1;
    static final int PROJ_QUIT = PROJ_PRINT + 1;

    static final String[] ProjCmds = {
        "new", "open", "close", "save", "saveAs", "importClass",
        "print", "quit"
    };

    static final KeyStroke[] ProjKeys = {
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK),
        null,
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK)
    };

    static final int[] ProjSeparators = {
        PROJ_SAVEAS, PROJ_IMPORTCLASS, PROJ_PRINT
    };

    static final int EDIT_COMMAND = PROJ_COMMAND + 100;
    static final int EDIT_NEWCLASS = EDIT_COMMAND;
    static final int EDIT_NEWPACKAGE = EDIT_NEWCLASS + 1;
    static final int EDIT_REMOVE = EDIT_NEWPACKAGE + 1;
    static final int EDIT_NEWUSES = EDIT_REMOVE + 1;
    static final int EDIT_NEWINHERITS = EDIT_NEWUSES + 1;
    static final int EDIT_REMOVEARROW = EDIT_NEWINHERITS + 1;


    static final String[] EditCmds = {
        "newClass", "newPackage", "remove", "newUses", "newInherits", 
        "removeArrow"
    };

    static final KeyStroke[] EditKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK),
        null,
        null,
        null,
        null,
        null
    };

    static final int[] EditSeparators = {
        EDIT_REMOVE  //, EDIT_REMOVEARROW
    };

    static final int TOOLS_COMMAND = EDIT_COMMAND + 100;
    static final int TOOLS_COMPILE = TOOLS_COMMAND;
    static final int TOOLS_COMPILESELECTED = TOOLS_COMPILE + 1;
    static final int TOOLS_REBUILD = TOOLS_COMPILESELECTED + 1;
    static final int TOOLS_BROWSE = TOOLS_REBUILD + 1;
    static final int TOOLS_PREFERENCES = TOOLS_BROWSE + 1;
//    static final int TOOLS_PREFERENCES = TOOLS_REBUILD + 1;

    static final String[] ToolsCmds = {
	"compile", "compileSelected", "rebuild", "browse", "preferences",
    };

    static final KeyStroke[] ToolsKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.SHIFT_MASK | Event.CTRL_MASK),
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK),
        null
    };

    static final int[] ToolsSeparators = {
	TOOLS_REBUILD,
	TOOLS_BROWSE
    };

    static final int VIEW_COMMAND = TOOLS_COMMAND + 100;
    static final int VIEW_SHOWUSES = VIEW_COMMAND;
    static final int VIEW_SHOWINHERITS = VIEW_SHOWUSES + 1;
    static final int VIEW_SHOWCONTROLS = VIEW_SHOWINHERITS + 1;
    static final int VIEW_SHOWTERMINAL = VIEW_SHOWCONTROLS + 1;
    static final int VIEW_CLEARTERMINAL = VIEW_SHOWTERMINAL + 1;

    static final String[] ViewCmds = {
        "showUses", "showInherits", "showExecControls", "showTerminal",
        "clearTerminal",
    };

    static final KeyStroke[] ViewKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_U, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK),
        null,
    };

    static final int[] ViewSeparators = {
        VIEW_SHOWINHERITS, VIEW_SHOWTERMINAL,
    };

    static final int GRP_COMMAND = VIEW_COMMAND + 100;
    static final int GRP_CREATE = GRP_COMMAND;
    static final int GRP_OPEN = GRP_CREATE + 1;
    static final int GRP_UPDATESELECTED = GRP_OPEN + 1;
    static final int GRP_UPDATEALL = GRP_UPDATESELECTED + 1;
    static final int GRP_COMMITSELECTED = GRP_UPDATEALL + 1;
    static final int GRP_COMMITALL = GRP_COMMITSELECTED + 1;
    static final int GRP_STATUSSELECTED = GRP_COMMITALL + 1;
    static final int GRP_STATUSALL = GRP_STATUSSELECTED + 1;
    static final int GRP_USERS = GRP_STATUSALL + 1;
    static final int GRP_CONFIG = GRP_USERS + 1;

    static final String[] GrpCmds = {
        "make", "open",
        "updateSelected", "updateAll",
        "commitSelected", "commitAll",
        "statusSelection", "statusAll",
        "users",
        "configuration"
    };

    static final KeyStroke[] GrpKeys = {
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
    };

    static final int[] GrpSeparators = {
        GRP_OPEN, GRP_COMMITALL, GRP_USERS
    };

    static final int HELP_COMMAND = GRP_COMMAND + 100;
    static final int HELP_ABOUT = HELP_COMMAND;
    static final int HELP_COPYRIGHT = HELP_ABOUT + 1;
    static final int HELP_TUTORIAL = HELP_COPYRIGHT + 1;
    static final int HELP_REFERENCE = HELP_TUTORIAL + 1;
    static final int HELP_STANDARDAPI = HELP_REFERENCE + 1;

    static final String[] HelpCmds = {
        "about", "copyright", "tutorial", "reference", "standardApi",
    };

    static final KeyStroke[] HelpKeys = {
        null,
        null,
        null,
        null,
        null,
    };

    static final int[] HelpSeparators = {
        HELP_COPYRIGHT
    };

    static final int[] CmdTypes = {
        PROJ_COMMAND, EDIT_COMMAND, TOOLS_COMMAND, VIEW_COMMAND,
        /* GRP_COMMAND, */ HELP_COMMAND
    };

    static final String[] CmdTypeNames = {
        "package", "edit", "tools", "view", /* "group", */ "help"
    };

    static final String[][] CmdStrings = {
        ProjCmds, EditCmds, ToolsCmds, ViewCmds, /* GrpCmds, */ HelpCmds
    };

    static final KeyStroke[][] CmdKeys = {
        ProjKeys, EditKeys, ToolsKeys, ViewKeys, /* GrpKeys, */ HelpKeys
    };

    static final int[][] CmdSeparators = {
        ProjSeparators, EditSeparators, ToolsSeparators, ViewSeparators,
        /* GrpSeparators, */ HelpSeparators
    };

    /**
     * Return the width of this frame without considering the width
     * of the contained GraphEditor.  Invoked by Package to resize
     * the frame once a new package has been opened in the GraphEditor.
     */
    //public abstract int getWidthWithoutGraphEditor();

    /**
     * Return the height of this frame without considering the height
     * of the contained GraphEditor.  Invoked by Package to resize
     * the frame once a new package has been opened in the GraphEditor.
     */
    //public abstract int getHeightWithoutGraphEditor();
}
