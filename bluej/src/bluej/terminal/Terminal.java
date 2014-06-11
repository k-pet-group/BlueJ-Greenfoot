/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2014  Michael Kolling and John Rosenberg 
 
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

import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerTerminal;
import bluej.debugmgr.ExecutionEvent;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * The Frame part of the Terminal window used for I/O when running programs
 * under BlueJ.
 *
 * @author  Michael Kolling
 * @author  Philip Stevens
 */
@SuppressWarnings("serial")
public final class Terminal extends JFrame
    implements KeyListener, BlueJEventListener, DebuggerTerminal
{
    private static final String WINDOWTITLE = Config.getApplicationName() + ": " + Config.getString("terminal.title");
    private static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    //private static final int ALT_SHORTCUT_MASK =
    //        SHORTCUT_MASK == Event.CTRL_MASK ? Event.CTRL_MASK : Event.META_MASK;

    private static final String TERMINALFONTPROPNAME = "bluej.terminal.font";
    private static final String TERMINALFONTSIZEPROPNAME = "bluej.terminal.fontsize";
    
    private static final String RECORDMETHODCALLSPROPNAME = "bluej.terminal.recordcalls";
    private static final String CLEARONMETHODCALLSPROPNAME = "bluej.terminal.clearscreen";
    private static final String UNLIMITEDBUFFERINGCALLPROPNAME = "bluej.terminal.buffering";
        
    // initialise to config value or zero.
    private static int terminalFontSize = Config.getPropInteger(
            TERMINALFONTSIZEPROPNAME, PrefMgr.getEditorFontSize());

    // -- instance --

    private Project project;
    
    private TermTextArea text;
    private TermTextArea errorText;
    private JScrollPane errorScrollPane;
    private JScrollPane scrollPane;
    private JSplitPane splitPane;
    private boolean isActive = false;
    private static boolean recordMethodCalls =
            Config.getPropBoolean(RECORDMETHODCALLSPROPNAME);
    private static boolean clearOnMethodCall =
            Config.getPropBoolean(CLEARONMETHODCALLSPROPNAME);
    private static boolean unlimitedBufferingCall =
            Config.getPropBoolean(UNLIMITEDBUFFERINGCALLPROPNAME);
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
     * Create a new terminal window with default specifications.
     */
    public Terminal(Project project)
    {
        super(WINDOWTITLE + " - " + project.getProjectName());
        this.project = project;
        BlueJEvent.addListener(this);
    }

    /**
     * Get the terminal font
     */
    private static Font getTerminalFont()
    {
        return Config.getFont(
                TERMINALFONTPROPNAME, "Monospaced", terminalFontSize);
    }

    /*
     * Set the terminal font size to equal either the passed parameter, or the
     * editor font size if the passed parameter is too low. Place the updated
     * value into the configuration.
     */
    private static void setTerminalFontSize(int size)
    {
        if (size <= 6) {
            terminalFontSize = PrefMgr.getEditorFontSize();
        } else {
            terminalFontSize = size;
        }
        Config.putPropInteger(TERMINALFONTSIZEPROPNAME, terminalFontSize);
    }

    
    /**
     * Initialise the terminal; create the UI.
     */
    private synchronized void initialise()
    {
        if(! initialised) {            
            buffer = new InputBuffer(256);
            int width = Config.isGreenfoot() ? 80 : Config.getPropInteger("bluej.terminal.width", 80);
            int height = Config.isGreenfoot() ? 10 : Config.getPropInteger("bluej.terminal.height", 22);
            makeWindow(width, height);
            initialised = true;
            text.setUnlimitedBuffering(unlimitedBufferingCall);
        }
    }

    /**
     * Show or hide the Terminal window.
     */
    public void showHide(boolean show)
    {
        DataCollector.showHideTerminal(project, show);
        
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
            if (!active) {
                text.getCaret().setVisible(false);
            }
            isActive = active;
        }
    }

    /**
     * Check whether the terminal is active (accepting input).
     */
    public boolean checkActive()
    {
        return isActive;
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
                null, false);
        if(fileName != null) {
            File f = new File(fileName);
            if (f.exists()){
                if (DialogManager.askQuestion(this, "error-file-exists") != 0)
                    return;
            }
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
    
    public void print()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        int printFontSize = Config.getPropInteger("bluej.fontsize.printText", 10);
        Font font = new Font("Monospaced", Font.PLAIN, printFontSize);
        if (job.printDialog()) {
            TerminalPrinter.printTerminal(job, text, job.defaultPage(), font);
        }
    }

    /**
     * Write some text to the terminal.
     */
    private void writeToPane(boolean stdout, String s)
    {
        prepare();
        if (!stdout)
            showErrorPane();
        
        // The form-feed character should clear the screen.
        int n = s.lastIndexOf('\f');
        if (n != -1) {
            clear();
            s = s.substring(n + 1);
        }
        
        TermTextArea tta = stdout ? text : errorText;
        
        tta.append(s);
        tta.setCaretPosition(tta.getDocument().getLength());       
    }
    
    public void writeToTerminal(String s)
    {
        writeToPane(true, s);
    }
    
    /**
     * Prepare the terminal for I/O.
     */
    private void prepare()
    {
        if (newMethodCall) {   // prepare only once per method call
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
    private void methodCall(String callString, boolean isVoid)
    {
        newMethodCall = false;
        if(clearOnMethodCall) {
            clear();
        }
        if(recordMethodCalls) {
            // If isVoid, it will have a ';' anyway.
            text.appendMethodCall(callString + "\n");
        }
        newMethodCall = true;
    }
    
    private void constructorCall(InvokerRecord ir)
    {
        newMethodCall = false;
        if(clearOnMethodCall) {
            clear();
        }
        if(recordMethodCalls) {
            String callString = ir.getResultTypeString() + " " + ir.getResultName() + " = " + ir.toExpression() + ";";
            text.appendMethodCall(callString + "\n");
        }
        newMethodCall = true;
    }
    
    private void methodResult(ExecutionEvent event)
    {
        if (recordMethodCalls) {
            String result = null;
            String resultType = event.getResult();
            
            if (resultType == ExecutionEvent.NORMAL_EXIT) {
                DebuggerObject object = event.getResultObject();
                if (object != null) {
                    if (event.getClassName() != null && event.getMethodName() == null) {
                        // Constructor call - the result object is the created object.
                        // Don't display the result separately:
                        return;
                    }
                    else {
                        // if the method returns a void, we must handle it differently
                        if (object.isNullObject()) {
                            return; // Don't show result of void calls
                        }
                        else {
                            // other - the result object is a wrapper with a single result field
                            DebuggerField resultField = object.getField(0);
                            result = "    returned " + resultField.getType().toString(true) + " ";
                            result += resultField.getValueString();
                        }
                    }
                }
            }
            else if (resultType == ExecutionEvent.EXCEPTION_EXIT) {
                result = "    Exception occurred.";
            }
            else if (resultType == ExecutionEvent.TERMINATED_EXIT) {
                result = "    VM terminated.";
            }
            
            if (result != null) {
                text.appendMethodCall(result + "\n");
            }
        }
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

    @Override
    public void keyPressed(KeyEvent event) { }
    
    @Override
    public void keyReleased(KeyEvent event) { }

    @Override
    public void keyTyped(KeyEvent event)
    {
        // We handle most things we are interested in here. The InputMap filters out
        // most other unwanted actions (but allows copy/paste).
        
        char ch = event.getKeyChar();
        switch (ch) {
        case KeyEvent.VK_UP:
        case KeyEvent.VK_DOWN:
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_RIGHT:
            if (PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT))
                return; // Let the arrow keys take effect
        
        
        case KeyEvent.VK_EQUALS: // increase the font size
        case KeyEvent.VK_PLUS: // increase the font size (non-uk keyboards)
            if (event.getModifiers() == SHORTCUT_MASK) {
                setTerminalFontSize(terminalFontSize + 1);
                project.getTerminal().resetFont();
                event.consume();
                break;
            }

        case KeyEvent.VK_MINUS: // decrease the font size
            if (event.getModifiers() == SHORTCUT_MASK) {
                setTerminalFontSize(terminalFontSize - 1);
                project.getTerminal().resetFont();
                event.consume();
                break;
            }

        // VK_(EQUALS|PLUS|MINUS) all fall through to here if no shortcut mask.
        default:
            if ((event.getModifiers() & Event.META_MASK) != 0) {
                return; // return without consuming the event
            }
            if (isActive) {
                switch (ch) {

                case 4:   // CTRL-D (unix/Mac EOF)
                case 26:  // CTRL-Z (DOS/Windows EOF)
                    buffer.signalEOF();
                    writeToTerminal("\n");
                    event.consume();
                    break;

                case '\b':  // backspace
                    if (buffer.backSpace()) {
                        try {
                            int length = text.getDocument().getLength();
                            text.replaceRange("", length - 1, length);
                        } catch (Exception exc) {
                            Debug.reportError("bad location " + exc);
                        }
                    }
                    event.consume();
                    break;

                case '\r':  // carriage return
                case '\n':  // newline
                    if (buffer.putChar('\n')) {
                        writeToTerminal(String.valueOf(ch));
                        buffer.notifyReaders();
                    }
                    event.consume();
                    break;

                default:
                    if (ch >= 32) {
                        if (buffer.putChar(ch)) {
                            writeToTerminal(String.valueOf(ch));
                        }
                        event.consume();
                    }
                    break;
                }
            }
            break;
        }
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
    @Override
    public void blueJEvent(int eventId, Object arg)
    {
        initialise();
        if(eventId == BlueJEvent.METHOD_CALL) {
            InvokerRecord ir = (InvokerRecord) arg;
            if (ir.getResultName() != null) {
                constructorCall(ir);
            }
            else {
                methodCall(ir.toExpression(), ir.hasVoidResult());
            }
        }
        else if (eventId == BlueJEvent.EXECUTION_RESULT) {
            methodResult((ExecutionEvent) arg);
        }
    }

    // ---- make window frame ----

    /**
     * Create the Swing window.
     */
    private void makeWindow(int columns, int rows)
    {
        Image icon = BlueJTheme.getIconImage();
        if (icon != null) {
            setIconImage(icon);
        }
        text = new TermTextArea(rows, columns, buffer, project, this, false);
        final InputMap origInputMap = text.getInputMap();
        text.setInputMap(JComponent.WHEN_FOCUSED, new InputMap() {
            {
                setParent(origInputMap);
            }
            
            @Override
            public Object get(KeyStroke keyStroke)
            {
                Object actionName = super.get(keyStroke);
                
                if (actionName == null) {
                    return null;
                }
                
                char keyChar = keyStroke.getKeyChar();
                if (keyChar == KeyEvent.CHAR_UNDEFINED || keyChar < 32) {
                    // We might want to filter the action
                    if ("copy-to-clipboard".equals(actionName)) {
                        return actionName;
                    }
                    if ("paste-from-clipboard".equals(actionName)) {
                        // Handled via paste() in TermTextArea
                        return actionName;
                    }
                    return PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT) ? actionName : null;
                }
                
                return actionName;
            }
        });
        
        scrollPane = new JScrollPane(text);
        text.setFont(getTerminalFont());
        text.setEditable(false);
        text.setMargin(new Insets(6, 6, 6, 6));
        text.addKeyListener(this);

        getContentPane().add(scrollPane, BorderLayout.CENTER);

        setJMenuBar(makeMenuBar());

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
            @Override
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
                DataCollector.showHideTerminal(project, false);
                win.setVisible(false);
            }
        });

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
            @Override
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
        errorText = new TermTextArea(Config.isGreenfoot() ? 20 : 5, 80, null, project, this, true);
        errorScrollPane = new JScrollPane(errorText);
        errorText.setFont(getTerminalFont());
        errorText.setEditable(false);
        errorText.setMargin(new Insets(6, 6, 6, 6));
        errorText.setUnlimitedBuffering(true);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                   scrollPane, errorScrollPane); 
    }
    
    /**
     * Show the errorPane for error output
     */
    private void showErrorPane()
    {
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
        }
        else {
            validate();
        }
        
        errorShown = true;
    }
    
    /**
     * Hide the pane with the error output.
     */
    private void hideErrorPane()
    {
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
        menu.add(new PrintAction());
        menu.add(new JSeparator());

        autoClear = new JCheckBoxMenuItem(new AutoClearAction());
        autoClear.setSelected(clearOnMethodCall);
        menu.add(autoClear);

        recordCalls = new JCheckBoxMenuItem(new RecordCallAction());
        recordCalls.setSelected(recordMethodCalls);
        menu.add(recordCalls);

        unlimitedBuffering = new JCheckBoxMenuItem(new BufferAction());
        unlimitedBuffering.setSelected(unlimitedBufferingCall);
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

        public void actionPerformed(ActionEvent e)
        {
            clear();
        }
    }

    private class SaveAction extends AbstractAction
    {
        public SaveAction()
        {
            super(Config.getString("terminal.save"));
        }

        public void actionPerformed(ActionEvent e)
        {
            save();
        }
    }
    
    private class PrintAction extends AbstractAction
    {
        public PrintAction()
        {
            super(Config.getString("terminal.print"));
        }

        public void actionPerformed(ActionEvent e)
        {
            print();
        }
    }

    private class CloseAction extends AbstractAction
    {
        public CloseAction()
        {
            super(Config.getString("terminal.close"));
        }

        public void actionPerformed(ActionEvent e)
        {
            showHide(false);
        }
    }

    private Action getCopyAction()
    {
        Action[] textActions = text.getActions();
        for (int i=0; i < textActions.length; i++) {
            if(textActions[i].getValue(Action.NAME).equals("copy-to-clipboard")) {
                return textActions[i];
            }
        }

        return null;
    }

    private class AutoClearAction extends AbstractAction
    {
        public AutoClearAction()
        {
            super(Config.getString("terminal.clearScreen"));
        }

        public void actionPerformed(ActionEvent e)
        {
            clearOnMethodCall = autoClear.isSelected();
            Config.putPropBoolean(CLEARONMETHODCALLSPROPNAME, clearOnMethodCall);
        }
    }

    private class RecordCallAction extends AbstractAction
    {
        public RecordCallAction()
        {
            super(Config.getString("terminal.recordCalls"));
        }

        public void actionPerformed(ActionEvent e)
        {
            recordMethodCalls = recordCalls.isSelected();
            Config.putPropBoolean(RECORDMETHODCALLSPROPNAME, recordMethodCalls);
        }
    }

    private class BufferAction extends AbstractAction
    {
        public BufferAction()
        {
            super(Config.getString("terminal.buffering"));
        }

        public void actionPerformed(ActionEvent e)
        {
            unlimitedBufferingCall = unlimitedBuffering.isSelected();
            text.setUnlimitedBuffering(unlimitedBufferingCall);
            Config.putPropBoolean(UNLIMITEDBUFFERINGCALLPROPNAME, unlimitedBufferingCall);
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

        @Override
        public boolean ready()
        {
            return ! buffer.isEmpty();
        }
        
        public void close() { }
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
            try {
                // We use invokeAndWait so that terminal output is limited to
                // the processing speed of the event queue. This means the UI
                // will still respond to user input even if the output is really
                // gushing.
                EventQueue.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        initialise();
                        writeToPane(!isErrorOut, new String(cbuf, off, len));
                    }
                });
            }
            catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
            catch (InterruptedException ie) {}
        }

        public void flush() { }

        public void close() { }
    }
}
