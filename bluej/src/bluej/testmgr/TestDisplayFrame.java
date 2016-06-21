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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.SourceLocation;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.utility.JavaNames;
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

    private Stage frame;

    private ObservableList<DebuggerTestResult> testEntries;
    private ListView<DebuggerTestResult> testNames;
    private ProgressBar progressBar;

    private CounterPanel counterPanel;
    private int errorCount;
    private int failureCount;
    private int totalTimeMs;
    private int testTotal;
    private boolean doingMultiple;
        
    // private FailureDetailView fdv;
    private TextArea exceptionMessageField;
    private Button showSourceButton;

    @OnThread(Tag.Any)
    private final AtomicBoolean frameShowing = new AtomicBoolean(false);

    private Project lastProject;
    
    public TestDisplayFrame()
    {
        testTotal = 0;
        errorCount = 0;
        failureCount = 0;
        totalTimeMs = 0;
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
        frame.setOnShown(e -> {frameShowing.set(true);});
        frame.setOnHidden(e -> {frameShowing.set(false);});

        Image icon = BlueJTheme.getIconImageFX();
        if (icon != null) {
            frame.getIcons().add(BlueJTheme.getIconImageFX());
        }

        Config.rememberPosition(frame, "bluej.testdisplay");

        VBox content = new VBox();

        testEntries = FXCollections.observableArrayList();
        testNames = new ListView();
        testNames.setItems(testEntries);
        //testNames.setCellRenderer(new MyCellRenderer());
        //testNames.addListSelectionListener(new MyListSelectionListener());
        //testNames.addMouseListener(new ShowSourceListener());

        content.getChildren().add(testNames);

        progressBar = new ProgressBar();
        content.getChildren().add(progressBar);

        counterPanel = new CounterPanel();
        content.getChildren().add(counterPanel);

        // exception message field (text area)
        exceptionMessageField = new TextArea("");
        exceptionMessageField.setEditable(false);
        // exceptionMessageField.setLineWrap(true);
        exceptionMessageField.setFocusTraversable(false);

        ScrollPane exceptionScrollPane = new ScrollPane(exceptionMessageField);
        content.getChildren().add(exceptionScrollPane);

        // "show source" and "close" buttons
        showSourceButton = new Button(Config.getString("testdisplay.showsource"));
        //showSourceButton.setOnAction(new ShowSourceListener());

        Button closeButton = new Button(Config.getString("close"));
        closeButton.setOnAction(e -> frame.hide());
            
        // Panel for "show source" and "close" buttons
        Pane buttonPanel = new HBox();
        buttonPanel.getChildren().addAll(showSourceButton, closeButton);
            
        content.getChildren().add(buttonPanel);
        frame.setScene(new Scene(content));
    }

    protected void reset()
    {
        testEntries.clear();
        
        errorCount = 0;
        failureCount = 0;
        totalTimeMs = 0;
        testTotal = 0;   

        exceptionMessageField.setText("");
        showSourceButton.setDisable(true);
        progressBar.reset();
        counterPanel.setTotal(0);
        counterPanel.setErrorValue(0);
        counterPanel.setFailureValue(0);
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
        testTotal = num;
        counterPanel.setTotal(testTotal);
        progressBar.setMaximum(testTotal);  
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
        lastProject = project;

        if (! doingMultiple) {
            reset();
            testTotal = num;
            counterPanel.setTotal(testTotal);
            progressBar.setMaximum(testTotal);  
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
                ++failureCount;
            else
                ++errorCount;
        }

        totalTimeMs += dtr.getRunTimeMs();
        
        testEntries.add(dtr);
        progressBar.step(testEntries.size(), dtr.isSuccess());
        
        counterPanel.setTotalTime(totalTimeMs);
        counterPanel.setFailureValue(failureCount);
        counterPanel.setErrorValue(errorCount);
        counterPanel.setRunValue(testEntries.size());
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