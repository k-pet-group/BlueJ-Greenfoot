package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.graph.GraphEditor;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import java.util.Properties;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * A sub package (or parent package)
 *
 * @author  Michael Cahill
 * @version $Id: PackageTarget.java 1149 2002-03-08 11:14:09Z mik $
 */
public class PackageTarget extends Target
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

    static String openStr = Config.getString("pkgmgr.packagemenu.open");
    static String removeStr = Config.getString("pkgmgr.packagemenu.remove");

    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    static final BasicStroke normalStroke = new BasicStroke(1);
    static final BasicStroke selectedStroke = new BasicStroke(3);


    public PackageTarget(Package pkg, String baseName)
    {
        super(pkg, baseName);

        setSize(calculateWidth(baseName), DEF_HEIGHT + TAB_HEIGHT);
    }

    /**
     * Return the target's base name (ie the name without the package name).
     * eg. Target
     */
    public String getBaseName()
    {
        return getIdentifierName();
    }

    /**
     * Return the target's name, including the package name.
     * eg. bluej.pkgmgr
     */
    public String getQualifiedName()
    {
        return getPackage().getQualifiedName(getBaseName());
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
     * Deletes applicable files (directory and ALL contentes) prior to
     * this PackageTarget being removed from a Package.
     */
    public void deleteFiles()
    {
        FileUtility.deleteDir(new File(getPackage().getPath(), getBaseName()));
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
        if(PrefMgr.getFlag(PrefMgr.USE_UML))
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
        if(PrefMgr.getFlag(PrefMgr.USE_UML))
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


    void drawBorders(Graphics2D g)
    {
        if(PrefMgr.getFlag(PrefMgr.USE_UML))
            drawUMLBorders(g);
        else
            super.drawBorders(g);

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

        pe.raiseOpenPackageEvent(this, getPackage().getQualifiedName(getBaseName()));
    }

    public void popupMenu(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        JPopupMenu menu = createMenu(null);
        if (menu != null)
            menu.show(editor, evt.getX(), evt.getY());
    }

    /**
     * Construct a popup menu which displays all our parent packages.
     */
    private JPopupMenu createMenu(Class cl)
    {
        JPopupMenu menu = new JPopupMenu(getBaseName());
        JMenuItem item;

        Action openAction = new OpenAction(openStr, this,
                                 getPackage().getQualifiedName(getBaseName()));

        item = menu.add(openAction);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);

        Action removeAction = new RemoveAction(removeStr, this);

        item = menu.add(removeAction);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);

        return menu;
    }

    private void addMenuItem(JPopupMenu menu, String itemString, String pkgName)
    {
        JMenuItem item;

        Action openAction = new OpenAction(itemString, this, pkgName);

        item = menu.add(openAction);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
    }

    private class OpenAction extends AbstractAction
    {
        private Target t;
        private String pkgName;

        public OpenAction(String menu, Target t, String pkgName)
        {
            super(menu);
            this.t = t;
            this.pkgName = pkgName;
        }

        public void actionPerformed(ActionEvent e)
        {
            getPackage().getEditor().raiseOpenPackageEvent(t, pkgName);
        }
    }

    private class RemoveAction extends AbstractAction
    {
        private Target t;

        public RemoveAction(String menu, Target t)
        {
            super(menu);
            this.t = t;
        }

        public void actionPerformed(ActionEvent e)
        {
            getPackage().getEditor().raiseRemoveTargetEvent(t);
        }
    }

}
