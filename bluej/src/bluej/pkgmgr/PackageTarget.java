package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.graph.GraphEditor;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import java.util.Properties;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 ** @version $Id: PackageTarget.java 429 2000-04-21 00:52:00Z mik $
 ** @author Michael Cahill
 **
 ** A link to a package embedded in another package.
 **/
public class PackageTarget extends Target implements ActionListener
{
    static final Color defaultbg = Config.getItemColour("colour.package.bg.default");
    static final Color ribboncolour = defaultbg.darker().darker();
    static final Color bordercolour = Config.getItemColour("colour.target.border");
    static final Color textbg = Config.getItemColour("colour.text.bg");
    static final Color textfg = Config.getItemColour("colour.text.fg");

    protected String packageDir = null;
    protected String packageName = null;

    static String useStr = Config.getString("browser.classchooser.packagemenu.use");
    static String openStr = Config.getString("browser.classchooser.packagemenu.open");

    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    public PackageTarget(Package pkg, String shortName, String fullName)
    {
	super(pkg, shortName);
	packageDir = shortName;
	packageName = fullName;
    }

    public PackageTarget(Package pkg)
    {
	super(pkg, null);
    }

    public String getName()
    {
	return packageName;
    }

    public void load(Properties props, String prefix) throws NumberFormatException
    {
	super.load(props, prefix);

//	packageDir = Config.getPath(props, prefix + ".packageDir");
	packageName = props.getProperty(prefix + ".packageName");
    }

    public void save(Properties props, String prefix)
    {
	super.save(props, prefix);

	props.put(prefix + ".type", "PackageTarget");
//	if(packageDir != null)
//	    Config.putPath(props, prefix + ".packageDir", packageDir);
	if(packageName != null)
	    props.put(prefix + ".packageName", packageName);
    }

    /**
     * Copy all the files belonging to this target to a new location.
     * For package targets, this has not yet been implemented.
     *
     * @arg directory The directory to copy into (ending with "/")
     */
    public boolean copyFiles(String directory)
    {
	DialogManager.showText(pkg.getFrame(),
			"\"Save As\" does not yet work for nested packages.");
	return true;
    }

    Color getBackgroundColour()
    {
	return defaultbg;
    }

    Color getBorderColour()
    {
	return bordercolour;
    }

    Color getTextColour()
    {
	return textfg;
    }

    Font getFont()
    {
        return (state == S_INVALID) ? PrefMgr.getStandoutFont() : PrefMgr.getStandardFont();
    }

    public void draw(Graphics2D g) {
	g.setColor(getBackgroundColour());
	g.fillRect(x, y, width, height);

	// draw "ribbon"
	g.setColor(ribboncolour);
	int rx = x + 2 * TEXT_BORDER;
	int ry = y + height - HANDLE_SIZE + 5;
	g.drawLine(rx, y, rx, y + height);
	g.drawLine(x, ry, x + width, ry);

	g.drawLine(rx -10, ry, rx - 10, ry - 3);
	g.drawLine(rx - 10, ry - 3, rx - 8, ry - 5);
	g.drawLine(rx - 8, ry - 5, rx - 5, ry - 5);
	g.drawLine(rx - 5, ry - 5, rx, ry);
	g.drawLine(rx, ry, rx + 10, ry + 10);

	g.drawLine(rx + 10, ry, rx + 10, ry - 3);
	g.drawLine(rx + 10, ry - 3, rx + 8, ry - 5);
	g.drawLine(rx + 8, ry - 5, rx + 5, ry - 5);
	g.drawLine(rx + 5, ry - 5, rx, ry);
	g.drawLine(rx, ry, rx - 10, ry + 10);

	g.setColor(textbg);
	g.fillRect(x + TEXT_BORDER, y + TEXT_BORDER,
		   width - 2*TEXT_BORDER, TEXT_HEIGHT);

	g.setColor(getBorderColour());
	g.setFont(getFont());
	Utility.drawCentredText(g, name,
				x + TEXT_BORDER, y + TEXT_BORDER,
				width - 2*TEXT_BORDER, TEXT_HEIGHT);
	g.drawRect(x + TEXT_BORDER, y + TEXT_BORDER,
		   width - 2*TEXT_BORDER, TEXT_HEIGHT);
	drawBorders(g);

	g.setColor(shadowCol);
	drawShadow(g);
    }

    /**
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     * Andy - needs to use the current frame for the new package if
     * the GraphEditor is within a LibraryBrowser.
     */
    public void doubleClick(MouseEvent evt, int x, int y, GraphEditor editor) 
    {
        Debug.reportError("package double click: NYI");
        /* mik:
          Package newpkg = Main.openPackage(pkg.getBaseDir(), packageName);
          // open a new Frame for the new package
          PkgFrame frame = newpkg.getFrame();
          frame.setVisible(true);
        */
    }
    public void popupMenu(MouseEvent evt, int x, int y, GraphEditor editor)
    {
	JPopupMenu menu = createMenu(null, editor.getFrame());
	if (menu != null)
	    menu.show(editor, evt.getX(), evt.getY());
    }

    private JPopupMenu createMenu(Class cl, JFrame editorFrame) {
	JPopupMenu menu = new JPopupMenu(getName() + " operations");

	return null;
    }

    private void addMenuItem(JPopupMenu menu, String itemString, boolean enabled)
    {
	JMenuItem item;

	menu.add(item = new JMenuItem(itemString));
	item.addActionListener(this);
	item.setFont(PrefMgr.getStandardMenuFont());
	item.setForeground(envOpColour);
	if(!enabled)
	    item.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e)
    {
	String cmd = e.getActionCommand();
	if (useStr.equals(cmd)) {
				// insert code to do same thing as double click here
//	    if (pkg.getEditor().getFrame() instanceof LibraryBrowserPkgMgrFrame)
//		((LibraryBrowserPkgMgrFrame)pkg.getEditor().getFrame()).usePackage(this);
//	} else if (openStr.equals(cmd)) {
				// insert code to do same thing as double click here
//	    if (pkg.getEditor().getFrame() instanceof LibraryBrowserPkgMgrFrame)
//		((LibraryBrowserPkgMgrFrame)pkg.getEditor().getFrame()).openPackage(this);
	}
    }
}
