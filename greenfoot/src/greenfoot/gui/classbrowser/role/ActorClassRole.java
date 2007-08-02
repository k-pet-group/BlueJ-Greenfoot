package greenfoot.gui.classbrowser.role;

import greenfoot.actions.SelectImageAction;

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
 * @version $Id: ActorClassRole.java 5139 2007-08-02 06:37:21Z davmac $
 */
public class ActorClassRole extends ImageClassRole
{
    protected final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    private String template = "actorclass.tmpl";

    private static final String newline = System.getProperty("line.separator");
    public static final String imports = "import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)" + newline;
    

    /**
     * Need to overide this method in order to delay the invocation of the
     * constructor until the object is placed into the world.
     */
    public List createConstructorActions(Class realClass)
    {
        List realActions = super.createConstructorActions(realClass);
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