package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;

import java.util.Hashtable;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 ** @version $Id: ExecControls.java 60 1999-05-03 02:22:57Z mik $
 ** @author Michael Kolling
 **
 ** Window for controlling the debugger
 **/
public class ExecControls extends JFrame 

    implements ActionListener, ListSelectionListener
{
    private static final String windowTitle = "Execution Controls";
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

    private DebuggerThread[] threads;		// most recently updated list
    private DebuggerThread selectedThread;	// the thread currently
						// selected

    public ExecControls()
    {
	super(windowTitle);
		
	threads = new DebuggerThread[0];
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
	else if(obj == stopButton) {
	    if(selectedThread != null) {
		selectedThread.stop();
		updateThreads();
		Debugger.debugger.threadStopped(selectedThread);
	    }
	}
	else if(obj == stepButton) {
	    if(selectedThread != null)
		selectedThread.step();
	}
	else if(obj == stepIntoButton) {
	    if(selectedThread != null)
		selectedThread.stepInto();
	}
	else if(obj == continueButton) {
	    if(selectedThread != null) {
		selectedThread.cont();
		Debugger.debugger.threadContinued(selectedThread);
		updateThreads();
	    }
	    else
		Debug.message("no thread...");
	}
	else if(obj == terminateButton) {
	    if(selectedThread != null)
		selectedThread.terminate();
	}
		
	// Debug.message("Obj[" + obj.toString() + "]" );
    }
	
    // ----- ListSelectionListener interface -----

    public void valueChanged(ListSelectionEvent event)
    {
	Object src = event.getSource();

	if(src == threadList) {
	    clearThreadDetails();
	    selectThread(threadList.getSelectedIndex());
	}
	else if(src == stackList) {
	    selectStackFrame(stackList.getSelectedIndex());
	}
	else if(src == instanceList) {
	}
	else if(src == localList) {
	}
    }

    // ----- end of ListSelectionListener interface -----

    public synchronized void updateThreads()
    {
	DefaultListModel listModel = (DefaultListModel)threadList.getModel();
	listModel.removeAllElements();

	try {
	    // remember all the threads that are explicitly halted
	    Hashtable haltedThreads = new Hashtable();
	    for(int i = 0; i < threads.length; i++)
		if(threads[i].isHalted())
		    haltedThreads.put(threads[i].getName(), "");

	    threads = Debugger.debugger.listThreads();

	    for(int i = 0; i < threads.length; i++)
		if(haltedThreads.get(threads[i].getName()) != null)
		    threads[i].setHalted(true);

	} catch(Exception e) {
	    Utility.reportError("could not get thread information");
	    return;
	}

	for(int i = 0; i < threads.length; i++) {
	    String name = threads[i].getName();
	    if(! name.startsWith("AWT-") && !name.equals("Screen Updater"))
		listModel.addElement(name + " [" + 
				     threads[i].getStatus() + "]");
	}
	if(listModel.getSize() > 0)
	    threadList.setSelectedIndex(0);  // always select the first one
	else
	    clearThreadDetails();
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
	return threads[index];
    }

    private void setThreadDetails()
    {
	Vector stack = selectedThread.getStack();
	if(stack.size() > 0) {
	    stackList.setListData(stack);
	    setVariableDetails(0);  // show details of top frame
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
	if (index >= 0)
	    setVariableDetails(index);
	Debugger.debugger.showSource(selectedThread, index);
    }
	
    private void setVariableDetails(int frameNo)
    {
	instanceList.setListData(selectedThread.getInstanceVariables(frameNo));
	localList.setListData(selectedThread.getLocalVariables(frameNo));
    }

    private void createWindow()
    {
	JPanel mainPanel = (JPanel)getContentPane();  // has BerderLayout by default
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
     * @arg imgRsrcName    The name of the image resource for the button.
     * @arg panel          The panel to add the button to.
     * @arg margin         The margin around the button.
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
