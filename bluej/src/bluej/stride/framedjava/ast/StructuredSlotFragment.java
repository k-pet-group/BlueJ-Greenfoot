package bluej.stride.framedjava.ast;

import java.util.Map;

import bluej.stride.framedjava.elements.CodeElement;

/**
 * Created by neil on 22/05/2016.
 */
public abstract class StructuredSlotFragment extends StringSlotFragment
{
    private String javaCode;

    public StructuredSlotFragment(String content, String javaCode)
    {
        super(content);
        // If we are using types from an old type text-slot, javaCode will be null.
        // In this case, we just use the content as the Java code:
        this.javaCode = javaCode == null ? content : javaCode;
    }

    // Used by XML serialisation:
    public String getJavaCode()
    {
        return javaCode;
    }

    public abstract Map<String, CodeElement> getVars();
}
