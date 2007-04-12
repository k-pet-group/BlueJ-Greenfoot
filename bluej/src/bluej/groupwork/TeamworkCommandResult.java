package bluej.groupwork;

/**
 * Represents the result of a teamwork command.<p>
 * 
 * This base class represents the default case - no error.
 * 
 * @author Davin McCall
 */
public class TeamworkCommandResult
{
    /**
     * Did an error occur during the processing of the command?
     * This includes authentication problems and command cancellation.
     */
    public boolean isError()
    {
        return false;
    }
    
    /**
     * Did the command fail due to authentication failure - invalid
     * username/password?
     */
    public boolean wasAuthFailure()
    {
        return false;
    }
    
    /**
     * Was the command aborted? (in this case, isError/wasAuthFailure will both
     * return false).
     */
    public boolean wasAborted()
    {
        return false;
    }
    
    /**
     * Get the error message explaining the problem that occurred.
     */
    public String getErrorMessage()
    {
        return null;
    }
}
