package org.bluej.extensions.submitter.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownServiceException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Superclass and manager of transport implementations.
 * @author Clive Miller
 * @version $Id: TransportSession.java 1463 2002-10-23 12:40:32Z jckm $
 */

public abstract class TransportSession
{
    /**
     * Create a suitable transporter.
     * @param url the URL defining the parameters of the transport.
     * <BR><TABLE border>
     * <TR><TH align=left>Protocol<TH align=left>Notes
     * <TR><TD><CODE>mailto</CODE><TD>Email through an SMTP host.
     *     <BR><CODE>subject</CODE> <I>subject line</I>
     *     <BR><CODE>body</CODE> <I>message body</I>
     * <TR><TD><CODE>http</CODE><TD>HTTP POST returns an html (probably) result from the server.
     *     Files are in fields named <CODE>FILE1</CODE>, <CODE>FILE2</CODE>...
     * <TR><TD><CODE>ftp</CODE><TD>FTP session. Standard URL format for username, password, path etc
     * </TABLE><BR>
     * @param environment a set of properties defining the environment
     * in which the transporter is going to operate. Depending on the
     * URL used for the transport itself, this may need to define some
     * of the following properties:<BR>
     * <TABLE border><TR><TH align=left>Key<TH align=left>Use
     * <TR><TD><CODE>smtphost</CODE><TD>Host name of an SMTP server
     * <TR><TD><CODE>useraddr</CODE><TD>Email address of the sender
     * <TR><TD><CODE>proxyhost</CODE><TD>HTTP Proxy server address
     * <TR><TD><CODE>proxyport</CODE><TD>HTTP Proxy server port
     * <TR><TD><CODE>fileprefix</CODE><TD>Prefix to the parameter that
     * carries the files for http post
     * </TABLE><BR>
     * These are used dynamically, so the Properties object can change and the
     * changes will be read when used.
     * @return an appropriate <CODE>TransportSession</CODE> implementation
     * @throws UnknownServiceException if this service does not support the protocol requested
     * @throws IllegalArgumentException if a necessary environment parameter is missing.
     */
    public static TransportSession createTransportSession (URL url, Properties environment) 
                                     throws UnknownServiceException
    {
        String protocol = url.getProtocol();
        TransportSession ts;
        if (protocol.equals ("mailto")) {
            if (environment.getProperty ("smtphost") == null)
                throw new IllegalArgumentException ("environment does not contain smtphost");
            if (environment.getProperty ("useraddr") == null)
                throw new IllegalArgumentException ("environment does not contain useraddr");
            ts = new SmtpSession (url, environment);
        }
        else if (protocol.equals ("http")) {
            ts = new HttpSession (url, environment);
        }
        else if (protocol.equals ("ftp")) {
            ts = new FtpSession (url, environment);
        }
        else if (protocol.equals ("file")) {
            ts = new FileSession (url, environment);
        }
        else if (protocol.equals ("message")) {
            ts = new MessageSession (url, environment);
        }
        else throw new UnknownServiceException (protocol);
        return ts;
    }

    public static TransportSession createJarTransportSession (URL url, Properties environment, String jarName) 
                                     throws UnknownServiceException
    {
        TransportSession ts = new JarSession (url, environment, jarName);
        return ts;
    }
    
    /**
     * Environment properties, as provided for <CODE>connect</CODE>
     */
    protected Properties envProps;
    
    /**
     * Properties derived from the Query part of the URL, but default
     * to envProps, therefore always use this in preference to envProps
     * to get access to both.
     */
    protected final Properties  urlProps;
    
    /**
     * The actual URL itself
     */
    protected final URL url;
    
    private final Collection listeners; // StatusListener

    private final StringWriter logWriter;
    protected final PrintWriter log;
    protected String result;
    protected SocketSession connection;

    TransportSession (URL url, Properties environment)
    {
        listeners = new ArrayList();
        this.url = url;
        this.envProps = environment;
        logWriter = new StringWriter();
        log = new PrintWriter (logWriter);
        urlProps = new Properties (envProps);
        String query = url.getQuery();
        if (query != null) {
            StringTokenizer st = new StringTokenizer (query, "&");
            while (st.hasMoreTokens()) {
                String param = st.nextToken();
                int equals = param.indexOf ('=');
                String key;
                String value;
                if (equals == -1) {
                    key = param;
                    value = "";
                } else {
                    key = param.substring (0,equals);
                    value = param.substring (equals+1);
                }
                value = URLDecoder.decode (value);
                urlProps.setProperty (key, value);
            }
        }
        result = "No connection established";
    }
    
    /**
     * Open a connection to the requested service.
     * <BR>Ignored if a connection has already been established.
     */
    abstract public void connect() throws IOException;
    
    /**
     * Send a stream through the <B>connected</B> session.
     * <BR>Ignored if a connection is not open.
     */
    abstract public void send (InputStream is, String name, boolean binary) throws IOException;
    
    /**
     * Close the session. This is ESSENTIAL for some protocols,
     * for the process to complete.
     * <BR>Ignored if a connection is not open.
     */
    abstract public void disconnect() throws IOException;
    
    /**
     * Check if a connection is currently open
     * @return <CODE>true</CODE> if a connection is open
     */
    public boolean isConnected()
    {
        return (connection != null);
    }
        
    /**
     * Get the response from the server. This should be called
     * <I>after</I> <CODE>disconnect()</CODE> for a meaningful
     * result.
     */
    public String getResult()
    {
        return result;
    }
        
    /**
     * Get a snapshot of the log of the connection
     * @return a String containing newlines describing the session events
     */
    public String getLog()
    {
        try {
            log.flush();
            logWriter.flush();
            if (logWriter.getBuffer().length() == 0)
                return null;
            else
                return logWriter.toString();
        }
        catch (Throwable ex)
        {
            return "Cannot open log due to "+ex;
        }
    }
    
    /**
     * Add a text listener to listen for status message changes
     * @param listener a listener that will get informed of every status message
     */
    public void addStatusListener (StatusListener listener)
    {
        listeners.add (listener);
    }

    final synchronized void setStatus (String status)
    {
        log.println (status);
        if (listeners == null) return;
        for (Iterator it=listeners.iterator(); it.hasNext();) {
            StatusListener listener = (StatusListener)it.next();
            listener.statusChanged (status);
        }
    }
}