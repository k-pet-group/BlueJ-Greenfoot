package org.bluej.extensions.submitter.transport;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * Implements RFC2616 to send files via HTTP
 * to a given path on a given (attached) server.
 *
 *
 * @author     Clive Miller
 * @version    $Id: HttpSession.java 1607 2003-01-27 09:44:39Z damiano $
 */

public class HttpSession extends TransportSession
{
    // NOTE: Changing this CONSTANT reflects on the SERVER side HTTP parser. DO NOT CHANGE IT, Damiano
    private final static String VAR_fileNamePrefix = "file";

    private final String boundaryString;
    private OutputStream out;
    private int fileCounter = 0;
    private HttpURLConnection connection;


    /**
     *Constructor for the HttpSession object
     *
     * @param  url          Description of the Parameter
     * @param  environment  Description of the Parameter
     */
    public HttpSession(URL url, Properties environment)
    {
        super(url, environment);
        out = null;
        String boundary = "";
        Random random = new java.util.Random();
        for (int i = 0; i < 30; i++)
            boundary += (char) SocketSession.encode((byte) random.nextInt(62));
        boundaryString = "---------------------------" + boundary;
    }


    /**
     *  Description of the Method
     *
     * @exception  IOException  Description of the Exception
     */
    public void connect() throws IOException
    {
        fileCounter = 0;

        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        setStatus("Creating message...");
        connection.setRequestProperty("Accept", "text/html");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundaryString);
        connection.setRequestProperty("Connection", "Close");

        out = connection.getOutputStream();
        //  for (Enumeration e=urlProps.propertyNames(); e.hasMoreElements();) {
        // keys() is used instead of propertyNames() to avoid inheriting
        // from the default list envProps.
        addBoundary(false);
        for (Enumeration e = urlProps.keys(); e.hasMoreElements(); ) {
            String field = (String) e.nextElement();
            String value = urlProps.getProperty(field);
            addMimeField(field, value);
        }
    }


    /**
     *  Description of the Method
     *
     * @param  is               Description of the Parameter
     * @param  name             Description of the Parameter
     * @param  binary           Description of the Parameter
     * @exception  IOException  Description of the Exception
     */
    public void send(InputStream is, String name, boolean binary) throws IOException
    {
        // The filecounter is needed to have unique variable names arriving at the HTTP server
        // it MUST be a ++filecounter since the counting starts at one....
        addMimeFile(is, name, binary, ++fileCounter);
    }


    /**
     *  Description of the Method
     *
     * @exception  IOException  Description of the Exception
     */
    public void disconnect() throws IOException
    {
        setStatus("Sending message...");
        result = send();
    }


    /**
     *  Adds a feature to the MimeFile attribute of the HttpSession object
     *
     * @param  is               The feature to be added to the MimeFile attribute
     * @param  name             The feature to be added to the MimeFile attribute
     * @param  binary           The feature to be added to the MimeFile attribute
     * @param  index            The feature to be added to the MimeFile attribute
     * @exception  IOException  Description of the Exception
     */
    private void addMimeFile(InputStream is, String name, boolean binary, int index) throws IOException
    {
        addMessage("Content-Disposition: form-data;"
                + " name=\"" + (VAR_fileNamePrefix + index) + "\";"
                + " filename=\"" + name + "\"");
        if (binary) {
            addMessage("Content-Type: application/octet-stream");
            log.println("Adding binary file " + name);
        }
        else {
            addMessage("Content-Type: text/plain");
            log.println("Adding text file " + name);
        }
        addMessage(null);
        addStream(is);
        addMessage(null);
        addBoundary(false);
    }


    /**
     *  Adds a feature to the MimeField attribute of the HttpSession object
     *
     * @param  field            The feature to be added to the MimeField attribute
     * @param  value            The feature to be added to the MimeField attribute
     * @exception  IOException  Description of the Exception
     */
    private void addMimeField(String field, String value) throws IOException
    {
        addMessage("Content-Disposition: form-data; name=\"" + field + "\"");
        addMessage(null);
        addMessage(value);
        addBoundary(false);
    }


    /**
     *  Description of the Method
     *
     * @return                  Description of the Return Value
     * @exception  IOException  Description of the Exception
     */
    private String send() throws IOException
    {
        setStatus("Waiting for response...");
        out.close();
        connection.connect();
        StringWriter sr = new StringWriter();
        InputStream is = connection.getInputStream();
        int b;
        while ((b = is.read()) != -1)
            sr.write(b);
        is.close();
        String response = sr.toString();
        setStatus("Sent OK");
        return response;
    }


    /**
     *  Adds a feature to the Boundary attribute of the HttpSession object
     *
     * @param  end              The feature to be added to the Boundary attribute
     * @exception  IOException  Description of the Exception
     */
    private void addBoundary(boolean end) throws IOException
    {
        addMessage("--" + boundaryString + (end ? "--" : ""));
    }


    /**
     *  Description of the Method
     *
     * @param  message  Description of the Parameter
     * @return          Description of the Return Value
     */
    private String formatMessage(String message)
    {
        if (message == null)
            message = "";
        int l = message.length();
        String end;
        if (l == 0)
            end = "";
        else if (l == 1)
            end = message;
        else
            end = message.substring(l - 2, l);
        if (end.indexOf('\r') == -1)
            message += '\r';
        if (end.indexOf('\n') == -1)
            message += '\n';
        return message;
    }


    /**
     *  Description of the Method
     *
     * @param  message          Description of the Parameter
     * @exception  IOException  Description of the Exception
     */
    private void sendMessage(String message) throws IOException
    {
        out.write(formatMessage(message).getBytes());
    }


    /**
     *  Adds a feature to the Message attribute of the HttpSession object
     *
     * @param  message          The feature to be added to the Message attribute
     * @exception  IOException  Description of the Exception
     */
    private void addMessage(String message) throws IOException
    {
        String toSend = formatMessage(message);
        out.write(toSend.getBytes(), 0, toSend.length());
    }


    /**
     *  Adds a feature to the Stream attribute of the HttpSession object
     *
     * @param  is               The feature to be added to the Stream attribute
     * @exception  IOException  Description of the Exception
     */
    private void addStream(InputStream is) throws IOException
    {
        byte[] input = new byte[1024];
        int read;
        while ((read = is.read(input)) > 0)
            out.write(input, 0, read);
    }
}

