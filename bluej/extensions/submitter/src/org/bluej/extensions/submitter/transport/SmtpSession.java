package org.bluej.extensions.submitter.transport;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;

/**
 * Implements RFC821 to send files via SMTP
 * to a given path on a given (attached) server.
 * 
 * @author Clive Miller
 * @version $Id: SmtpSession.java 1958 2003-05-16 15:30:56Z iau $
 */

public class SmtpSession extends TransportSession
{
    private final String boundaryString;
    private OutputStream out;
    private int fileCounter;
    private final DateFormat rfc822date = 
        new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss z");
    private final byte[] CRLF={'\r','\n'};

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
        reportEvent ("Connecting to host "+smtpHost+"...");

        int port = 25;
        if (url.getPort() != -1) port = url.getPort();
        connection = new SocketSession (smtpHost, port);
        reportEvent ("Sending message...");

        // WARNING: transportReport MUST be set to take effect here.
        connection.setTransportReport(transportReport);
        
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
        if (subject == null) {
            sendMessage ("Subject: BlueJ Submission");
        } else {
            sendMessage ("Subject: "+subject);
        }
        sendMimeHeaders();
        if (body != null) {
            sendBoundary (false);
            sendMimeText (body);
        }
        reportEvent ("OK");
    }
    
    public void send (InputStream is, String name, boolean binary) throws IOException
    {
        String packageName = name.replace ('/','_');
        sendBoundary (false);
        if ( binary )
          sendMimeBinaryFile (is, packageName );
        else
          sendMimeTextFile (is, packageName );
        fileCounter++;
    }

    public void disconnect() throws IOException
    {
        reportEvent ("Disconnecting...");
        sendBoundary (true);
        connection.send (".");
        connection.expect (new String[] {"250 "},
                           new String[] {"552","554","451","452"});
        connection.send ("QUIT");
        connection.expect ("221 ");
        connection.close();
        connection = null;
        reportEvent ("Sent.");
        result = null;
    }

    private void sendMimeHeaders() throws IOException
    {
        sendMessage ("MIME-Version: 1.0 (produced automatically by BlueJ)");
        sendMessage ("Content-Type: multipart/mixed; boundary=\""+boundaryString+"\"");
        sendMessage ("");
        sendMessage ("This is a multi-part message in MIME format.");
        sendMessage ("");
    }

    private void sendMimeText (String theText) throws IOException
    {
        sendMessage ("Content-Type: text/plain");
        sendMessage ("Content-Transfer-Encoding: 7bit");
        sendMessage ("");
        sendMessage (theText);
    }
            
  private void sendMimeBinaryFile (InputStream is, String name ) throws IOException
    {
    sendMessage ("Content-Type: application/octet-stream; name=\""+name+"\"");
    sendMessage ("Content-Transfer-Encoding: base64");
    sendMessage ("Content-Disposition: attachment; filename=\""+name+"\"");
    sendMessage ("");
    SocketSession.MIMEEncode (is, out);
    reportLog ("===> sent binary file "+name);
    }


  private void sendMimeTextFile ( InputStream is, String name ) throws IOException
    {
    String oneLine;
    int lineCount=0;

    BufferedReader aReader = new BufferedReader(new InputStreamReader(is));
    
    sendMessage ("Content-Type: text/plain; name=\""+name+"\"");
    sendMessage ("Content-Transfer-Encoding: 7bit");
    sendMessage ("Content-Disposition: attachment; filename=\""+name+"\"");
    sendMessage ("");

    /* Now we have to go line by line, until EOF and spit them out watching
     * for leading dots
     */
    while ( (oneLine=aReader.readLine()) != null )
      {
      // If the line starts with a dot, prepend a dot (RFC821 section 4.5.2)
      if ( oneLine.startsWith(".")) out.write('.');
      out.write(oneLine.getBytes());       // Then we write what we just read
      out.write(CRLF);       // and terminate with standard CRLF
      lineCount++;
      }

    out.flush();
    reportLog ("===> sent text file "+name+" lineCount="+lineCount);
    }

    private void sendBoundary (boolean end) throws IOException
    {
        sendMessage ("--"+boundaryString+ (end ? "--":""));
    }


  private void sendMessage (String message) throws IOException
    {
    if ( message == null ) 
      {
      reportLog ("sendMessage: ERROR: message==null");
      return;
      }

    if ( message.length() == 0 )
      {
      // We just want to write a newline
      reportLog (">>");
      out.write(CRLF);
      return;        
      }
      
    BufferedReader aReader = new BufferedReader(new StringReader(message));

    String oneLine;
    while ( (oneLine=aReader.readLine()) != null )
      {
      // If the line starts with a dot, prepend a dot (RFC821 section 4.5.2)
      if ( oneLine.startsWith(".")) out.write('.');
      out.write(oneLine.getBytes());
      out.write(CRLF);

      reportLog (">> "+oneLine);
      }

    out.flush();
    }
}    
