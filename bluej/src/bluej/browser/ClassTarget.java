package bluej.browser;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import bluej.Config;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.utility.JavaNames;

/**
 * A general target for the browser
 *
 * @author  Michael Cahill
 * @version $Id: ClassTarget.java 1700 2003-03-13 03:34:20Z ajp $
 */
public class ClassTarget extends Target
{
    static final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    static final Color abstractbg = Config.getItemColour("colour.class.bg.abstract");
    static final Color interfacebg = Config.getItemColour("colour.class.bg.interface");

    Class cl;

    public ClassTarget(Class cl)
    {
        super(JavaNames.stripPrefix(cl.getName()));

        this.cl = cl;

        setBorder(BorderFactory.createEmptyBorder(0,0, SHAD_SIZE, SHAD_SIZE));

        enableEvents(AWTEvent.MOUSE_EVENT_MASK);

        MouseListener ml = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setSelected(!getSelected());
            }
        };
        addMouseListener(ml);
    }

    protected Color getBackgroundColour()
    {
        if(cl.isInterface())
            return interfacebg;
        else
            return defaultbg;
    }

    protected void popupMenu(int x, int y)
    {
        JPopupMenu menu = new JPopupMenu();

        PkgMgrFrame[] openFrames = PkgMgrFrame.getAllFrames();

        if(openFrames != null) {
            for(int i=0; i<openFrames.length; i++) {

                Action useAction = new UseAction("Use in package " +
                                          openFrames[i].getPackage().getId(),
                                          openFrames[i].getPackage());
            	useAction.setEnabled(true);
                menu.add(useAction);
            }

             menu.show(this,x,y);
        }
    }

    private class UseAction extends AbstractAction
    {
        private Package pkg;

        public UseAction(String menu, Package pkg)
        {
            super(menu);
            this.pkg = pkg;
        }

        public void actionPerformed(ActionEvent e) {
//            pkg.insertLibClass(cl.getName());
        }
    }
}
