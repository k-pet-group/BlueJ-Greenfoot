/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014  Michael Kolling and John Rosenberg 
 
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.SourceLocation;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.utility.GradientFillPanel;
import bluej.utility.JavaNames;
import java.awt.Image;

/**
 * A Swing based user interface to run tests.
 *
 * @author  Andrew Patterson
 */
public class TestDisplayFrame
{
    // -- static singleton factory method --

    static TestDisplayFrame singleton = null;

    public synchronized static TestDisplayFrame getTestDisplay()
    {
        if(singleton == null) {
            singleton = new TestDisplayFrame();
        }
        return singleton;
    }

    public static boolean isFrameShown()
    {
        if(singleton == null) {
            return false;
        }
        else {
            return singleton.isShown();
        }
    }

    private JFrame frame;
    private DefaultListModel testEntries;

    private JList testnames;
    private ProgressBar progressBar;
    private GridBagConstraints pbConstraints;
    private JPanel statusLabel;
    private JPanel bottomPanel;
    
    // index of the progress bar in the topPanel's components
    private final static int PROGRESS_BAR_INDEX = 0;
    
    private CounterPanel counterPanel;
    private int errorCount;
    private int failureCount;
    private int totalTimeMs;
    private int testTotal;
    private boolean doingMultiple;
        
    // private FailureDetailView fdv;
    private JTextArea exceptionMessageField;
    private JButton showSourceButton;
    
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
        frame.setVisible(doShow);
    }

    /**
     * Return true if the window is currently displayed.
     */
    public boolean isShown()
    {
        return frame.isShowing();
    }
    
    /**
     * Create the user-interface for the error display dialog.
     */
    protected void createUI()
    {
        frame = new JFrame(Config.getString("testdisplay.title"));
        if (! Config.isRaspberryPi()) {
            frame.setContentPane(new GradientFillPanel(frame.getContentPane().getLayout()));
        }else{
            frame.setContentPane(new JPanel(frame.getContentPane().getLayout()));
        }

        Image icon = BlueJTheme.getIconImage();
        if (icon != null) {
            frame.setIconImage(icon);
        }
        frame.setLocation(Config.getLocation("bluej.testdisplay"));

        // save position when window is moved
        frame.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent event)
            {
                Config.putLocation("bluej.testdisplay", frame.getLocation());
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(BlueJTheme.generalBorder);
        splitPane.setResizeWeight(0.5);
        if (!Config.isRaspberryPi()) splitPane.setOpaque(false);
        
        JScrollPane resultScrollPane = new JScrollPane();
        {
            testEntries = new DefaultListModel();
            testnames = new JList(testEntries);
            testnames.setCellRenderer(new MyCellRenderer());
            testnames.addListSelectionListener(new MyListSelectionListener());
            testnames.addMouseListener(new ShowSourceListener());
                        
            resultScrollPane.setViewportView(testnames);
        }
        splitPane.setTopComponent(resultScrollPane);
        
        bottomPanel = new JPanel();
        if (!Config.isRaspberryPi()) bottomPanel.setOpaque(false);
        {
            bottomPanel.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1.0;
            constraints.weighty = 0;
            constraints.gridx = 0;
            
            progressBar = new ProgressBar();
            bottomPanel.add(progressBar, constraints);
            
            bottomPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth), constraints);
            
            counterPanel = new CounterPanel();
            if (!Config.isRaspberryPi()) counterPanel.setOpaque(false);
            bottomPanel.add(counterPanel, constraints);
            bottomPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth), constraints);
        
            // exception message field (text area)
            exceptionMessageField = new JTextArea("");
            exceptionMessageField.setEditable(false);
            exceptionMessageField.setRows(6);
            exceptionMessageField.setColumns(42);
            // exceptionMessageField.setLineWrap(true);
            exceptionMessageField.setFocusable(false);
            
            Dimension size = exceptionMessageField.getPreferredSize();
            // size.width = exceptionMessageField.getMaximumSize().width;
            // exceptionMessageField.setPreferredSize(size);
            size.width = exceptionMessageField.getMinimumSize().width;
            exceptionMessageField.setMinimumSize(size);
            JScrollPane exceptionScrollPane = new JScrollPane(exceptionMessageField);
            exceptionScrollPane.setMinimumSize(size);

            // "show source" and "close" buttons
            showSourceButton = new JButton(Config.getString("testdisplay.showsource"));
            showSourceButton.addActionListener(new ShowSourceListener());
            
            JButton closeButton = new JButton(Config.getString("close"));
            closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                }
            });
            
            // Panel for "show source" and "close" buttons
            JPanel buttonPanel = new JPanel();
            if (!Config.isRaspberryPi()) buttonPanel.setOpaque(false);
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(showSourceButton);
            buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(closeButton);
            
            constraints.weighty = 1.0;
            bottomPanel.add(exceptionScrollPane, constraints);
            constraints.weighty = 0;
            bottomPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth), constraints);
            bottomPanel.add(buttonPanel, constraints);

            constraints.gridy = PROGRESS_BAR_INDEX;
            pbConstraints = constraints;
        }
        splitPane.setBottomComponent(bottomPanel);
        
        frame.getContentPane().add(splitPane);
        frame.pack();
    }

    protected void reset()
    {
        testEntries.clear();
        
        errorCount = 0;
        failureCount = 0;
        totalTimeMs = 0;
        testTotal = 0;   

        exceptionMessageField.setText("");
        showSourceButton.setEnabled(false);
        progressBar.reset();
        counterPanel.setTotal(0);
        counterPanel.setErrorValue(0);
        counterPanel.setFailureValue(0);
        
        bottomPanel.remove(PROGRESS_BAR_INDEX);
        bottomPanel.add(progressBar, pbConstraints, PROGRESS_BAR_INDEX);
        bottomPanel.validate();
        progressBar.repaint();
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
        setResultLabel();
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
        
        testEntries.addElement(dtr);
        progressBar.step(testEntries.getSize(), dtr.isSuccess());
        
        counterPanel.setTotalTime(totalTimeMs);
        counterPanel.setFailureValue(failureCount);
        counterPanel.setErrorValue(errorCount);
        counterPanel.setRunValue(testEntries.getSize());

        if (!doingMultiple &&
                (progressBar.getValue() == progressBar.getMaximum())) {
            setResultLabel();
        }
    }
    
    /**
     * Change the progress bar into a red or green label, depending on
     * success/failure status. Should be called on the swing event thread.
     */
    private void setResultLabel()
    {
        statusLabel = new JPanel();

        if ((errorCount + failureCount) == 0) {
            statusLabel.setBackground(ProgressBar.greenBarColour);
        } else {
            statusLabel.setBackground(ProgressBar.redBarColour);
        }

        statusLabel.setMinimumSize(progressBar.getMinimumSize());
        statusLabel.setMaximumSize(progressBar.getMaximumSize());
        statusLabel.setPreferredSize(progressBar.getSize());
        statusLabel.setOpaque(true);
        bottomPanel.remove(PROGRESS_BAR_INDEX);
        bottomPanel.add(statusLabel, pbConstraints, PROGRESS_BAR_INDEX);
        bottomPanel.validate();
        statusLabel.repaint();
    }

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

                spackage.showSource(sourceName, lineno, "", false);
            }
        }
    }
}

class MyCellRenderer extends JLabel implements ListCellRenderer
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
