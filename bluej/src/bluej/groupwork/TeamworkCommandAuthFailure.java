package bluej.groupwork;

import bluej.utility.DialogManager;

public class TeamworkCommandAuthFailure extends TeamworkCommandResult
{
    public boolean isError()
    {
        return true;
    }
    
    public boolean wasAuthFailure()
    {
        return true;
    }
    
    public String getErrorMessage()
    {
        return DialogManager.getMessage("team-authentication-problem");
    }
}
