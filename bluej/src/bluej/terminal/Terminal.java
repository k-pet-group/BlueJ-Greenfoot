package bluej.terminal;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import bluej.*;
import bluej.debugger.*;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.utility.*;

/**
 * The Frame part of the Terminal window used for I/O when running programs
 * under BlueJ.
 *
 * @author  Michael Kolling
 * @version $Id: Terminal.java 2798 2004-07-14 16:22:29Z mik $
 */
public final class Terminal extends JFrame
    implements KeyListener, BlueJEventListener, DebuggerTerminal
{
    private static final String WINDOWTITLE = Config.getString("terminal.title");
    private static final int windowHeight =
        Config.getPropInteger("bluej.terminal.height", 22);
    private static final int windowWidth =
        Config.getPropInteger("bluej.terminal.width", 80);

    private static final Color fgColour = Color.black;
    private static final Color errorColour = Color.red;
    private static final Image iconImage =
        Config.getImageAsIcon("image.icon.terminal").getImage();

    private static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        //Event.CTRL_MASK;

    // -- static singleton factory method --

//    static Terminal frame = null;
    static boolean enabled = true;


    // -- instance --

    private Project project;
    
    private TermTextArea text;
    private JTextArea errorText;
    private JScrollPane errorScrollPane;
    private JScrollPane scrollPane;
    private JSplitPane splitPane;
    private boolean isActive = false;
    private boolean recordMethodCalls = false;
    private boolean clearOnMethodCall = false;
    private boolean newMethodCall = false;
    private boolean errorShown = false;
    private InputBuffer buffer;

    private JCheckBoxMenuItem autoClear;
    private JCheckBoxMenuItem recordCalls;
    private JCheckBoxMenuItem unlimitedBuffering;

    private Reader in = new TerminalReader();
    private Writer out = new TerminalWriter(false);
    private Writer err = new TerminalWriter(true);


    /**
     * Create a new terminal window with default specifications.
     */
    public Terminal(Project project)
    {
        this(WINDOWTITLE + " - " + project.getProjectName(), windowWidth, windowHeight);

        this.project = project;
    }


    /**
     * Create a new terminal window.
     */
    private Terminal(String title, int columns, int rows)
    {
        super(title);

        buffer = new InputBuffer(256);
        makeWindow(columns, rows);
        BlueJEvent.addListener(this);
    }

    /**
     * Show or hide the ExecControl window.
     */
    public void showHide(boolean show)
    {
        setVisible(show);
        if(show) {
            text.requestFocus();
        }
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
        if(errorText!=null) {
            errorText.setText("");
        }
        hideErrorPane();
    }


    /**
     * Save the terminal text to file.
     */
    public void save()
    {
        String fileName = FileUtility.getFileName(this,
                                 Config.getString("terminal.save.title"),
                                 Config.getString("terminal.save.buttonText"),
                                 false, null, false);
        if(fileName != null) {
            try {
                FileWriter writer = new FileWriter(fileName);
                text.write(writer);
                writer.close();
            }
            catch (IOException ex) {
                DialogManager.showError(this, "error-save-file");
            }
        }
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
     * Write a character to the terminal.
     */
    private void writeToTerminal(char ch)
    {
        if(ch == '\f') {
            clear();
        }
        else {
            text.append(String.valueOf(ch));
            text.setCaretPosition(text.getDocument().getLength());
        }
    }


    /**
     * Write some text to error output.
     */
    private void writeToErrorOut(String s)
    {       
        showErrorPane();
        errorText.append(s);
        errorText.setCaretPosition(errorText.getDocument().getLength());
    }

    private StringBuffer erroutBuffer = new StringBuffer(120);
    
    /**
     * Write a character to error output.
     */
    private void writeToErrorOut(char ch)
    {
        erroutBuffer.append(ch);
        if(ch == '\n') {
            
            // TEMPORARY: filter out known annoying but harmless error messages
            // from MacOS Java v. 1.4.1
            if(erroutBuffer.indexOf("Java_apple_awt_") == -1) {
                showErrorPane();
                
                errorText.append(erroutBuffer.toString());
                errorText.setCaretPosition(errorText.getDocument().getLength());
            }
            erroutBuffer.setLength(0);
        }
    }


    /**
     * Prepare the terminal for I/O.
     */
    private void prepare()
    {
        if(newMethodCall) {   // prepare only once per method call
            showHide(true);
            newMethodCall = false;
        }
    }

    /**
     * An interactive method call has been made by a user.
     */
    private void methodCall(String callString)
    {
        newMethodCall = false;
        if(clearOnMethodCall) {
            clear();
        }
        if(recordMethodCalls) {
            try {
                if(text.getCaretPosition() !=
                   text.getLineStartOffset(text.getLineCount())) {
                    writeToTerminal('\n');
                }
            }
            catch(BadLocationException exc) {
                writeToTerminal('\n');
            }
            if(callString != null) {
                writeToTerminal("[ ");
                writeToTerminal(callString);
                writeToTerminal(" ]\n");
            }
        }
        newMethodCall = true;
    }


    /**
     * Return the input stream that can be used to read from this terminal.
     */
    public Reader getReader()
    {
        return in;
    }


    /**
     * Return the output stream that can be used to write to this terminal
     */
    public Writer getWriter()
    {
        return out;
    }


    /**
     * Return the output stream that can be used to write error output to this terminal
     */
    public Writer getErrorWriter()
    {
        return err;
    }


    // ---- KeyListener interface ----

    public void keyPressed(KeyEvent event) {
        if(event.getModifiers() != SHORTCUT_MASK)  // let menu commands pass
            event.consume();
    }

    public void keyReleased(KeyEvent event) {
        if(event.getModifiers() != SHORTCUT_MASK)
            event.consume();
    }

    public void keyTyped(KeyEvent event)
    {
        if(isActive) {
            char ch = event.getKeyChar();

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
                    writeToTerminal(ch);
                    buffer.notifyReaders();
                }
                break;

            default:
                if(buffer.putChar(ch))
                    writeToTerminal(ch);
                break;
            }
        }
        event.consume();	// make sure the text area doesn't handle this
    }


    // ---- BlueJEventListener interface ----

    /**
     * Called when a BlueJ event is raised. The event can be any BlueJEvent
     * type. The implementation of this method should check first whether
     * the event type is of interest an return immediately if it isn't.
     *
     * @param eventId  A constant identifying the event. One of the event id
     *                 constants defined in BlueJEvent.
     * @param arg      An event specific parameter. See BlueJEvent for
     *                 definition.
     */
    public void blueJEvent(int eventId, Object arg)
    {
        if(eventId == BlueJEvent.METHOD_CALL) {
            methodCall((String)arg);
        }
    }

    // ---- make window frame ----

    /**
     * Create the Swing window.
     */
    private void makeWindow(int columns, int rows)
    {
        setIconImage(iconImage);

        text = new TermTextArea(rows, columns);
        scrollPane = new JScrollPane(text);
        text.setFont(PrefMgr.getTerminalFont());
        text.setEditable(true);		// TODO: changed when removed active state tracking
        text.setLineWrap(false);
        text.setForeground(fgColour);
        text.setMargin(new Insets(6, 6, 6, 6));
        //text.setBackground(inactiveBgColour);
        text.addKeyListener(this);

        getContentPane().add(scrollPane, BorderLayout.CENTER);

        setJMenuBar(makeMenuBar());

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event)
            {
                Window win = (Window)event.getSource();
                
                // don't allow them to close the window if the debug machine
                // is running.. tries to stop them from closing down the
                // input window before finishing off input in the terminal
                if (project != null) {
                    if (project.getDebugger().getStatus() == Debugger.RUNNING)
                        return;
                }
                win.setVisible(false);
            }
        });

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.terminal", getLocation());
                }
            });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        setLocation(Config.getLocation("bluej.terminal"));

        pack();
    }

    /**
     * Create a second scrollled text area to the window, for error output.
     */
    private void createErrorPane()
    {
        errorText = new JTextArea(5, text.getColumns());
        errorScrollPane = new JScrollPane(errorText);
        errorText.setFont(PrefMgr.getTerminalFont());
        errorText.setEditable(false);
        errorText.setLineWrap(false);
        errorText.setForeground(errorColour);
        errorText.setMargin(new Insets(6, 6, 6, 6));

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                   scrollPane, errorScrollPane); 
        
        
        
        
    }
    
    /**
     * Show the errorPane for error ouput
     *
     */
    private void showErrorPane() {
        if(errorShown) {
            return;
        }
        
        //the first time the errortext is shown we need to pack() it
        //to make it have the right size.
        boolean doPack = false; 
        if(errorText == null) {
            doPack = true;
            createErrorPane();
        }
     
        getContentPane().remove(scrollPane);
        getContentPane().add(splitPane, BorderLayout.CENTER);       
        splitPane.resetToPreferredSizes();
            
        if(doPack) {
            pack();
        } else {
            validate();
        }
        
        errorShown = true;
    }
    
    /**
     * Hide the pane with the error output.
     *
     */
    private void hideErrorPane() {
        if(!errorShown) {
            return;
        }
        getContentPane().remove(splitPane);
        getContentPane().add(scrollPane, BorderLayout.CENTER);        
        errorShown = false; 
        validate();
    }
    
    /**
     * Create the terminal's menubar, all menus and items.
     */
    private JMenuBar makeMenuBar()
    {
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu(Config.getString("terminal.options"));
        JMenuItem item;
        item = menu.add(new ClearAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K,
                                                   SHORTCUT_MASK));
        item = menu.add(getCopyAction());
        item.setText(Config.getString("terminal.copy"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                                   SHORTCUT_MASK));
        item = menu.add(new SaveAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                                   SHORTCUT_MASK));
        menu.add(new JSeparator());

        autoClear = new JCheckBoxMenuItem(new AutoClearAction());
        menu.add(autoClear);

        recordCalls = new JCheckBoxMenuItem(new RecordCallAction());
        menu.add(recordCalls);

        unlimitedBuffering = new JCheckBoxMenuItem(new BufferAction());
        menu.add(unlimitedBuffering);

        menu.add(new JSeparator());
        item = menu.add(new CloseAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                                                   SHORTCUT_MASK));

        menubar.add(menu);
        return menubar;
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
            showHide(false);
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

    private class AutoClearAction extends AbstractAction
    {
        public AutoClearAction()
        {
            super(Config.getString("terminal.clearScreen"));
        }

        public void actionPerformed(ActionEvent e) {
            clearOnMethodCall = autoClear.isSelected();
        }
    }

    private class RecordCallAction extends AbstractAction
    {
        public RecordCallAction()
        {
            super(Config.getString("terminal.recordCalls"));
        }

        public void actionPerformed(ActionEvent e) {
            recordMethodCalls = recordCalls.isSelected();
        }
    }

    private class BufferAction extends AbstractAction
    {
        public BufferAction()
        {
            super(Config.getString("terminal.buffering"));
        }

        public void actionPerformed(ActionEvent e) {
            text.setUnlimitedBuffering(unlimitedBuffering.isSelected());
        }
    }
            
    /**
     * A Reader which reads from the terminal.
     */
    private class TerminalReader extends Reader
    {
        public int read(char[] cbuf, int off, int len)
        {
            int charsRead = 0;

            while(charsRead < len) {
                cbuf[off + charsRead] = buffer.getChar();
                charsRead++;
                if(buffer.numberOfCharacters() == 0)
                    break;
            }
            return charsRead;
        }

        public void close()
        {
        }
    }

    /**
     * A writer which writes to the terminal. It can be flagged for error output.
     * The idea is that error output could be presented differently from standard
     * output.
     */
    private class TerminalWriter extends Writer
    {
        private boolean isErrorOut;

        TerminalWriter(boolean isError)
        {
            super();
            isErrorOut = isError;
        }

        public void write(char[] cbuf, int off, int len)
        {
            if (enabled) {
                prepare();
                if(isErrorOut)
                    writeToErrorOut(new String(cbuf, off, len));
                else
                    writeToTerminal(new String(cbuf, off, len));
            }
        }

        public void write(int ch)
        {
            if (enabled) {
                prepare();
                if(isErrorOut)
                    writeToErrorOut((char)ch);
                else
                    writeToTerminal((char)ch);
            }
        }

        public void flush()
        {
        }

        public void close()
        {
        }
    }

}
