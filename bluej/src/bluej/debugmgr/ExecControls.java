package bluej.debugmgr;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import bluej.*;
import bluej.debugger.*;
import bluej.debugmgr.inspector.*;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;

import com.sun.jdi.ThreadReference;

/**
 * Window for controlling the debugger
 *
 * @author  Michael Kolling
 * @version $Id: ExecControls.java 2032 2003-06-12 05:04:28Z ajp $
 */
public class ExecControls extends JFrame
    implements ActionListener, ListSelectionListener, TreeSelectionListener, TreeModelListener
{
    private static final String windowTitle =
        Config.getString("debugger.execControls.windowTitle");
    private static final String stackTitle =
        Config.getString("debugger.execControls.stackTitle");
    private static final String staticTitle =
        Config.getString("debugger.execControls.staticTitle");
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
    private static final String haltButtonText =
        Config.getString("debugger.execControls.haltButtonText");
    private static final String stepButtonText =
        Config.getString("debugger.execControls.stepButtonText");
    private static final String stepIntoButtonText =
        Config.getString("debugger.execControls.stepIntoButtonText");
    private static final String continueButtonText =
        Config.getString("debugger.execControls.continueButtonText");
    private static final String terminateButtonText =
        Config.getString("debugger.execControls.terminateButtonText");



    private static String[] empty = new String[0];

    // === instance ===

	// the display for the list of active threads
    //private JList threadList;
	//private List threads;
	private JTree threadTree; 
	private DebuggerThreadTreeModel threadModel;
	
    
    private JList stackList, staticList, instanceList, localList;
    private JButton stopButton, stepButton, stepIntoButton, continueButton,
        terminateButton;
    private JButton updateButton, closeButton;

	// the Project that owns this debugger
    private Project project;

	//	the debug machine this control is looking at
    private Debugger debugger = null;				

	// the thread currently selected
	private DebuggerThread selectedThread;
	
    private DebuggerClass currentClass;	    // the current class for the
                                            //  selected stack frame
    private DebuggerObject currentObject;	// the "this" object for the
                                            //  selected stack frame
    private int currentFrame = 0;		// currently selected frame

    public ExecControls(Project project, Debugger debugger)
    {
        super(windowTitle);

		if (debugger == null)
			throw new NullPointerException();
			
		this.project = project;
		this.debugger = debugger;

        //threads = new ArrayList();
        createWindow();
    }

	/**
	 * Show or hide the exec control window.
	 */
	public void showHide(boolean show, boolean update,
								DebuggerThread thread)
	{
		setVisible(show);
		if(show && update)
			updateThreads(thread);
	}

	
    // ----- ActionListener interface -----

    public void actionPerformed(ActionEvent event)
    {
        Object obj = event.getSource();

		if(obj == terminateButton) {
			project.restartVM();
			//threadTree.expandRow(0);
			// dispose();
			return;
		}

        if(obj == updateButton) {
            updateThreads(selectedThread);
            // ((bluej.debugger.jdi.JdiDebugger)debugger).dumpThreadInfo();
        }
        else if(obj == closeButton) {
            setVisible(false);
        }

        if(obj == stopButton) {
			if (selectedThread != null) {
				if (!selectedThread.isSuspended())
					selectedThread.halt();
			}
            //TODO: debugger.halt(selectedThread);
            updateThreads(selectedThread);
        }
        
        if(obj == continueButton) {
			if (selectedThread != null) {
				if (selectedThread.isSuspended())
					selectedThread.cont();
			}
        }
        else if(selectedThread != null) {
            if(obj == stepButton) {
                selectedThread.step();
            }
            else if(obj==stepIntoButton) {
                selectedThread.stepInto();
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

        if(src == stackList) {
            selectStackFrame(stackList.getSelectedIndex());
        }

        // ststicList, instanceList and localList are ignored - single click
        // doesn't do anything
    }

    // ----- end of ListSelectionListener interface -----

	public void valueChanged(TreeSelectionEvent event)
	{
		Object src = event.getSource();

		if(src == threadTree) {
			clearThreadDetails();

			DefaultMutableTreeNode node =
			 (DefaultMutableTreeNode) threadTree.getLastSelectedPathComponent();

			if (node == null)
				return;

			DebuggerThread dt = threadModel.getNodeAsDebuggerThread(node);        

			if (dt != null)
				selectThread(dt);
			else
				unselectThread();
		}
	}
	
	public void treeNodesChanged(TreeModelEvent e)
	{
		Object nodes[] = e.getChildren();

		for(int i=0; i<nodes.length; i++) {
			if (nodes[i] == null || selectedThread == null)
				return;
			
			if (selectedThread.equals(threadModel.getNodeAsDebuggerThread(nodes[i])))
				selectThread(selectedThread);
		}	
	}
	
	public void treeNodesInserted(TreeModelEvent e) { }
	public void treeNodesRemoved(TreeModelEvent e) { }
	public void treeStructureChanged(TreeModelEvent e) { }
	
		
    public void listDoubleClick(MouseEvent event)
    {
        Component src = event.getComponent();

        if(src == staticList && staticList.getSelectedIndex() >= 0) {
            viewStaticField(staticList.getSelectedIndex());
        }
        else if(src == instanceList && instanceList.getSelectedIndex() >= 0) {
            viewInstanceField(instanceList.getSelectedIndex());
        }
        else if(src == localList && localList.getSelectedIndex() >= 0) {
            viewLocalVar(localList.getSelectedIndex());
        }
    }

    public void updateThreads(final DebuggerThread select)
    {
    	return;
    	/*
        // because this is responding to events in a different thread we need
        // to get these graphics updates to be run on the swing thread using
        // SwingUtilities.invokeLater()
        Runnable doAllUpdates = new Runnable() {
            public void run() {
                DefaultListModel listModel = (DefaultListModel)threadList.getModel();
                listModel.removeAllElements();

				threads = debugger.listThreads();

                int selectionIndex = 0;  // default: select first

                threads = selectThreadsForDisplay(threads, select);

                String selectName = (select == null ? "" : select.getName());

				Iterator it = threads.iterator();
				while(it.hasNext()) {
					DebuggerThread thread = (DebuggerThread) it.next();

					if(thread.getName().equals(selectName))
						;					

					String status = thread.getStatus();
					listModel.addElement(thread.getName() + " [" + status + "]");
				}
*/
/*                    if(listModel.getSize() > 0) {
                        if(selectionIndex != -1) {
                            threadList.setSelectedIndex(selectionIndex);
                            threadList.ensureIndexIsVisible(selectionIndex);
                        }
                    }
                    else    // no threads displayed
                        clearThreadDetails(); */
  /*          }
        };

        SwingUtilities.invokeLater(doAllUpdates); */
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
 /*       boolean showSystem = showSystemThreads.isSelected();

        List displayThreads = new ArrayList();
        String selectedName = (selected == null? "" : selected.getName());

        for(Iterator i=threads.iterator(); i.hasNext(); ) {
            DebuggerThread thread = (DebuggerThread)i.next();
            boolean showThread = (showSystem ||
                                  !thread.isKnownSystemThread() ||
                                  thread.getName().equals(selectedName));
            if(showThread && !thread.getStatus().equals("finished"))
               displayThreads.add(thread);
        } */
        return threads;
    }

	private void unselectThread()
	{
		selectedThread = null;
		stopButton.setEnabled(false);
		stepButton.setEnabled(false);
		stepIntoButton.setEnabled(false);
		continueButton.setEnabled(false);
	}
	
	private void selectThread(DebuggerThread dt)
	{
		selectedThread = dt;

		boolean isSuspended = selectedThread.isSuspended();
		
		stopButton.setEnabled(!isSuspended);
		stepButton.setEnabled(isSuspended);
		stepIntoButton.setEnabled(isSuspended);
		continueButton .setEnabled(isSuspended);
		terminateButton.setEnabled(true);

		setThreadDetails();
	}

    private void selectThread(int index)
    {
 /*       if (index >= 0 && index < threads.size()) {
            selectedThread = getThread(index);
            setThreadDetails();
        }
        else
            selectedThread = null; */
    }

/*    private synchronized DebuggerThread getThread(int index)
    {
        return (DebuggerThread)threads.get(index);
    } */

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
        staticList.setListData(empty);
        instanceList.setListData(empty);
        localList.setListData(empty);
    }

    private void selectStackFrame(int index)
    {
        if (index >= 0) {
            setStackFrameDetails(index);
            selectedThread.setSelectedFrame(index);
            //TODO: debugger.showSource(selectedThread);
            currentFrame = index;
        }
    }

    private void setStackFrameDetails(int frameNo)
    {
        currentClass = selectedThread.getCurrentClass(frameNo);
        currentObject = selectedThread.getCurrentObject(frameNo);
        if(currentClass != null) {
            staticList.setFixedCellWidth(-1);
            staticList.setListData(
               currentClass.getStaticFields(false).toArray(new Object[0]));
        }
        if(currentObject != null) {
            instanceList.setFixedCellWidth(-1);
            instanceList.setListData(
               currentObject.getInstanceFields(false).toArray(new Object[0]));
        }
        if(selectedThread != null) {
            localList.setFixedCellWidth(-1);
            localList.setListData(
              selectedThread.getLocalVariables(frameNo).toArray(new Object[0]));
        }
    }

    private void viewStaticField(int index)
    {
        if(currentClass.staticFieldIsObject(index)) {
            ObjectInspector viewer = ObjectInspector.getInstance(false,
                                          currentClass.getStaticFieldObject(index),
                                          null, null, null, this);
        }
    }

    private void viewInstanceField(int index)
    {
        if(currentObject.instanceFieldIsObject(index)) {
            ObjectInspector viewer = ObjectInspector.getInstance(false,
                                          currentObject.getInstanceFieldObject(index),
                                          null, null, null, this);
        }
    }

    private void viewLocalVar(int index)
    {
        if(selectedThread.varIsObject(currentFrame, index)) {
            ObjectInspector viewer = ObjectInspector.getInstance(false,
                           selectedThread.getStackObject(currentFrame, index),
                           null, null, null, this);
        }
    }

    /**
     * Create and arrange the GUI components.
     */
    private void createWindow()
    {
    	setIconImage(BlueJTheme.getIconImage());
    	
        JPanel contentPane = (JPanel)getContentPane();  // has BorderLayout by default
        contentPane.setLayout(new BorderLayout(6,6));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        // Create the control button panel

        JPanel buttonBox = new JPanel();
        buttonBox.setLayout(new GridLayout(1,0));

        Insets margin = new Insets(0, 0, 0, 0);
        stopButton = addButton("image.stop", haltButtonText, buttonBox, margin);
        stepButton = addButton("image.step", stepButtonText, buttonBox, margin);
        stepIntoButton = addButton("image.step_into", stepIntoButtonText, buttonBox, margin);
        continueButton = addButton("image.continue", continueButtonText, buttonBox, margin);
        terminateButton = addButton("image.terminate", terminateButtonText, buttonBox, margin);

        contentPane.add(buttonBox, BorderLayout.SOUTH);

        // Create static variable panel

        staticList = new JList(new DefaultListModel());
        staticList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        staticList.addListSelectionListener(this);
        staticList.setVisibleRowCount(3);
        staticList.setFixedCellWidth(150);
        JScrollPane staticScrollPane = new JScrollPane(staticList);
        staticScrollPane.setColumnHeaderView(new JLabel(staticTitle));


        // Create instance variable panel

        instanceList = new JList(new DefaultListModel());
        instanceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instanceList.addListSelectionListener(this);
        instanceList.setVisibleRowCount(4);
        instanceList.setFixedCellWidth(150);
        JScrollPane instanceScrollPane = new JScrollPane(instanceList);
        instanceScrollPane.setColumnHeaderView(new JLabel(instanceTitle));


        // Create local variable panel

        localList = new JList(new DefaultListModel());
        localList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        localList.addListSelectionListener(this);
        localList.setVisibleRowCount(4);
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
        staticList.addMouseListener(mouseListener);
        instanceList.addMouseListener(mouseListener);
        localList.addMouseListener(mouseListener);

        // Create variable display area

        JSplitPane innerVarPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                 staticScrollPane, instanceScrollPane);
        innerVarPane.setDividerSize(6);

        JSplitPane varPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                            innerVarPane, localScrollPane);
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
        
        
/*        threadList = new JList(new DefaultListModel());
        threadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadList.addListSelectionListener(this);
        threadList.setVisibleRowCount(4); */
		threadModel = (DebuggerThreadTreeModel) debugger.getThreadTreeModel();
		threadModel.addTreeModelListener(this);
		
		threadTree = new JTree(threadModel);
		threadTree.addTreeSelectionListener(this);		       
		threadTree.getSelectionModel().setSelectionMode
										(TreeSelectionModel.SINGLE_TREE_SELECTION);
		threadTree.setVisibleRowCount(8);
		threadTree.setShowsRootHandles(false);
		threadTree.setRootVisible(false);
										        
        JScrollPane threadScrollPane = new JScrollPane(threadTree);
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

        JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                              threadPanel, splitPane);
        mainPanel.setDividerSize(6);

        contentPane.add(mainPanel, BorderLayout.CENTER);

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event){
                Window win = (Window)event.getSource();
                win.setVisible(false);
                // Main.execWindowHidden();  // inform all frames that exec win is gone
            }
        });

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent event){
                Config.putLocation("bluej.debugger", getLocation());
            }
        });

        setLocation(Config.getLocation("bluej.debugger"));

        pack();

    }

    private void makeButtonNotGrow(JButton button)
    {
        Dimension pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
    }

    /**
     * Create a text & image button and add it to a panel.
     *
     * @param imgRsrcName    The name of the image resource for the button.
     * @param panel          The panel to add the button to.
     * @param margin         The margin around the button.
     */
    private JButton addButton(String imgRsrcName, String buttonText, JPanel panel, Insets margin)
    {
        JButton button;
        button = new JButton(buttonText, Config.getImageAsIcon(imgRsrcName));
        button.setVerticalTextPosition(AbstractButton.BOTTOM);
        button.setHorizontalTextPosition(AbstractButton.CENTER);

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
            break;
         case Debugger.RUNNING:
            stopButton.setEnabled(true);
            stepButton.setEnabled(false);
            stepIntoButton.setEnabled(false);
            continueButton .setEnabled(false);
            break;
         case Debugger.SUSPENDED:
            stopButton.setEnabled(false);
            stepButton.setEnabled(true);
            stepIntoButton.setEnabled(true);
            continueButton .setEnabled(true);
            break;
        }
    }

}
