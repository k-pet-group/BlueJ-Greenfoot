package org.bluej.extensions.submitter.transport;

import java.util.Properties;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

/**
 * Copies files
 * to a given path on this machine.
 * 
 * @author Clive Miller
 * @version $Id: FileSession.java 1463 2002-10-23 12:40:32Z jckm $
 */

public class FileSession extends TransportSession
{
    private int fileCounter;
    private File path;
    
    public FileSession (URL url, Properties environment)
    {
        super (url, environment);
        result = "Not sent";
        fileCounter = 0;
    }

    public void connect() throws IOException
    {
        path = new File ("//"+url.getHost()+url.getFile());
    }

    public void send (InputStream is, String name, boolean binary) throws IOException
    {
        if (!path.exists()) {
            throw new FileNotFoundException (path.getPath());
        }
        
        File destination = new File (path, name);
        destination.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream (destination);
        byte[] buffer = new byte [1024];
        for(int c; (c = is.read(buffer)) != -1; ) out.write(buffer, 0, c);
        out.close();
        fileCounter++;
    }
    
    public void disconnect() throws IOException
    {
        setStatus ("Connection closed.");
        result = null;
    }
}
    
