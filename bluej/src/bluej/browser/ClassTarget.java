package bluej.browser;

import bluej.Config;
import bluej.utility.Debug;
import bluej.graph.Vertex;
import bluej.graph.GraphEditor;
import bluej.utility.MultiEnumeration;
import bluej.utility.SortableVector;
import bluej.utility.Utility;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Main;

import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: ClassTarget.java 276 1999-11-16 00:55:47Z ajp $
 ** @author Michael Cahill
 **
 ** A general target for the browser
 **/
public class ClassTarget extends Target
{
    static final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    static final Color abstractbg = Config.getItemColour("colour.class.bg.abstract");
    static final Color interfacebg = Config.getItemColour("colour.class.bg.interface");

    Class cl;

    public ClassTarget(Class cl)
    {
        super(Utility.stripPackagePrefix(cl.getName()));

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

        Package[] openpackages = bluej.pkgmgr.Main.getAllOpenPackages();
            
        if(openpackages != null) {
            for(int i=0; i<openpackages.length; i++) {

                Action useAction = new UseAction("Use in package " + openpackages[i].getId(),
                                                openpackages[i]); 

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
            pkg.insertLibClass(cl.getName());
        }
    }
}
