package bluej.groupwork;
import java.io.PrintStream;
import org.netbeans.lib.cvsclient.event.CVSAdapter;
import org.netbeans.lib.cvsclient.event.MessageEvent;

public class BasicListener extends CVSAdapter
{
    /**
     * Stores a tagged line
     */
    private final StringBuffer taggedLine = new StringBuffer();

    /**
     * Called when the server wants to send a message to be displayed to
     * the user. The message is only for information purposes and clients
     * can choose to ignore these messages if they wish.
     * @param e the event
     */
    public void messageSent(MessageEvent e)
    {
        String line = e.getMessage();
        PrintStream stream = e.isError() ? System.err
                                         : System.out;

        if (e.isTagged())
        {
            String message = MessageEvent.parseTaggedMessage(taggedLine, line);
	    // if we get back a non-null line, we have something
	    // to output. Otherwise, there is more to come and we
	    // should do nothing yet.
            if (message != null)
            {
                stream.println(message);
            }
        }
        else
        {
            stream.println(line);
        }
    }
}