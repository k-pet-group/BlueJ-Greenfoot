package bluej.groupwork.cvsnb;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.*;

/**
 * Base class for Cvs commands. Provides functionality to allow cancelling CVS
 * operations, and converting CVS library errors (exceptions) to appropriate
 * teamwork result objects.
 * 
 * @author Davin McCall
 */
public abstract class CvsCommand
    implements TeamworkCommand
{
    protected CvsRepository repository;
    private BlueJCvsClient client;
    private boolean cancelled;
    
    protected CvsCommand(CvsRepository repository)
    {
        this.repository = repository;
        cancelled = false;
    }
    
    /**
     * Get a new client to be used for command processing.
     */
    protected BlueJCvsClient getClient() throws CommandAbortedException,
        AuthenticationException
    {
        // We don't synchronize this whole method, because then we're holding the
        // monitor for (potentially) a long time during setupConnection() call.
        // So we must individually synchronize access to the protected variables.
        
        BlueJCvsClient myClient;
        synchronized (this) {
            if (cancelled) {
                throw new CommandAbortedException("","");
            }
            client = repository.getClient();
            myClient = client;
        }
        
        repository.setupConnection(myClient);
        
        synchronized (this) {
            if (cancelled) {
                throw new CommandAbortedException("","");
            }
            return client;
        }
    }
    
    public synchronized void cancel()
    {
        if (! cancelled) {
            cancelled = true;
            if (client != null) {
                client.abort();
            }
        }
    }

    public TeamworkCommandResult getResult()
    {
        try {
            BasicServerResponse response = doCommand();

            if (response.isError()) {
                String message = response.getMessage();
                String translatedMessage = CvsServerMessageTranslator.translate(message);
                if (translatedMessage == null) {
                    translatedMessage = message;
                }
                return new TeamworkCommandError(translatedMessage);
            }

            // command completed successfully
            return new TeamworkCommandResult();
        }
        catch (CommandAbortedException cae) {
            return new TeamworkCommandAborted();
        }
        catch (CommandException ce) {
            return new TeamworkCommandError(ce.getLocalizedMessage());
        }
        catch (AuthenticationException ae) {
            return new TeamworkCommandAuthFailure();
        }
    }
    
    /**
     * Actually peform the command and return a response object.
     * 
     * @param client  A client, with connection established, for use in processing the
     *                command.
     * 
     * @throws CommandAbortedException
     * @throws CommandException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    protected abstract BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException;
}
