package org.bluej.extensions.submitter.transport;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Implements RFC821 to send files via SMTP
 * to a given path on a given (attached) server.
 * 
 * @author Clive Miller
 * @version $Id: SmtpSession.java 1588 2002-12-14 19:23:46Z iau $
 */

public class SmtpSession extends TransportSession
{
    private final String boundaryString;
    private OutputStream out;
    private int fileCounter;
    private static final DateFormat rfc822date = 
        new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss z");

    public SmtpSession (URL url, Properties environment)
    {
        super (url, environment);
        result = "Not sent";
        out = null;
        String boundary = "";
        java.util.Random random = new java.util.Random();
        for (int i=0; i<30; i++) boundary += (char)SocketSession.encode((byte)random.nextInt(62));
        boundaryString = "part_boundary_"+boundary;
        fileCounter = 0;
    }

    public void connect() throws IOException
    {
        String smtpHost = urlProps.getProperty ("smtphost");
        if (smtpHost == null || smtpHost.length() == 0) throw new IllegalArgumentException ("SMTP Host has not been set");
        String userAddress = urlProps.getProperty ("useraddr");
        if (userAddress == null 
            || userAddress.indexOf ('@') < 1 
            || userAddress.indexOf ('@') == userAddress.length()-1) throw new IllegalArgumentException ("User Email address invalid");
        String sendAddress = url.getPath();
        String subject = urlProps.getProperty ("subject");
        String body = urlProps.getProperty ("body");
        setStatus ("Connecting to host "+smtpHost+"...");

        int port = 25;
        if (url.getPort() != -1) port = url.getPort();
        connection = new SocketSession (smtpHost, port);
        setStatus ("Sending message...");

        connection.setLogger (log);
//        try { connection.setLogfile("c:/tmp/smtp.txt"); } catch ( Exception e ) {}
        
        out = connection.getOutputStream();
        connection.expect ("220 ", "421 ");
        connection.send ("HELO "+connection.getLocalHost());
        connection.expect (new String[] {"250 "},
                           new String[] {"500","501","504","421"});
        connection.send ("MAIL FROM:<"+userAddress+">");
        connection.expect (new String[] {"250 "},
                           new String[] {"552","451","452","500","501","421"});
        connection.send ("RCPT TO:<"+sendAddress+">");
        connection.expect (new String[] {"250 ","251 "},
                           new String[] {"550","551","552","553","450","451","452","500","501","503","421"});
        connection.send ("DATA");
        connection.expect (new String[] {"354 "},
                           new String[] {"451","554","500","501","503","421"});
        out.flush();
        sendMessage ("Date: "+rfc822date.format (new java.util.Date()));
        sendMessage ("From: "+userAddress);
        sendMessage ("To: "+sendAddress);
        if (subject != null) {
            sendMessage ("Subject: "+subject);
        }
        sendMimeHeaders();
        if (body != null) {
            sendBoundary (false);
            sendMimeText (body);
        }
        setStatus ("OK");
    }
    
    public void send (InputStream is, String name, boolean binary) throws IOException
    {
        String packageName = name.replace ('/','_');
        sendBoundary (false);
        sendMimeFile (is, packageName, binary);
        fileCounter++;
    }

    public void disconnect() throws IOException
    {
        setStatus ("Disconnecting...");
        sendBoundary (true);
        connection.send (".");
        connection.expect (new String[] {"250 "},
                           new String[] {"552","554","451","452"});
        connection.send ("QUIT");
        connection.expect ("221 ");
        connection.close();
        connection = null;
        setStatus ("Sent.");
        result = null;
    }

    private void sendMimeHeaders() throws IOException
    {
        sendMessage ("MIME-Version: 1.0 (produced automatically by BlueJ)");
        sendMessage ("Content-Type: multipart/mixed;");
        sendMessage ("        boundary=\""+boundaryString+"\"");
        sendMessage (null);
        sendMessage ("This is a multi-part message in MIME format.");
        sendMessage (null);
    }

    private void sendMimeText (String theText) throws IOException
    {
        sendMessage ("Content-Type: text/plain");
        sendMessage ("Content-Transfer-Encoding: 7bit");
        sendMessage (null);
        sendMessage (theText);
    }
            
    private void sendMimeFile (InputStream is, String name, boolean binary) throws IOException
    {
        if (binary)
        {
            sendMessage ("Content-Type: application/octet-stream;");
            sendMessage ("        name=\""+name+"\"");
            sendMessage ("Content-Transfer-Encoding: base64");
            sendMessage ("Content-Disposition: attachment;");
            sendMessage ("        filename=\""+name+"\"");
            sendMessage (null);
            SocketSession.MIMEEncode (is, out);
            log.println ("[sent binary file "+name+"]");
        }
        else
        {
            int state = 0;
            sendMessage ("Content-Type: text/plain;");
            sendMessage ("        name=\""+name+"\"");
            sendMessage ("Content-Transfer-Encoding: 7bit");
            sendMessage ("Content-Disposition: attachment;");
            sendMessage ("        filename=\""+name+"\"");
            sendMessage (null);
            for(int c; (c = is.read()) != -1; ) {
                if (c == '\n' || c == '\r') {
                    if (state == 0) state = 1;
                    else if (state == 2) {
                        state = 1;
                        out.write ('.');
                    }
                }
                else if (c == '.' && state == 1) state = 2;
                out.write(c);
            }
            out.write ('\n');
            log.println ("[sent text file "+name+"]");
        }
    }

    private void sendBoundary (boolean end) throws IOException
    {
        sendMessage ("--"+boundaryString+ (end ? "--":""));
    }

    private void sendMessage (String message) throws IOException
    {
        if (message == null) message = "";
        int l = message.length();
        String end;
        if (l == 0) end = "";
        else if (l == 1) end = message;
        else end = message.substring (l-2);
        if (end.indexOf ('\r') == -1) message += '\r';
        if (end.indexOf ('\n') == -1) message += '\n';
        int p=0;
        while ((p=message.indexOf ('.', p)) != -1) {
            if ((p==0 || message.charAt (p-1)=='\n' || message.charAt (p-1)=='\r')
                && (message.charAt (p+1)=='\n' || message.charAt (p+1)=='\r'))
                message = message.substring (0,p) + '.' + message.substring (p);
            p++;
        }
        out.write (message.getBytes());
        log.print (">>"+message);
    }
}    
