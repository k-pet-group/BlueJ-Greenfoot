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
 * @version $Id: ParentPackageTarget.java 607 2000-06-30 04:23:32Z ajp $
 */
public class ParentPackageTarget extends PackageTarget implements ActionListener
{
    final static String openStr = Config.getString("pkgmgr.parentpackagetarget.open");
    final static String openUnamedStr = Config.getString("pkgmgr.parentpackagetarget.openunamed");

    public ParentPackageTarget(Package pkg)
    {
        super(pkg, "<go up>");
    }

    public void load(Properties props, String prefix)
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

    public boolean isSaveable()
    {
        return false;
    }

    /**
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     */
    public void doubleClick(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        PackageEditor pe = (PackageEditor) editor;

        pe.raiseOpenPackageEvent(this,
                JavaNames.getPrefix(getPackage().getQualifiedName()));
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

        String item = JavaNames.getPrefix(getPackage().getQualifiedName());

        while(!item.equals("")) {
            addMenuItem(menu, openStr + " " + item, item);
            item = JavaNames.getPrefix(item);
        }

        addMenuItem(menu, openUnamedStr, "");

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
}
