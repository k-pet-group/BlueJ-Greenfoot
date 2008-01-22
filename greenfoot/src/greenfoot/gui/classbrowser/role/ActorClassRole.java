package greenfoot.gui.classbrowser.role;

import greenfoot.actions.SelectImageAction;
import greenfoot.core.GProject;
import greenfoot.event.WorldEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import bluej.Config;

/**
 * A role for Actor classes 
 * 
 * @author Poul Henriksen
 * @version $Id: ActorClassRole.java 5477 2008-01-22 14:05:55Z polle $
 */
public class ActorClassRole extends ImageClassRole
{
    protected final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    private String template = "actorclass.tmpl";

    private static final String newline = System.getProperty("line.separator");
    public static final String imports = "import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)" + newline;

	private List<Action> constructorItems = new ArrayList<Action>();
	private boolean enableConstrutors = false;
	
    public ActorClassRole(GProject project)
    {
    	super(project);
    }
    
    /**
     * Need to overide this method in order to delay the invocation of the
     * constructor until the object is placed into the world.
     */
    public List<Action> createConstructorActions(Class realClass, GProject project)
    {
        List<Action> realActions = super.createConstructorActions(realClass, project);
        constructorItems = new ArrayList<Action>();
        for (Action realAction : realActions) {
            Action tempAction = createDragProxyAction(realAction);
        	tempAction.setEnabled(enableConstrutors);
            constructorItems.add(tempAction);
        }
 
        return constructorItems;
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

    @Override
	public void worldCreated(WorldEvent e) {
		enableConstrutors = true;
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				for (Action action : constructorItems) {
					action.setEnabled(true);
				}
			}
		});
	}

	@Override
	public void worldRemoved(WorldEvent e) {
		enableConstrutors = false;
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				for (Action action : constructorItems) {
					action.setEnabled(false);
				}
			}
		});
	}   
}