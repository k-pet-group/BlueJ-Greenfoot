package bluej.runtime;

import java.awt.*;
import java.awt.event.*;

import java.util.Vector;

import bluej.utility.Debug;
import bluej.utility.Utility;

/**
 ** @version $Id: TerminalCanvas.java 36 1999-04-27 04:04:54Z mik $
 ** @author Justin Tan
 ** @author Michael Cahill
 **
 ** The Canvas part of the Terminal window used for I/O when running programs
 ** under BlueJ.
 ** Note that while there is some thread-aware code here, this class is not 
 ** MT-safe - there are numerous race conditions.
 **/
public class TerminalCanvas extends Component implements ComponentListener, KeyListener
{
	private static final Font screenFont = new Font("Monospaced", Font.PLAIN, 12);	// The text font for Editor Screen
	private static final Color bgColour = Color.white;
	private static final Color fgColour = Color.black;
	private static final int TAB_SIZE = 8;
	private static final int MARGIN = 3;

	private char[][] screen = null;		// the characters on the screen
	private Dimension textsize = new Dimension();
	private Point pos = new Point();
	private boolean cursorOn = true;	// is the cursor displayed?
	
	private int fontwidth;
	private int fontascent;
	private int fontheight;
	
	private int unbuffered = 0;	// number of threads waiting on unbuffered input
	
	public TerminalCanvas(int width, int height)
	{
		FontMetrics fontmetrics = getFontMetrics(screenFont);
			
		fontwidth = fontmetrics.charWidth('a');
		fontascent = fontmetrics.getAscent();
		fontheight = fontmetrics.getHeight();
		
		Debug.assert((fontwidth != 0) && (fontascent != 0) && (fontheight != 0));

		addComponentListener(this);
		addKeyListener(this);

		setScreenSize(width, height);				
	}

	public void setScreenSize(int w, int h)
	{
		if(((textsize.width == w) && (textsize.height == h))
		  || (w <= 0) || (h <= 0))
			return;
			
		char[][] newBuffer = new char[h][w];
		
		if(screen != null)
		{
			int width = Math.min(textsize.width, w);
			int height = Math.min(textsize.height, h);
			
			for (int row = 0; row < height; row++)
				System.arraycopy(screen[row], 0, newBuffer[row], 0, width);
		}
		
		screen = newBuffer;
		
		textsize.width = w;
		textsize.height = h;
		pos.x = Math.min(pos.x, w - 1);
		pos.y = Math.min(pos.y, h - 1);
	}
	
	public Dimension getPreferredSize()
	{
		return getMinimumSize();
	}
	
	public Dimension getMinimumSize()
	{
		Dimension size = new Dimension(textsize.width * fontwidth + 2 * MARGIN, 
				textsize.height * fontheight + 2 * MARGIN);
		// Debug.message("TerminalCanvas: minsize = (" + size.width + ", " + size.width + ")");
		return size;
	}

	public void componentResized(ComponentEvent e)
	{
		Dimension size = getSize();
		int newW = size.width / fontwidth;
		int newH = size.height / fontheight;
			
		setScreenSize(newW, newH);
	}
	
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentHidden(ComponentEvent e) {}

