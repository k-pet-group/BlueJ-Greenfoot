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

import java.util.Enumeration;

/**
 * A Proxy TransportSession which Jars files and redirects them to an
 * underlying TransportSession.
 * Adapted to have some vars inserted into the jar name.
 *
 * @author     Clive Miller
 * @version    $Id: JarSession.java 1606 2003-01-27 09:43:12Z damiano $
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


    /**
     *Constructor for the JarSession object
     *
     * @param  url                          Description of the Parameter
     * @param  environment                  Description of the Parameter
     * @param  i_jarName                    Description of the Parameter
     * @exception  UnknownServiceException  Description of the Exception
     */
    JarSession(URL url, Properties environment, String i_jarName) throws UnknownServiceException
    {
        super(url, environment);
        onwardTS = createTransportSession(url, environment);

        jarName = expandJarName(i_jarName);
        sendingThread = null;
    }


    /**
     * This has a jar name and looks for specifickeyword to substitute
     * the syntax is the usual <varname>.....fixstring
     * So, I need to bouild a new jarname with the old split in tokens
     *
     * @param  i_jarName  Description of the Parameter
     * @return            Description of the Return Value
     */
    private String expandJarName(String i_jarName)
    {
        String o_jarName = "noNameGiven.jar";
        // I am tryng to be nice with a nice default... Should I ?

        // Some mad may have done this, no reason to get too angry
        if (i_jarName == null)
            return o_jarName;

        // This is a user error, lets try to be nice with the user.
        if (i_jarName.length() <= 0)
            return o_jarName;

        // Now, the real problem is to manage ALL possible combination of <>
        // Possibly the best way to do it is this one.
        StringBuffer risul = new StringBuffer(100);
        StringBuffer varName = new StringBuffer(50);
        char[] inputName = i_jarName.toCharArray();
        boolean isVarName = false;

        for (int index = 0; index < inputName.length; index++) {
            char thisChar = inputName[index];

            if (thisChar == '<') {
                isVarName = true;
                continue;
            }

            if (thisChar == '>') {
                isVarName = false;
                // First I look up this varName into the system properties
                String aValue = envProps.getProperty(varName.toString());
                // Then, if not found I look it up into the properties inserted by the user by GUI
                if (aValue == null)
                    aValue = urlProps.getProperty(varName.toString());
                // If I have found something I ad it up to the risul...
                if (aValue != null)
                    risul.append(aValue);
                // Of course I have to zap the varName, othervise I keep appending...
                varName = new StringBuffer(50);
                continue;
            }

            if (isVarName)
                varName.append(thisChar);
            else
                risul.append(thisChar);
        }

        return risul.toString();
    }


    /**
     * Open a connection to the requested service.
     * <BR>Ignored if a connection has already been established.
     *
     * @exception  IOException  Description of the Exception
     */
    public void connect() throws IOException
    {
        onwardTS.connect();
        pos = new PipedOutputStream();
        pis = new PipedInputStream(pos);
        jos = new JarOutputStream(pos);
        failure = null;
        sendingThread =
            new Thread()
            {
                public void run()
                {
                    try {
                        onwardTS.send(pis, jarName, true);
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
     *
     * @param  is               Description of the Parameter
     * @param  name             Description of the Parameter
     * @param  binary           Description of the Parameter
     * @exception  IOException  Description of the Exception
     */
    public void send(InputStream is, String name, boolean binary) throws IOException
    {
        JarEntry je = new JarEntry(name);
        jos.putNextEntry(je);
        for (int c; (c = is.read()) != -1; )
            jos.write(c);
        jos.closeEntry();
        if (failure != null)
            throw failure;
    }


    /**
     * Close the session. This is ESSENTIAL for some protocols,
     * for the process to complete.
     * <BR>Ignored if a connection is not open.
     *
     * @exception  IOException  Description of the Exception
     */
    public void disconnect() throws IOException
    {
        jos.finish();
        jos.close();
        if (failure != null)
            throw failure;
        while (sendingThread != null && sendingThread.isAlive())
            Thread.yield();
        if (failure != null)
            throw failure;
        onwardTS.disconnect();
    }


    // Proxy onpassing...

    /**
     *  Gets the connected attribute of the JarSession object
     *
     * @return    The connected value
     */
    public boolean isConnected()
    {
        return onwardTS.isConnected();
    }


    /**
     *  Gets the result attribute of the JarSession object
     *
     * @return    The result value
     */
    public String getResult()
    {
        return onwardTS.getResult();
    }


    /**
     *  Gets the log attribute of the JarSession object
     *
     * @return    The log value
     */
    public String getLog()
    {
        return onwardTS.getLog();
    }


    /**
     *  Adds a feature to the StatusListener attribute of the JarSession object
     *
     * @param  listener  The feature to be added to the StatusListener attribute
     */
    public void addStatusListener(StatusListener listener)
    {
        onwardTS.addStatusListener(listener);
    }
}
