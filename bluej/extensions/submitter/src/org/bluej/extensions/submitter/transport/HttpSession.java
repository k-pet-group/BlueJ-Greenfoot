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
 * @author Clive Miller
 * @version $Id: HttpSession.java 1463 2002-10-23 12:40:32Z jckm $
 */

public class HttpSession extends TransportSession   
{
    private final String filefieldPrefix;
    private final String boundaryString;
    private OutputStream out;
    private int fileCounter;
    private HttpURLConnection connection;

    public HttpSession (URL url, Properties environment)
    {
        super (url, environment);
        out = null;
        String boundary = "";
        Random random = new java.util.Random();
        for (int i=0; i<30; i++) boundary += (char)SocketSession.encode((byte)random.nextInt(62));
        boundaryString = "---------------------------"+boundary;
        filefieldPrefix = urlProps.getProperty ("fileprefix");
    }

    public void connect() throws IOException
    {
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput (true);
        setStatus ("Creating message...");
        connection.setRequestProperty ("Accept", "text/html");
        connection.setRequestProperty ("Content-Type","multipart/form-data; boundary="+boundaryString);
        connection.setRequestProperty ("Connection", "Close");

        out = connection.getOutputStream();
        //  for (Enumeration e=urlProps.propertyNames(); e.hasMoreElements();) {
        // keys() is used instead of propertyNames() to avoid inheriting 
        // from the default list envProps.
        addBoundary (false);
        for (Enumeration e=urlProps.keys(); e.hasMoreElements();) {
            String field = (String)e.nextElement();
            String value = urlProps.getProperty (field);
            addMimeField (field, value);
        }
        fileCounter = 0;

    }
    
    public void send (InputStream is, String name, boolean binary) throws IOException
    {
        addMimeFile (is, name, binary, fileCounter++);
    }


    public void disconnect() throws IOException
    {
        setStatus ("Sending message...");
        result = send();
    }
  
    private void addMimeFile (InputStream is, String name, boolean binary, int index) throws IOException
    {
        addMessage ("Content-Disposition: form-data;"
//                    +" name=\""+(filefieldPrefix+index)+"\";"
                    +" name=\""+(filefieldPrefix)+"[]\";"
                    +" filename=\""+name+"\"");
        if (binary)
        {
            addMessage ("Content-Type: application/octet-stream");
            log.println ("Adding binary file "+name);
        }
        else
        {
            addMessage ("Content-Type: text/plain");
            log.println ("Adding text file "+name);
        }
        addMessage (null);
        addStream (is);
        addMessage (null);
        addBoundary (false);
    }

    private void addMimeField (String field, String value) throws IOException
    {
        addMessage ("Content-Disposition: form-data; name=\""+field+"\"");
        addMessage (null);
        addMessage (value);
        addBoundary (false);
    }
    
    private String send() throws IOException
    {
        setStatus ("Waiting for response...");
        out.close();
        connection.connect();
        StringWriter sr = new StringWriter();
        InputStream is = connection.getInputStream();
        int b;
        while ((b=is.read()) != -1) sr.write (b);
        is.close();
        String response = sr.toString();
        setStatus ("Sent OK");
        return response;
    }

    private void addBoundary (boolean end) throws IOException
    {
        addMessage ("--"+boundaryString+ (end ? "--":""));
    }

    private String formatMessage (String message)
    {
        if (message == null) message = "";
        int l = message.length();
        String end;
        if (l == 0) end = "";
        else if (l == 1) end = message;
        else end = message.substring (l-2, l);
        if (end.indexOf ('\r') == -1) message += '\r';
        if (end.indexOf ('\n') == -1) message += '\n';
        return message;
    }

    private void sendMessage (String message) throws IOException
    {
        out.write (formatMessage(message).getBytes());
    }

    private void addMessage (String message) throws IOException
    {
        String toSend = formatMessage (message);
        out.write (toSend.getBytes(), 0, toSend.length());
    }

    private void addStream (InputStream is) throws IOException
    {
        byte[] input = new byte [1024];
        int read;
        while ((read=is.read (input)) > 0) out.write (input, 0, read);
    }
}
    
