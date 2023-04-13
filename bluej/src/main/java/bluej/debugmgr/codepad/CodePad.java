/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2016,2017,2018,2019,2020,2021,2022,2023  Michael Kolling and John Rosenberg
 
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
import bluej.pkgmgr.PkgMgrFrame.PkgMgrPane;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Utility;
import bluej.utility.javafx.AbstractOperation;
import bluej.utility.javafx.AbstractOperation.Combine;
import bluej.utility.javafx.AbstractOperation.ContextualItem;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
@OnThread(Tag.FXPlatform)
public class CodePad extends VBox
    implements ValueCollection
{
    /**
     * The list view containing all the history items:
     */
    private final ListView<HistoryRow> historyView;

    /**
     * The edit field for input
     */
    private final TextField inputField;
    /**
     * The pane on which to draw the arrows indicating what happens
     * to clicked objects.
     */
    private final Pane arrowOverlay;
    private BooleanBinding shadowShowing;
    private ObjectBinding shadowShowBinding;
    private ContextMenu contextMenu;

    // Different styles used for the rows
    @OnThread(Tag.Any)
    public enum RowStyle
    {
        COMMAND_PARTIAL("bj-codepad-cmd-partial"),
        COMMAND_END("bj-codepad-cmd-end"),
        ERROR("bj-codepad-error"),
        OUTPUT("bj-codepad-output");

        private final String pseudo;

        public String getPseudoClass()
        {
            return pseudo;
        }

        private RowStyle(String pseudo)
        {
            this.pseudo = pseudo;
        }
    }
    
    /**
     * A data item which backs a single row in the code pad.
     * This might be the currently edited row (the last row), or
     * a read-only item detailing a past command or command outcome;
     */
    private abstract @OnThread(Tag.FX) class HistoryRow implements ContextualItem<HistoryRow>
    {
        // Text content of the row
        private final String text;
        private boolean lastRowBeforeReset;

        public HistoryRow(String text, boolean lastRowBeforeReset)
        {
            this.text = text;
            this.lastRowBeforeReset = lastRowBeforeReset;
        }

        public HistoryRow(String text)
        {
            this(text, false);
        }

        public final String getText() { return text; }

        @Override
        public String toString()
        {
            return getText();
        }

        public abstract Node getGraphic();
        /** Gets the graphical style that should be used for displaying this row. */
        public abstract RowStyle getStyle();

        /** Makes a copy of this row */
        public abstract HistoryRow clone();

        /** Makes a copy that displays as the last row before a reset */
        public final HistoryRow asLastRowBeforeReset()
        {
            HistoryRow historyRow = clone();
            historyRow.lastRowBeforeReset = true;
            return historyRow;
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public List<? extends AbstractOperation<HistoryRow>> getContextOperations()
        {
            return List.of(new CopyOperation(), new ClearOperation(), new SelectAllOperation());
        }
    }

    @OnThread(Tag.FX)
    public abstract class IndentedRow extends HistoryRow
    {
        // Crude way of making sure all lines are spaced the same as ones with an object image;
        // use an invisible rectangle as a spacer:
        protected Rectangle r;

        public IndentedRow(String text)
        {
            super(text);
            r  = new Rectangle(objectImage.getWidth(), objectImage.getHeight());
            r.setVisible(false);
        }

        /** Gets the graphic to display alongside the row */
        @Override
        public Node getGraphic() { return r; }
    }

    // Handy array with all the different row pseudo-class styles.
    private static final String[] allRowStyles;
    static {
        allRowStyles = new String[RowStyle.values().length];
        for (int i = 0; i < RowStyle.values().length; i++)
        {
            allRowStyles[i] = RowStyle.values()[i].getPseudoClass();
        }
    }

    /**
     * A row with a previously entered command.  This may be a single
     * complete row (e.g. 1+2) or it may be part of a multi-row
     * command.  We have a different pseudo-class for the last row
     * of commands, but currently don't use it differently in the CSS file.
     */
    @OnThread(Tag.FX)
    private class CommandRow extends HistoryRow
    {
        private final boolean isFinalLine;
        public CommandRow(String text, boolean isFinalLine)
        {
            super(text);
            this.isFinalLine = isFinalLine;
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

        @Override
        public HistoryRow clone()
        {
            return new CommandRow(getText(), isFinalLine);
        }
    }

    /**
     * The successful output of a previous command.  It may or may not
     * have an object as an output.
     */
    @OnThread(Tag.FX)
    private class OutputSuccessRow extends IndentedRow
    {
        private final ImageView graphic;
        private Path arrow;
        private FXPlatformRunnable cancelAddToBench;
        private final ObjectInfo objInfo;

        public OutputSuccessRow(String text, ObjectInfo objInfo)
        {
            super(text);
            this.objInfo = objInfo;
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
                    // Won't infinitely recurse, we always change back to false:
                    if (b.booleanValue())
                        graphic.setMouseTransparent(false);
                });
                graphic.setCursor(Cursor.HAND);
                graphic.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                    if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1)
                    {
                        // It seems odd to nest a run-later inside a delay, but JavaFX doesn't let you
                        // show a modal dialog from an animation (which is how delay is implemented)
                        // hence we use animation for delay, then run-later to be able to go modal.
                        // If a double-click occurs soon after the single-click, the
                        // single-click transfer will be cancelled in favour of the double-click
                        // inspect.
                        cancelAddToBench = JavaFXUtil.runAfter(Duration.millis(500), () -> JavaFXUtil.runAfterCurrent(() -> {
                            addResultToBench();
                            cancelAddToBench = null;
                        }));
                        e.consume();
                    }
                    else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                    {
                        // Cancel the single click that was adding to bench:
                        if (cancelAddToBench != null)
                        {
                            cancelAddToBench.run();
                            cancelAddToBench = null;
                        }
                        inspectResult();
                        e.consume();
                    }
                });
                graphic.setOnMouseEntered(e -> {
                    graphic.setImage(objectImageHighlight);
                    graphic.setEffect(new ColorAdjust(0.0, -0.2, 0.45, 0.0));
                    if (arrow == null)
                    {
                        arrow = new Path();
                        JavaFXUtil.addStyleClass(arrow, "codepad-add-object-arrow");
                        double centreAngle = 35.0;
                        arrow.getElements().addAll(
                                new MoveTo(40.0, 10.0),
                                new QuadCurveTo(25.0, -10.0, 0.0, 10.0), //new LineTo(0.0, 10.0),
                                new LineTo(0 + Math.cos(Math.toRadians(centreAngle + 45.0))*10.0, 10.0 - Math.sin(Math.toRadians(centreAngle + 45.0))*10.0),
                                new MoveTo(0.0, 10.0),
                                new LineTo(0 + Math.cos(Math.toRadians(centreAngle - 45.0))*10.0, 10.0 - Math.sin(Math.toRadians(centreAngle - 45.0))*10.0)
                        );
                    }
                    Bounds b = arrowOverlay.sceneToLocal(graphic.localToScene(graphic.getBoundsInLocal()));
                    arrow.setLayoutX(b.getMinX() - 40.0);
                    arrow.setLayoutY(b.getMinY() - 10.0 + b.getHeight()*0.5);
                    arrowOverlay.getChildren().add(arrow);

                });
                graphic.setOnMouseExited(e -> {
                    graphic.setImage(objectImage);
                    graphic.setEffect(null);
                    arrowOverlay.getChildren().remove(arrow);
                });
            }
            else
                graphic = null;
        }

        @OnThread(Tag.FXPlatform)
        private void inspectResult()
        {
            Project project = frame.getProject();
            if (project != null)
            {
                project.getInspectorInstance(objInfo.obj, "", frame.getPackage(), objInfo.ir, frame.getWindow(), graphic).bringToFront();
            }
        }

        @OnThread(Tag.FXPlatform)
        private void addResultToBench()
        {
            Stage fxWindow = frame.getWindow();
            Point2D from = graphic.localToScene(new Point2D(0.0, 0.0));
            frame.getPackage().getEditor().raisePutOnBenchEvent(fxWindow, objInfo.obj, objInfo.obj.getGenType(), objInfo.ir, true, Optional.of(from));
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

        @Override
        public HistoryRow clone()
        {
            return new OutputSuccessRow(getText(), objInfo);
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public List<? extends AbstractOperation<HistoryRow>> getContextOperations()
        {
            ArrayList<AbstractOperation<HistoryRow>> ops = new ArrayList<>(super.getContextOperations());
            if (objInfo != null)
            {
                ops.add(new AbstractOperation<HistoryRow>("addtobench", Combine.ONE, null)
                {
                    @Override
                    public void activate(List<HistoryRow> historyRows)
                    {
                        for (HistoryRow historyRow : historyRows)
                        {
                            if (historyRow instanceof OutputSuccessRow)
                            {
                                OutputSuccessRow o = (OutputSuccessRow) historyRow;
                                if (o.objInfo != null)
                                    o.addResultToBench();
                            }
                        }
                    }

                    @Override
                    public List<ItemLabel> getLabels()
                    {
                        return List.of(new ItemLabel(new ReadOnlyStringWrapper(Config.getString("codepad.addToBench")), MenuItemOrder.CODEPAD_ADD_TO_BENCH));
                    }
                });
                ops.add(new AbstractOperation<HistoryRow>("inspect", Combine.ONE, null)
                {
                    @Override
                    public void activate(List<HistoryRow> historyRows)
                    {
                        for (HistoryRow historyRow : historyRows)
                        {
                            if (historyRow instanceof OutputSuccessRow)
                            {
                                OutputSuccessRow o = (OutputSuccessRow) historyRow;
                                if (o.objInfo != null)
                                    o.inspectResult();
                            }
                        }
                    }

                    @Override
                    public List<ItemLabel> getLabels()
                    {
                        return List.of(new ItemLabel(new ReadOnlyStringWrapper(Config.getString("codepad.inspect")), MenuItemOrder.CODEPAD_INSPECT));
                    }
                });
            }
            return ops;
        }
    }

    /**
     * A row with an error output of a previous command.
     */
    @OnThread(Tag.FX)
    private class ErrorRow extends IndentedRow
    {
        public ErrorRow(String text)
        {
            super(text);
        }

        @Override
        public RowStyle getStyle()
        {
            return RowStyle.ERROR;
        }

        @Override
        public HistoryRow clone()
        {
            return new ErrorRow(getText());
        }
    }

    private static final String nullLabel = "null";
    
    private static final String uninitializedWarning = Config.getString("pkgmgr.codepad.uninitialized");

    private static final Image objectImage =
            Config.getImageAsFXImage("image.eval.object");
    private static final Image objectImageHighlight =
            Config.getImageAsFXImage("image.eval.object");
    
    private final PkgMgrFrame frame;
    @OnThread(Tag.FX)
    private String currentCommand = "";
    @OnThread(Tag.FX)
    private IndexHistory history;
    private Invoker invoker = null;
    private TextAnalyzer textParser = null;
    
    // Keeping track of invocation
    private boolean firstTry;
    private boolean wrappedResult;
    private String errorMessage;

    private boolean busy = false;

    private List<CodepadVar> localVars = new ArrayList<CodepadVar>();
    private List<CodepadVar> newlyDeclareds;
    private List<String> autoInitializedVars;
    // The action which removes the hover state on the object icon
    private Runnable removeHover;

    public CodePad(PkgMgrFrame frame, Pane arrowOverlay)
    {
        this.frame = frame;
        this.arrowOverlay = arrowOverlay;
        JavaFXUtil.addStyleClass(this, "codepad");
        setMinWidth(100.0);
        inputField = new TextField();
        JavaFXUtil.addStyleClass(inputField, "codepad-input");
        inputField.setFocusTraversable(true);
        inputField.setEditable(true);
        historyView = new ListView<>();
        historyView.setEditable(false);
        JavaFXUtil.addFocusListener(historyView, focused -> {
            if (focused && historyView.getSelectionModel().isEmpty())
                historyView.getSelectionModel().selectLast();
        });

        inputField.styleProperty().bind(PrefMgr.getEditorFontCSS(PrefMgr.FontCSS.EDITOR_SIZE_ONLY));
        historyView.styleProperty().bind(PrefMgr.getEditorFontCSS(PrefMgr.FontCSS.EDITOR_SIZE_ONLY));

        Nodes.addInputMap(inputField, InputMap.sequence(
            InputMap.consume(EventPattern.keyPressed(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), e -> Utility.increaseFontSize(PrefMgr.getEditorFontSize())),
            InputMap.consume(EventPattern.keyPressed(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), e -> Utility.decreaseFontSize(PrefMgr.getEditorFontSize()))
        ));

        // We can't lookup the scroll bar until we're in the scene and showing.
        // But also, we don't care about showing the shadow until there are items in the history
        // to be scrolled.  So a neat solution is to add the effect the first time items
        // appear in the history (which can only happen once it's on screen).
        historyView.getItems().addListener(new ListChangeListener<HistoryRow>()
        {
            @Override
            @OnThread(Tag.FX)
            public void onChanged(Change<? extends HistoryRow> c)
            {
                // When the codepad history is not at the very bottom, add a shadow to indicate
                // that there is more beneath.  Otherwise, if you scroll to just the right point,
                // it looks like you are looking at the most recent item when in fact you're scrolled up.
                ScrollBar scrollBar = (ScrollBar)historyView.lookup(".scroll-bar");
                // Need to keep a permanent reference to avoid GCing weak reference:
                shadowShowing = scrollBar.visibleProperty().and(scrollBar.valueProperty().isNotEqualTo(1.0, 0.01));
                shadowShowBinding = Bindings.when(shadowShowing).<Effect>then(new DropShadow(6.0, 0.0, -3.0, Color.GRAY)).otherwise((Effect)null);
                inputField.effectProperty().bind(shadowShowBinding);
                historyView.getItems().removeListener(this);
            }
        });

        JavaFXUtil.addStyleClass(historyView, "codepad-history");

        getChildren().setAll(historyView, inputField);
        VBox.setVgrow(historyView, Priority.ALWAYS);

        //defineKeymap();
        history = new IndexHistory(20);

        historyView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Add context menu with copy:
        historyView.setOnContextMenuRequested(e -> {
            showContextMenu(e.getScreenX(), e.getScreenY());
        });
        
        // Allow "clicking off" the context menu (clicking back on the codepad history) to dismiss it.
        historyView.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && contextMenu != null)
            {
                contextMenu.hide();
                contextMenu = null;
                // Do not consume the event, still let it select.
            }
        });
        
        // Add keyboard shortcut ourselves:
        historyView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.C && e.isShortcutDown())
            {
                copySelectedRows();
                e.consume();
            }
            else if (e.getCode() == KeyCode.A && e.isShortcutDown())
            {
                historyView.getSelectionModel().selectAll();
                e.consume();
            }
            else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE)
            {
                Bounds screenBounds = historyView.localToScreen(historyView.getBoundsInLocal());
                showContextMenu(screenBounds.getCenterX(), screenBounds.getCenterY());
                e.consume();
            }
        });
        inputField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.UP)
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
        inputField.setOnAction(e -> {
            String line = inputField.getText();
            if (line.trim().isEmpty())
                return; // Don't allow entry of blank lines
            command(line, true);
            inputField.setEditable(false);    // don't allow input while we're thinking
            inputField.setText("");
            currentCommand = (currentCommand + line).trim();
            if(currentCommand.length() != 0)
            {
                history.add(line);
                String cmd = currentCommand;
                currentCommand = "";
                executeCommand(cmd);
            }
        });

        historyView.setCellFactory(lv -> new ListCell<HistoryRow>() {
            {
                JavaFXUtil.addStyleClass(this, "codepad-row");
            }

            @Override
            @OnThread(Tag.FX)
            public void updateItem(HistoryRow item, boolean empty)
            {
                super.updateItem(item, empty);
                if (!empty && item != null)
                {
                    setGraphic(item.getGraphic());
                    setText(item.getText());
                    JavaFXUtil.selectPseudoClass(this, Arrays.asList(allRowStyles).indexOf(item.getStyle().getPseudoClass()), allRowStyles);
                    JavaFXUtil.setPseudoclass("bj-codepad-terminated-after", item.lastRowBeforeReset, this);
                }
                else
                {
                    setGraphic(null);
                    setText("");
                    JavaFXUtil.selectPseudoClass(this, -1, allRowStyles);
                }
            }
        });
    }

    private void showContextMenu(double screenX, double screenY)
    {
        if (contextMenu != null)
            contextMenu.hide();
        contextMenu = AbstractOperation.getMenuItems(historyView.getSelectionModel().getSelectedItems(), true).makeContextMenu();
        contextMenu.show(historyView, screenX, screenY);
    }

    private void copySelectedRows()
    {
        // If they right click on background with no items selected,
        // copy all items:
        if (historyView.getSelectionModel().isEmpty())
            historyView.getSelectionModel().selectAll();
        String copied = historyView.getSelectionModel().getSelectedItems().stream().map(HistoryRow::getText).collect(Collectors.joining("\n"));
        Clipboard.getSystemClipboard().setContent(Collections.singletonMap(DataFormat.PLAIN_TEXT, copied));
        historyView.getSelectionModel().clearSelection();
    }

    /**
     * Clear the local variables.
     */
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
    public Iterator<CodepadVar> getValueIterator()
    {
        return localVars.iterator();
    }
    
    /*
     * @see bluej.debugmgr.ValueCollection#getNamedValue(java.lang.String)
     */
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
        @OnThread(Tag.FXPlatform)
        public void beginCompile()
        {
        }

        /*
         * @see bluej.debugmgr.ResultWatcher#beginExecution()
         */
        @Override
        @OnThread(Tag.FXPlatform)
        public void beginExecution(InvokerRecord ir)
        {
            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, ir, frame.getProject());
        }

        /*
         * @see bluej.debugmgr.ResultWatcher#putResult(bluej.debugger.DebuggerObject, java.lang.String, bluej.testmgr.record.InvokerRecord)
         */
        @Override
        @OnThread(Tag.FXPlatform)
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
                    error(warnLine);
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
                    output(resultString);
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
                        objectOutput(resultOutputString, new ObjectInfo(resultObject, ir));
                    }
                    else
                    {
                        String resultType = resultField.getType().toString(true);
                        String resultOutputString = resultString + "   (" + resultType + ")";
                        DataCollector.codePadSuccess(frame.getPackage(), ir.getOriginalCommand(), resultOutputString);
                        output(resultOutputString);
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
            inputField.setEditable(true);    // allow next input
            busy = false;
        }

        private void updateInspectors()
        {
            Project proj = frame.getPackage().getProject();
            proj.updateInspectors();
        }

        /**
         * An invocation has failed - here is the error message
         */
        @Override
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
        public void putVMTerminated(InvokerRecord ir, boolean terminatedByUserCode)
        {
            if (autoInitializedVars != null)
                autoInitializedVars.clear();

            removeNewlyDeclareds();


            String message = Config.getString("pkgmgr.codepad.vmTerminated");
            DataCollector.codePadError(frame.getPackage(), ir.getOriginalCommand(), message);
            error(message);

            completeExecution();
        }
    }
    
    /**
     * Remove the newly declared variables from the value collection.
     * (This is needed if compilation fails, or execution bombs with an exception).
     */
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
    private void showErrorMsg(final String message)
    {
        error("Error: " + message);
        completeExecution();
    }
    
    /**
     * Show an exception message, and allow further command input.
     */
    private void showExceptionMsg(final String message)
    {
        error("Exception: " + message);
        completeExecution();
    }
    
    /**
     * Execution of the current command has finished (one way or another).
     * Allow further command input.
     */
    private void completeExecution()
    {
        inputField.setEditable(true);
        busy = false;
    }

    /**
     * Record part of a command
     * @param s
     */
    private void command(String s, boolean isFinalLine)
    {
        addRow(new CommandRow(s, isFinalLine));
    }

    private void addRow(HistoryRow row)
    {
        historyView.getSelectionModel().clearSelection();
        historyView.getItems().add(row);
        historyView.scrollTo(historyView.getItems().size() - 1);
    }

    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    private void output(String s)
    {
        addRow(new OutputSuccessRow(s, null));
    }
    
    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    private void objectOutput(String s, ObjectInfo objInfo)
    {
        addRow(new OutputSuccessRow(s, objInfo));
    }
    
    /**
     * Write an error message to the text area.
     * @param s The message
     */
    private void error(String s)
    {
        addRow(new ErrorRow(s));
    }

    public void clear()
    {
        clearVars();
        if (!historyView.getItems().isEmpty())
        {
            int lastIndex = historyView.getItems().size() - 1;
            // We must replace the item with a copy, rather than changing
            // the flag in place in order to trigger a new layout of the ListView
            // component to display the line:
            historyView.getItems().set(lastIndex, historyView.getItems().get(lastIndex).asLastRowBeforeReset());
        }
    }

    /**
     * Clear the CodePad after closing the project that the only one is opened,
     * When opening a new project, the CodePad appears again and it is clear.
     */
    public void clearHistoryView()
    {
        historyView.getItems().clear();
    }

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
                    
                    // If they used var and we couldn't work out the type, give an error:
                    if (dv.getDeclaredType() == null)
                    {
                        showErrorMsg("Could not determine variable type");
                        removeNewlyDeclareds();
                        return;
                    }
                    
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
        String line = inputField.getText();
        if (line.trim().isEmpty())
            return; // Don't allow entry of blank lines
        currentCommand += line + " ";
        history.add(line);
        command(line, false);
        inputField.setText("");
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
        inputField.setText(line);
        // When going back in history, seems best to put cursor at the end of the field
        // but by default it gets put at the beginning when setting new text.:
        inputField.end();
    }

    private void historyForward()
    {
        String line = history.getNext();
        if(line != null) {
            setInput(line);
        }
    }

    public PkgMgrPane getHistoryPane()
    {
        return () -> historyView;
    }

    public PkgMgrPane getInputFieldPane()
    {
        return () -> inputField;
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
    
    private static class CopyOperation extends AbstractOperation<HistoryRow>
    {
        public CopyOperation()
        {
            super("copy", Combine.ALL, null);
        }

        @Override
        public void activate(List<HistoryRow> historyRows)
        {
            String copied = historyRows.stream().map(HistoryRow::getText).collect(Collectors.joining("\n"));
            Clipboard.getSystemClipboard().setContent(Collections.singletonMap(DataFormat.PLAIN_TEXT, copied));
        }
        
        @Override
        public List<ItemLabel> getLabels()
        {
            return List.of(new ItemLabel(new ReadOnlyStringWrapper(Config.getString("editor.copyLabel")), MenuItemOrder.CODEPAD_COPY));
        }
    }

    private class ClearOperation extends AbstractOperation<HistoryRow>
    {
        public ClearOperation()
        {
            super("clear", Combine.ANY, null);
        }

        @Override
        public void activate(List<HistoryRow> historyRows)
        {
            historyView.getSelectionModel().clearSelection();
            historyView.getItems().clear();
        }

        @Override
        public List<ItemLabel> getLabels()
        {
            return List.of(new ItemLabel(new ReadOnlyStringWrapper(Config.getString("codepad.clear")), MenuItemOrder.CODEPAD_CLEAR));
        }
    }

    private class SelectAllOperation extends AbstractOperation<HistoryRow>
    {
        public SelectAllOperation()
        {
            super("selectAll", Combine.ANY, null);
        }

        @Override
        public void activate(List<HistoryRow> historyRows)
        {
            historyView.getSelectionModel().selectAll();
        }

        @Override
        public List<ItemLabel> getLabels()
        {
            return List.of(new ItemLabel(new ReadOnlyStringWrapper(Config.getString("codepad.selectAll")), MenuItemOrder.CODEPAD_SELECT_ALL));
        }
    }
}
