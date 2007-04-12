package bluej.groupwork;

/**
 * A teamwork command result representing a general error during command
 * execution.
 * 
 * @author Davin McCall
 */
public class TeamworkCommandError extends TeamworkCommandResult
{
    private String errMsg;
    
    public TeamworkCommandError(String errMsg)
    {
        this.errMsg = errMsg;
    }
    
    public boolean isError()
    {
        return true;
    }
    
    public String getErrorMessage()
    {
        return errMsg;
    }
}
