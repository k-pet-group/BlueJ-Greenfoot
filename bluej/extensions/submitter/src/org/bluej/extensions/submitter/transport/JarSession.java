package org.bluej.extensions.submitter.transport;

import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.net.UnknownServiceException;

/**
 * A Proxy TransportSession which Jars files and redirects them to an
 * underlying TransportSession
 * @author Clive Miller
 * @version $Id: JarSession.java 1463 2002-10-23 12:40:32Z jckm $
 */

public class JarSession extends TransportSession
{
    private TransportSession onwardTS;
    private JarOutputStream jos;
    private PipedOutputStream pos;
    private PipedInputStream pis;
    private String jarName;
    private IOException failure;
    private Thread sendingThread;
    
    JarSession (URL url, Properties environment, String jarName) throws UnknownServiceException
    {
        super (url, environment);
        onwardTS = createTransportSession (url, environment);
        this.jarName = jarName;
        sendingThread = null;
    }
    
    /**
     * Open a connection to the requested service.
     * <BR>Ignored if a connection has already been established.
     */
    public void connect() throws IOException
    {
        onwardTS.connect();
        pos = new PipedOutputStream();
        pis = new PipedInputStream (pos);
        jos = new JarOutputStream (pos);
        failure = null;
        sendingThread = new Thread() {
            public void run() {
                try {
                    onwardTS.send (pis, jarName, true);
                } catch (IOException ex) {
                    failure = ex;
                }
            }
        };
        sendingThread.start();
    }
    
    /**
     * Send a stream through the <B>connected</B> session.
     * <BR>Ignored if a connection is not open.
     */
    public void send (InputStream is, String name, boolean binary) throws IOException
    {
        JarEntry je = new JarEntry (name);
        jos.putNextEntry (je);
        for(int c; (c = is.read()) != -1; ) jos.write(c);
        jos.closeEntry();
        if (failure != null) throw failure;
    }
    
    /**
     * Close the session. This is ESSENTIAL for some protocols,
     * for the process to complete.
     * <BR>Ignored if a connection is not open.
     */
    public void disconnect() throws IOException
    {
        jos.finish();
        jos.close();
        if (failure != null) throw failure;
        while (sendingThread != null && sendingThread.isAlive()) Thread.yield();
        if (failure != null) throw failure;
        onwardTS.disconnect();
    }
    
    
    // Proxy onpassing...
    
    public boolean isConnected()
    {
        return onwardTS.isConnected();
    }
        
    public String getResult()
    {
        return onwardTS.getResult();
    }
        
    public String getLog()
    {
        return onwardTS.getLog();
    }
    
    public void addStatusListener (StatusListener listener)
    {
        onwardTS.addStatusListener (listener);
    }
}