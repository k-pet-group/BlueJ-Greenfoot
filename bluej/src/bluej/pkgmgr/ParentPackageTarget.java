package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.graph.GraphEditor;
import bluej.utility.*;

import java.util.Properties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A parent package
 *
 * @author  Andrew Patterson
 * @version $Id: ParentPackageTarget.java 533 2000-06-09 04:24:08Z ajp $
 */
public class ParentPackageTarget extends PackageTarget implements ActionListener
{
    public ParentPackageTarget(Package pkg)
    {
        super(pkg, "<go up>");
    }

    public void load(Properties props, String prefix) throws NumberFormatException
    {
    }

    public void save(Properties props, String prefix)
    {
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

    public void draw(Graphics2D g)
    {
        g.setColor(getBackgroundColour());
        g.fillRect(0, 0, width, height);

        // draw "ribbon"
        g.setColor(ribboncolour);
        int rx = 2 * TEXT_BORDER;
        int ry = height - HANDLE_SIZE + 5;
        g.drawLine(rx, 0, rx, height);
        g.drawLine(0, ry, width, ry);

        g.drawLine(rx - 5, ry, rx - 5, ry - 1);
        g.drawLine(rx - 5, ry - 1, rx - 4, ry - 2);
        g.drawLine(rx - 4, ry - 2, rx - 2, ry - 2);
        g.drawLine(rx - 2, ry - 2, rx, ry);
        g.drawLine(rx, ry, rx + 5, ry + 5);

        g.drawLine(rx + 5, ry, rx + 5, ry - 1);
        g.drawLine(rx + 5, ry - 1, rx + 4, ry - 2);
        g.drawLine(rx + 4, ry - 2, rx + 2, ry - 2);
        g.drawLine(rx + 2, ry - 2, rx, ry);
        g.drawLine(rx, ry, rx - 5, ry + 5);

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

    /**
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     */
    public void doubleClick(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        PackageEditor pe = (PackageEditor) editor;

        pe.raiseOpenPackageEvent(this, JavaNames.getPrefix(getPackage().getQualifiedName()));
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

        String item = JavaNames.getPrefix(getPackage().getQualifiedName());

        while(item != "") {
            addMenuItem(menu, "Open " + item, item);
            item = JavaNames.getPrefix(item);
        }

        addMenuItem(menu, "Open unnamed package", "");

        return menu;
    }

    private void addMenuItem(JPopupMenu menu, String itemString, String pkgName)
    {
        JMenuItem item;

        Action openAction = new OpenAction(itemString, this, pkgName);

        item = menu.add(openAction);
        item.setFont(PrefMgr.getStandardMenuFont());
        item.setForeground(envOpColour);
    }

    private class OpenAction extends AbstractAction
    {
        private String pkgName;
        private Target t;

        public OpenAction(String menu, Target t, String pkgName)
        {
            super(menu);
            this.pkgName = pkgName;
            this.t = t;
        }

        public void actionPerformed(ActionEvent e)
        {
            getPackage().getEditor().raiseOpenPackageEvent(t, pkgName);
        }
    }
}
