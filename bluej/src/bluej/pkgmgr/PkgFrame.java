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
 ** @version $Id: PkgFrame.java 305 1999-12-09 23:50:57Z ajp $
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

    public abstract Package getPackage();

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

    protected void doNewPackage()
    {
        String newname = getFileNameDialog(newpkgTitle, createLabel);

        if (newname != null) {

	    // check whether name is already in use
	    File dir = new File(newname);
	    if(dir.exists()) {
		DialogManager.showError(this, "directory-exists");
		return;
	    }

	    Package newPkg;

	    // Open Here if current window is empty
	    if (pkg.getDirName() == noTitle || pkg.getDirName() == null) {
		newPkg = new Package(newname, this);
		newPkg.save();
		doOpenPackage(newname);
	    }
	    else {
		// Otherwise open it in a new window
		PkgFrame frame = PkgMgrFrame.createFrame(null);
		newPkg = new Package(newname, frame);
		newPkg.save();
		frame.doOpenPackage(newname);
		frame.setVisible(true);
	    }
	}
    }

    /**
     * return a file chooser for choosing any directory (default behaviour)
     */
    private JFileChooser getFileChooser(boolean directoryOnly)
    {
        if(fileChooser == null) {
            fileChooser = new JFileChooser(".");
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
            pkgChooser = new PackageChooser(".");
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
        Main.addPackage(pkg);
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
        getGlassPane().setVisible(wait);
    }

    /**
     * Removes current package details and reverts the frame to 
     * similar to opening with no existing package.
     */
    public void removePackage() {
        closePackage();
        pkg = new Package(noTitle,this);
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
            Main.removePackage(pkg);

            pkg = null;

        }
    }    

    /**
     * Commands - for lookup from events
     */
    Hashtable actions = new Hashtable();	// mapping from event source -> action

    static final int PKG_COMMAND = 1000;
    static final int PKG_NEW = PKG_COMMAND;
    static final int PKG_OPEN = PKG_NEW + 1;
    static final int PKG_CLOSE = PKG_OPEN + 1;
    static final int PKG_SAVE = PKG_CLOSE + 1;
    static final int PKG_SAVEAS = PKG_SAVE + 1;
    static final int PKG_IMPORTCLASS = PKG_SAVEAS + 1;
    static final int PKG_IMPORTPKG = PKG_IMPORTCLASS + 1;
    static final int PKG_EXPORTPKG = PKG_IMPORTPKG + 1;
    static final int PKG_PRINT = PKG_EXPORTPKG + 1;
    static final int PKG_QUIT = PKG_PRINT + 1;

    static final String[] PkgCmds = {
        "new", "open", "close", "save", "saveAs", "importClass", "importPackage", 
        "exportPackage", "print", "quit"
    };

    static final KeyStroke[] PkgKeys = {
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK),
        null,
        null,
        null,
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK)
    };

    static final int[] PkgSeparators = {
        PKG_SAVEAS, PKG_EXPORTPKG, PKG_PRINT
    };

    static final int EDIT_COMMAND = PKG_COMMAND + 100;
    static final int EDIT_NEWCLASS = EDIT_COMMAND;
    static final int EDIT_REMOVECLASS = EDIT_NEWCLASS + 1;
    static final int EDIT_NEWUSES = EDIT_REMOVECLASS + 1;
    static final int EDIT_NEWINHERITS = EDIT_NEWUSES + 1;
    static final int EDIT_REMOVEARROW = EDIT_NEWINHERITS + 1;


    static final String[] EditCmds = {
        "newClass", "removeClass", "newUses", "newInherits", "removeArrow" 
    };

    static final KeyStroke[] EditKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK), 
        KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK),
        null,
        null,
        null,
        null
    };

    static final int[] EditSeparators = {
        EDIT_REMOVECLASS //, EDIT_REMOVEARROW
    };

    static final int TOOLS_COMMAND = EDIT_COMMAND + 100;
    static final int TOOLS_COMPILE = TOOLS_COMMAND;
    static final int TOOLS_COMPILESELECTED = TOOLS_COMPILE + 1;
    static final int TOOLS_REBUILD = TOOLS_COMPILESELECTED + 1;
    static final int TOOLS_BROWSE = TOOLS_REBUILD + 1;
    static final int TOOLS_PREFERENCES = TOOLS_BROWSE + 1;

    static final String[] ToolsCmds = {
	"compile", "compileSelected", "rebuild", "browse", "preferences"
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
        PKG_COMMAND, EDIT_COMMAND, TOOLS_COMMAND, VIEW_COMMAND,
        /* GRP_COMMAND, */ HELP_COMMAND
    };

    static final String[] CmdTypeNames = {
        "package", "edit", "tools", "view", /* "group", */ "help"
    };

    static final String[][] CmdStrings = {
        PkgCmds, EditCmds, ToolsCmds, ViewCmds, /* GrpCmds, */ HelpCmds
    };

    static final KeyStroke[][] CmdKeys = {
        PkgKeys, EditKeys, ToolsKeys, ViewKeys, /* GrpKeys, */ HelpKeys
    };

    static final int[][] CmdSeparators = {
        PkgSeparators, EditSeparators, ToolsSeparators, ViewSeparators, 
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
