package greenfoot.gui.classbrowser.role;

import greenfoot.core.GClass;
import greenfoot.core.WorldInvokeListener;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.Image;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import bluej.debugmgr.ConstructAction;
import bluej.utility.Debug;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.View;
import bluej.views.ViewFilter;


/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassRole.java 3827 2006-03-16 00:50:27Z davmac $
 */
public abstract class ClassRole
{
    public abstract void buildUI(ClassView classView, GClass rClass);

    public Image getImage()
    {
        return null;
    }

    /**
     * @return
     */
    public abstract void createSkeleton(String className, String superClassName, FileWriter writer);

    public List createConstructorActions(Class realClass)
    {
        View view = View.getView(realClass);
        List actions = new ArrayList();
        ConstructorView[] constructors = view.getConstructors();

        for (int i = 0; i < constructors.length; i++) {
            try {

                CallableView m = constructors[constructors.length - i - 1];

                ViewFilter filter = new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PACKAGE);
                if (!filter.accept(m))
                    continue;

                WorldInvokeListener invocListener = new WorldInvokeListener(realClass);

                String prefix = "new ";
                Action callAction = new ConstructAction((ConstructorView) m, invocListener, prefix + m.getLongDesc());

                if (callAction != null) {
                    actions.add(callAction);
                }
            }
            catch (Exception e) {
                Debug.reportError("Exception accessing methods: " + e);
                e.printStackTrace();
            }
        }
        return actions;
    }
    
    public void addPopupMenuItems(JPopupMenu menu)
    {
        // default implementation does nothing
    }
}