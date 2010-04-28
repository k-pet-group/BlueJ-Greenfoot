package bluej.groupwork.cvsnb;

import java.io.IOException;

import javax.net.SocketFactory;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.connection.AbstractConnection;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.ConnectionModifier;
import org.netbeans.lib.cvsclient.util.LoggedDataInputStream;
import org.netbeans.lib.cvsclient.util.LoggedDataOutputStream;

import bluej.utility.Debug;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.Session;

/**
 * Provides SSH tunnel for :ext: connection method, using the trilead library.
 * 
 * Based loosely on the SSHConnection class which used the jsch library, by Maros Sandor.
 * 
 * @author Davin McCall
 */
public class GSSHConnection extends AbstractConnection
{
    private static final String CVS_SERVER_COMMAND = System.getProperty("Env-CVS_SERVER", "cvs") + " server";  // NOI18N
    
    // private final SocketFactory socketFactory;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    
    private Session session;
    private Connection connection;
    // private ChannelExec channel;

    /**
     * Creates new SSH connection object.
     * 
     * @param socketFactory socket factory to use when connecting to SSH server
     * @param host host names of the SSH server
     * @param port port number of SSH server
     * @param username SSH username
     * @param password SSH password
     */ 
    public GSSHConnection(SocketFactory socketFactory, String host, int port, String username, String password)
    {
        //this.socketFactory = socketFactory;
        this.host = host;
        this.port = port;
        this.username = username != null ? username : System.getProperty("user.name"); // NOI18N
        this.password = password;
    }

    public void open() throws AuthenticationException, CommandAbortedException
    {
        try {
            connection = new Connection(host, port);
            connection.connect(null, 20000, 20000); // 20s timeout
            
            boolean isPwAvailable = connection.isAuthMethodAvailable(username, "password");
            boolean isKIAvailable = connection.isAuthMethodAvailable(username, "keyboard-interactive");

            boolean auth = false; 
            if (isPwAvailable) {
                auth = connection.authenticateWithPassword(username, password);
            }
            
            if (! auth && isKIAvailable) {
                // Try the "keyboard interactive" method instead.
                auth = connection.authenticateWithKeyboardInteractive(username, new InteractiveCallback() {
                    public String[] replyToChallenge(String name, String instruction,
                            int numPrompts, String[] prompt, boolean[] echo)
                    throws Exception {
                        String [] result = new String[numPrompts];
                        for (int i = 0; i < numPrompts; i++) {
                            result[i] = password;
                        }
                        return result;
                    }
                });
            }

            if (! auth) { 
                String msg = "SSH authentication failed: Wrong username/password?";
                reset();
                throw new AuthenticationException(msg, msg);
            }
            
            session = connection.openSession();
            session.execCommand(CVS_SERVER_COMMAND);
            setInputStream(new LoggedDataInputStream(session.getStdout()));
            setOutputStream(new LoggedDataOutputStream(session.getStdin()));
        }
        catch (IOException ioe) {
            Debug.message("SSH connection: " + ioe.getMessage());
            reset();
            throw new AuthenticationException(ioe, "SSH connection failure");
        }
    }

    /**
     * Verifies that we can successfuly connect to the SSH server and run 'cvs server' command on it.
     * 
     * @throws AuthenticationException if connection to the SSH server cannot be established (network problem)
     */ 
    public void verify() throws AuthenticationException
    {
        try {
            open();
            close();
        } catch (CommandAbortedException e) {
            throw new AuthenticationException(CVS_SERVER_COMMAND, "CommandAbortedException");
        } catch (IOException e) {
            throw new AuthenticationException(CVS_SERVER_COMMAND, "IOException");
        } finally {
            reset();
        }
    }

    private void reset()
    {
        if (session != null) {
            session.close();
            session = null;
        }
        if (connection != null) {
            connection.close();
            connection = null;
        }
        setInputStream(null);
        setOutputStream(null);
    }
    
    public void close() throws IOException
    {
        reset();
    }

    public boolean isOpen() {
        return session != null;
    }

    public int getPort() {
        return port;
    }

    public void modifyInputStream(ConnectionModifier modifier) throws IOException {
        modifier.modifyInputStream(getInputStream());
    }

    public void modifyOutputStream(ConnectionModifier modifier) throws IOException {
        modifier.modifyOutputStream(getOutputStream());
    }
    
}
