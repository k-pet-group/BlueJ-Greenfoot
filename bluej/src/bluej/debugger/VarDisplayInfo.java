package bluej.debugger;

import bluej.debugger.gentype.JavaType;
import com.sun.jdi.LocalVariable;

import java.lang.reflect.Modifier;

/**
 * Created by neil on 18/05/2017.
 */
public class VarDisplayInfo
{
    private final String access; // May be null (for local vars)
    private final String type;
    private final String name;
    private final String value;

    public VarDisplayInfo(DebuggerField field)
    {
        int mods = field.getModifiers();
        String access = "";
        if (Modifier.isPrivate(mods)) {
            access = "private ";
        }
        else if (Modifier.isPublic(mods)) {
            access = "public ";
        }
        else if (Modifier.isProtected(mods)) {
            access = "protected ";
        }

        if (field.isHidden()) {
            access += "(hidden) ";
        }

        this.access = access;

        type = field.getType().toString(true);
        name = field.getName();
        value = field.getValueString();
    }

    public VarDisplayInfo(JavaType vartype, LocalVariable var, String value)
    {
        access = null;
        type = vartype.toString(true);
        name = var.name();
        this.value = value;
    }

    //TEMPorary until we do properly formatted display:
    @Override
    public String toString()
    {
        return (access == null ? "" : access + " ") + type + " " + name + " = " + value;
    }
}
