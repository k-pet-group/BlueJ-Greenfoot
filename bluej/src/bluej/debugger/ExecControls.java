package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 ** @version $Id: ExecControls.java 599 2000-06-28 05:43:18Z mik $
 ** @author Michael Kolling
 **
 ** Window for controlling the debugger
 **/
public class ExecControls extends JFrame 

    implements ActionListener, ListSelectionListener
{
    private static final String windowTitle = 
        Config.getString("debugger.execControls.windowTitle");
    private static final String stackTitle = 
        Config.getString("debugger.execControls.stackTitle");
    private static final String instanceTitle = 
        Config.getString("debugger.execControls.instanceTitle");
    private static final String localTitle = 
        Config.getString("debugger.execControls.localTitle");
    private static final String threadTitle = 
        Config.getString("debugger.execControls.threadTitle");
    private static final String updateText = 
        Config.getString("debugger.execControls.updateText");
    private static final String closeText = 
        Config.getString("close");
    private static final String systemThreadText = 
        Config.getString("debugger.execControls.systemThreads");

    private static String[] empty = new String[0];

    // === static factory ===

    /** the debugger control window */
    private static ExecControls execCtrlWindow = null;

    /**
     * getExecControls - return the Execution Control window.
     */
    public static ExecControls getExecControls()
    {
        if(execCtrlWindow == null) {
            execCtrlWindow = new ExecControls();
            //DialogManager.centreWindow(execCtrlWindow, this);
        }
        return execCtrlWindow;
    }

    public static boolean execControlsShown()
    {
        return (execCtrlWindow != null && execCtrlWindow.isShowing());
    }

    /**
     * Show or hide the exec control window.
     */
    public static void showHide(boolean show, boolean update, 
                                DebuggerThread thread)
    {
        getExecControls().setVisible(show);
        if(show && update)
            getExecControls().updateThreads(thread);
    }

    // === instance ===

    private JList threadList;
    private JList stackList, instanceList, localList;
    private JButton stopButton, stepButton, stepIntoButton, continueButton, 
        terminateButton;
    private JButton updateButton, closeButton;
    private JCheckBox showSystemThreads;

    private List threads;
    private DebuggerThread selectedThread;	// the thread currently
    //  selected
    private DebuggerObject currentObject;	// the "this" object for the
    //  selected stack frame
    private int currentFrame = 0;		// currently selected frame

    private ExecControls()
    {
        super(windowTitle);
		
        threads = new ArrayList();
        createWindow();
    }

    // ----- ActionListener interface -----

    public void actionPerformed(ActionEvent event)
    {
        Object obj = event.getSource();
        int machineStatus = Debugger.debugger.getStatus();

        if(obj == updateButton) {
            updateThreads(selectedThread);
        }
        else if(obj == closeButton) {
            setVisible(false);
        }
        else if(obj == showSystemThreads) {
            updateThreads(selectedThread);
        }
        else if(obj == stopButton && machineStatus == Debugger.RUNNING) {
            Debugger.debugger.halt(selectedThread);
            updateThreads(selectedThread);
        }
        else if(obj==continueButton && machineStatus==Debugger.SUSPENDED) {
            Debugger.debugger.cont();
            updateThreads(selectedThread);
        }
        else if(selectedThread != null) {
            if(obj == stepButton && machineStatus == Debugger.SUSPENDED) {
                selectedThread.step();
            }
            else if(obj==stepIntoButton && machineStatus==Debugger.SUSPENDED) {
                selectedThread.stepInto();
            }
            else if(obj == terminateButton && machineStatus != Debugger.IDLE) {
                selectedThread.terminate();
            }
        }
        else
            Debug.message("no thread selected...");
    }
	
    // ----- ListSelectionListener interface -----

    /**
     *  A list item was selected. This can be either in the thread list,
     *  the stack list, or one of the variable lists.
     */
    public void valueChanged(ListSelectionEvent event)
    {
        if(event.getValueIsAdjusting())  // ignore mouse down, dragging, etc.
            return;

        Object src = event.getSource();

        if(src == threadList) {
            clearThreadDetails();
            selectThread(threadList.getSelectedIndex());
        }
        else if(src == stackList) {
            selectStackFrame(stackList.getSelectedIndex());
        }

        // instanceList and localList are ignored - single click doesn't do
        // anything
    }

    // ----- end of ListSelectionListener interface -----

    public void listDoubleClick(MouseEvent event)
    {
        Component src = event.getComponent();

        if(src == instanceList) {
            viewInstanceField(instanceList.getSelectedIndex());
        }
        else if(src == localList) {
            viewLocalVar(localList.getSelectedIndex());
        }
    }

    public synchronized void updateThreads(DebuggerThread select)
    {
        int machineStatus = Debugger.debugger.getStatus();

        if(machineStatus == Debugger.RUNNING) {
            clearThreadDetails();
        }
        else {

            int selectionIndex = -1;

            DefaultListModel listModel = (DefaultListModel)threadList.getModel();
            listModel.removeAllElements();

            threads = Debugger.debugger.listThreads();
            if(threads == null) {
                Debug.reportError("cannot get thread info!");
                listModel.addElement("(error: cannot list threads)");
            }
            else {
                threads = selectThreadsForDisplay(threads, select);
                String selectName = (select == null ? "" : select.getName());
                for(int i = 0; i < threads.size(); i++) {
                    DebuggerThread thread = (DebuggerThread)threads.get(i);
                    if(thread.getName().equals(selectName))
                        selectionIndex = i;
                    String status = thread.getStatus();
                    listModel.addElement(thread.getName() + " [" + status + "]");
                }
            }
            if(listModel.getSize() > 0) {
                if(selectionIndex != -1) {
                    threadList.setSelectedIndex(selectionIndex);
                    threadList.ensureIndexIsVisible(selectionIndex);
                }
            }
            else    // no threads displayed
                clearThreadDetails();
        }
        setButtonsEnabled(machineStatus);
    }
    
    /**
     * Delete from the threads list all those threads that we don't want to
     * see.
     * We want to see
     *     - the selected thread (system or not)
     *     - the user threads is they are not finished
     *     - other system threads only if system thread checkbox is selected
     */
    private List selectThreadsForDisplay(List threads, DebuggerThread selected)
    {
        boolean showSystem = showSystemThreads.isSelected();

        List displayThreads = new ArrayList();
        String selectedName = (selected == null? "" : selected.getName());

        for(Iterator i=threads.iterator(); i.hasNext(); ) {
            DebuggerThread thread = (DebuggerThread)i.next();
            boolean showThread = (showSystem || !thread.isKnownSystemThread())
                                 && (!thread.getStatus().equals("finished"));
            if(showThread || thread.getName().equals(selectedName))
               displayThreads.add(thread);
        }
        return displayThreads;
    }


    private void selectThread(int index)
    {
        if (index >= 0) {
            selectedThread = getThread(index);
            setThreadDetails();
        }
        else
            selectedThread = null;
    }
	
    private synchronized DebuggerThread getThread(int index)
    {
        return (DebuggerThread)threads.get(index);
    }

    private void setThreadDetails()
    {
        stackList.setFixedCellWidth(-1);
        List stack = selectedThread.getStack();
        if(stack.size() > 0) {
            stackList.setListData(stack.toArray(new Object[0]));
            setStackFrameDetails(0);  // show details of top frame
        }
    }
	
    private void clearThreadDetails()
    {
        stackList.setListData(empty);
        instanceList.setListData(empty);
        localList.setListData(empty);
    }

    private void selectStackFrame(int index)
    {
        if (index >= 0) {
            setStackFrameDetails(index);
            selectedThread.setSelectedFrame(index);
            Debugger.debugger.showSource(selectedThread);
            currentFrame = index;
        }
    }
	
    private void setStackFrameDetails(int frameNo)
    {
        currentObject = selectedThread.getCurrentObject(frameNo);
        if(currentObject != null) {
            instanceList.setFixedCellWidth(-1);
            localList.setFixedCellWidth(-1);
            instanceList.setListData(
               currentObject.getAllFields(false).toArray(new Object[0]));
            localList.setListData(
             selectedThread.getLocalVariables(frameNo).toArray(new Object[0]));
        }
    }

    private void viewInstanceField(int index)
    {
        if(currentObject.fieldIsObject(index)) {
            ObjectViewer viewer = ObjectViewer.getViewer(true, 
                                                         currentObject.getFieldObject(index), 
                                                         null, null, false, this);
        }
    }

    private void viewLocalVar(int index)
    {
        if(selectedThread.varIsObject(currentFrame, index)) {
            ObjectViewer viewer = ObjectViewer.getViewer(true, 
                           selectedThread.getStackObject(currentFrame, index),
                           null, null, false, this);
        }
    }

    /**
     * Create and arrange the GUI components.
     */
    private void createWindow()
    {
        JPanel contentPane = (JPanel)getContentPane();  // has BorderLayout by default
        contentPane.setLayout(new BorderLayout(6,6));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        // Create the control button panel

        JPanel buttonBox = new JPanel();
        buttonBox.setLayout(new GridLayout(1,0));

        Insets margin = new Insets(0, 0, 0, 0);
        stopButton = addButton("image.stop", buttonBox, margin);
        stepButton = addButton("image.step", buttonBox, margin);
        stepIntoButton = addButton("image.step_into", buttonBox, margin);
        continueButton = addButton("image.continue", buttonBox, margin);
        terminateButton = addButton("image.terminate", buttonBox, margin);
		
        contentPane.add(buttonBox, BorderLayout.SOUTH);
		
        // Create instance variable panel

        instanceList = new JList(new DefaultListModel());
            instanceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            instanceList.addListSelectionListener(this);
            instanceList.setVisibleRowCount(5);
            instanceList.setFixedCellWidth(150);
        JScrollPane instanceScrollPane = new JScrollPane(instanceList);
            instanceScrollPane.setColumnHeaderView(new JLabel(instanceTitle));


        // Create local variable panel

        localList = new JList(new DefaultListModel());
            localList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            localList.addListSelectionListener(this);
            localList.setVisibleRowCount(5);
            localList.setFixedCellWidth(150);
        JScrollPane localScrollPane = new JScrollPane(localList);
            localScrollPane.setColumnHeaderView(new JLabel(localTitle));

            // add mouse listener to monitor for double clicks

            MouseListener mouseListener = new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            listDoubleClick(e);
                        }
                    }
                };
            instanceList.addMouseListener(mouseListener);
            localList.addMouseListener(mouseListener);

        // Create variable display area

        JSplitPane varPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                           instanceScrollPane,localScrollPane);
        varPane.setDividerSize(6);

        // Create stack listing panel

        stackList = new JList(new DefaultListModel());
            stackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            stackList.addListSelectionListener(this);
            stackList.setFixedCellWidth(150);
        JScrollPane stackScrollPane = new JScrollPane(stackList);
            stackScrollPane.setColumnHeaderView(new JLabel(stackTitle));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                              stackScrollPane, varPane);
        splitPane.setDividerSize(6);

        // Create thread panel
        JPanel threadPanel = new JPanel(new BorderLayout());
            threadList = new JList(new DefaultListModel());
            threadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            threadList.addListSelectionListener(this);
            threadList.setVisibleRowCount(4);
        JScrollPane threadScrollPane = new JScrollPane(threadList);
            threadScrollPane.setColumnHeaderView(new JLabel(threadTitle));
            threadPanel.add(threadScrollPane, BorderLayout.CENTER);


        JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, 
                                                BoxLayout.Y_AXIS));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

            updateButton = new JButton(updateText);
            updateButton.addActionListener(this);
            buttonPanel.add(updateButton);
            makeButtonNotGrow(updateButton);

            closeButton = new JButton(closeText);
            closeButton.addActionListener(this);
            buttonPanel.add(closeButton);
            makeButtonNotGrow(closeButton);

            buttonPanel.add(Box.createVerticalGlue());

        threadPanel.add(buttonPanel, BorderLayout.EAST);

        showSystemThreads = new JCheckBox(systemThreadText);
            Font smallFont = showSystemThreads.getFont().deriveFont(10);
            showSystemThreads.setFont(smallFont);
            showSystemThreads.addActionListener(this);
        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
        //checkBoxPanel.setBorder(BorderFactory.createEmptyBorder(0,5,5,0));
            checkBoxPanel.add(showSystemThreads);
        threadPanel.add(checkBoxPanel, BorderLayout.SOUTH);

        JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                              threadPanel, splitPane);
        mainPanel.setDividerSize(6);

        contentPane.add(mainPanel, BorderLayout.CENTER);
		
        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent event)
                {
                    Window win = (Window)event.getSource();
                    win.setVisible(false);
                    // Main.execWindowHidden();  // inform all frames that exec win is gone
                }
            });

        pack();

    }

    private void makeButtonNotGrow(JButton button)
    {
        Dimension pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
    }

    /**
     * Create a button and add it to a panel.
     *
     * @param imgRsrcName    The name of the image resource for the button.
     * @param panel          The panel to add the button to.
     * @param margin         The margin around the button.
     */
    private JButton addButton(String imgRsrcName, JPanel panel, Insets margin)
    {
        JButton button;
        button = new JButton(new ImageIcon(Config.getImageFilename(imgRsrcName)));
        button.setMargin(margin);
        button.addActionListener(this);
        panel.add(button);
        return button;
    }

    private void setButtonsEnabled(int machineStatus)
    {
        switch(machineStatus) {
        case Debugger.IDLE:
            stopButton.setEnabled(false);
            stepButton.setEnabled(false);
            stepIntoButton.setEnabled(false);
            continueButton .setEnabled(false);
            terminateButton.setEnabled(false);
            break;
        case Debugger.RUNNING:
            stopButton.setEnabled(true);
            stepButton.setEnabled(false);
            stepIntoButton.setEnabled(false);
            continueButton .setEnabled(false);
            terminateButton.setEnabled(false);
            break;
        case Debugger.SUSPENDED:
            stopButton.setEnabled(false);
            stepButton.setEnabled(true);
            stepIntoButton.setEnabled(true);
            continueButton .setEnabled(true);
            terminateButton.setEnabled(true);
            break;
        }
    }

}
