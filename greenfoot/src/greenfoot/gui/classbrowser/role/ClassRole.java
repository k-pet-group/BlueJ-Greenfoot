package greenfoot.gui.classbrowser.role;

import greenfoot.core.GClass;
import greenfoot.core.WorldInvokeListener;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import bluej.debugmgr.ConstructAction;
import bluej.utility.Debug;
import bluej.views.ConstructorView;
import bluej.views.View;
import bluej.views.ViewFilter;


/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassRole.java 4871 2007-03-22 03:58:03Z davmac $
 */
public abstract class ClassRole
{
    public abstract void buildUI(ClassView classView, GClass rClass);

    public Image getImage()
    {
        return null;
    }

    /**
     * Get the name for the template file used to create the initial source for a new class.
     * 
     */
    public abstract String getTemplateFileName();

    public List createConstructorActions(Class realClass)
    {
        View view = View.getView(realClass);
        List<Action> actions = new ArrayList<Action>();
        ConstructorView[] constructors = view.getConstructors();

        for (int i = 0; i < constructors.length; i++) {
            try {
                ConstructorView m = constructors[constructors.length - i - 1];

                ViewFilter filter = new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PACKAGE);
                if (!filter.accept(m))
                    continue;

                WorldInvokeListener invocListener = new WorldInvokeListener(realClass);

                String prefix = "new ";
                Action callAction = new ConstructAction(m, invocListener, prefix + m.getLongDesc());

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