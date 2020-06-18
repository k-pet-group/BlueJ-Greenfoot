/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.SourceLocation;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.utility.JavaNames;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
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
    @OnThread(Tag.FXPlatform)
    private static TestDisplayFrame singleton = null;
    @OnThread(Tag.FXPlatform)
    private static BooleanProperty frameShowing = new SimpleBooleanProperty(false);
    
    @OnThread(Tag.FXPlatform)
    public static TestDisplayFrame getTestDisplay()
    {
        if(singleton == null) {
            singleton = new TestDisplayFrame();
        }
        return singleton;
    }

    public static BooleanProperty showingProperty()
    {
        return frameShowing;
    }

    static
    {
        JavaFXUtil.addChangeListenerPlatform(frameShowing, doShow -> {
            TestDisplayFrame testDisplayFrame = getTestDisplay();

            // Note that we can end up here due to the window state changing,
            // so be careful not to end up in a loop by showing/hiding again:
            if (doShow)
            {
                if (!testDisplayFrame.frame.isShowing())
                {
                    testDisplayFrame.frame.show();
                }
                testDisplayFrame.frame.toFront();
            }
            else
            {
                if (testDisplayFrame.frame.isShowing())
                {
                    testDisplayFrame.frame.hide();
                }
            }
        });
    }

    private final Image failureIcon = Config.getFixedImageAsFXImage("failure.gif");
    private final Image errorIcon = Config.getFixedImageAsFXImage("error.gif");
    private final Image okIcon = Config.getFixedImageAsFXImage("ok.gif");

    /** The actual window */
    private Stage frame;

    /** The list of test results which is displayed in testNames */
    private ObservableList<DebuggerTestResult> testEntries;
    /** The list of test methods which is used to compute the progress status */
    private ObservableList<String> testEntriesMethodName;
    /** The top list of test names */
    private ListView<DebuggerTestResult> testNames;
    private ProgressBar progressBar;

    private final SimpleIntegerProperty errorCount;
    private final SimpleIntegerProperty failureCount;
    private final SimpleIntegerProperty totalTimeMs;
    private final SimpleIntegerProperty testTotal;
    /** Keeps track of whether we are running a single test
     * or multiple tests */
    private boolean doingMultiple;

    // Bindings passed to bindPseudoclass can get GCed, so we need to keep track
    // in a field even though they are trivial manipulations of the earlier properties:
    private BooleanBinding hasErrors;
    private BooleanBinding hasFailures;
    private BooleanBinding hasFailuresOrErrors;
        
    // private FailureDetailView fdv;
    /** The text field showing the exception message */
    private TextArea exceptionMessageField;
    private Button showSourceButton;

    private Project project;

    public TestDisplayFrame()
    {
        testTotal = new SimpleIntegerProperty(0);
        // add a listener on this value to update the progress bar accordingly.
        JavaFXUtil.addChangeListenerPlatform(testTotal, (n) -> updateProgressBar());

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
        frameShowing.set(doShow);
        if (doShow)
        {
            frame.toFront();
        }
    }

    /**
     * Create the user-interface for the error display dialog.
     */
    protected void createUI()
    {
        frame = new Stage();
        frame.setTitle(Config.getString("testdisplay.title"));
        frame.setOnShown(e -> {
            // Note that we can get here because of a change in the property,
            // so be sure not to end up in a loop by setting the property again:
            if (!frameShowing.get())
                frameShowing.set(true);
            /*org.scenicview.ScenicView.show(frame.getScene());*/
        });
        frame.setOnHidden(e -> {
            if (frameShowing.get())
                frameShowing.set(false);
        });

        BlueJTheme.setWindowIconFX(frame);

        frame.setMinWidth(500.0);
        frame.setMinHeight(250.0);

        Config.loadAndTrackPositionAndSize(frame, "bluej.testdisplay");

        SplitPane mainDivider = new SplitPane();
        mainDivider.setOrientation(Orientation.VERTICAL);

        testEntries = FXCollections.observableArrayList();
        testEntriesMethodName = FXCollections.observableArrayList();
        // add listener on this list to update the progress bar accordingly.
        testEntriesMethodName.addListener((ListChangeListener<String>) c -> updateProgressBar());

        testNames = new ListView();
        testNames.setMinHeight(50.0);
        testNames.setPrefHeight(150.0);
        testNames.setEditable(false);
        testNames.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        JavaFXUtil.addChangeListener(testNames.getSelectionModel().selectedItemProperty(), this::selected);
        testNames.setDisable(false);
        JavaFXUtil.addStyleClass(testNames, "test-names");
        testNames.setItems(testEntries);
        testNames.setCellFactory(col -> new TestResultCell());
        
        mainDivider.getItems().add(testNames);
        VBox content = new VBox();
        mainDivider.getItems().add(content);
        // Must call this after setting children so that divider exists:
        Config.rememberDividerPosition(frame, mainDivider, "bluej.testdisplay.dividerpos");

        progressBar = new ProgressBar();
        progressBar.setProgress(0.0); // inital status of the bar --> 0%
        JavaFXUtil.addStyleClass(progressBar, "test-progress-bar");
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
        fNumberOfRuns.textProperty().bind(Bindings.size(testEntries).asString());
        fTotalTime.textProperty().bind(totalTimeMs.asString().concat("ms"));

        HBox errorPanel = new HBox(new ImageView(errorIcon), new Label(Config.getString("testdisplay.counter.errors")), fNumberOfErrors);
        JavaFXUtil.addStyleClass(errorPanel, "error-panel");
        hasErrors = Bindings.greaterThan(errorCount, 0);
        JavaFXUtil.bindPseudoclass(errorPanel, "bj-non-zero", hasErrors);
        HBox.setHgrow(errorPanel, Priority.ALWAYS);

        HBox failurePanel = new HBox(new ImageView(failureIcon), new Label(Config.getString("testdisplay.counter.failures")), fNumberOfFailures);
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
        // If in accessible mode, allow editing the exception message, which permits better keyboard navigation
        // (even if we don't really want the user to be able to edit)
        exceptionMessageField.editableProperty().bind(PrefMgr.flagProperty(PrefMgr.ACCESSIBILITY_SUPPORT));
        // exceptionMessageField.setLineWrap(true);

        content.getChildren().add(exceptionMessageField);

        // "show source" and "close" buttons
        showSourceButton = new Button(Config.getString("testdisplay.showsource"));
        showSourceButton.setOnAction(e -> showSource(testNames.getSelectionModel().getSelectedItem()));
        showSourceButton.setDisable(true);
        JavaFXUtil.addStyleClass(showSourceButton, "test-show-source");

        Button closeButton = new Button(Config.getString("close"));
        closeButton.setOnAction(e -> frame.hide());
            
        // Panel for "show source" and "close" buttons
        BorderPane buttonPanel = new BorderPane();
        buttonPanel.setLeft(showSourceButton);
        buttonPanel.setRight(closeButton);
            
        content.getChildren().add(buttonPanel);
        VBox surround = new VBox(mainDivider);
        VBox.setVgrow(mainDivider, Priority.ALWAYS);
        JavaFXUtil.addStyleClass(surround, "test-results");
        JavaFXUtil.addStyleClass(content, "test-results-content");
        frame.setScene(new Scene(surround));
        Config.addTestsStylesheets(frame.getScene());
        
        surround.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE)
            {
                frame.hide();
                e.consume();
            }
        });
    }

    /**
     * Updates the progress value of the progress bar as a ratio between
     * - the number of currently completed test methods,
     * - the total number of test methods for the tests run.
     */
    private void updateProgressBar()
    {
        if (progressBar != null)
        {
            progressBar.setProgress(testEntriesMethodName.size() / Math.max(1.0, testTotal.getValue()));
        }
    }

    protected void reset()
    {
        testEntries.clear();
        testEntriesMethodName.clear();
        
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
    public void startMultipleTests(Project proj, int num)
    {
        this.project = proj;
        proj.setTestMode(true);
        doingMultiple = true;
        
        reset();
        testTotal.set(num);
        showTestDisplay(true);
    }
    
    public void endMultipleTests()
    {
        doingMultiple = false;
        project.setTestMode(false);
    }  

    /**
     * Tell the dialog we are about to start a test run.
     * 
     * @param num   the number of tests we will run
     */
    public void startTest(Project proj, int num)
    {
        this.project = proj;
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

        // Update the list of the test methods for this test if the method isn't already listed.
        if (!testEntriesMethodName.contains(dtr.getQualifiedMethodName()))
        {
            testEntriesMethodName.add(dtr.getQualifiedMethodName());
        }
    }

    public Window getWindow()
    {
        return frame;
    }

    @OnThread(Tag.FX)
    private class TestResultCell extends ListCell<DebuggerTestResult>
    {
        private final ImageView imageView;

        public TestResultCell()
        {
            setEditable(false);
            setText("");
            imageView = new ImageView();
            imageView.setMouseTransparent(true);
            setGraphic(imageView);
            setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY)
                    showSource(getItem());
            });
        }

        @Override
        public void updateItem(DebuggerTestResult item, boolean empty)
        {
            super.updateItem(item, empty);
            if (item == null || empty)
            {
                imageView.setImage(null);
                setText("");
                setTooltip(null);
                setAccessibleText("");
            }
            else
            {
                String acc;
                if (item.isSuccess())
                {
                    imageView.setImage(okIcon);
                    acc = "Pass ";
                }
                else if (item.isFailure())
                {
                    imageView.setImage(failureIcon);
                    acc = "Fail ";
                }
                else
                {
                    imageView.setImage(errorIcon);
                    acc = "Error ";
                }
                
                // This checks if the JUnit executes all tests at the same time,
                // We have used zero execution time for individual test as there is no way so
                // far to extract the runtime of individual test.
                if (item.getRunTimeMs() == 0)
                {
                    setText(item.getQualifiedClassName() + "." + item.getMethodName());
                }
                else
                {
                    setText(item.getQualifiedClassName() + item.getMethodName() + " (" + item.getRunTimeMs() + "ms)");
                }

                // Add a tooltip on the entry (display name)
                Tooltip displayNameToolTip = new Tooltip(item.getDisplayName());
                JavaFXUtil.addStyleClass(displayNameToolTip, "test-results-tooltip");
                setTooltip(displayNameToolTip);
                acc += item.getDisplayName();
                setAccessibleText(acc);
                setAccessibleRole(AccessibleRole.LIST_ITEM);
                // It's not clear why but at least on Mac, the screen-reader reads out the image not the
                // list cell, even though it's the list cell that is focused.  We work around this by copying
                // our accessible text on to the image view so that it still gets read out:
                imageView.setAccessibleText(acc);
                imageView.setAccessibleRole(AccessibleRole.LIST_ITEM);
            }
        }
    }
    
    private void selected(DebuggerTestResult dtr)
    {
        if (dtr != null && (dtr.isError() || dtr.isFailure())) {
            // fdv.showFailure(dtr.getExceptionMessage() + "\n---\n" + dtr.getTrace());
            exceptionMessageField.setText(dtr.getExceptionMessage()
                + "\n---\n" + dtr.getTrace());
            exceptionMessageField.positionCaret(0);

            showSourceButton.setDisable(dtr.getExceptionLocation() == null);
        } else {
            exceptionMessageField.setText("");
            showSourceButton.setDisable(true);
        }
    }

    /**
     * Increments the total execution time of tests.
     * @param value the value to which the totalTimeMs variable will be incremented by.
     */
    @OnThread(Tag.FXPlatform)
    public void updateTotalTimeMs(int value)
    {
       totalTimeMs.set(totalTimeMs.get() + value);
    }

    private void showSource(DebuggerTestResult dtr)
    {
        if ((dtr != null) && (dtr.isError() || dtr.isFailure()))
        {
            SourceLocation exceptionLocation = dtr.getExceptionLocation();

            if (exceptionLocation == null)
            {
                return;
            }

            String packageName = JavaNames.getPrefix(exceptionLocation.getClassName());

            Package spackage = project.getPackage(packageName);

            if (spackage == null)
            {
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