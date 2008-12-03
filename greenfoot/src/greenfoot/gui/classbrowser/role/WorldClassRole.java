package greenfoot.gui.classbrowser.role;

import greenfoot.actions.SelectImageAction;
import greenfoot.actions.ShowApiDocAction;
import greenfoot.core.GProject;

import javax.swing.JPopupMenu;

import bluej.Config;

/**
 * @author Poul Henriksen
 * @version $Id: WorldClassRole.java 6002 2008-12-03 17:24:17Z polle $
 */
public class WorldClassRole extends ImageClassRole
{
    private String template = "worldclass.tmpl";

    public WorldClassRole(GProject project)
    {
    	super(project);
    }
    
    @Override
    public String getTemplateFileName()
    {
        return template;
    }
    
    /* (non-Javadoc)
     * @see greenfoot.gui.classbrowser.role.ClassRole#addPopupMenuItems(javax.swing.JPopupMenu, boolean)
     */
    public void addPopupMenuItems(JPopupMenu menu, boolean coreClass)
    {
        if (! coreClass) {
            menu.add(createMenuItem(new SelectImageAction(classView, this)));
        }
        else {
            menu.add(createMenuItem(new ShowApiDocAction(Config.getString("show.apidoc"), "greenfoot/Actor.html")));
        }
    }

}
