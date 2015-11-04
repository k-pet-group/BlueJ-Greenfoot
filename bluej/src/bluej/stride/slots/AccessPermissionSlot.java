package bluej.stride.slots;

import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by neil on 22/02/2015.
 */
public class AccessPermissionSlot extends ChoiceSlot<AccessPermission>
{
    private static Map<AccessPermission, String> hints;

    public AccessPermissionSlot(InteractionManager editor, Frame parentFrame, FrameContentRow row, String stylePrefix)
    {
        super(editor, parentFrame, row, AccessPermission.all(), AccessPermission::isValid, stylePrefix, getHints());
    }

    private static Map<AccessPermission, String> getHints()
    {
        if (hints == null)
        {
            hints = new HashMap<>();
            hints.put(AccessPermission.PRIVATE, "Accessible only from this class");
            hints.put(AccessPermission.PROTECTED, "Accessible from this class and subclasses");
            hints.put(AccessPermission.PUBLIC, "Accessible from all classes");
        }
        return hints;
    }

}
