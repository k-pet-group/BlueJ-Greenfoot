/*********************************************************************

@version 1.00 7/17/96
@author James Driscoll maus@io.com


Usage -

Version History:
0.8 5/15/96  - First version
0.9 5/16/96  - fixed date code, added localhost to HELO,
               fixed Subject bug
0.91 7/10/96  - Yet another date fix, for European TimeZones.  Man, they
                gotta fix that code...
1.00 7/17/96  - renamed to Qsmtp, as I have plans for the SMTP code,
                and I want to get this out and announced.  Also cleaned it
                up and commented out the DEBUG code (for size, just in case
                the compiler didn't optimize it out on your machine - mine
                didn't (Symantec Cafe Lite, you get what you pay for, and
                I paid for a book)).
1.01 9/18/96  - Fixed the call to getLocalHost local, which 1.02 JDK didn't
                like (Cafe Lite didn't mind, though).  Think I'll be using
                JDK for all compliations from now on.  Also, added a close
                method, since finalize() is not guarenteed to be called(!).
1.1 12/26/96 -  Fixed problem with EOL, I was using the Unix EOL, not the
                network end of line.  A fragile mail server was barfing.
                I can't beleive I wrote this - that's what half a year will do.
                Also, yanked out the debug code.  It annoyed me.
1.11 12/27/97 - Forgot to flush(), println used to do that for me...


***********************************************************************/


import java.net.*;
import java.io.*;
import java.util.*;

public class Qsmtp {

    static final int DEFAULT_PORT = 25;
    static final String EOL = "\r\n"; // network end of line

    protected DataInputStream reply = null;
    protected PrintStream send = null;
    protected Socket sock = null;

    /**
     *   Create a Qsmtp object pointing to the specified host
     *   @param hostid The host to connect to.
     *   @exception UnknownHostException
     *   @exception IOException
     */
    public Qsmtp( String hostid) throws UnknownHostException, IOException {
        this(hostid, DEFAULT_PORT);
    }

    public Qsmtp( String hostid, int port) throws UnknownHostException, IOException {
        sock = new Socket( hostid, port );
        reply = new DataInputStream( sock.getInputStream() );
        send = new PrintStream( sock.getOutputStream() );
        String rstr = reply.readLine();
        if (!rstr.startsWith("220")) throw new ProtocolException(rstr);
        while (rstr.indexOf('-') == 3) {
            rstr = reply.readLine();
            if (!rstr.startsWith("220")) throw new ProtocolException(rstr);
        }
    }

    public Qsmtp( InetAddress address ) throws IOException {
        this(address, DEFAULT_PORT);
    }

    public Qsmtp( InetAddress address, int port ) throws IOException {
        sock = new Socket( address, port );
        reply = new DataInputStream( sock.getInputStream() );
        send = new PrintStream( sock.getOutputStream() );
        String rstr = reply.readLine();
        if (!rstr.startsWith("220")) throw new ProtocolException(rstr);
        while (rstr.indexOf('-') == 3) {
            rstr = reply.readLine();
            if (!rstr.startsWith("220")) throw new ProtocolException(rstr);
        }
    }

    public void sendmsg( String from_address, String to_address,
                         String subject, String message )
                         throws IOException, ProtocolException {

        String rstr;
        String sstr;

        InetAddress local;
        try {
          local = InetAddress.getLocalHost();
        }
        catch (UnknownHostException ioe) {
          System.err.println("No local IP address found - is your network up?");
          throw ioe;
        }
        String host = local.getHostName();
        send.print("HELO " + host);
        send.print(EOL);
        send.flush();
        rstr = reply.readLine();
        if (!rstr.startsWith("250")) throw new ProtocolException(rstr);
        sstr = "MAIL FROM: " + from_address ;
        send.print(sstr);
        send.print(EOL);
        send.flush();
        rstr = reply.readLine();
        if (!rstr.startsWith("250")) throw new ProtocolException(rstr);
        sstr = "RCPT TO: " + to_address;
        send.print(sstr);
        send.print(EOL);
        send.flush();
        rstr = reply.readLine();
        if (!rstr.startsWith("250")) throw new ProtocolException(rstr);
        send.print("DATA");
        send.print(EOL);
        send.flush();
        rstr = reply.readLine();
        if (!rstr.startsWith("354")) throw new ProtocolException(rstr);
        send.print("From: " + from_address);
        send.print(EOL);
        send.print("To: " + to_address);
        send.print(EOL);
        send.print("Subject: " + subject);
        send.print(EOL);

        // Create Date - we'll cheat by assuming that local clock is right

        Date today_date = new Date();
        send.print("Date: " + msgDateFormat(today_date));
        send.print(EOL);
        send.flush();

        // Warn the world that we are on the loose - with the comments header:
        send.print("Comment: Unauthenticated sender");
        send.print(EOL);
        send.print("X-Mailer: JNet Qsmtp");
        send.print(EOL);

        // Sending a blank line ends the header part.
        send.print(EOL);

        // Now send the message proper
        send.print(message);
        send.print(EOL);
        send.print(".");
        send.print(EOL);
        send.flush();

        rstr = reply.readLine();
        if (!rstr.startsWith("250")) throw new ProtocolException(rstr);
    }

    public void close() {
      try {
        send.print("QUIT");
        send.print(EOL);
        send.flush();
        sock.close();
      }
      catch (IOException ioe) {
        // As though there's anything I can do about it now...
      }
    }

    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    private String msgDateFormat( Date senddate) {
        String formatted = "hold";

        String Day[] = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        String Month[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        formatted = Day[senddate.getDay()] + ", ";
        formatted = formatted + String.valueOf(senddate.getDate()) + " ";
        formatted = formatted + Month[senddate.getMonth()] + " ";
        if (senddate.getYear() > 99)
            formatted = formatted + String.valueOf(senddate.getYear() + 1900) + " ";
        else
            formatted = formatted + String.valueOf(senddate.getYear()) + " ";
        if (senddate.getHours() < 10) formatted = formatted + "0";
        formatted = formatted + String.valueOf(senddate.getHours()) + ":";
        if (senddate.getMinutes() < 10) formatted = formatted + "0";
        formatted = formatted + String.valueOf(senddate.getMinutes()) + ":";
        if (senddate.getSeconds() < 10) formatted = formatted + "0";
        formatted = formatted + String.valueOf(senddate.getSeconds()) + " ";
        if (senddate.getTimezoneOffset() < 0)
            formatted = formatted + "+";
        else
            formatted = formatted + "-";
        if (Math.abs(senddate.getTimezoneOffset())/60 < 10) formatted = formatted + "0";
        formatted = formatted + String.valueOf(Math.abs(senddate.getTimezoneOffset())/60);
        if (Math.abs(senddate.getTimezoneOffset())%60 < 10) formatted = formatted + "0";
        formatted = formatted + String.valueOf(Math.abs(senddate.getTimezoneOffset())%60);

        return formatted;
    }
}
