package bluej.collect;

/**
 * Package-visible Enum with all the events that we might send to the server.
 * 
 *
 */
enum EventName
{
    BLUEJ_START("bluej_start"),
    BLUEJ_FINISH("bluej_finish"),
    COMPILE("compile"),
    MULTI_LINE_EDIT("multi_line_edit"),
    PROJECT_OPENING("project_opening"),
    PROJECT_CLOSING("project_closing"),
    RESETTING_VM("resetting_vm");
    
    private final String name;
    
    private EventName(String name)
    {
        this.name = name;
    }
    
    public String getName()
    {
        return name;
    }
    
}
