package bluej.groupwork;

/**
 * A utility class for combining multiple commands into one.
 * 
 * @author Davin McCall
 */
public class CompoundCommand implements TeamworkCommand
{
    private TeamworkCommand command1;
    private TeamworkCommand command2;
    
    public CompoundCommand(TeamworkCommand command1, TeamworkCommand command2)
    {
        this.command1 = command1;
        this.command2 = command2;
    }
    
    public void cancel()
    {
        command1.cancel();
        command2.cancel();
    }

    public TeamworkCommandResult getResult()
    {
        TeamworkCommandResult result = command1.getResult();
        
        if (result.wasAborted() || result.isError()) {
            return result;
        }
        
        result = command2.getResult();
        return result;
    }

}