	public void keyTyped(KeyEvent e)
	{
		char keyChar = e.getKeyChar();
		
		// System.err.println("Got key " + keyChar);
			
		// special case: handle newlines
		if(keyChar == '\r')
			keyChar = '\n';
				
		updateBuffer(keyChar);
	}
	
	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}

	public void paint(Graphics g)
	{
		Rectangle toPaint = g.getClipBounds();
		
		int top = (toPaint.y - MARGIN) / fontheight;
		int left = (toPaint.x - MARGIN) / fontwidth;
		
		g.setFont(screenFont);

		g.setColor(bgColour);
		g.fillRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
		g.setColor(fgColour);
			
		// the "- 2" below is so that the cursor is displayed correctly without
		// redrawing an extra character
		int width = (toPaint.width + fontwidth - 2) / fontwidth;
		int height = (toPaint.height + fontheight - 1) / fontheight;

		width = Math.min(width, textsize.width - left);
		height = Math.min(height, textsize.height - top);
		
		for(int row = top; row < top + height; row++)
		{
			g.setColor(fgColour);
			// replace '\0's with spaces
			char[] buf = new char[width];
			for(int i = 0; i < width; i++)
			{
				char c = screen[row][left + i];
				buf[i] = (c == '\0') ? ' ' : c;
			}
			
			g.drawChars(buf, 0, width, left * fontwidth + MARGIN, row * fontheight + fontascent + MARGIN);
		}

		if(cursorOn)
			showCursor(g);
	}

	public void showCursor(Graphics g)
	{
		g.setColor(Color.red);
		g.drawLine(pos.x * fontwidth + MARGIN, pos.y * fontheight + MARGIN, pos.x * fontwidth + MARGIN, (pos.y + 1) * fontheight + MARGIN - 1);
	}
	
	void redrawChar(int row, int col)
	{
		// the "+ 1" is so the cursor will be display correctly
		repaint(col * fontwidth + MARGIN, row * fontheight + MARGIN, fontwidth + 1, fontheight);
	}
	
	public void cursorTo(int x, int y)
	{
		x = Math.min(x, textsize.width - 1);
		y = Math.min(y, textsize.height - 1);
		
		redrawChar(pos.y, pos.x);
		pos.x = x;
		pos.y = y;
		redrawChar(pos.y, pos.x);
	}
	
	public void setCursor(boolean cursorOn)
	{
		this.cursorOn = cursorOn;
		redrawChar(pos.y, pos.x);
	}
	
	public void clear()
	{
		for(int i = 0; i < textsize.height; i++)
			clearRow(i);
		pos.x = pos.y = 0;
		repaint();
	}
	
	public void clearRow(int row)
	{
		for(int j = 0; j < textsize.width; j++)
			screen[row][j] = '\0';
	}
	
	/**
	 ** putchar - write a character at the current position, update screen and pos.
	 **/
	public void putchar(char c)
	{
		switch(c)
		{
		case '\t':	// tab
			for(int n = TAB_SIZE - (pos.x % TAB_SIZE); n > 0; n--)
				putchar(' ');
			break;
		
		case '\r':	// carriage return
			carriage_return();
			break;
			
		case '\n':	// newline
			line_feed();
			break;
			
		case '\b':	// backspace
			backspace();
			break;
			
		default:
			screen[pos.y][pos.x] = c;
			redrawChar(pos.y, pos.x);
			if(++pos.x == textsize.width)
				line_feed();
			break;
		}
	}
	
	void carriage_return()
	{
		if(pos.x > 0)
		{
			redrawChar(pos.y, pos.x);	// fix cursor
			pos.x = 0;
			redrawChar(pos.y, pos.x);	// fix cursor
		}
	}
	
	void line_feed()
	{
		redrawChar(pos.y, pos.x);	// fix cursor
		pos.x = 0;
		if(pos.y == textsize.height - 1)
			scroll();
		else
			pos.y++;
		redrawChar(pos.y, pos.x);	// fix cursor
	}
	
	void scroll()
	{
		char[] tmpline = screen[0];
		System.arraycopy(screen, 1, screen, 0, textsize.height - 1);
		screen[textsize.height - 1] = tmpline;
		clearRow(textsize.height - 1);
			
		repaint();
	}
	
	void backspace()
	{
		if((pos.x == 0) && (pos.y == 0))	// top left - can't backspace
			return;
			
		redrawChar(pos.y, pos.x);	// fix cursor
		if(pos.x > 0)
			screen[pos.y][--pos.x] = '\0';
		else
		{
			--pos.y;
			pos.x = textsize.width - 1;
			while((pos.x > 0) && (screen[pos.y][pos.x - 1] == '\0'))
				--pos.x;
		}
		redrawChar(pos.y, pos.x);	// fix cursor
	}

	
	class Buffer
	{
		char[] data = new char[16];
		int size = 0;
		int read = 0;
		
		void addChar(char c)
		{
			if(size == data.length)
			{
				char[] newData = new char[data.length * 2];
				System.arraycopy(data, 0, newData, 0, data.length);
				data = newData;
			}
			data[size++] = c;
		}
		
		void removeChar()
		{
			if(size > 0)
				size--;
		}
		
		int getChar()
		{
			return (read < size) ? (int)data[read++] : -1;
		}
		
		int available()
		{
			return size - read;
		}
		
		boolean isEmpty()
		{
			return (read >= size);
		}
	}
	
	Vector inputLines = new Vector();		// contains a Buffer per input line
	Buffer currentBuffer = new Buffer();
	
	int getChar()
	{
		while(bufferEmpty())
		{
			try {
				synchronized(this) {
					wait();		// sleep until there is some input
				}
			} catch(InterruptedException e) {
				// ignore it
			}
		}
			
		// return the next character
		return nextChar();
	}
	
	synchronized int nextChar()
	{
		Buffer buffer = (Buffer)inputLines.elementAt(0);
		int ret = buffer.getChar();
		if(buffer.isEmpty())
			inputLines.removeElementAt(0);
		return ret;
	}
	
	char getCharUnbuffered()
	{
		while(bufferEmpty() && currentBuffer.isEmpty())
		{
			try {
				synchronized(this) {
					++unbuffered;
					wait();		// sleep until there is some input
					--unbuffered;
				}
			} catch(InterruptedException e) {
				// ignore it
			}
		}
		
		if(!bufferEmpty())
			return (char)nextChar();
		else
			return (char)currentBuffer.getChar();
	}
	
	boolean askCharUnbuffered()
	{
		return !bufferEmpty() || !currentBuffer.isEmpty();
	}
	
	synchronized void updateBuffer(char c)
	{
		switch(c)
		{
		case '\r':	// carriage return
		case '\n':	// newline
			currentBuffer.addChar('\n');
			inputLines.addElement(currentBuffer);
			currentBuffer = new Buffer();
			putchar(c);
			synchronized(this) {
				notify();	// wake up a thread that is waiting on input
			}
			return;
			
		case '\b':	// backspace
			if(unbuffered == 0)
			{
				if(currentBuffer.isEmpty())
					return;
				currentBuffer.removeChar();
			}
			/* else fallthrough */
			
		default:
			currentBuffer.addChar(c);
			break;
		}
		
		putchar(c);
	}
	
	boolean bufferEmpty()
	{
		return inputLines.isEmpty();
	}
	
	/**
	 ** Number of characters available - i.e. in buffer
	 **/
	int available()
	{
		if(bufferEmpty())
			return 0;
		else
			return ((Buffer)inputLines.elementAt(0)).available();
	}
}
