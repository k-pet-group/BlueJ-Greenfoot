/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.testmgr;

import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerTestResult;
import bluej.pkgmgr.Project;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A JavaFX based user interface to run tests.
 *
 * @author  Andrew Patterson
 */
public @OnThread(Tag.FXPlatform) class TestDisplayFrame
{
    // -- static singleton factory method --
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private static TestDisplayFrame singleton = null;
    
    @OnThread(Tag.FXPlatform)
    public synchronized static TestDisplayFrame getTestDisplay()
    {
        if(singleton == null) {
            singleton = new TestDisplayFrame();
        }
        return singleton;
    }

    @OnThread(Tag.Any)
    public synchronized static boolean isFrameShown()
    {
        if(singleton == null) {
            return false;
        }
        else {
            return singleton.isShown();
        }
    }

    private final Image fFailureIcon = Config.getFixedImageAsFXImage("failure.gif");
    private final Image fErrorIcon = Config.getFixedImageAsFXImage("error.gif");

    private Stage frame;

    private ObservableList<DebuggerTestResult> testEntries;
    private ListView<DebuggerTestResult> testNames;
    private ProgressBar progressBar;

    private final SimpleIntegerProperty errorCount;
    private final SimpleIntegerProperty failureCount;
    private final SimpleIntegerProperty totalTimeMs;
    private final SimpleIntegerProperty testTotal;
    private boolean doingMultiple;

    // Bindings passed to bindPseudoclass can get GCed, so we need to keep track
    // in a field even though they are trivial manipulations of the earlier properties:
    private BooleanBinding hasErrors;
    private BooleanBinding hasFailures;
    private BooleanBinding hasFailuresOrErrors;
        
    // private FailureDetailView fdv;
    private TextArea exceptionMessageField;
    private Button showSourceButton;

    @OnThread(Tag.Any)
    private final AtomicBoolean frameShowing = new AtomicBoolean(false);

    public TestDisplayFrame()
    {
        testTotal = new SimpleIntegerProperty(0);
        errorCount = new SimpleIntegerProperty(0);
        failureCount = new SimpleIntegerProperty(0);
        totalTimeMs = new SimpleIntegerProperty(0);
        doingMultiple = false;

        createUI();
    }

    /**
     * Show or hide the test display window.
     */
    public void showTestDisplay(boolean doShow)
    {
        if (doShow)
            frame.show();
        else
            frame.hide();
    }

    /**
     * Return true if the window is currently displayed.
     */
    @OnThread(Tag.Any)
    public boolean isShown()
    {
        return frameShowing.get();
    }
    
    /**
     * Create the user-interface for the error display dialog.
     */
    protected void createUI()
    {
        frame = new Stage();
        frame.setTitle(Config.getString("testdisplay.title"));
        frame.setOnShown(e -> {frameShowing.set(true);/*org.scenicview.ScenicView.show(frame.getScene());*/});
        frame.setOnHidden(e -> {frameShowing.set(false);});

        BlueJTheme.setWindowIconFX(frame);

        Config.rememberPosition(frame, "bluej.testdisplay");

        VBox content = new VBox();

        testEntries = FXCollections.observableArrayList();
        testNames = new ListView();
        JavaFXUtil.addStyleClass(testNames, "test-names");
        testNames.setItems(testEntries);
        //testNames.setCellRenderer(new MyCellRenderer());
        //testNames.addListSelectionListener(new MyListSelectionListener());
        //testNames.addMouseListener(new ShowSourceListener());

        content.getChildren().add(testNames);

        progressBar = new ProgressBar();
        JavaFXUtil.addStyleClass(progressBar, "test-progress-bar");
        progressBar.progressProperty().bind(Bindings.add(0.0, Bindings.size(testEntries)).divide(Bindings.max(1, testTotal)));
        hasFailuresOrErrors = Bindings.greaterThan(failureCount.add(errorCount), 0);
        JavaFXUtil.bindPseudoclass(progressBar, "bj-error", hasFailuresOrErrors);
        content.getChildren().add(progressBar);

        HBox counterPanel = new HBox();
        JavaFXUtil.addStyleClass(counterPanel, "counter-panel");
        Label fNumberOfErrors = new Label();
        Label fNumberOfFailures = new Label();
        Label fNumberOfRuns = new Label();
        Label fTotalTime = new Label();

        HBox.setHgrow(fNumberOfRuns, Priority.ALWAYS);
        HBox.setHgrow(fTotalTime, Priority.ALWAYS);

        fNumberOfErrors.textProperty().bind(errorCount.asString());
        fNumberOfFailures.textProperty().bind(failureCount.asString());
        fNumberOfRuns.textProperty().bind(Bindings.size(testEntries).asString().concat("/").concat(testTotal.asString()));
        fTotalTime.textProperty().bind(totalTimeMs.asString().concat("ms"));

        HBox errorPanel = new HBox(new Label(Config.getString("testdisplay.counter.errors")), fNumberOfErrors);
        JavaFXUtil.addStyleClass(errorPanel, "error-panel");
        hasErrors = Bindings.greaterThan(errorCount, 0);
        JavaFXUtil.bindPseudoclass(errorPanel, "bj-non-zero", hasErrors);
        HBox.setHgrow(errorPanel, Priority.ALWAYS);

        HBox failurePanel = new HBox(new Label(Config.getString("testdisplay.counter.failures")), fNumberOfFailures);
        JavaFXUtil.addStyleClass(failurePanel, "error-panel");
        // Need to keep a reference to avoid GC:
        hasFailures = Bindings.greaterThan(failureCount, 0);
        JavaFXUtil.bindPseudoclass(failurePanel, "bj-non-zero", hasFailures);
        HBox.setHgrow(failurePanel, Priority.ALWAYS);
        
        counterPanel.getChildren().addAll(
                new Label(Config.getString("testdisplay.counter.runs")),
                fNumberOfRuns,
                errorPanel,
                failurePanel,
                new Label(Config.getString("testdisplay.counter.totalTime")),
                fTotalTime
        );
        content.getChildren().add(counterPanel);

        // exception message field (text area)
        exceptionMessageField = new TextArea("");
        JavaFXUtil.addStyleClass(exceptionMessageField, "test-output");
        VBox.setVgrow(exceptionMessageField, Priority.ALWAYS);
        exceptionMessageField.setEditable(false);
        // exceptionMessageField.setLineWrap(true);
        exceptionMessageField.setFocusTraversable(false);

        content.getChildren().add(exceptionMessageField);

        // "show source" and "close" buttons
        showSourceButton = new Button(Config.getString("testdisplay.showsource"));
        //showSourceButton.setOnAction(new ShowSourceListener());

        Button closeButton = new Button(Config.getString("close"));
        closeButton.setOnAction(e -> frame.hide());
            
        // Panel for "show source" and "close" buttons
        BorderPane buttonPanel = new BorderPane();
        buttonPanel.setLeft(showSourceButton);
        buttonPanel.setRight(closeButton);
            
        content.getChildren().add(buttonPanel);
        JavaFXUtil.addStyleClass(content, "test-results");
        frame.setScene(new Scene(content));
        Config.addTestsStylesheets(frame.getScene());
    }

    protected void reset()
    {
        testEntries.clear();
        
        errorCount.set(0);
        failureCount.set(0);
        totalTimeMs.set(0);
        testTotal.set(0);

        exceptionMessageField.setText("");
        showSourceButton.setDisable(true);
    }
    
    /**
     * Indicate that we are starting a bunch of tests.
     * 
     * @param num  The total number of tests to be run
     */
    public void startMultipleTests(int num)
    {
        doingMultiple = true;    
        
        reset();
        testTotal.set(num);
        showTestDisplay(true);
    }
    
    public void endMultipleTests()
    {
        doingMultiple = false;
    }  

    /**
     * Tell the dialog we are about to start a test run.
     * 
     * @param num   the number of tests we will run
     */
    public void startTest(Project project, int num)
    {
        if (! doingMultiple) {
            reset();
            testTotal.set(num);
        }
    }

    /**
     * Add a test result to the test displayer.
     * 
     * @param dtr  The test result to add
     * @param quiet  True if the result should be added "quietly" (do not make
     *               test frame visible or bring it to front)
     */
    public void addResult(DebuggerTestResult dtr, boolean quiet)
    {
        addResultQuietly(dtr);

        if (! quiet) {
            showTestDisplay(true);
        }
    }

    /**
     * Add a test result to the test displayer but do not
     * bring the test display window to the front.
     * 
     * @param dtr  The test result to add
     */ 
    public void addResultQuietly(final DebuggerTestResult dtr)
    {
        if (!dtr.isSuccess()) {
            if (dtr.isFailure())
                failureCount.set(failureCount.get() + 1);
            else
                errorCount.set(errorCount.get() + 1);
        }

        totalTimeMs.set(totalTimeMs.get() + dtr.getRunTimeMs());
        testEntries.add(dtr);
    }
    /*
    class MyListSelectionListener implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            if (testnames.getSelectedValue() != null) {
                DebuggerTestResult dtr = (DebuggerTestResult) testnames.getSelectedValue();

                if (dtr.isError() || dtr.isFailure()) {
                    // fdv.showFailure(dtr.getExceptionMessage() + "\n---\n" + dtr.getTrace());
                    exceptionMessageField.setText(dtr.getExceptionMessage()
                            + "\n---\n" + dtr.getTrace());
                    exceptionMessageField.setCaretPosition(0);

                    // Set the column count to a small number; the text area
                    // will use the available space anyway, and this prevents
                    // unncessary horizontal scrollbar from appearing
                    exceptionMessageField.setColumns(1);
                    showSourceButton.setEnabled(dtr.getExceptionLocation() != null);
                } else {
                    exceptionMessageField.setText("");
                    showSourceButton.setEnabled(false);
                }
            }
        }
    }
    
    class ShowSourceListener extends MouseAdapter implements ActionListener
    {
        public void mouseClicked(MouseEvent e)
        {
            int cc = e.getClickCount();
            if (cc == 2) {
                showSource();
            }
        }
        
        public void actionPerformed(ActionEvent e)
        {
            showSource();
        }

        
        private void showSource()
        {
            DebuggerTestResult dtr = (DebuggerTestResult) testnames.getSelectedValue();

            if ((dtr != null) && (dtr.isError() || dtr.isFailure())) {
                SourceLocation exceptionLocation = dtr.getExceptionLocation();

                if (exceptionLocation == null) {
                    return;
                }

                String packageName = JavaNames.getPrefix(exceptionLocation.getClassName());

                Package spackage = lastProject.getPackage(packageName);

                if (spackage == null) {
                    return;
                }

                // We have the package name. Now get the source name and
                // line number.
                String sourceName = exceptionLocation.getFileName();
                int lineno = exceptionLocation.getLineNumber();

                spackage.showSource(sourceName, lineno);
            }
        }
    }

    private static class MyCellRenderer extends JLabel implements ListCellRenderer
    {
        final static Icon errorIcon = Config.getFixedImageAsIcon("error.gif");
        final static Icon failureIcon = Config.getFixedImageAsIcon("failure.gif");
        final static Icon okIcon = Config.getFixedImageAsIcon("ok.gif");
    
        // This is the only method defined by ListCellRenderer.
        // We just reconfigure the JLabel each time we're called.
        public Component getListCellRendererComponent(
                JList list,
                Object value,            // value to display
                int index,               // cell index
                boolean isSelected,      // is the cell selected
                boolean cellHasFocus)    // the list and the cell have the focus
        {
            if (value instanceof DebuggerTestResult) {
                DebuggerTestResult dtr = (DebuggerTestResult) value;
                setText(dtr.getName() + " (" + dtr.getRunTimeMs() + "ms)");
                setIcon((dtr.isSuccess()) ? okIcon : (dtr.isFailure() ? failureIcon : errorIcon));
            } else {
                setText(value.toString());
            }
    
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                setOpaque(true);
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                setOpaque(false);
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
    
            return this;
        }
    }
    */

}