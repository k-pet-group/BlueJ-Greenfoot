package org.bluej.extensions.submitter.transport;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.ProtocolException;
import java.net.Socket;

/**
 * Provide buffering, logging and time-out for sessions through sockets
 * @author Clive Miller
 * @version $Id: SocketSession.java 1586 2002-12-13 13:29:57Z damiano $
 */
class SocketSession
{
    private static final int TIMEOUT_DATA = 5000;
    private static final int TIMEOUT_OPEN = 60000;
    private static final int TIMEOUT_CONT = 1000;

    private Socket channel;
    private BufferedReader in;
    private OutputStream out;
    private PrintWriter log;
    private String lastCommand = "";
    
    public SocketSession (String host, int port) throws IOException
    {
        channel = new Socket (host, port);
        in = new BufferedReader (new InputStreamReader (channel.getInputStream()));
        out = channel.getOutputStream();
        log = new PrintWriter(new PipedOutputStream());
    }

    public void setLogfile (String filename) throws Exception
    {
        FileOutputStream fos = new FileOutputStream (filename);
        log = new PrintWriter (fos);
        log.println ("Socket open to "+getHost());
    }

    public void setLogger (PrintWriter logPrinter)
    {
        log = logPrinter;
        log.println ("Socket open to "+getHost());
    }
    
    public String getHost()
    {
        return channel.getInetAddress().getHostName()+":"+channel.getPort();
    }

    String getLocalHost()
    {
        return channel.getLocalAddress().getHostName();
    }
    
    /**
     * Waits for a response from the server beginning with one of the supplied strings.
     * If a line is received but not the correct one, it is ignored.
     * If a line is received which matches an error response, a ProtocolException is
     * thrown.
     * Finally, if no correct response is forthcoming, the last received line is
     * used as the message for a new ProtocolException.
     * @param response an array of healthy responses
     * @return the (last) line actually received.
     */
    public String expect (String[] response, String[] badResponse) throws IOException
    {
        String getLine, lastLine = null;
        channel.setSoTimeout (TIMEOUT_OPEN);
        getLine = in.readLine();
        log.println ("<<"+getLine);
        log.flush();
        try {
            while (getLine != null)
            {
                for (int i=0; i<response.length; i++)
                    if (getLine.startsWith (response[i]))
                        return getLine;
                for (int i=0; i<badResponse.length; i++)
                    if (getLine.startsWith (badResponse[i]))
                        throw new ProtocolException (getLine);
                lastLine = getLine;
                channel.setSoTimeout (TIMEOUT_CONT);
                getLine = in.readLine();
                log.println ("<<"+getLine);
                log.flush();
            }
            throw new IOException();
        } catch (ProtocolException ex) {
            throw ex;
        } catch (IOException ex) {
            ex.printStackTrace (log);
            if (lastLine == null || lastLine.trim().equals (""))
                lastLine = "No response from server";
            throw new ProtocolException (lastLine);
        }
    }

    public String expect (String[] responses) throws IOException
    {
        return expect (responses, new String[0]);
    }
    
    public String expect (String response, String badResponse) throws IOException
    {
        return expect (new String[] {response}, new String[] {badResponse});
    }
    
    public String expect (String response) throws IOException
    {
        return expect (new String[] {response}, new String[0]);
    }
    
    public void send (String data) throws IOException
    {
        out.write ((data+"\n").getBytes());
        log.println (">>"+data);
        log.flush();
        lastCommand = data;
    }

    public OutputStream getOutputStream()
    {
        return out;
    }

    public void close() throws IOException
    {
        out.flush();
        channel.shutdownOutput();
        channel.shutdownInput();
        channel.close();
        log.flush();
    }

    /**
     * Utility method to MIME encode a stream
     * @param inputStream the source of the data
     * @return outputStream the destination for the encoded output
     */
    public static void MIMEEncode (InputStream inputStream, OutputStream outputStream) throws IOException
    {
        // This is a bit nasty, but the problem is that bytes will not always be available,
        // but unless the stream has actually closed, we MUST wait for them.
        int in0, in1, in2;
        byte[] out = new byte[4];
        int line = 0;
        int size = 3;
        do {
            in0 = inputStream.read();
            in1 = inputStream.read();
            in2 = inputStream.read();
            if (in0 == -1) break;
            if (in2 == -1) { size = 2; in2 = 0; } // This should only happen the last time
            if (in1 == -1) { size = 1; in1 = 0; } // This should only happen the last time
            out[0] = encode ((byte)((in0 & 0xFC) >>> 2));
            out[1] = encode ((byte)((in0 & 0x03) << 4 | ((in1 & 0xF0) >>> 4)));
            out[2] = encode ((byte)((in1 & 0x0F) << 2 | ((in2 & 0xC0) >>> 6)));
            out[3] = encode ((byte)(in2 & 0x3F));
            if (size < 3) out[3]='=';
            if (size < 2) out[2]='=';
            if (line+size > 57) // 76 = 4/3 * 57
            {
                outputStream.write ('\r');
                outputStream.write ('\n');
                line = 0;
            }
            outputStream.write (out);
            line += size;
        } while (true);
        outputStream.write ('\r');
        outputStream.write ('\n');
    }

    public static byte encode (byte in)
    {
        byte out;
        if (in < 0) throw new Error ("Input < 0! in = "+in);
        if (in < 26) out = (byte) (in + 'A');
        else if (in < 52) out = (byte) (in - 26 + 'a');
        else if (in < 62) out = (byte) (in - 52 + '0');
        else if (in == 62) out = '+';
        else if (in == 63) out = '/';
        else throw new Error ("Input too large! in = "+in);
        return out;
    }

}
