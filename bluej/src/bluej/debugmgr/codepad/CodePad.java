/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2016  Michael Kolling and John Rosenberg
 
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

package bluej.debugmgr.codepad;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import bluej.BlueJEvent;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.IndexHistory;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.ValueCollection;
import bluej.parser.TextAnalyzer;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A code pad which can evaluate fragments of Java code.
 * 
 * In JavaFX this is built using a ListView.  ListViews have virtualised
 * cells, which means that only enough rows are created to display
 * the currently visible portion of the scroll pane, not every row in the list.
 * So if the user has a history 200 lines long, but only 8 lines are visible
 * in the codepad, then no matter where they scroll to, only 8 display lines will be
 * created, not 200.
 * 
 * ListViews allow editing of any individual cell, but we hack it here so that
 * (a) only the last row can be edited, and (b) the last row is *always* in an editing state.
 * This means the users see one text field at the bottom of the list, and rows
 * above that with the history (styled as we like).
 */
@OnThread(Tag.FX)
public class CodePad extends ListView<CodePad.CodePadRow>
    implements ValueCollection, PkgMgrFrame.PkgMgrPane
{
    /**
     * The last row in the listview, which is always in an editing
     * state.
     */
    private final EditRow editRow;

    /**
     * A data item which backs a single row in the code pad.
     * This might be the currently edited row (the last row), or
     * a read-only item detailing a past command or command outcome;
     */
    public abstract static @OnThread(Tag.FX) class CodePadRow
    {
        // Text content of the row
        protected String text = "";
        // Crude way of making sure all lines are spaced the same as ones with an object image;
        // use an invisible rectangle as a spacer:
        protected Rectangle r = new Rectangle(objectImage.getWidth(), objectImage.getHeight());
        {
            r.setVisible(false);
        }
        
        // Different styles used for the rows
        @OnThread(Tag.Any)
        public static enum RowStyle
        {
            COMMAND_PARTIAL("bj-codepad-cmd-partial"),
            COMMAND_END("bj-codepad-cmd-end"),
            ERROR("bj-codepad-error"),
            OUTPUT("bj-codepad-output"),
            EDIT("bj-codepad-edit");
            
            private final String pseudo;
            public String getPseudoClass() { return pseudo;}
            private RowStyle(String pseudo) { this.pseudo = pseudo; }
        }
        
        /**
         * Gets the text which is showing for the row.
         * Note that EditRow overrides this to use the text from
         * the textfield, so there is a difference between row.text and row.getText()
         */
        public String getText() { return text; }
        /** Is the row editable?  Only true for the single edit row. */
        public boolean isEditable() { return false; }
        /** Gets the graphic to display alongside the row */
        public Node getGraphic() { return r; }
        /** Records the text field for the row (only used for EditRow) */
        public void setTextField(TextField textField) { }
        /** Gets the graphical style that should be used for displaying this row. */
        public abstract RowStyle getStyle();
    }

    // Handy array with all the different row pseudo-class styles.
    private static final String[] allRowStyles;
    static {
        allRowStyles = new String[CodePadRow.RowStyle.values().length];
        for (int i = 0; i < CodePadRow.RowStyle.values().length; i++)
        {
            allRowStyles[i] = CodePadRow.RowStyle.values()[i].getPseudoClass();
        }
    }

    /**
     * A row with a previously entered command.  This may be a single
     * complete row (e.g. 1+2) or it may be part of a multi-row
     * command.  We have a different pseudo-class for the last row
     * of commands, but currently don't use it differently in the CSS file.
     */
    @OnThread(Tag.FX)
    private static class CommandRow extends CodePadRow
    {
        private final boolean isFinalLine;
        public CommandRow(String text, boolean isFinalLine)
        {
            this.text = text; this.isFinalLine = isFinalLine;
        }

        // No indent spacer on command rows in our current style:
        @Override
        public Node getGraphic()
        {
            return null;
        }

        @Override
        public RowStyle getStyle()
        {
            return isFinalLine ? RowStyle.COMMAND_END : RowStyle.COMMAND_PARTIAL;
        }
    }

    /**
     * The single final editable row.
     */
    @OnThread(Tag.FX)
    private class EditRow extends CodePadRow
    {
        private TextField textField;

        public EditRow(String text)
        {
            this.text = text;
        }

        @Override
        public String getText()
        {
            if (textField != null)
                return textField.getText();
            else
                return super.getText();
        }

        @Override
        public boolean isEditable()
        {
            return true;
        }

        @Override
        public void setTextField(TextField textField)
        {
            this.textField = textField;
            if (getItems().size() == 1)
                textField.setPromptText(Config.getString("codepad.prompt"));
            else
                textField.setPromptText("");
        }
        
        public void setText(String text)
        {
            if (this.textField != null)
                this.textField.setText(text);
        }

        @Override
        public RowStyle getStyle()
        {
            return RowStyle.EDIT;
        }

        @Override
        public Node getGraphic()
        {
            return null;
        }
    }

    /**
     * The successful output of a previous command.  It may or may not
     * have an object as an output.
     */
    @OnThread(Tag.FX)
    private class OutputRow extends CodePadRow
    {
        private final ImageView graphic;

        public OutputRow(String text, ObjectInfo objInfo)
        {
            this.text = text;
            if (objInfo != null)
            {
                graphic = new ImageView(objectImage);
                graphic.setMouseTransparent(false);
                // It turns out that LabeledSkinBase contains this code:
                // // RT-19851 Only setMouseTransparent(true) for an ImageView.  This allows the button
                // // to be picked regardless of the changing images on top of it.
                // if (graphic instanceof ImageView) {
                //    graphic.setMouseTransparent(true);
                //}
                // Our graphic is an ImageView, so it gets its mouse transparent set to
                // true by that code.  To ward this off, we add a listener that always
                // sets it back to false whenever it has been turned true:
                JavaFXUtil.addChangeListener(graphic.mouseTransparentProperty(), b -> {
                    // Won't infinitely recurse, only change back to true once:
                    if (b.booleanValue())
                        graphic.setMouseTransparent(false);
                });
                graphic.setCursor(Cursor.HAND);
                graphic.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                    // Don't really care about click count; double click can do it too, that's fine.
                    if (e.getButton() == MouseButton.PRIMARY)
                    {
                        Stage fxWindow = frame.getFXWindow();
                        SwingUtilities.invokeLater(() -> {
                            frame.getPackage().getEditor().raisePutOnBenchEvent(fxWindow, objInfo.obj, objInfo.obj.getGenType(), objInfo.ir);
                        });
                        e.consume();
                    }
                });
                graphic.setOnMouseEntered(e -> {
                    graphic.setImage(objectImageHighlight);
                });
                graphic.setOnMouseExited(e -> {
                    graphic.setImage(objectImage);
                });
            }
            else
                graphic = null;
        }

        // Graphic is an object icon if applicable, otherwise
        // we use the invisible spacer from the parent:
        @Override
        public Node getGraphic()
        {
            return graphic != null ? graphic : super.getGraphic();
        }

        @Override
        public RowStyle getStyle()
        {
            return RowStyle.OUTPUT;
        }
    }

    /**
     * A row with an error output of a previous command.
     */
    @OnThread(Tag.FX)
    private static class ErrorRow extends CodePadRow
    {
        public ErrorRow(String text)
        {
            this.text = text;
        }

        @Override
        public RowStyle getStyle()
        {
            return RowStyle.ERROR;
        }
    }

    private static final String nullLabel = "null";
    
    private static final String uninitializedWarning = Config.getString("pkgmgr.codepad.uninitialized");

    private static final Image objectImage =
            Config.getImageAsFXImage("image.eval.object.add");
    private static final Image objectImageHighlight =
            Config.getImageAsFXImage("image.eval.object.add.highlight");
    
    private final PkgMgrFrame frame;
    @OnThread(Tag.FX)
    private String currentCommand = "";
    @OnThread(Tag.FX)
    private IndexHistory history;
    @OnThread(Tag.Swing)
    private Invoker invoker = null;
    @OnThread(Tag.Swing)
    private TextAnalyzer textParser = null;
    
    // Keeping track of invocation
    @OnThread(Tag.Swing)
    private boolean firstTry;
    @OnThread(Tag.Swing)
    private boolean wrappedResult;
    @OnThread(Tag.Swing)
    private String errorMessage;

    @OnThread(Tag.Swing)
    private boolean busy = false;

    @OnThread(Tag.Swing)
    private List<CodepadVar> localVars = new ArrayList<CodepadVar>();
    @OnThread(Tag.Swing)
    private List<CodepadVar> newlyDeclareds;
    @OnThread(Tag.Swing)
    private List<String> autoInitializedVars;
    // The action which removes the hover state on the object icon
    private Runnable removeHover;

    public CodePad(PkgMgrFrame frame)
    {
        super();
        
        this.frame = frame;
        setFocusTraversable(false);
        //defineKeymap();
        history = new IndexHistory(20);

        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Add context menu with copy:
        setContextMenu(new ContextMenu(JavaFXUtil.makeMenuItem(Config.getString("editor.copyLabel"), () -> {
            copySelectedRows();
        }, null)));
        
        // Add keyboard shortcut ourselves:
        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.C && e.isShortcutDown())
            {
                copySelectedRows();
                e.consume();
            }
            else if (e.getCode() == KeyCode.UP)
            {
                historyBack();
                e.consume();
            }
            else if (e.getCode() == KeyCode.DOWN)
            {
                historyForward();
                e.consume();
            }
            else if (e.getCode() == KeyCode.ENTER && e.isShiftDown())
            {
                softReturn();
                e.consume();
            }
        });
        
        StringConverter<CodePadRow> converter = new StringConverter<CodePadRow>()
        {
            @Override
            @OnThread(Tag.FX)
            public String toString(CodePadRow object)
            {
                return object == null ? "" : object.text;
            }

            @Override
            @OnThread(Tag.FX)
            public CodePadRow fromString(String string)
            {
                // This is only called on commitEdit, in which case it must be an edit row:
                editRow.text = string;
                return editRow;
            }
        };
        setCellFactory(lv -> new TextFieldListCellWithGraphic<CodePadRow>(converter) {
            {
                JavaFXUtil.addStyleClass(this, "codepad-row");
            }
            @Override
            @OnThread(Tag.FX)
            public void commitEdit(CodePadRow newValue)
            {
                String text = newValue.getText();
                ((EditRow)newValue).text = "";
                super.commitEdit(newValue);
                setEditable(false);    // don't allow input while we're thinking
                command(text, true);
                currentCommand = (currentCommand + text).trim();
                if(currentCommand.length() != 0)
                {
                    history.add(text);
                    String cmd = currentCommand;
                    currentCommand = "";
                    SwingUtilities.invokeLater(() -> executeCommand(cmd));
                }
            }

            @Override
            @OnThread(Tag.FX)
            public void updateItem(CodePadRow item, boolean empty)
            {
                // Must set it before calling super method:
                tagGraphic = item != null ? item.getGraphic() : null;
                super.updateItem(item, empty);
                if (!empty && item != null)
                {
                    setEditable(item.isEditable());

                    if (isEditable())
                    {
                        // Must startEdit first, as that may be what initialises
                        // textField (if we haven't edited this cell before):
                        super.startEdit();
                        item.setTextField(textField);
                    }
                    else
                    {
                        super.cancelEdit();
                    }
                    JavaFXUtil.selectPseudoClass(this, Arrays.asList(allRowStyles).indexOf(item.getStyle().getPseudoClass()), allRowStyles);
                }
                else
                {
                    setEditable(false);
                    super.cancelEdit();
                    JavaFXUtil.selectPseudoClass(this, -1, allRowStyles);
                }
            }

            @Override
            @OnThread(Tag.FX)
            public void cancelEdit()
            {
            }

            @Override
            @OnThread(Tag.FX)
            public void startEdit()
            {
            }
        });

        editRow = new EditRow("");
        getItems().add(editRow);
        setEditable(true);
    }

    private void copySelectedRows()
    {
        String copied = getSelectionModel().getSelectedItems().stream().map(CodePadRow::getText).collect(Collectors.joining("\n"));
        Clipboard.getSystemClipboard().setContent(Collections.singletonMap(DataFormat.PLAIN_TEXT, copied));
    }

    /**
     * Clear the local variables.
     */
    @OnThread(Tag.Swing)
    public void clearVars()
    {
        localVars.clear();
        if (textParser != null && frame.getProject() != null) {
            textParser.newClassLoader(frame.getProject().getClassLoader());
        }
    }
    
    //   --- ValueCollection interface ---
    
    /*
     * @see bluej.debugmgr.ValueCollection#getValueIterator()
     */
    @OnThread(Tag.Swing)
    public Iterator<CodepadVar> getValueIterator()
    {
        return localVars.iterator();
    }
    
    /*
     * @see bluej.debugmgr.ValueCollection#getNamedValue(java.lang.String)
     */
    @OnThread(Tag.Swing)
    public NamedValue getNamedValue(String name)
    {
        Class<Object> c = Object.class;
        NamedValue nv = getLocalVar(name);
        if (nv != null) {
            return nv;
        }
        else {
            return frame.getObjectBench().getNamedValue(name);
        }
    }
    
    /**
     * Search for a named local variable, but do not fall back to the object
     * bench if it cannot be found (return null in this case).
     * 
     * @param name  The name of the variable to search for
     * @return    The named variable, or null
     */
    @OnThread(Tag.Swing)
    private NamedValue getLocalVar(String name)
    {
        Iterator<CodepadVar> i = localVars.iterator();
        while (i.hasNext()) {
            NamedValue nv = (NamedValue) i.next();
            if (nv.getName().equals(name))
                return nv;
        }
        
        // not found
        return null;
    }
    
    private class CodePadResultWatcher implements ResultWatcher
    {
        private final String command;

        public CodePadResultWatcher(String command)
        {
            this.command = command;
        }

        /*
                 * @see bluej.debugmgr.ResultWatcher#beginExecution()
                 */
        @Override
        @OnThread(Tag.Swing)
        public void beginCompile()
        {
        }

        /*
         * @see bluej.debugmgr.ResultWatcher#beginExecution()
         */
        @Override
        @OnThread(Tag.Swing)
        public void beginExecution(InvokerRecord ir)
        {
            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, ir);
        }

        /*
         * @see bluej.debugmgr.ResultWatcher#putResult(bluej.debugger.DebuggerObject, java.lang.String, bluej.testmgr.record.InvokerRecord)
         */
        @Override
        @OnThread(Tag.Swing)
        public void putResult(final DebuggerObject result, final String name, final InvokerRecord ir)
        {
            frame.getObjectBench().addInteraction(ir);
            updateInspectors();

            // Newly declared variables are now initialized
            if (newlyDeclareds != null)
            {
                Iterator<CodepadVar> i = newlyDeclareds.iterator();
                while (i.hasNext())
                {
                    CodepadVar cpv = (CodepadVar)i.next();
                    cpv.setInitialized();
                }
                newlyDeclareds = null;
            }

            boolean giveUninitializedWarning = autoInitializedVars != null && autoInitializedVars.size() != 0;

            if (giveUninitializedWarning && Utility.firstTimeThisRun("TextEvalPane.uninitializedWarning"))
            {
                // Some variables were automatically initialized - warn the user that
                // this won't happen in "real" code.

                String warning = uninitializedWarning;

                int findex = 0;
                while (findex < warning.length())
                {
                    int nindex = warning.indexOf('\n', findex);
                    if (nindex == -1)
                        nindex = warning.length();

                    String warnLine = warning.substring(findex, nindex);
                    Platform.runLater(() -> error(warnLine));
                    findex = nindex + 1; // skip the newline character
                }

                autoInitializedVars.clear();
            }

            if (!result.isNullObject())
            {
                DebuggerField resultField = result.getField(0);
                String resultString = resultField.getValueString();

                if (resultString.equals(nullLabel))
                {
                    DataCollector.codePadSuccess(frame.getPackage(), ir.getOriginalCommand(), resultString);
                    Platform.runLater(() -> output(resultString));
                }
                else
                {
                    boolean isObject = resultField.isReferenceType();

                    if (isObject)
                    {
                        DebuggerObject resultObject = resultField.getValueObject(null);
                        String resultType = resultObject.getGenType().toString(true);
                        String resultOutputString = resultString + "   (" + resultType + ")";
                        DataCollector.codePadSuccess(frame.getPackage(), ir.getOriginalCommand(), resultOutputString);
                        Platform.runLater(() -> objectOutput(resultOutputString, new ObjectInfo(resultObject, ir)));
                    }
                    else
                    {
                        String resultType = resultField.getType().toString(true);
                        String resultOutputString = resultString + "   (" + resultType + ")";
                        DataCollector.codePadSuccess(frame.getPackage(), ir.getOriginalCommand(), resultOutputString);
                        Platform.runLater(() -> output(resultOutputString));
                    }
                }
            }
            else
            {
                //markCurrentAs(TextEvalSyntaxView.OUTPUT, false);
            }

            ExecutionEvent executionEvent = new ExecutionEvent(frame.getPackage());
            executionEvent.setCommand(command);
            executionEvent.setResult(ExecutionEvent.NORMAL_EXIT);
            executionEvent.setResultObject(result);
            BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);

            textParser.confirmCommand();
            Platform.runLater(() -> setEditable(true));    // allow next input
            busy = false;
        }

        @OnThread(Tag.Swing)
        private void updateInspectors()
        {
            Project proj = frame.getPackage().getProject();
            Platform.runLater(() -> proj.updateInspectors());
        }

        /**
         * An invocation has failed - here is the error message
         */
        @Override
        @OnThread(Tag.Swing)
        public void putError(String message, InvokerRecord ir)
        {
            if (firstTry)
            {
                if (wrappedResult)
                {
                    // We thought we knew what the result type should be, but there
                    // was a compile time error. So try again, assuming that we
                    // got it wrong, and we'll use the dynamic result type (meaning
                    // we won't get type arguments).
                    wrappedResult = false;
                    errorMessage = null; // use the error message from this second attempt
                    invoker = new Invoker(frame, CodePad.this, command, this);
                    invoker.setImports(textParser.getImportStatements());
                    invoker.doFreeFormInvocation("");
                }
                else
                {
                    // We thought there was going to be a result, but compilation failed.
                    // Try again, but assume we have a statement this time.
                    firstTry = false;
                    invoker = new Invoker(frame, CodePad.this, command, this);
                    invoker.setImports(textParser.getImportStatements());
                    invoker.doFreeFormInvocation(null);
                    if (errorMessage == null)
                    {
                        errorMessage = message;
                    }
                }
            }
            else
            {
                if (errorMessage == null)
                {
                    errorMessage = message;
                }

                // An error. Remove declared variables.
                if (autoInitializedVars != null)
                {
                    autoInitializedVars.clear();
                }

                removeNewlyDeclareds();
                DataCollector.codePadError(frame.getPackage(), ir.getOriginalCommand(), errorMessage);
                showErrorMsg(errorMessage);
                errorMessage = null;
            }
        }

        /**
         * A runtime exception occurred.
         */
        @Override
        @OnThread(Tag.Swing)
        public void putException(ExceptionDescription exception, InvokerRecord ir)
        {
            ExecutionEvent executionEvent = new ExecutionEvent(frame.getPackage());
            executionEvent.setCommand(command);
            executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
            executionEvent.setException(exception);
            BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
            updateInspectors();

            if (autoInitializedVars != null)
            {
                autoInitializedVars.clear();
            }

            removeNewlyDeclareds();
            String message = exception.getClassName() + " (" + exception.getText() + ")";
            DataCollector.codePadException(frame.getPackage(), ir.getOriginalCommand(), message);
            showExceptionMsg(message);
        }

        /**
         * The remote VM terminated before execution completed (or as a result of
         * execution).
         */
        @Override
        @OnThread(Tag.Swing)
        public void putVMTerminated(InvokerRecord ir)
        {
            if (autoInitializedVars != null)
                autoInitializedVars.clear();

            removeNewlyDeclareds();


            String message = Config.getString("pkgmgr.codepad.vmTerminated");
            DataCollector.codePadError(frame.getPackage(), ir.getOriginalCommand(), message);
            Platform.runLater(() -> error(message));

            completeExecution();
        }
    }
    
    /**
     * Remove the newly declared variables from the value collection.
     * (This is needed if compilation fails, or execution bombs with an exception).
     */
    @OnThread(Tag.Swing)
    private void removeNewlyDeclareds()
    {
        if (newlyDeclareds != null) {
            Iterator<CodepadVar> i = newlyDeclareds.iterator();
            while (i.hasNext()) {
                localVars.remove(i.next());
            }
            newlyDeclareds = null;
        }
    }
    
    //   --- end of ResultWatcher interface ---
    
    /**
     * Show an error message, and allow further command input.
     */
    @OnThread(Tag.Swing)
    private void showErrorMsg(final String message)
    {
        Platform.runLater(() -> error("Error: " + message));
        completeExecution();
    }
    
    /**
     * Show an exception message, and allow further command input.
     */
    @OnThread(Tag.Swing)
    private void showExceptionMsg(final String message)
    {
        Platform.runLater(() -> error("Exception: " + message));
        completeExecution();
    }
    
    /**
     * Execution of the current command has finished (one way or another).
     * Allow further command input.
     */
    @OnThread(Tag.Swing)
    private void completeExecution()
    {
        Platform.runLater(() -> setEditable(true));
        busy = false;
    }

    /**
     * Record part of a command
     * @param s
     */
    private void command(String s, boolean isFinalLine)
    {
        getItems().add(getItems().size() - 1, new CommandRow(s, isFinalLine));
    }

    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    private void output(String s)
    {
        getItems().add(getItems().size() - 1, new OutputRow(s, null));
    }
    
    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    private void objectOutput(String s, ObjectInfo objInfo)
    {
        getItems().add(getItems().size() - 1, new OutputRow(s, objInfo));
//        markAs(TextEvalSyntaxView.OBJECT, objInfo);
    }
    
    /**
     * Write an error message to the text area.
     * @param s The message
     */
    private void error(String s)
    {
        getItems().add(getItems().size() - 1, new ErrorRow(s));
    }

    /**
     * Return the object stored with the line at position 'pos'.
     * If that line does not have an object, return null.
     */
    private ObjectInfo objectAtPosition(int pos)
    {
        //Element line = getLineAt(pos);
        //return (ObjectInfo) line.getAttributes().getAttribute(TextEvalSyntaxView.OBJECT);
        return null;
    }

    public void clear()
    {
        SwingUtilities.invokeLater(this::clearVars);
    }

    public void resetFontSize()
    {

    }

    @OnThread(Tag.Swing)
    private void executeCommand(String command)
    {
        if (busy) {
            return;
        }
        
        firstTry = true;
        busy = true;
        if (textParser == null) {
            textParser = new TextAnalyzer(frame.getProject().getEntityResolver(),
                    frame.getPackage().getQualifiedName(), CodePad.this);
        }
        String retType;
        retType = textParser.parseCommand(command);
        wrappedResult = (retType != null && retType.length() != 0);
        
        // see if any variables were declared
        if (retType == null) {
            firstTry = false; // Only try once.
            command = textParser.getAmendedCommand();
            List<DeclaredVar> declaredVars = textParser.getDeclaredVars();
            if (declaredVars != null) {
                Iterator<DeclaredVar> i = declaredVars.iterator();
                while (i.hasNext()) {
                    if (newlyDeclareds == null) {
                        newlyDeclareds = new ArrayList<CodepadVar>();
                    }
                    if (autoInitializedVars == null) {
                        autoInitializedVars = new ArrayList<String>();
                    }
                    
                    DeclaredVar dv = i.next();
                    String declaredName = dv.getName();
                    
                    if (getLocalVar(declaredName) != null) {
                        // The variable has already been declared
                        String errMsg = Config.getString("pkgmgr.codepad.redefinedVar");
                        errMsg = Utility.mergeStrings(errMsg, declaredName);
                        showErrorMsg(errMsg);
                        removeNewlyDeclareds();
                        return;
                    }
                    
                    CodepadVar cpv = new CodepadVar(dv.getName(), dv.getDeclaredType(), dv.isFinal());
                    newlyDeclareds.add(cpv);
                    localVars.add(cpv);

                    // If the variable was declared but not initialized, the codepad
                    // auto-initializes it. We add to a list so that we can display
                    // a warning to that effect, once the command has completed.
                    if (! dv.isInitialized()) {
                        autoInitializedVars.add(dv.getName());
                    }
                }
            }
        }

        CodePadResultWatcher watcher = new CodePadResultWatcher(command);
        invoker = new Invoker(frame, CodePad.this, command, watcher);
        invoker.setImports(textParser.getImportStatements());
        if (!invoker.doFreeFormInvocation(retType)) {
            // Invocation failed
            firstTry = false;
            watcher.putError("Invocation failed.", null);
        }
    }

    private void softReturn()
    {
        String line = editRow.getText();
        currentCommand += line + " ";
        history.add(line);
        command(line, false);
        editRow.setText("");
    }

    private void historyBack()
    {
        String line = history.getPrevious();
        if(line != null) {
            setInput(line);
        }
    }

    private void setInput(String line)
    {
        editRow.setText(line);
    }

    private void historyForward()
    {
        String line = history.getNext();
        if(line != null) {
            setInput(line);
        }
    }

    public void focusEditRow()
    {
        int index = getItems().size() - 1;
        scrollTo(index);
        getFocusModel().focus(index);
        editRow.textField.requestFocus();
    }

    @OnThread(Tag.Any)
    final class ObjectInfo {
        DebuggerObject obj;
        InvokerRecord ir;
        
        /**
         * Create an object holding information about an invocation.
         */
        public ObjectInfo(DebuggerObject obj, InvokerRecord ir) {
            this.obj = obj;
            this.ir = ir;
        }
    }
    
    final class CodepadVar implements NamedValue {
        
        String name;
        boolean finalVar;
        boolean initialized = false;
        JavaType type;
        
        public CodepadVar(String name, JavaType type, boolean finalVar)
        {
            this.name = name;
            this.finalVar = finalVar;
            this.type = type;
        }
        
        public String getName()
        {
            return name;
        }
        
        public JavaType getGenType()
        {
            return type;
        }
        
        public boolean isFinal()
        {
            return finalVar;
        }
        
        public boolean isInitialized()
        {
            return initialized;
        }
        
        public void setInitialized()
        {
            initialized = true;
        }
    }
    
}
