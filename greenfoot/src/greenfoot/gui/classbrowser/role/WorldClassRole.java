package greenfoot.gui.classbrowser.role;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import greenfoot.GreenfootImage;
import greenfoot.actions.SelectImageAction;
import greenfoot.core.GClass;
import greenfoot.core.GreenfootMain;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;

/**
 * @author Poul Henriksen
 * @version $Id: WorldClassRole.java 5139 2007-08-02 06:37:21Z davmac $
 */
public class WorldClassRole extends ImageClassRole
{
    private String template = "worldclass.tmpl";

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
