package greenfoot.gui.classbrowser.role;

import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassView;

/**
 * Role for a "normal" (non-Actor, non-World) class.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NormalClassRole.java 5646 2008-03-14 17:49:03Z polle $
 */
public class NormalClassRole extends ClassRole
{
    private static NormalClassRole instance;

    private String template = "stdclass.tmpl";
    
    /**
     * Get the (singleton) instance of NormalClassRole.
     */
    public static NormalClassRole getInstance()
    {
        if (instance == null) {
            instance = new NormalClassRole();
        }
        
        return instance;
    }
    
    private NormalClassRole()
    {
        // Nothing to do.
    }
    
    public void buildUI(ClassView classView, GClass gClass)
    {
        classView.setIcon(null);
        classView.setText(gClass.getQualifiedName());
    }

    @Override
    public String getTemplateFileName()
    {
        return template;
    }

    @Override
    public void remove()
    {
       //do nothing
    }

    
}
