package org.bluej.extensions.submitter.transport;

import java.util.Properties;
import java.util.StringTokenizer;
import java.io.InputStream;
import java.io.IOException;
import java.net.UnknownServiceException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Superclass and manager of transport implementations.
 *
 * @author     Clive Miller
 * @version    $Id: TransportSession.java 2398 2003-11-28 12:05:30Z damiano $
 */

public abstract class TransportSession
{
    /**
     * Create a suitable transporter.
     *
     * @param  url                        the URL defining the parameters of the transport.
     * <BR><TABLE border>
     * <TR><TH align=left>Protocol<TH align=left>Notes
     * <TR><TD><CODE>mailto</CODE><TD>Email through an SMTP host.
     *     <BR><CODE>subject</CODE> <I>subject line</I>
     *     <BR><CODE>body</CODE> <I>message body</I>
     * <TR><TD><CODE>http</CODE><TD>HTTP POST returns an html (probably) result from the server.
     *     Files are in fields named <CODE>FILE1</CODE>, <CODE>FILE2</CODE>...
     * <TR><TD><CODE>ftp</CODE><TD>FTP session. Standard URL format for username, password, path etc
     * </TABLE><BR>
     * @param  environment                a set of properties defining the environment
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
     * @return                            an appropriate <CODE>TransportSession</CODE> implementation
     * @throws  UnknownServiceException   if this service does not support the protocol requested
     * @throws  IllegalArgumentException  if a necessary environment parameter is missing.
     */
    public static TransportSession createTransportSession(URL url, Properties environment)
             throws UnknownServiceException
    {
        String protocol = url.getProtocol();
        TransportSession ts;
        if (protocol.equals("mailto")) {
            if (environment.getProperty("smtphost") == null)
                throw new IllegalArgumentException("environment does not contain smtphost");
            if (environment.getProperty("useraddr") == null)
                throw new IllegalArgumentException("environment does not contain useraddr");
            ts = new SmtpSession(url, environment);
        }
        else if (protocol.equals("http"))
            ts = new HttpSession(url, environment);

        else if (protocol.equals("ftp"))
            ts = new FtpSession(url, environment);

        else if (protocol.equals("file"))
            ts = new FileSession(url, environment);

        else if (protocol.equals("message"))
            ts = new MessageSession(url, environment);

        else
            throw new UnknownServiceException(protocol);
        return ts;
    }


    /**
     *  Description of the Method
     *
     * @param  url                          Description of the Parameter
     * @param  environment                  Description of the Parameter
     * @param  jarName                      Description of the Parameter
     * @return                              Description of the Return Value
     * @exception  UnknownServiceException  Description of the Exception
     */
    public static TransportSession createJarTransportSession(URL url, Properties environment, String jarName)
             throws UnknownServiceException
    {
        TransportSession ts = new JarSession(url, environment, jarName);
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
    protected final Properties urlProps;

    /**
     *  Description of the Field
     */
    protected final URL url;
    /**
     *  Description of the Field
     */
    protected String result;
    /**
     *  Description of the Field
     */
    protected SocketSession connection;
    /**
     *  Description of the Field
     */
    protected TransportReport transportReport;


    /**
     *Constructor for the TransportSession object
     *
     * @param  url          Description of the Parameter
     * @param  environment  Description of the Parameter
     */
    TransportSession(URL url, Properties environment)
    {
        this.url = url;
        this.envProps = environment;
        urlProps = new Properties(envProps);
        String query = url.getQuery();
        if (query != null) {
            StringTokenizer st = new StringTokenizer(query, "&");
            while (st.hasMoreTokens()) {
                String param = st.nextToken();
                int equals = param.indexOf('=');
                String key;
                String value;
                if (equals == -1) {
                    key = param;
                    value = "";
                }
                else {
                    key = param.substring(0, equals);
                    value = param.substring(equals + 1);
                }
                value = URLDecoder.decode(value);
                urlProps.setProperty(key, value);
            }
        }
        result = "No connection established";
    }


    /**
     * Open a connection to the requested service.
     * <BR>Ignored if a connection has already been established.
     *
     * @exception  IOException  Description of the Exception
     */
    public abstract void connect() throws IOException;


    /**
     * Send a stream through the <B>connected</B> session.
     * <BR>Ignored if a connection is not open.
     *
     * @param  is               Description of the Parameter
     * @param  name             Description of the Parameter
     * @param  binary           Description of the Parameter
     * @exception  IOException  Description of the Exception
     */
    public abstract void send(InputStream is, String name, boolean binary) throws IOException;


    /**
     * Close the session. This is ESSENTIAL for some protocols,
     * for the process to complete.
     * <BR>Ignored if a connection is not open.
     *
     * @exception  IOException  Description of the Exception
     */
    public abstract void disconnect() throws IOException;


    /**
     * Check if a connection is currently open
     *
     * @return    <CODE>true</CODE> if a connection is open
     */
    public boolean isConnected()
    {
        return (connection != null);
    }


    /**
     * Get the response from the server. This should be called
     * <I>after</I> <CODE>disconnect()</CODE> for a meaningful
     * result.
     *
     * @return    The result value
     */
    public String getResult()
    {
        return result;
    }


    /**
     * Gets the delivery method. basically it just returns url.getProtocol();
     *
     * @return    The protocol value
     */
    public String getProtocol()
    {
        return url.getProtocol();
    }


    /**
     * Sets the class that will get the reporting events
     *
     * @param  i_transportReport  The new transportReport value
     */
    public void setTransportReport(TransportReport i_transportReport)
    {
        transportReport = i_transportReport;
    }


    /**
     * report an event to the one that is looking for it
     *
     * @param  message  Description of the Parameter
     */
    protected final void reportEvent(String message)
    {
        if (transportReport == null)
            return;

        transportReport.reportEvent(message);
    }


    /**
     * report a LOG to the one that is looking for it
     *
     * @param  message  Description of the Parameter
     */
    protected final void reportLog(String message)
    {
        if (transportReport == null)
            return;

        transportReport.reportLog(message);
    }

}
