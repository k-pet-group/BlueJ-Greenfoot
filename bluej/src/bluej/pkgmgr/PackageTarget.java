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
 * A sub package (or parent package)
 *
 * @author  Michael Cahill
 * @version $Id: PackageTarget.java 530 2000-06-01 07:09:30Z bquig $
 */
public class PackageTarget extends Target implements ActionListener
{
    static final Color defaultbg = Config.getItemColour("colour.package.bg.default");
    //static final Color umldefaultbg = Config.getItemColour("colour.class.bg.uml.default");
    static final Color umldefaultbg = Config.getItemColour("colour.package.bg.uml.default");

    static final Color ribboncolour = defaultbg.darker().darker();
    static final Color bordercolour = Config.getItemColour("colour.target.border");
    static final Color textbg = Config.getItemColour("colour.text.bg");
    static final Color textfg = Config.getItemColour("colour.text.fg");
    
    static final int TAB_HEIGHT = 12;
    private int tabWidth;


    protected String packageDir = null;
    protected String packageName = null;

    static String useStr = Config.getString("browser.classchooser.packagemenu.use");
    static String openStr = Config.getString("browser.classchooser.packagemenu.open");

    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    static final BasicStroke normalStroke = new BasicStroke(1);
    static final BasicStroke selectedStroke = new BasicStroke(3);


    public PackageTarget(Package pkg, String shortName)
    {
        super(pkg, shortName, nextX(), nextY(), calculateWidth(shortName), DEF_HEIGHT + TAB_HEIGHT);
        packageDir = shortName;
        packageName = shortName;
    }

    public String getName()
    {
        return packageName;
    }

    public void load(Properties props, String prefix) throws NumberFormatException
    {
        super.load(props, prefix);
    }

    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        props.put(prefix + ".type", "PackageTarget");
    }

    /**
     * Copy all the files belonging to this target to a new location.
     * For package targets, this has not yet been implemented.
     *
     * @arg directory The directory to copy into (ending with "/")
     */
    public boolean copyFiles(String directory)
    {
//XXX not working
        return true;
    }

    Color getBackgroundColour()
    {
        if(PrefMgr.isUML())
            return umldefaultbg;
        else
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

    public void draw(Graphics2D g)
    {
        if(PrefMgr.isUML())
            drawUMLStyle(g);
        else
            drawBlueStyle(g);
    }

    public void drawUMLStyle(Graphics2D g)
    {
        tabWidth = width / 3;

        g.setColor(getBackgroundColour());
        //g.fillRect(0, 0, width, height);
        g.fillRect(0, 0, tabWidth, TAB_HEIGHT);
        g.fillRect(0, TAB_HEIGHT, width, height - TAB_HEIGHT);

        g.setColor(shadowCol);
        drawUMLShadow(g);

        g.setColor(getBorderColour());
        g.setFont(getFont());
        Utility.drawCentredText(g, getDisplayName(),
        			TEXT_BORDER, TEXT_BORDER + TAB_HEIGHT,
        			width - 2*TEXT_BORDER, TEXT_HEIGHT);
        drawUMLBorders(g);
    }

    public void drawBlueStyle(Graphics2D g)
    {
        g.setColor(getBackgroundColour());
        g.fillRect(0, 0, width, height);

        // draw "ribbon"
        g.setColor(ribboncolour);
        int rx = 2 * TEXT_BORDER;
        int ry = height - HANDLE_SIZE + 5;
        g.drawLine(rx, 0, rx, height);
        g.drawLine(0, ry, width, ry);

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
        g.fillRect(TEXT_BORDER, TEXT_BORDER,
        	   width - 2*TEXT_BORDER, TEXT_HEIGHT);

        g.setColor(shadowCol);
        drawShadow(g);

        g.setColor(getBorderColour());
        g.setFont(getFont());
        Utility.drawCentredText(g, getDisplayName(),
        			TEXT_BORDER, TEXT_BORDER,
        			width - 2*TEXT_BORDER, TEXT_HEIGHT);
        g.drawRect(TEXT_BORDER, TEXT_BORDER,
        	   width - 2*TEXT_BORDER, TEXT_HEIGHT);
        drawBorders(g);
    }

    void drawUMLBorders(Graphics2D g)
    {

        if(!((flags & F_SELECTED) == 0))
            g.setStroke(selectedStroke);

        g.drawRect(0, 0, tabWidth, TAB_HEIGHT);
        g.drawRect(0, TAB_HEIGHT, width, height - TAB_HEIGHT);

        if((flags & F_SELECTED) == 0)
                return;

        g.setStroke(normalStroke);
        // Draw lines showing resize tag
        g.drawLine(width - HANDLE_SIZE - 2, height,
                   width, height - HANDLE_SIZE - 2);
        g.drawLine(width - HANDLE_SIZE + 2, height,
                   width, height - HANDLE_SIZE + 2);
    }

    void drawUMLShadow(Graphics2D g)
    {
        g.fillRect(SHAD_SIZE, height , width, SHAD_SIZE);
        g.fillRect(width, SHAD_SIZE + TAB_HEIGHT, SHAD_SIZE, height - TAB_HEIGHT);
    }


    /**
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     */
    public void doubleClick(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        PackageEditor pe = (PackageEditor) editor;

        pe.raiseOpenPackageEvent(this, getPackage().getQualifiedName(getName()));
    }

    public void popupMenu(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        JPopupMenu menu = createMenu(null);
        if (menu != null)
            menu.show(editor, evt.getX(), evt.getY());
    }

    private JPopupMenu createMenu(Class cl)
    {
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
