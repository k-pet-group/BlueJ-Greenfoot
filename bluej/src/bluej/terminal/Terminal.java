package bluej.terminal;

import java.awt.Dimension;
import java.io.*;

import bluej.utility.Debug;

/**
 ** A simple I/O interface for BlueJ programs. 
 ** 
 ** These functions should not be called from the BlueJ environment, but
 ** are provided to be called by user programs. The terminal object being
 ** called here runs on the remote machine!
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: Terminal.java 95 1999-05-28 06:08:18Z mik $
 **/
public class Terminal
{
    private static TerminalCanvas term = BlueJRuntime.terminal.term;
    static BufferedReader in = new BufferedReader(new InputStreamReader(BlueJRuntime.terminal.getInputStream()));
    private static PrintWriter out = new PrintWriter(BlueJRuntime.terminal.getOutputStream());
    // private static PrintWriter out = new PrintWriter(System.out);
	
    public static void setVisible(boolean visible)
    {
	BlueJRuntime.terminal.setVisible(visible);
    }
	
    public static void print(String s)
    {
	out.print(s);
    }
	
    public static void println(String s)
    {
	out.println(s);
    }
	
    public static void flush()
    {
	out.flush();
    }
	
    public static String readLine()
    {
	try {
	    return in.readLine();
	} catch(IOException e) {
	    return null;
	}
    }
	
    /**
     ** Gets a single character from the terminal (unbuffered).
     ** Blocks until a key is pressed.
     **/
    public static char getChar()
    {
	return term.getCharUnbuffered();
    }
	
    /**
     ** Returns a boolean indicating whether there is a character available
     ** for getChar() to read. A return value of false indicates that getChar
     ** will block.
     **/
    public static boolean askChar()
    {
	return term.askCharUnbuffered();
    }
	
    public static int getWidth()
    {
	Dimension size = term.getSize();
	return size.width;
    }
	
    public static int getHeight()
    {
	Dimension size = term.getSize();
	return size.height;
    }
	
    public static void clear()
    {
	term.clear();
    }
	
    public static void cursorTo(int x, int y)
    {
	out.flush();
	term.cursorTo(x, y);
    }
	
    public static void showCursor(boolean cursorOn)
    {
	term.setCursor(cursorOn);
    }
}
