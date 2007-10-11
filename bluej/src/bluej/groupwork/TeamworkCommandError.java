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
    
    /**
     * Construct a new Teamwork command error result. The supplied error message
     * must have already been localized (if possible).
     */
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
