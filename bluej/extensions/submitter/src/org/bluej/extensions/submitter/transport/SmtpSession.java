package org.bluej.extensions.submitter.transport;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * Implements RFC821 to send files via SMTP
 * to a given path on a given (attached) server.
 * 
 * NOTE: YOU MUST be shure that ALL text lines have a dot prepended 
 * if the line starts with a dot.
 * 
 * @author Clive Miller
 * @version $Id: SmtpSession.java 1959 2003-05-17 14:23:40Z damiano $
 */

public class SmtpSession extends TransportSession
{
    private String boundaryString;
    private DateFormat rfc822date = new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss z");
    private int fileCounter;

    public SmtpSession (URL url, Properties environment)
    {
        super (url, environment);
        result = "Not sent";
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
        
        connection.expect ("220 ", "421 ");
        connection.sendln ("HELO "+connection.getLocalHost());
        connection.expect (new String[] {"250 "},
                           new String[] {"500","501","504","421"});
        connection.sendln ("MAIL FROM:<"+userAddress+">");
        connection.expect (new String[] {"250 "},
                           new String[] {"552","451","452","500","501","421"});
        connection.sendln ("RCPT TO:<"+sendAddress+">");
        connection.expect (new String[] {"250 ","251 "},
                           new String[] {"550","551","552","553","450","451","452","500","501","503","421"});
        connection.sendln ("DATA");
        connection.expect (new String[] {"354 "},
                           new String[] {"451","554","500","501","503","421"});
        connection.flush();
        connection.sendln ("Date: "+rfc822date.format (new java.util.Date()));
        connection.sendln ("From: "+userAddress);
        connection.sendln ("To: "+sendAddress);
        if (subject == null) 
            connection.sendln ("Subject: BlueJ Submission");
        else 
            connection.sendln ("Subject: "+subject);
        
        sendMimeHeaders();

        if (body != null) 
            {
            sendBoundary (false);
            connection.sendln ("Content-Type: text/plain");
            connection.sendln ("Content-Transfer-Encoding: 7bit");
            connection.sendln ("");
            StringReader aReader = new StringReader(body);
            sendStreamToServer (aReader);
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
      connection.sendln (".");
      connection.expect (new String[] {"250 "},
                         new String[] {"552","554","451","452"});
      connection.sendln ("QUIT");
      connection.expect ("221 ");
      connection.close();
      connection = null;
      reportEvent ("Sent.");
      result = null;
      }

    private void sendMimeHeaders() throws IOException
      {
      connection.sendln ("MIME-Version: 1.0 (produced automatically by BlueJ)");
      connection.sendln ("Content-Type: multipart/mixed; boundary=\""+boundaryString+"\"");
      connection.sendln ("");
      connection.sendln ("This is a multi-part message in MIME format.");
      connection.sendln ("");
      }

    private void sendMimeText (String theText) throws IOException
      {
      connection.sendln ("Content-Type: text/plain");
      connection.sendln ("Content-Transfer-Encoding: 7bit");
      connection.sendln ("");
      StringReader aReader = new StringReader(theText);
      sendStreamToServer (aReader);
      }
            
    private void sendMimeBinaryFile (InputStream is, String name ) throws IOException
      {
      connection.sendln ("Content-Type: application/octet-stream; name=\""+name+"\"");
      connection.sendln ("Content-Transfer-Encoding: base64");
      connection.sendln ("Content-Disposition: attachment; filename=\""+name+"\"");
      connection.sendln ("");
      connection.sendMimeStream( is );
      reportLog ("===> sent binary file "+name);
      }


    private void sendMimeTextFile ( InputStream is, String name ) throws IOException
      {
      connection.sendln ("Content-Type: text/plain; name=\""+name+"\"");
      connection.sendln ("Content-Transfer-Encoding: 7bit");
      connection.sendln ("Content-Disposition: attachment; filename=\""+name+"\"");
      connection.sendln ("");

      int lineCount = sendStreamToServer ( new InputStreamReader(is) );

      reportLog ("===> sent text file "+name+" lineCount="+lineCount);
      }

    private void sendBoundary (boolean end) throws IOException
    {
        connection.sendln ("--"+boundaryString+ (end ? "--":""));
    }


  /**
   * Send the given input stream to the remote mailer.
   * The trick here is that ALL lines beginnig with a dot MUST have a dot prepended.
   * AND the line is terminated by CRLF
   * It will nicely return the number of lines sent
   */
  private int sendStreamToServer ( Reader inputReader ) throws IOException
    {
    int lineCount = 0;
    
    // You never Know, better be safe
    if ( inputReader == null ) return 0;
    
    BufferedReader aReader = new BufferedReader(inputReader);  

    String oneLine;
    while ( (oneLine=aReader.readLine()) != null )
      {
      // If the line starts with a dot, prepend a dot (RFC821 section 4.5.2)
      if ( oneLine.startsWith(".")) connection.send(".");
      connection.nologSendln(oneLine);
      lineCount++;
      }

    connection.flush();
    return lineCount;
    }


}    
