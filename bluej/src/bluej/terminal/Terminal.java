/**
 ** The Frame part of the Terminal window used for I/O when running programs
 ** under BlueJ.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: Terminal.java 219 1999-08-10 04:22:08Z mik $
 **/

package bluej.terminal;

import bluej.utility.Debug;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Terminal extends JFrame
	implements KeyListener
{
    private static final String WINDOWTITLE = "BlueJ Terminal Window";
    private static final int FONTSIZE = 12;
    private static final Color activeBgColour = Color.white;
    private static final Color inactiveBgColour = new Color(224, 224, 224);
    private static final Color fgColour = Color.black;

    // -- static singleton factory method --

    static Terminal frame = null;
    public synchronized static Terminal getTerminal()
    {
	if(frame == null)
	    frame = new Terminal();
	return frame;
    }

    // -- instance --

    private JTextArea text;
    private boolean isActive = false;
    private InputBuffer buffer;

    /**
     * Create a new terminal window with default specifications.
     */
    private Terminal()
    {
	this(WINDOWTITLE, 80, 25);
    }


    /**
     * Create a new terminal window.
     */
    private Terminal(String title, int columns, int rows)
    {
	super(title);

	buffer = new InputBuffer(256);

	text = new JTextArea(rows, columns);
	JScrollPane scrollPane = new JScrollPane(text);
	text.setFont(new Font("Monospaced", Font.PLAIN, FONTSIZE));
	text.setEditable(false);
	text.setLineWrap(false);
	text.setForeground(fgColour);
	text.setMargin(new Insets(6, 6, 6, 6));
	//text.setBackground(inactiveBgColour);

	getContentPane().setLayout(new BorderLayout());
	getContentPane().add(scrollPane, BorderLayout.CENTER);

	createMenu();

	text.addKeyListener(this);

	// Close Action when close button is pressed
	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent event) {
		Window win = (Window)event.getSource();
		win.setVisible(false);
	    }
	});

	pack();
    }


    /**
     * Show or hide the terminal window.
     */
    public void showTerminal(boolean doShow)
    {
	setVisible(doShow);
	text.requestFocus();
    }


    /**
     * Return true if the window is currently displayed.
     */
    public boolean isShown()
    {
	return isShowing();
    }


    /**
     * Make the window active.
     */
    public void activate(boolean active)
    {
	if(active != isActive) {
	    text.setEditable(active);
	    //text.setEnabled(active);
	    //text.setBackground(active ? activeBgColour : inactiveBgColour);
	    isActive = active;
	}
    }


    /**
     * Clear the terminal.
     */
    public void clear()
    {
	text.setText("");
    }


    /**
     * Write some text to the terminal.
     */
    private void writeToTerminal(String s)
    {
	text.append(s);
	text.setCaretPosition(text.getDocument().getLength());
    }


    /**
     * Set the terminal size the the specified number of rows and columns.
     */
    private void setScreenSize(int columns, int rows)
    {
	text.setColumns(columns);
	text.setRows(rows);
	pack();
    }


    /**
     * Prepare the terminal for I/O.
     */
    private void prepare()
    {
	if(!isShown())
	    showTerminal(true);
    }

    /**
     * Create a new input stream which reads from the terminal.
     */
    InputStream in = new InputStream() {
	public int available()
	{
	    return buffer.numberOfCharacters();
	}

	public int read()
	{
	    return buffer.getChar();
	}

	public int read(byte b[], int off, int len) throws IOException
	{
  	    int bytesRead = 0;

  	    while(bytesRead < len) {
  		b[off + bytesRead] = (byte)buffer.getChar();
		bytesRead++;
  		if(buffer.numberOfCharacters() == 0)
		    break;
	    }

  	    return bytesRead;
	}
    };

    /**
     * Return the input stream that can be used to read from this terminal.
     */
    public InputStream getInputStream()
    {
	return in;
    }


    /**
     * Create a new output stream which writes to the terminal.
     */
    OutputStream out = new OutputStream() {
	public void write(int b) throws IOException
	{
	    prepare();
	    writeToTerminal("" + (char)b);
	}

	public void write(byte[] b, int off, int len) throws IOException
	{
	    prepare();
	    writeToTerminal(new String(b, off, len));
	}
    };

    /**
     * Return the output stream that can be used to write to this terminal
     */
    public OutputStream getOutputStream()
    {
	return out;
    }


    /**
     * Create a pop-up menu with terminal commands.
     */
    private void createMenu()
    {
    }


    // ---- KeyListener interface ----

    public void keyPressed(KeyEvent event) { event.consume(); }
    public void keyReleased(KeyEvent event) { event.consume(); }

    public void keyTyped(KeyEvent event)
    {
	if(isActive) {

	    char ch = event.getKeyChar();

	    switch(ch) {
	    
	    case '\b':	// backspace
		if(buffer.backSpace()) {
		    try {
			int length = text.getDocument().getLength();
			text.replaceRange("", length-2, length-1);
		    }
		    catch (Exception exc) { 
			Debug.reportError("bad location " + exc);
		    }
		}
		break;

	    case '\r':	// carriage return
	    case '\n':	// newline
		if(buffer.putChar('\n')) {
		    writeToTerminal("" + ch);
		    buffer.notifyReaders();
		}
		break;

	    default:
		if(Character.isISOControl(ch)) {
		    // control character - ignore
		    // later: bind to functions!
		}
		else {
		    if(buffer.putChar(ch))
			writeToTerminal("" + ch);
		    break;
		}
	    }
	}
	event.consume();	// make sure the text area doesn't handle this
    }

}
