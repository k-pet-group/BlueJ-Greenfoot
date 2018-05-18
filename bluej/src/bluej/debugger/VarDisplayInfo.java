package bluej.debugger;

import bluej.debugger.gentype.JavaType;
import bluej.utility.javafx.FXPlatformSupplier;
import com.sun.jdi.LocalVariable;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.reflect.Modifier;
import java.util.function.Supplier;

/**
 * Created by neil on 18/05/2017.
 */
public class VarDisplayInfo
{
    private final String access; // May be null (for local vars)
    private final String type;
    private final String name;
    private final String value;
    // If null, means item was not an inspectable object (probably null or primitive):
    @OnThread(Tag.Any)
    private final Supplier<DebuggerObject> getObjectToInspect;

    @OnThread(Tag.FXPlatform)
    public VarDisplayInfo(DebuggerField field)
    {
        int mods = field.getModifiers();
        String access = "";
        if (Modifier.isPrivate(mods)) {
            access = "private";
        }
        else if (Modifier.isPublic(mods)) {
            access = "public";
        }
        else if (Modifier.isProtected(mods)) {
            access = "protected";
        }

        if (field.isHidden()) {
            access += "(hidden)";
        }

        this.access = access;

        type = field.getType().toString(true);
        name = field.getName();
        value = field.getValueString();
        if (field.isReferenceType() && ! field.isNull())
        {
            getObjectToInspect = () -> field.getValueObject(null);
        }
        else
        {
            getObjectToInspect = null;
        }
    }

    @OnThread(Tag.FXPlatform)
    public VarDisplayInfo(JavaType vartype, LocalVariable var, String value, Supplier<DebuggerObject> getObjectToInspect)
    {
        access = null;
        type = vartype.toString(true);
        name = var.name();
        this.value = value;
        this.getObjectToInspect = getObjectToInspect;
    }

    public String getAccess()
    {
        return access;
    }

    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    @OnThread(Tag.Any)
    public Supplier<DebuggerObject> getFetchObject()
    {
        return getObjectToInspect;
    }
}
