/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2014,2015,2016,2017,2018,2019,2021,2022  Michael Kolling and John Rosenberg
 
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
import bluej.editor.base.LineDisplay;
import bluej.editor.base.TextLine;
import bluej.editor.base.TextLine.StyledSegment;
import bluej.editor.flow.FlowEditor;
import bluej.editor.flow.FlowEditor.OffScreenFlowEditorPaneListener;
import bluej.editor.base.LineContainer;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.print.PrintProgressDialog;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.JavaNames;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Frame part of the Terminal window used for I/O when running programs
 * under BlueJ.
 *
 * @author  Michael Kolling
 * @author  Philip Stevens
 */
@SuppressWarnings("serial")
public final class Terminal
    implements BlueJEventListener, DebuggerTerminal
{
    private static final int MAX_BUFFER_LINES = 200;

    // The style for text in the stdout pane: was it output by the program, or input by the user?
    // Or third option: details about method recording
    private static final List<String> STDOUT_OUTPUT = Collections.singletonList("terminal-output");
    private static final List<String> STDOUT_INPUT = Collections.singletonList("terminal-input");
    private static final List<String> STDOUT_METHOD_RECORDING = Collections.singletonList("terminal-method-record");
    private static final List<String> STDERR_NORMAL = Collections.singletonList("terminal-error");
    private static final List<String> STDERR_LINKED_STACK_TRACE = Collections.singletonList("terminal-stack-link");
    private static final List<String> STDERR_FOREIGN_STACK_TRACE = Collections.singletonList("terminal-stack-foreign");

    private static final String WINDOWTITLE = Config.getApplicationName() + ": " + Config.getString("terminal.title");

    private static final String RECORDMETHODCALLSPROPNAME = "bluej.terminal.recordcalls";
    private static final String CLEARONMETHODCALLSPROPNAME = "bluej.terminal.clearscreen";
    private static final String UNLIMITEDBUFFERINGCALLPROPNAME = "bluej.terminal.buffering";

    private final String title;

    // -- instance --

    private final Project project;
    
    private final TerminalTextPane text;
    private TerminalTextPane errorText;
    private final TextField input;
    private final SplitPane splitPane;
    private boolean isActive = false;
    private static BooleanProperty recordMethodCalls =
            Config.getPropBooleanProperty(RECORDMETHODCALLSPROPNAME);
    private static BooleanProperty clearOnMethodCall =
            Config.getPropBooleanProperty(CLEARONMETHODCALLSPROPNAME);
    private static BooleanProperty unlimitedBufferingCall =
            Config.getPropBooleanProperty(UNLIMITEDBUFFERINGCALLPROPNAME);
    private boolean newMethodCall = true;
    private boolean errorShown = false;
    private final InputBuffer buffer;
    private final BooleanProperty showingProperty = new SimpleBooleanProperty(false);

    @OnThread(Tag.Any) private final Reader in = new TerminalReader();
    @OnThread(Tag.Any) private final Writer out = new TerminalWriter(false);
    @OnThread(Tag.Any) private final Writer err = new TerminalWriter(true);

    private Stage window;

    /**
     * Create a new terminal window with default specifications.
     */
    public Terminal(Project project)
    {
        this.title = WINDOWTITLE + " - " + project.getProjectName();
        this.project = project;

        buffer = new InputBuffer(65536);
        text = new TerminalTextPane() {
            @Override
            public void focusPrevious()
            {
                if (errorText != null)
                    errorText.requestFocusAndShowCaret();
                else
                    input.requestFocus(); // If it's disabled and errorText == null, nothing more we can do anyway.
            }

            @Override
            public void focusNext()
            {
                if (!input.isDisable())
                    input.requestFocus();
                else if (errorText != null)
                    errorText.requestFocusAndShowCaret();
            }
        };
        text.getStyleClass().add("terminal-output");
        text.addSelectionListener((caret, anchor) -> {
            if (errorText != null && errorText.getCaretEditorPosition().getPosition() != errorText.getAnchorEditorPosition().getPosition())
            {
                errorText.deselect();
            }
        });
        JavaFXUtil.addChangeListenerPlatform(unlimitedBufferingCall, unlimited -> {
            // Toggle unlimited buffering; need to chop if necessary
            if (!unlimited)
                text.trimToMostRecentNLines(MAX_BUFFER_LINES);
        });

        input = new TextField();
        input.getStyleClass().add("terminal-input-field");
        input.setOnAction(e -> {
            sendInput(false);
            e.consume();
        });
        input.styleProperty().bind(PrefMgr.getEditorFontCSS(true));
        input.setEditable(false);
        // Mainly for visuals, we disable when not in use:
        JavaFXUtil.addChangeListenerAndCallNow(input.editableProperty(), newVal ->
        {
            input.setDisable(!newVal);
            input.setPromptText((newVal) ? Config.getString("terminal.running") : Config.getString("terminal.notRunning"));
        });

        Nodes.addInputMap(input, InputMap.sequence(
                // CTRL-D (unix/Mac EOF)
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN)), e -> {sendInput(true); e.consume();}),
                // CTRL-Z (DOS/Windows EOF)
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN)), e -> {sendInput(true); e.consume();}),
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN)), e -> Utility.increaseFontSize(PrefMgr.getEditorFontSize())),
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN)), e -> Utility.decreaseFontSize(PrefMgr.getEditorFontSize())),
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN)), e -> PrefMgr.getEditorFontSize().set(PrefMgr.DEFAULT_JAVA_FONT_SIZE)),
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHIFT_DOWN)), e -> {text.requestFocusAndShowCaret();}),
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.TAB)), e -> {(errorText != null ? errorText : text).requestFocusAndShowCaret();})
        ));

        splitPane = new SplitPane(new BorderPane(text, null, null, input, null));
        JavaFXUtil.addStyleClass(splitPane, "terminal-split");

        BorderPane mainPanel = new BorderPane();
        mainPanel.setCenter(splitPane);

        mainPanel.setTop(makeMenuBar());
        window = new Stage();
        window.setWidth(500);
        window.setHeight(500);
        BlueJTheme.setWindowIconFX(window);
        window.setTitle(title);
        Scene scene = new Scene(mainPanel);
        Config.addTerminalStylesheets(scene);
        window.setScene(scene);
        JavaFXUtil.addMacMinimiseShortcutHandler(window);
        // There is a slight awkward state where if the input field becomes disabled,
        // it is no longer the focus owner (which becomes null), so tab no longer works
        // (because the standard JavaFX tabbing mechanism doesn't work in this window).
        // We don't want to move focus away to the output pane immediately because the input
        // may become re-enabled.
        // But we need a special case where if they do press tab/shift-tab, we focus the next field: 
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (scene.getFocusOwner() == null && e.getCode() == KeyCode.TAB)
            {
                e.consume();
                if (e.isShiftDown() && errorText != null)
                    errorText.requestFocusAndShowCaret();
                else
                    text.requestFocusAndShowCaret();
            }
        });

        // Close Action when close button is pressed
        window.setOnCloseRequest(e -> {
            // We consume the event whatever happens, then decide if we can close:
            e.consume();

            // don't allow them to close the window if the debug machine
            // is running.. tries to stop them from closing down the
            // input window before finishing off input in the terminal
            if (project != null) {
                if (project.getDebugger().getStatus() == Debugger.RUNNING)
                    return;
            }
            showHide(false);
        });
        window.setOnShown(e -> {
            showingProperty.set(true);
            //org.scenicview.ScenicView.show(window.getScene());
        });
        window.setOnHidden(e -> showingProperty.set(false));

        JavaFXUtil.addChangeListenerPlatform(showingProperty, this::showHide);
                
        Config.loadAndTrackPositionAndSize(window, "bluej.terminal");
        BlueJEvent.addListener(this);
    }

    /**
     * Copy whichever of the stdout/stderr panes actually has a selection.
     */
    private void doCopy()
    {
        if (errorText != null && errorText.getCaretEditorPosition().getPosition() != errorText.getAnchorEditorPosition().getPosition()) 
        {
            errorText.copy();
        }
        else if (text.getCaretEditorPosition().getPosition() != text.getAnchorEditorPosition().getPosition()) 
        {
            text.copy();
        }
    }

    private void sendInput(boolean eof)
    {
        String inputString = this.input.getText() + (eof ? "" : "\n");
        buffer.putString(inputString);
        if (eof)
        {
            buffer.signalEOF();
        }
        else
        {
            buffer.notifyReaders();
        }
        this.input.clear();
        writeToPane(text, inputString, STDOUT_INPUT);
    }

    /**
     * Show or hide the Terminal window.
     */
    public void showHide(boolean show)
    {
        DataCollector.showHideTerminal(project, show);

        if (show)
        {
            window.show();
            input.requestFocus();
        }
        else
        {
            window.hide();
        }
    }
    
    public void dispose()
    {
        showHide(false);
        window = null;
    }

    /**
     * Return true if the window is currently displayed.
     */
    public boolean isShown()
    {
        return window.isShowing();
    }

    /**
     * Make the input field active, or not
     */
    public void activate(boolean active)
    {
        if(active != isActive) {
            input.setEditable(active);
            isActive = active;
        }
    }

    /**
     * Clear the terminal.
     */
    public void clear()
    {
        text.clear();
        if (errorText!=null) {
            errorText.clear();
        }
        hideErrorPane();
    }

    /**
     * Save the terminal text to file.
     */
    public void save()
    {
        File fileName = FileUtility.getSaveFileFX(window,
                Config.getString("terminal.save.title"),
                null, false);
        if(fileName != null) {
            if (fileName.exists()){
                if (DialogManager.askQuestionFX(window, "error-file-exists") != 0)
                    return;
            }

            try
            {
                FileWriter writer = new FileWriter(fileName);
                String output = String.join(System.lineSeparator(), text.getLines());
                writer.write(output);
                writer.close();
            }
            catch (IOException ex)
            {
                DialogManager.showErrorFX(window, "error-save-file");
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    public void print()
    {
        PrinterJob job = JavaFXUtil.createPrinterJob();
        if (job == null)
        {
            DialogManager.showErrorFX(window,"print-no-printers");
        }
        else if (job.showPrintDialog(window))
        {
            List<List<TextLine.StyledSegment>> lines = text.getStyledLines();
                        
            BorderPane root = new BorderPane();
            Scene scene = new Scene(root);
            Config.addTerminalStylesheets(scene);
            // JavaFX seems to always print at 72 DPI, regardless of printer DPI:
            // This means that that the point width (1/72 of an inch) is actually the pixel width, too:
            double pixelWidth = job.getJobSettings().getPageLayout().getPrintableWidth();
            double pixelHeight = job.getJobSettings().getPageLayout().getPrintableHeight();
            root.resize(pixelWidth, pixelHeight);
            
            LineDisplay lineDisplay = new LineDisplay(new ReadOnlyDoubleWrapper(0), new ReadOnlyStringWrapper(""), false, new OffScreenFlowEditorPaneListener());
            LineContainer lineContainer = new LineContainer(lineDisplay, true);
            root.setCenter(lineContainer);

            root.requestLayout();
            root.layout();
            root.applyCss();

            PrintProgressDialog printProgressDialog = new PrintProgressDialog(window, false);
            // Run in background thread:
            new Thread(new Runnable()
            {
                @Override
                @OnThread(value = Tag.FX, ignoreParent = true)
                public void run()
                {
                    FlowEditor.printPages(job, root, n -> {}, lineContainer, lineDisplay, lines, false, printProgressDialog.getWithinFileUpdater());
                    job.endJob();
                    printProgressDialog.finished();
                }
            }, "Print text").start();
            printProgressDialog.showAndWait();
        }
    }

    /**
     * Write some text to the terminal.
     */
    private void writeToPane(TerminalTextPane pane, String s, List<String> cssClasses)
    {
        prepare();
        if (errorText != null && pane == errorText)
            showErrorPane();
        
        // The form-feed character should clear the screen.
        int n = s.lastIndexOf('\f');
        if (n != -1) {
            clear();
            s = s.substring(n + 1);
        }

        pane.append(new StyledSegment(cssClasses, s));

        if (errorText != null && pane != errorText)
        {
            if (!unlimitedBufferingCall.get())
                pane.trimToMostRecentNLines(MAX_BUFFER_LINES);
        }

        pane.scrollToEnd();
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
            if (!window.isShowing()) {
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
        if(clearOnMethodCall.get()) {
            clear();
        }
        if(recordMethodCalls.get()) {
            text.append(new StyledSegment(STDOUT_METHOD_RECORDING, callString + "\n"));
        }
        newMethodCall = true;
    }

    /**
     * Check if "clear on method call" option is selected.
     */
    public boolean clearOnMethodCall()
    {
        return clearOnMethodCall.getValue();
    }

    private void constructorCall(InvokerRecord ir)
    {
        newMethodCall = false;
        if(clearOnMethodCall.get()) {
            clear();
        }
        if(recordMethodCalls.get()) {
            String callString = ir.getResultTypeString() + " " + ir.getResultName() + " = " + ir.toExpression() + ";";
            text.append(new StyledSegment(STDOUT_METHOD_RECORDING, callString + "\n"));
        }
        newMethodCall = true;
    }
    
    private void methodResult(ExecutionEvent event)
    {
        if (recordMethodCalls.get()) {
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
                text.append(new StyledSegment(STDOUT_METHOD_RECORDING, result + "\n"));
            }
        }
    }

    /**
     * Looks through the contents of the terminal for lines
     * that look like they are part of a stack trace.
     */
    private void scanForStackTrace()
    {
        try {
            List<String> allLines = errorText.getLines();

            Pattern fileAndLine = java.util.regex.Pattern.compile("at (\\S+)\\((\\S+)\\.java:(\\d+)\\)");
            Pattern noSource = java.util.regex.Pattern.compile("at \\S+\\((Native Method|Unknown Source)\\)");
            
            // Matches things like:
            // at greenfoot.localdebugger.LocalDebugger$QueuedExecution.run(LocalDebugger.java:267)
            //    ^--------------------group 1----------------------------^ ^--group 2--^      ^3^
            for (int i = 0; i < allLines.size(); i++)
            {
                String line = allLines.get(i);
                Matcher m = fileAndLine.matcher(line);
                
                while (m.find())
                {
                    String fullyQualifiedMethodName = m.group(1);
                    String javaFile = m.group(2);
                    int lineNumber = Integer.parseInt(m.group(3));

                    // The fully qualified method name will end in ".method", so we can
                    // definitely remove that:

                    String fullyQualifiedClassName = JavaNames.getPrefix(fullyQualifiedMethodName);
                    // The class name may be an inner class, so we want to take the package:
                    String packageName = JavaNames.getPrefix(fullyQualifiedClassName);

                    //Find out if that file is available, and only link if it is:
                    Package pkg = project.getPackage(packageName);

                    if (pkg != null && pkg.getAllClassnames().contains(javaFile))
                    {
                        errorText.setStyleForLineSegment(i, m.start(1), m.end(), STDERR_LINKED_STACK_TRACE, new ExceptionSourceLocation(m.start(1), m.end(), pkg, javaFile, lineNumber));
                    }
                    else
                    {
                        errorText.setStyleForLineSegment(i, m.start(), m.end(), STDERR_FOREIGN_STACK_TRACE, null);
                    }
                }

                //Also mark up native method lines in stack traces with a marker for font colour:

                
                // Matches things like:
                //  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                m = noSource.matcher(line);
                while (m.find())
                {
                    errorText.setStyleForLineSegment(i, m.start(), m.end(), STDERR_FOREIGN_STACK_TRACE, null);
                }
            }
        }
        catch (NumberFormatException e ) {
            //In case it looks like an exception but has a large line number:
            e.printStackTrace();
        }
        errorText.refreshDisplay();
    }



    /**
     * Return the input stream that can be used to read from this terminal.
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Reader getReader()
    {
        return in;
    }


    /**
     * Return the output stream that can be used to write to this terminal
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Writer getWriter()
    {
        return out;
    }

    /**
     * It implements the method on the interface DebuggerTerminal which is called
     * when there is reading request from the terminal on the remote virtual machine
     */
    @OnThread(Tag.Any)
    public void showOnInput()
    {
        Platform.runLater(() -> {
            if (!this.isShown()) {
                this.showHide(true);
            }

            if (this.isShown()) {
                Utility.bringToFrontFX(window);
                input.requestFocus();
            }
        });
    }

    /**
     * Return the output stream that can be used to write error output to this terminal
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Writer getErrorWriter()
    {
        return err;
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
     * @param prj      A project where the event happens
     */
    @Override
    public void blueJEvent(int eventId, Object arg, Project prj)
    {
        if(eventId == BlueJEvent.METHOD_CALL && this.project == prj) {
            InvokerRecord ir = (InvokerRecord) arg;
            if (ir.getResultName() != null) {
                constructorCall(ir);
            }
            else {
                boolean isVoid = ir.hasVoidResult();
                if (isVoid) {
                    methodCall(ir.toStatement());
                }
                else {
                    methodCall(ir.toExpression());
                }
            }
        }
        else if (eventId == BlueJEvent.EXECUTION_RESULT) {
            methodResult((ExecutionEvent) arg);
        }
    }

    // ---- make window frame ----

    /**
     * Show the errorPane for error output
     */
    private void showErrorPane()
    {
        if(errorShown) {
            return;
        }

        if(errorText == null) {
            errorText = new TerminalTextPane() {
                @Override
                public void focusPrevious()
                {
                    if (!input.isDisable())
                        input.requestFocus();
                    else
                        text.requestFocusAndShowCaret();
                }

                @Override
                public void focusNext()
                {
                    text.requestFocusAndShowCaret();
                }
            };
            errorText.getStyleClass().add("terminal-error");
            errorText.styleProperty().bind(PrefMgr.getEditorFontCSS(true));
            // Any selection in the error pane should clear existing selection in the output text pane

            errorText.addSelectionListener((caret, anchor) -> {
                if (text != null && text.getCaretEditorPosition().getPosition() != text.getAnchorEditorPosition().getPosition())
                {
                    text.deselect();
                }
            });
            errorText.addTextChangeListener(this::scanForStackTrace);
        }
        splitPane.getItems().add(errorText);
        Config.rememberDividerPosition(window, splitPane, "bluej.terminal.dividerpos");
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
        splitPane.getItems().remove(errorText);
        errorShown = false;
    }


    public BooleanProperty showingProperty()
    {
        return showingProperty;
    }
    
    /**
     * Create the terminal's menubar, all menus and items.
     */
    private MenuBar makeMenuBar()
    {
        MenuBar menubar = new MenuBar();
        menubar.setUseSystemMenuBar(true);
        Menu menu = new Menu(Config.getString("terminal.options"));
        MenuItem clearItem = new MenuItem(Config.getString("terminal.clear"));
        clearItem.setOnAction(e -> clear());
        clearItem.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN));

        MenuItem copyItem = new MenuItem(Config.getString("terminal.copy"));
        copyItem.setOnAction(e -> doCopy());
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));

        MenuItem saveItem = new MenuItem(Config.getString("terminal.save"));
        saveItem.setOnAction(e -> save());
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));

        MenuItem printItem = new MenuItem(Config.getString("terminal.print"));
        printItem.setOnAction(e -> print());
        printItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN));

        menu.getItems().addAll(clearItem, copyItem, saveItem, printItem, new SeparatorMenuItem());

        CheckMenuItem autoClear = new CheckMenuItem(Config.getString("terminal.clearScreen"));
        autoClear.selectedProperty().bindBidirectional(clearOnMethodCall);

        CheckMenuItem recordCalls = new CheckMenuItem(Config.getString("terminal.recordCalls"));
        recordCalls.selectedProperty().bindBidirectional(recordMethodCalls);

        CheckMenuItem unlimitedBuffering = new CheckMenuItem(Config.getString("terminal.buffering"));
        unlimitedBuffering.selectedProperty().bindBidirectional(unlimitedBufferingCall);

        menu.getItems().addAll(autoClear, recordCalls, unlimitedBuffering);

        MenuItem closeItem = new MenuItem(Config.getString("terminal.close"));
        closeItem.setOnAction(e -> showHide(false));
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));

        menu.getItems().addAll(new SeparatorMenuItem(), closeItem);

        menubar.getMenus().add(menu);
        return menubar;
    }

    public Stage getWindow()
    {
        return window;
    }

    /**
     * Cleanup any resources or listeners the terminal has created/registered.
     * Called when the project is closing.
     */
    public void cleanup()
    {
        BlueJEvent.removeListener(this);
    }

    /**
     * A Reader which reads from the terminal.
     */
    @OnThread(Tag.Any)
    private class TerminalReader extends Reader
    {
        public int read(char[] cbuf, int off, int len)
        {
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
    @OnThread(Tag.Any)
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
                // We use a wait so that terminal output is limited to
                // the processing speed of the event queue. This means the UI
                // will still respond to user input even if the output is really
                // gushing.
                CompletableFuture<Boolean> written = new CompletableFuture<>();
                Platform.runLater(() -> {
                    try
                    {
                        String s = new String(cbuf, off, len);
                        if (isErrorOut)
                        {
                            showErrorPane();
                            writeToPane(errorText, s, STDERR_NORMAL);
                        }
                        else
                            writeToPane(text, s, STDOUT_OUTPUT);
                    }
                    catch (Throwable t)
                    {
                        Debug.reportError(t);
                    }
                    finally
                    {
                        written.complete(true);
                    }
                });
                // Timeout in case something goes wrong with the printing:
                written.get(2000, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException | ExecutionException | TimeoutException ie)
            {
                Debug.reportError(ie);
            }
        }

        public void flush() { }

        public void close() { }
    }
}
