package org.bluej.extensions.submitter;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URL stream handler that returns the given message as
 * the content of the URL, when requested.
 * 
 * @author Clive Miller
 * @version $Id: MessageHandler.java 1463 2002-10-23 12:40:32Z jckm $
 */
public class MessageHandler extends URLStreamHandler
{
    private String message;
    
    public MessageHandler (String message)
    {
        this.message = message;
    }
    
    protected URLConnection openConnection (URL url)
    {
        return new URLConnection (url) {
            public void connect() {}
            public Object getContent() {
                return MessageHandler.this.message;
            }
        };
    }
}
            
