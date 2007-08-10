package greenfoot.gui.classbrowser.role;

import greenfoot.actions.SelectImageAction;
import greenfoot.core.GProject;

import javax.swing.JPopupMenu;

/**
 * @author Poul Henriksen
 * @version $Id: WorldClassRole.java 5154 2007-08-10 07:02:51Z davmac $
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
    }

}
