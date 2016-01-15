package bluej.compiler;

/**
 * Created by neil on 15/01/16.
 */
public enum CompileReason
{
    EARLY("early"), LATE("late"), INVOKE("invoke"), REBUILD("rebuild"), EXTENSION("extension"),
    LOADED("loaded"), MODIFIED("modified"), NEW_CLASS("new_class"), USER("user");

    private final String serverString;

    CompileReason(String serverString)
    {
        this.serverString = serverString;
    }

    public String getServerString()
    {
        return serverString;
    }
}
