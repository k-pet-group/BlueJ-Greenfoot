package org.bluej.extensions.submitter.transport;

import java.util.Properties;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;

/**
 * Implements RFC959 to send files via FTP
 * to a given path on a given (attached) server. It uses FTP in passive mode.
 * 
 * @author Clive Miller
 * @version $Id: FtpSession.java 1959 2003-05-17 14:23:40Z damiano $
 */

public class FtpSession extends TransportSession
{
    private int fileCounter;
    private String rootDir;
    
    public FtpSession (URL url, Properties environment)
    {
        super (url, environment);
        result = "Not sent";
        fileCounter = 0;
    }

    public void connect() throws IOException
    {
        String ftpHost = url.getHost();
        String userInfo = url.getUserInfo();
        String username, password;
        if (userInfo == null) {
            username = "anonymous";
            password = urlProps.getProperty ("useraddr","");
        } else {
            int colon = userInfo.indexOf (':');
            username = (colon == -1) ? userInfo : userInfo.substring (0,colon);
            password = (colon == -1) ? "" : userInfo.substring (colon+1);
        }
        reportEvent ("Starting FTP...");

        String reply;
        int port = 21;
        if (url.getPort() != -1) port = url.getPort();
        connection = new SocketSession (ftpHost, port);

        // WARNING: transportReport MUST be set to take effect here.
        connection.setTransportReport(transportReport);

        connection.expect ("220 ", "421");
        connection.sendln ("USER "+username);
        reply = connection.expect (new String[] {"331 ","230 "},
                                   new String[] {"530","500","501","421","332"});
        if (reply.startsWith ("331")) {
            if (password.length() == 0) throw new ProtocolException ("No password provided!");
            connection.sendln ("PASS "+password);
              connection.expect (new String[] {"230 ", "202 "},
                                 new String[] {"202","530","500","501","503","421","332"});
        }
    }

    public void send (InputStream is, String name, boolean binary) throws IOException
    {
        String sendPath = url.getPath();

        if ( rootDir == null ) {
            connection.sendln("PWD");
            String aResult = connection.expect (new String[] {"257 "},
                           new String[] {"500","501","502","421","530","550"});

            int firstQuote = aResult.indexOf ("\"");
            int lastQuote  = aResult.indexOf("\"",firstQuote+1);
            rootDir = aResult.substring(firstQuote+1,lastQuote);
        }

        connection.sendln ("CWD "+rootDir);
        connection.expect (new String[] {"250 "},
                           new String[] {"500","501","502","421","530","550"});
                           
        if (!sendPath.equals ("") && !sendPath.equals ("/")) {
            connection.sendln ("CWD "+sendPath.substring(1));
            String resp = connection.expect (new String[] {"250 ","550"},
                                             new String[] {"500","501","502","421","530"});
            if (resp.startsWith("550")) {
                throw new FileNotFoundException ("ftp://"+url.getHost()+sendPath);
            }
        }
        
        int slash;
        while ((slash = name.indexOf ('/')) != -1) {
            String dir = name.substring (0,slash);
            name = name.substring (slash+1);
            connection.sendln ("CWD "+dir);
            if (connection.expect (new String[] {"250 ","550 "},
                                   new String[] {"500","501","502","421","530"}).startsWith ("550 ")) {
                connection.sendln ("MKD "+dir);
                connection.expect (new String[] {"257 "},
                                   new String[] {"500","501","502","421","530","550"});
                connection.sendln ("CWD "+dir);
                connection.expect (new String[] {"250 "},
                                   new String[] {"500","501","502","421","530","550"});
            }
        }
            
        if (binary) {
            connection.sendln ("TYPE I");
            connection.expect (new String[] {"200 "},
                               new String[] {"500","501","504","421","530"});
        } else {
            connection.sendln ("TYPE A");
            connection.expect (new String[] {"200 "},
                               new String[] {"500","501","504","421","530"});
        }
        connection.sendln ("PASV");
        String portString = connection.expect (new String[] {"227 "},
                                               new String[] {"500","501","502","421","530"}); 
            // == 227 Entering Passive Mode (129,12,3,176,187,240)
        int p = portString.indexOf ('(');
        int[] param = new int[6];
        int paramNo = 0;
        for (int i=p+1; i<portString.length() && paramNo < 6; i++)
        if (Character.getType(portString.charAt(i)) != Character.DECIMAL_DIGIT_NUMBER)
        {
             param[paramNo++]=Integer.parseInt (portString.substring (p+1,i));
             p=i;
        }
        if (paramNo < 6) throw new ProtocolException ("Unexpected message from FTP server");

        String host = param[0]+"."+param[1]+"."+param[2]+"."+param[3];
        int port = (param[4] << 8) + param[5];
        Socket data = new Socket (host, port);
        connection.sendln ("STOR "+name);
        connection.expect (new String[] {"150 ","125 "},
                           new String[] {"425","426","451","551","552","532","450","452","553","500","501","421","530"});
        OutputStream out = data.getOutputStream();
        byte[] buffer = new byte [data.getSendBufferSize()];
        for(int c; (c = is.read(buffer)) != -1; ) out.write(buffer, 0, c);
        data.shutdownOutput();
        data.close();
        connection.expect (new String[] {"226 ", "250 "},
                           new String[] {"425","426","451","551","552","532","450","452","553","500","501","421","530"});
        fileCounter++;
    }
    
    public void disconnect() throws IOException
    {
        connection.sendln ("QUIT");
        connection.expect ("221 ", "500");
        connection.close();
        connection = null;
        reportEvent ("Connection closed.");
        result = null;
    }
}
    
