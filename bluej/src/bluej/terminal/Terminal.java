/**
 ** The Frame part of the Terminal window used for I/O when running programs
 ** under BlueJ.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: Terminal.java 99 1999-05-31 06:29:40Z mik $
 **/

package bluej.terminal;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Terminal extends JFrame
{
    static final String defaultTitle = "BlueJ Terminal Window";

    // -- static singleton factory method --

    static Terminal frame = null;
    public synchronized static Terminal getTerminal()
    {
	if(frame == null)
	    frame = new Terminal();
	return frame;
    }

    // -- instance --

    TerminalCanvas term;
    private boolean isActive;

    private Terminal()
    {
	this(defaultTitle, 80, 25);
    }

    private Terminal(String title, int width, int height)
    {
	super(title);

	term = new TerminalCanvas(width, height);

	getContentPane().setLayout(new BorderLayout());
	getContentPane().add(term, "Center");
	setScreenSize(width, height);

	// Close Action when close button is pressed
	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent event)
		{
		    Window win = (Window)event.getSource();
		    win.setVisible(false);
		    //win.dispose();
		}
	});
    }

    protected void processWindowEvent(WindowEvent e)
    {
	super.processWindowEvent(e);
		
	if (e.getID() == WindowEvent.WINDOW_CLOSING)
	    show(false);
    }


    public void showTerminal(boolean doShow)
    {
	show(doShow);
    }

    public boolean isShown()
    {
	return isShowing();
    }

    public void clear()
    {
	term.clear();
    }

    /**
     * Make the window active.
     */
    public void activate(boolean active)
    {
	if(active != isActive) {
	    term.setEnabled(active);
	    term.activate(active);
	    isActive = active;
	}
    }

    protected void setScreenSize(int w, int h)
    {
	term.setScreenSize(w,h);
	pack();
    }

    InputStream in = new InputStream() {
	public int available()
	{
	    return term.available();
	}

	public int read()
	{
	    if(!isVisible())
	        setVisible(true);
	    if(!isActive) {
    		activate(true);
		term.requestFocus();
	    }
	    return term.getChar();
	}

	public int read(byte b[], int off, int len) throws IOException
	{
	    int nBytes = 0;

	    while(nBytes < len)
	    {
		b[off + (nBytes++)] = (byte)term.getChar();
		if(term.available() == 0)
		break;
	    }

	    return nBytes;
	}
    };

    public InputStream getInputStream()
    {
	return in;
    }

    OutputStream out = new OutputStream() {
	public void write(int b) throws IOException
	{
	    if(!isVisible())
	        setVisible(true);
	    if(!isActive) {
    		activate(true);
		term.requestFocus();
	    }
	    term.putchar((char)b);
	}
    };

    public OutputStream getOutputStream()
    {
	return out;
    }
}
