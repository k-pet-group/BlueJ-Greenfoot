package bluej.pkgmgr.target.role;

import java.awt.Color;
import javax.swing.JPopupMenu;
import bluej.Config;
import bluej.pkgmgr.target.ClassTarget;
import bluej.prefmgr.PrefMgr;

/**
 * A role object to represent the behaviour of enums.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: EnumClassRole.java 4735 2006-12-04 03:17:45Z bquig $
 */
public class EnumClassRole extends ClassRole
{
    public final static String ENUM_ROLE_NAME = "EnumTarget";
    private static final Color enumbg = Config.getItemColour("colour.class.bg.enum");
    
    /**
     * Create the enum class role.
     */
    public EnumClassRole()
    {
    }

    public String getRoleName()
    {
        return ENUM_ROLE_NAME;
    }

    public String getStereotypeLabel()
    {
        return "enum";
    }

    /**
     * Return the intended background colour for this type of target.
     */
    public Color getBackgroundColour()
    {
        return enumbg;
    }

    /**
     * Creates a class menu containing any constructors.
     *
     * Because we are an enum class we cannot have any constructors
     * so we override this method to do nothing.
     *
     * @param menu the popup menu to add the class menu items to
     * @param cl Class object associated with this class target
     */
    public boolean createClassConstructorMenu(JPopupMenu menu, ClassTarget ct, Class cl)
    {
        return false;
    }
    
    /**
     * Adds role specific items at the bottom of the popup menu for this class target.
     *
     * @param menu the menu object to add to
     * @param ct ClassTarget object associated with this class role
     * @param state the state of the ClassTarget
     *
     * @return true if any menu items have been added
     */
    public boolean createRoleMenuEnd(JPopupMenu menu, ClassTarget ct, int state)
    {
        if(PrefMgr.getFlag(PrefMgr.SHOW_TEST_TOOLS)) {
            if (ct.getAssociation() == null) {
                menu.addSeparator();
                addMenuItem(menu, ct.new CreateTestAction(), true);
            }
        }
        return true;
    }
}
