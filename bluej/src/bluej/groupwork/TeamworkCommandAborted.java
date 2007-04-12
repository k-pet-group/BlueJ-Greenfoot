package bluej.groupwork;

/**
 * A teamwork command result representing an aborted command.
 * 
 * @author Davin McCall
 */
public class TeamworkCommandAborted extends TeamworkCommandResult
{
    public boolean isError()
    {
        return true;
    }
    
    public boolean wasAborted()
    {
        return true;
    }
}
