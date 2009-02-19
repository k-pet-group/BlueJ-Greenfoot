/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.terminal;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerTerminal;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.Utility;

/**
 * The Frame part of the Terminal window used for I/O when running programs
 * under BlueJ.
 *
 * @author  Michael Kolling
 * @version $Id: Terminal.java 6164 2009-02-19 18:11:32Z polle $
 */
@SuppressWarnings("serial")
public final class Terminal extends JFrame
    implements KeyListener, BlueJEventListener, DebuggerTerminal
{
    private static final String WINDOWTITLE = Config.getApplicationName() + ": " + Config.getString("terminal.title");
    private static final int windowHeight =
        Config.getPropInteger("bluej.terminal.height", 22);
    private static final int windowWidth =
        Config.getPropInteger("bluej.terminal.width", 80);

    private static final Color fgColour = Color.black;
    private static final Color errorColour = Color.red;

    private static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    static boolean enabled = true;

    private static final String terminalFontPropertyName = "bluej.terminal.font";
    private static final String terminalFontSizePropertyName = "bluej.terminal.fontsize";

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
    private boolean newMethodCall = true;
    private boolean errorShown = false;
    private InputBuffer buffer;

    private JCheckBoxMenuItem autoClear;
    private JCheckBoxMenuItem recordCalls;
    private JCheckBoxMenuItem unlimitedBuffering;

    private Reader in = new TerminalReader();
    private Writer out = new TerminalWriter(false);
    private Writer err = new TerminalWriter(true);

    /** Used for lazy initialisation  */
    private boolean initialised = false; 
    
    /**
     * Get the terminal font
     */
    private static Font getTerminalFont()
    {
        int fontSize = Config.getPropInteger(terminalFontSizePropertyName, 0);
        if (fontSize == 0) {
            fontSize = PrefMgr.getEditorFontSize();
        }
         
        return Config.getFont(terminalFontPropertyName, "Monospaced", fontSize); 
    }
    
    /**
     * Create a new terminal window with default specifications.
     */
    public Terminal(Project project)
    {
        super(WINDOWTITLE + " - " + project.getProjectName());
        this.project = project;
        BlueJEvent.addListener(this);
    }

    
    /**
     * Initialise the terminal; create the UI.
     */
    private synchronized void initialise()
    {
        if(! initialised) {            
            buffer = new InputBuffer(256);
            makeWindow(windowWidth, windowHeight);
            initialised = true;
        }
    }

    /**
     * Show or hide the Terminal window.
     */
    public void showHide(boolean show)
    {
        initialise();
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
        initialise();
        return isShowing();
    }

    /**
     * Make the window active.
     */
    public void activate(boolean active)
    {
        if(active != isActive) {
            initialise();
            text.setEditable(active);
            //text.setEnabled(active);
            //text.setBackground(active ? activeBgColour : inactiveBgColour);
            isActive = active;
        }
    }

    /**
     * Reset the font according to preferences.
     */
    public void resetFont()
    {
        initialise();
        Font terminalFont = getTerminalFont();
        text.setFont(terminalFont);
        if (errorText != null) {
            errorText.setFont(terminalFont);
        }
    }

    /**
     * Clear the terminal.
     */
    public void clear()
    {
        initialise();
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
        initialise();
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
        prepare();
        
        // The form-feed character should clear the screen.
        int n = s.lastIndexOf('\f');
        if (n != -1) {
            clear();
            s = s.substring(n + 1);
        }
        
        text.append(s);
        text.setCaretPosition(text.getDocument().getLength());
    }

    /**
     * Write some text to error output.
     */
    private void writeToErrorOut(String s)
    {
        erroutBuffer.append(s);
        int endOfLine = erroutBuffer.indexOf("\n");
        while (endOfLine != -1) {
            String line = erroutBuffer.substring(0, endOfLine + 1);

            // TEMPORARY: filter out known annoying but harmless error messages
            // from MacOS Java v. 1.4.1
            if((line.indexOf("CFMessagePort") == -1) &&
                    (line.indexOf("bootstrap_defs.h") == -1)) {
                prepare();
                showErrorPane();
                
                errorText.append(line);
                errorText.setCaretPosition(errorText.getDocument().getLength());
            }
            StringBuffer newBuffer = new StringBuffer();
            //We need to cast the CharSequence to an Object to make it work on Java 1.4 which does not have an append method for CharSequence.
            newBuffer.append((Object) erroutBuffer.subSequence(endOfLine + 1, erroutBuffer.length()));
            erroutBuffer = newBuffer;
            endOfLine = erroutBuffer.indexOf("\n");
        }
    }

    private StringBuffer erroutBuffer = new StringBuffer(120);

    
    /**
     * Prepare the terminal for I/O.
     */
    private void prepare()
    {
        if(newMethodCall) {   // prepare only once per method call
            showHide(true);
            newMethodCall = false;
        }
        else if (Config.isGreenfoot()) {
            // In greenfoot new output should always show the terminal
            if (! isVisible()) {
                showHide(true);
            }
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
                    writeToTerminal("\n");
                }
            }
            catch(BadLocationException exc) {
                writeToTerminal("\n");
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
        // Let menu commands and dead keys (if active) pass
        // Dead keys are passed because they wont work on Windows otherwise
        if(event.getModifiers() != SHORTCUT_MASK && !(Utility.isDeadKey(event) && isActive) )  
            event.consume();
    }
    
    public void keyReleased(KeyEvent event) {
        // Let menu commands and dead keys (if active) pass
        // Dead keys are passed because they wont work on Windows otherwise
        if(event.getModifiers() != SHORTCUT_MASK && !(Utility.isDeadKey(event) && isActive) )  
            event.consume();
    }

    public void keyTyped(KeyEvent event)
    {
        initialise();
        if(isActive) {
            char ch = event.getKeyChar();

            switch(ch) {

            case 4:   // CTRL-D (unix/Mac EOF)
            case 26:  // CTRL-Z (DOS/Windows EOF)
                buffer.signalEOF();
                writeToTerminal("\n");
                break;
                
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
                    writeToTerminal(String.valueOf(ch));
                    buffer.notifyReaders();
                }
                break;

            default:
                if(buffer.putChar(ch))
                    writeToTerminal(String.valueOf(ch));
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
        initialise();
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
        setIconImage(Config.getImage("image.icon.terminal"));        
        text = new TermTextArea(rows, columns);
        scrollPane = new JScrollPane(text);
        text.setFont(getTerminalFont());
        text.setEditable(true);
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
     * Create a second scrolled text area to the window, for error output.
     */
    private void createErrorPane()
    {
        errorText = new JTextArea(5, text.getColumns());
        errorScrollPane = new JScrollPane(errorText);
        errorText.setFont(getTerminalFont());
        errorText.setEditable(false);
        errorText.setLineWrap(false);
        errorText.setForeground(errorColour);
        errorText.setMargin(new Insets(6, 6, 6, 6));

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                   scrollPane, errorScrollPane); 
    }
    
    /**
     * Show the errorPane for error output
     */
    private void showErrorPane() {
        if(errorShown) {
            return;
        }
        
        //the first time the errortext is shown we need to pack() it
        //to make it have the right size.
        boolean isFirstShow = false; 
        if(errorText == null) {
            isFirstShow = true;
            createErrorPane();
        }
     
        getContentPane().remove(scrollPane);
  
        // We want to know if it is not the first time
        // This means a "clear" has been used to remove the splitpane
        // when this re-adds the scrollPane to the terminal area
        // it implicitly removes it from the splitpane as it can only have one
        // owner. The side-effect of this is the splitpane's
        // top component becomes null.
        if(!isFirstShow)
            splitPane.setTopComponent(scrollPane);
        getContentPane().add(splitPane, BorderLayout.CENTER);       
        splitPane.resetToPreferredSizes();
            
        if(isFirstShow) {
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
            initialise();
            int charsRead = 0;

            while(charsRead < len) {
                cbuf[off + charsRead] = buffer.getChar();
                charsRead++;
                if(buffer.isEmpty())
                    break;
            }
            return charsRead;
        }

        public boolean ready()
        {
            return ! buffer.isEmpty();
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

        public void write(final char[] cbuf, final int off, final int len)
        {
            if (enabled) {
                try {
                    // We use invokeAndWait so that terminal output is limited to
                    // the processing speed of the event queue. This means the UI
                    // will still respond to user input even if the output is really
                    // gushing.
                    EventQueue.invokeAndWait(new Runnable() {
                        public void run()
                        {
                            initialise();
                            if(isErrorOut) {
                                writeToErrorOut(new String(cbuf, off, len));
                            }
                            else {
                                writeToTerminal(new String(cbuf, off, len));
                            }
                        }
                    });
                }
                catch (InvocationTargetException ite) {
                    ite.printStackTrace();
                }
                catch (InterruptedException ie) {}
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
