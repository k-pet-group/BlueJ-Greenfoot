package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;

import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 ** @version $Id: ExecControls.java 136 1999-06-21 06:35:48Z mik $
 ** @author Michael Kolling
 **
 ** Window for controlling the debugger
 **/
public class ExecControls extends JFrame 

    implements ActionListener, ListSelectionListener
{
    private static final String windowTitle = "BlueJ Debugger";
    private static final String stackTitle = "Call Sequence";
    private static final String instanceTitle = "Instance Variables";
    private static final String localTitle = "Local Variables";
    private static final String threadTitle = "Threads";
    private static final String updateText = "Update";
    private static final String closeText = "Close";

    private static String[] empty = new String[0];

    private JList threadList;
    private JList stackList, instanceList, localList;
    private JButton stopButton, stepButton, stepIntoButton, continueButton, 
		    terminateButton;
    private JButton updateButton, closeButton;

    private Vector threads;
    private DebuggerThread selectedThread;	// the thread currently
						//  selected
    private DebuggerObject currentObject;	// the "this" object for the
						//  selected stack frame

    public ExecControls()
    {
	super(windowTitle);
		
	threads = new Vector();
	createWindow();
    }

    // ----- ActionListener interface -----

    public void actionPerformed(ActionEvent event)
    {
	Object obj = event.getSource();

	if(obj == updateButton) {
	    updateThreads();
	}
	else if(obj == closeButton) {
	    setVisible(false);
	}
	else if(selectedThread != null) {
	    if(obj == stopButton) {
		selectedThread.stop();
		Debugger.debugger.threadStopped(selectedThread);
		updateThreads();
	    }
	    else if(obj == stepButton) {
		Debugger.debugger.threadContinued(selectedThread);
		selectedThread.step();
	    }
	    else if(obj == stepIntoButton) {
		Debugger.debugger.threadContinued(selectedThread);
		selectedThread.stepInto();
	    }
	    else if(obj == continueButton) {
		Debugger.debugger.threadContinued(selectedThread);
		selectedThread.cont();
		updateThreads();
	    }
	    else if(obj == terminateButton) {
		Debugger.debugger.threadContinued(selectedThread);
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
	    //viewField(instanceList.getSelectedIndex());
	}
    }

    public synchronized void updateThreads()
    {
	DefaultListModel listModel = (DefaultListModel)threadList.getModel();
	listModel.removeAllElements();

//  	if(Debugger.debugger.isRunning()) {
//  	    // machine is currently running - can't inspect
//  	    listModel.addElement("(machine is running)");
//  	}
//  	else {
	    threads = Debugger.debugger.listThreads();
	    if(threads == null) {
		Debug.reportError("cannot get thread info!");
		listModel.addElement("(error: cannot list threads)");
	    }
	    else {
		for(int i = 0; i < threads.size(); i++) {
		    DebuggerThread thread = (DebuggerThread)threads.get(i);
		    String status = thread.getStatus();
		    if(! status.equals("finished"))
			listModel.addElement(thread.getName() + " [" +
					     status + "]");
		}
		if(listModel.getSize() > 0)
		    threadList.setSelectedIndex(0);  // select the first one
		else
		    clearThreadDetails();
	    }
//  	}
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
	Vector stack = selectedThread.getStack();
	if(stack.size() > 0) {
	    stackList.setListData(stack);
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
	}
    }
	
    private void setStackFrameDetails(int frameNo)
    {
	currentObject = selectedThread.getCurrentObject(frameNo);
	instanceList.setListData(currentObject.getAllFields(false));
	localList.setListData(selectedThread.getLocalVariables(frameNo));
    }

    private void viewInstanceField(int index)
    {
	if(currentObject.fieldIsObject(index)) {
	    ObjectViewer viewer = ObjectViewer.getViewer(true, 
					currentObject.getFieldObject(index), 
					null, null, false, this);
	}
    }

    private void createWindow()
    {
	JPanel mainPanel = (JPanel)getContentPane();  // has BorderLayout by default
	mainPanel.setLayout(new BorderLayout(6,6));
	mainPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

	// Create the control button panel

	JPanel buttonBox = new JPanel();
	buttonBox.setLayout(new GridLayout(1,0));

	Insets margin = new Insets(0, 0, 0, 0);
	stopButton = addButton("image.stop", buttonBox, margin);
	stepButton = addButton("image.step", buttonBox, margin);
	stepIntoButton = addButton("image.step_into", buttonBox, margin);
	continueButton = addButton("image.continue", buttonBox, margin);
	terminateButton = addButton("image.terminate", buttonBox, margin);
		
	mainPanel.add(buttonBox, BorderLayout.SOUTH);
		
	// Create stack listing panel

	stackList = new JList(new DefaultListModel());
	stackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	stackList.addListSelectionListener(this);
	JScrollPane stackScrollPane = new JScrollPane(stackList);
	stackScrollPane.setColumnHeaderView(new JLabel(stackTitle));

	// Create variable display area

	JPanel varArea = new JPanel();
	varArea.setLayout(new GridLayout(0, 1));

	// Create instance variable panel

	JScrollPane scrollPane;
	instanceList = new JList(new DefaultListModel());
	instanceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	instanceList.addListSelectionListener(this);
	instanceList.setVisibleRowCount(5);
	scrollPane = new JScrollPane(instanceList);
	scrollPane.setColumnHeaderView(new JLabel(instanceTitle));

	varArea.add(scrollPane);

	// Create local variable panel

	localList = new JList(new DefaultListModel());
	localList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	localList.addListSelectionListener(this);
	localList.setVisibleRowCount(5);
	scrollPane = new JScrollPane(localList);
	scrollPane.setColumnHeaderView(new JLabel(localTitle));

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


	varArea.add(scrollPane);

	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					      stackScrollPane, varArea);
	splitPane.setDividerSize(5);
	splitPane.setDividerLocation(120);

	mainPanel.add(splitPane, BorderLayout.CENTER);

	// Create thread panel
	JPanel threadPanel = new JPanel(new BorderLayout());

	threadList = new JList(new DefaultListModel());
	threadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	threadList.addListSelectionListener(this);
	threadList.setVisibleRowCount(4);
	scrollPane = new JScrollPane(threadList);
	scrollPane.setColumnHeaderView(new JLabel(threadTitle));
	threadPanel.add(scrollPane, BorderLayout.CENTER);

	JPanel buttonPanel = new JPanel(new GridLayout(0, 1));

	updateButton = new JButton(updateText);
	updateButton.addActionListener(this);
	buttonPanel.add(updateButton);

	closeButton = new JButton(closeText);
	closeButton.addActionListener(this);
	buttonPanel.add(closeButton);

	buttonPanel.add(Box.createVerticalGlue());
	threadPanel.add(buttonPanel, BorderLayout.EAST);

	mainPanel.add(threadPanel, BorderLayout.NORTH);
		
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
	Dimension size = splitPane.getSize();
	splitPane.setPreferredSize(new Dimension(350, size.height));
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
}
