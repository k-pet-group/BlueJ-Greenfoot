package greenfoot.gui.classbrowser.role;

import greenfoot.actions.SelectImageAction;
import greenfoot.core.GProject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import bluej.Config;

/**
 * A role for Actor classes 
 * 
 * @author Poul Henriksen
 * @version $Id: ActorClassRole.java 5158 2007-08-16 05:00:00Z davmac $
 */
public class ActorClassRole extends ImageClassRole
{
    protected final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    private String template = "actorclass.tmpl";

    private static final String newline = System.getProperty("line.separator");
    public static final String imports = "import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)" + newline;
    
    public ActorClassRole(GProject project)
    {
    	super(project);
    }
    
    /**
     * Need to overide this method in order to delay the invocation of the
     * constructor until the object is placed into the world.
     */
    public List createConstructorActions(Class realClass, GProject project)
    {
        List realActions = super.createConstructorActions(realClass, project);
        List<Action> tempActions = new ArrayList<Action>();
        for (Iterator iter = realActions.iterator(); iter.hasNext();) {
            Action realAction = (Action) iter.next();
            Action tempAction = createDragProxyAction(realAction);
            tempActions.add(tempAction);
        }
 
        return tempActions;
    }
        
    /* (non-Javadoc)
     * @see greenfoot.gui.classbrowser.role.ClassRole#addPopupMenuItems(javax.swing.JPopupMenu, boolean)
     */
    public void addPopupMenuItems(JPopupMenu menu, boolean coreClass)
    {
        if (! coreClass) {
            menu.add(createMenuItem(new SelectImageAction(classView, this)));
        }
    }

    @Override
    public String getTemplateFileName()
    {
        return template;
    }

}