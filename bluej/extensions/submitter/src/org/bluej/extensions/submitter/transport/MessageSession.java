package org.bluej.extensions.submitter.transport;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Copies files
 * to a given path on this machine.
 * 
 * @author Clive Miller
 * @version $Id: MessageSession.java 1463 2002-10-23 12:40:32Z jckm $
 */

public class MessageSession extends TransportSession
{
    public MessageSession (URL url, Properties environment)
    {
        super (url, environment);
    }

    public void connect() throws IOException
    {
        result = (String)url.getContent();
    }

    public void send (InputStream is, String name, boolean binary) throws IOException
    {
    }
    
    public void disconnect() throws IOException
    {
    }
}
    
