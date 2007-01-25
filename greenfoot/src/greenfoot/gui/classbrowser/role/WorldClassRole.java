package greenfoot.gui.classbrowser.role;

import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassView;

/**
 * @author Poul Henriksen
 * @version $Id: WorldClassRole.java 4823 2007-01-25 17:03:30Z polle $
 */
public class WorldClassRole extends ClassRole
{
    private String template = "worldclass.tmpl";

    public void buildUI(ClassView classView, GClass gClass)
    {
        classView.setIcon(null);
        classView.setText(gClass.getName());
    }

    @Override
    public String getTemplateFileName()
    {
        return template;
    }

}