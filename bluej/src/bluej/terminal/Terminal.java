/**
 ** The Frame part of the Terminal window used for I/O when running programs
 ** under BlueJ.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: Terminal.java 523 2000-06-01 02:44:00Z mik $
 **/

package bluej.terminal;

import bluej.Config;
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
    private static final String WINDOWTITLE = Config.getString("terminal.title");
    private static final int FONTSIZE = 12;
    private static final Color activeBgColour = Color.white;
    private static final Color inactiveBgColour = new Color(224, 224, 224);
    private static final Color fgColour = Color.black;

    private static final char CHAR_CLEAR = 11;  // CTRL-K
    private static final char CHAR_COPY = 3;    // CTRL-C
    private static final char CHAR_SAVE = 19;   // CTRL-S
    private static final char CHAR_CLOSE = 23;  // CTRL-W

    // -- static singleton factory method --

    static Terminal frame = null;
    public synchronized static Terminal getTerminal()
    {
	if(frame == null)
	    frame = new Terminal();
	return frame;
    }

    // -- instance --

    private TermTextArea text;
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

	text = new TermTextArea(rows, columns);
	JScrollPane scrollPane = new JScrollPane(text);
	text.setFont(new Font("Monospaced", Font.PLAIN, FONTSIZE));
	text.setEditable(false);
	text.setLineWrap(false);
	text.setForeground(fgColour);
	text.setMargin(new Insets(6, 6, 6, 6));
	//text.setBackground(inactiveBgColour);

	getContentPane().add(scrollPane, BorderLayout.CENTER);

	text.addKeyListener(this);

	JMenuBar menubar = new JMenuBar();
	JMenu menu = new JMenu("Options");
        JMenuItem item;
            item = menu.add(new JMenuItem(new ClearAction()));
            item.setAccelerator(KeyStroke.getKeyStroke(
                                             KeyEvent.VK_K, Event.CTRL_MASK));
            item = menu.add(new JMenuItem(getCopyAction()));
            item.setText(Config.getString("terminal.copy"));
            item.setAccelerator(KeyStroke.getKeyStroke(
                                             KeyEvent.VK_C, Event.CTRL_MASK));
            item = menu.add(new JMenuItem(new SaveAction()));
            item.setAccelerator(KeyStroke.getKeyStroke(
                                             KeyEvent.VK_S, Event.CTRL_MASK));
            menu.add(new JSeparator());
            item = menu.add(new JMenuItem(new CloseAction()));
            item.setAccelerator(KeyStroke.getKeyStroke(
                                             KeyEvent.VK_W, Event.CTRL_MASK));

        menubar.add(menu);
	setJMenuBar(menubar);

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
	if(doShow)
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
     * Save the terminal text to file.
     */
    public void save()
    {
	//  NYI
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


    // ---- KeyListener interface ----

    public void keyPressed(KeyEvent event) { event.consume(); }
    public void keyReleased(KeyEvent event) { event.consume(); }

    public void keyTyped(KeyEvent event)
    {
        char ch = event.getKeyChar();

        // first, handle general terminal operations (menu shortcuts)
        if(Character.isISOControl(ch)) {

             switch(ch) {
                 case CHAR_CLEAR: clear();
                      break;
                 case CHAR_COPY: getCopyAction().actionPerformed(
                                     new ActionEvent(event.getSource(), 0, ""));
                      break;
                 case CHAR_SAVE: save();
                      break;
                 case CHAR_CLOSE: showTerminal(false);
                      break;
             }
        }

        // now, handle text input
	if(isActive) {

	    switch(ch) {
	    
	    case '\b':	// backspace
		if(buffer.backSpace()) {
		    try {
			int length = text.getDocument().getLength();
			text.replaceRange("", length-1, length);
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


    private class ClearAction extends AbstractAction
    {
        public ClearAction()
        {
            super(Config.getString("terminal.clear"));
        }

        public void actionPerformed(ActionEvent e) {
            clear();
        }
    }

    private class SaveAction extends AbstractAction
    {
        public SaveAction()
        {
            super(Config.getString("terminal.save"));
        }

        public void actionPerformed(ActionEvent e) {
            save();
        }
    }

    private class CloseAction extends AbstractAction
    {
        public CloseAction()
        {
            super(Config.getString("terminal.close"));
        }

        public void actionPerformed(ActionEvent e) {
            showTerminal(false);
        }
    }

    private Action getCopyAction()
    {
        Action[] textActions = text.getActions();
        for (int i=0; i < textActions.length; i++)
            if(textActions[i].getValue(Action.NAME).equals("copy-to-clipboard"))
                return textActions[i];

	return null;
    }
}
