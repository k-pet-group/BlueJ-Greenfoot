package bluej.debugmgr;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import bluej.*;
import bluej.debugger.*;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;

/**
 * Window for controlling the debugger
 *
 * @author  Michael Kolling
 * @version $Id: ExecControls.java 2077 2003-06-26 14:09:27Z mik $
 */
public class ExecControls extends JFrame
    implements ActionListener, ListSelectionListener, TreeSelectionListener, TreeModelListener
{
   // private static final String windowTitle =
        
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

	private static final int SHORTCUT_MASK =
		Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();


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
    //private JButton closeButton;
	private CardLayout cardLayout;
	private JPanel flipPanel;
    private JCheckBoxMenuItem systemThreadItem;
	
	// the Project that owns this debugger
    private Project project;

	// the debug machine this control is looking at
    private Debugger debugger = null;				

	// the thread currently selected
	private DebuggerThread selectedThread;
	
    private DebuggerClass currentClass;	    // the current class for the
                                            //  selected stack frame
    private DebuggerObject currentObject;	// the "this" object for the
                                            //  selected stack frame
    private int currentFrame = 0;		    // currently selected frame
    

	/**
	 * Create a window to view and interact with a debug VM.
	 * 
	 * @param project  the project this window is associated with
	 * @param debugger the debugger this window is debugging
	 */
    public ExecControls(Project project, Debugger debugger)
    {
        super(Config.getString("debugger.execControls.windowTitle"));

		if (project == null || debugger == null)
			throw new NullPointerException("project or debugger null in ExecControls");
			
		this.project = project;
		this.debugger = debugger;

        createWindow();
    }

	/**
	 * Show or hide the ExecControl window.
	 */
	public void showHide(boolean show)
	{
		setVisible(show);
	}

    // ----- ActionListener interface -----

    public void actionPerformed(ActionEvent event)
    {
        Object obj = event.getSource();

		if(obj == terminateButton) {
			try {
				// throws an illegal state exception
				// if we press this whilst we are already
				// restarting the remote VM
				project.restartVM();
			}
			catch (IllegalStateException ise) { }

			return;
		}
        //if(obj == closeButton) {
        //    setVisible(false);
        //}

		// All the buttons after this require a selected
		// thread. If no thread selected, exit now.
		if (selectedThread == null)
			return;

        if (obj == stopButton) {
			clearThreadDetails();
			if (!selectedThread.isSuspended()) {
				selectedThread.halt();
			}
        }
        if (obj == continueButton) {
			clearThreadDetails();
			if (selectedThread.isSuspended()) {
				selectedThread.cont();
			}
        }
        if (obj == stepButton) {
			clearThreadDetails();
			if (selectedThread.isSuspended()) {
            	selectedThread.step();
			}
        }
		if (obj == stepIntoButton) {
			clearThreadDetails();
			if (selectedThread.isSuspended()) {
	            selectedThread.stepInto();
			}
		}
    }

    // ----- ListSelectionListener interface -----

    /**
     * A list item was selected. This can be either in the
     * stack list, or one of the variable lists.
     */
    public void valueChanged(ListSelectionEvent event)
    {
		// ignore mouse down, dragging, etc.
		if(event.getValueIsAdjusting())
            return;

        if(event.getSource() == stackList) {
            selectStackFrame(stackList.getSelectedIndex());
        }
        // staticList, instanceList and localList are ignored
        // a single click doesn't do anything
    }

    // ----- end of ListSelectionListener interface -----

	// ----- TreeSelectionListener interface -----

	/**
	 * A tree item was selected. This is in the thread list.
	 */
	public void valueChanged(TreeSelectionEvent event)
	{
		Object src = event.getSource();
		
		if(src == threadTree) {
			clearThreadDetails();

			// check for "unselecting" a node
			// (happens when the VM is restarted)
			if (!event.isAddedPath()) {
				setSelectedThread(null);
				return;
			}

			DefaultMutableTreeNode node =
			 (DefaultMutableTreeNode) threadTree.getLastSelectedPathComponent();
				
			if (node == null)
				return;

			DebuggerThread dt = threadModel.getNodeAsDebuggerThread(node);        

			// the thread can not be found, dt will end up as null and
			// the selected thread will be cleared
			setSelectedThread(dt);
		}
	}

	// ----- end of TreeSelectionListener interface -----

	// ----- TreeModelListener interface -----
	
	/**
	 * When a thread changes state in the tree, we may need to update
	 * the controls for the selected thread.
	 */
	public void treeNodesChanged(TreeModelEvent e)
	{
		if (selectedThread == null)
			return;
			
		Object nodes[] = e.getChildren();

		for(int i=0; i<nodes.length; i++) {
			if (nodes[i] == null)
				continue;
			
			if (selectedThread.equals(threadModel.getNodeAsDebuggerThread(nodes[i])))
				setSelectedThread(selectedThread);
		}	
	}
	
	public void treeNodesInserted(TreeModelEvent e) { }
	public void treeNodesRemoved(TreeModelEvent e) { }
	public void treeStructureChanged(TreeModelEvent e) { }
	
	// ----- end of TreeModelListener interface -----

	/**
     * A list item was double clicked.
     * 
     * This will be in one of the variable lists. We try to
     * view the relevant object that was double clicked on.
	 */
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

	/**
	 * Selects a thread for display of its details.
	 * 
	 * If the thread is already selected, this method
	 * will ensure that the status details are up to date.
	 * 
	 * @param  dt  the thread to hilight in the thread
	 *             tree and whose status we want to display.
	 */
	public void selectThread(DebuggerThread dt)
	{
		TreePath tp = threadModel.findNodeForThread(dt);
		
		if (tp != null) {
			threadTree.clearSelection();
			threadTree.addSelectionPath(tp);
		}
		else {
			Debug.message("Thread " + dt + " no longer available for selection");
		}
	}

	/**
	 * Set our internally selected thread and update the
	 * UI to reflect its status.
	 * 
	 * It is currently true that this thread will be
	 * selected in the tree view before this method is called.
	 * At the moment, this method does not rely on this fact
	 * but if the method is changed _to_ rely on it, this
	 * comment should be fixed.
	 * 
	 * @param dt  the thread to select or null if the thread
	 *            selection has been cleared
	 */
	private void setSelectedThread(DebuggerThread dt)
	{
		selectedThread = dt;

		if (selectedThread == null) {
			stopButton.setEnabled(false);
			stepButton.setEnabled(false);
			stepIntoButton.setEnabled(false);
			continueButton.setEnabled(false);

			cardLayout.show(flipPanel, "blank");
		}
		else {	
			boolean isSuspended = selectedThread.isSuspended();
		
			stopButton.setEnabled(!isSuspended);
			stepButton.setEnabled(isSuspended);
			stepIntoButton.setEnabled(isSuspended);
			continueButton .setEnabled(isSuspended);

			cardLayout.show(flipPanel, isSuspended ? "split" : "blank");

			setThreadDetails();		
		}
	}

    /**
     * Display the details for the currently selected thread.
     * These details include showing the therads stack, and displaying 
     * the details for the top stack frome.
     */
    private void setThreadDetails()
    {
        stackList.setFixedCellWidth(-1);
        List stack = selectedThread.getStack();
        if(stack.size() > 0) {
            stackList.setListData(stack.toArray(new Object[0]));
			// show details of top frame
			selectStackFrame(0);
        }
    }

    /**
     * Clear the display of thread details (stack and variables).
     */
    private void clearThreadDetails()
    {
        stackList.setListData(empty);
        staticList.setListData(empty);
        instanceList.setListData(empty);
        localList.setListData(empty);
    }

    /**
     * Make a stack frame in the stack display the selected stack frame.
     * This will cause this frame's details (local variables, etc.) to be
     * displayed, as well as the current source position being marked.
     */
    private void selectStackFrame(int index)
    {
        if (index >= 0) {
            setStackFrameDetails(index);
            selectedThread.setSelectedFrame(index);

			project.debuggerEvent(new DebuggerEvent(this, DebuggerEvent.THREAD_SHOWSOURCE, selectedThread));
            currentFrame = index;
        }
    }

    /**
     * Display the detail information (current object fields and local var's)
     * for a specific stack frame.
     */
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

    /**
     * Display an object inspector for an object in a static field.
     */
    private void viewStaticField(int index)
    {
        if(currentClass.staticFieldIsObject(index)) {
            ObjectInspector viewer = ObjectInspector.getInstance(false,
                                          currentClass.getStaticFieldObject(index),
                                          null, null, null, this);
        }
    }

    /**
     * Display an object inspector for an object in an instance field.
     */
    private void viewInstanceField(int index)
    {
        if(currentObject.instanceFieldIsObject(index)) {
            ObjectInspector viewer = ObjectInspector.getInstance(false,
                                          currentObject.getInstanceFieldObject(index),
                                          null, null, null, this);
        }
    }

    /**
     * Display an object inspector for an object in a local variable.
     */
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
    	
		setJMenuBar(makeMenuBar());

        JPanel contentPane = (JPanel)getContentPane();  // has BorderLayout by default
        contentPane.setLayout(new BorderLayout(6,6));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        // Create the control button panel

        JPanel buttonBox = new JPanel();
        {
			buttonBox.setLayout(new GridLayout(1,0));

			stopButton = addButton("image.stop", haltButtonText, buttonBox);
			stepButton = addButton("image.step", stepButtonText, buttonBox);
			stepIntoButton = addButton("image.step_into", stepIntoButtonText, buttonBox);
			continueButton = addButton("image.continue", continueButtonText, buttonBox);

			// terminate is always on
			terminateButton = addButton("image.terminate", terminateButtonText, buttonBox);
			terminateButton.setEnabled(true);
        }

        contentPane.add(buttonBox, BorderLayout.SOUTH);

		// create a mouse listener to monitor for double clicks
		MouseListener mouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					listDoubleClick(e);
				}
			}
		};

		// create static variable panel
		JScrollPane staticScrollPane = new JScrollPane();
		{
			staticList = new JList(new DefaultListModel());
			{
				staticList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				staticList.addListSelectionListener(this);
				staticList.setVisibleRowCount(3);
				staticList.setFixedCellWidth(150);
				staticList.addMouseListener(mouseListener);
			}
			staticScrollPane.setViewportView(staticList);
			staticScrollPane.setColumnHeaderView(new JLabel(staticTitle));
		}

        // create instance variable panel
		JScrollPane instanceScrollPane = new JScrollPane();
    	{
			instanceList = new JList(new DefaultListModel());
    		{
				instanceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				instanceList.addListSelectionListener(this);
				instanceList.setVisibleRowCount(4);
				instanceList.setFixedCellWidth(150);
				instanceList.addMouseListener(mouseListener);
    		}
			instanceScrollPane.setViewportView(instanceList);
			instanceScrollPane.setColumnHeaderView(new JLabel(instanceTitle));
    	}

        // create local variable panel
		JScrollPane localScrollPane = new JScrollPane();
    	{
			localList = new JList(new DefaultListModel());
			{
				localList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				localList.addListSelectionListener(this);
				localList.setVisibleRowCount(4);
				localList.setFixedCellWidth(150);
				localList.addMouseListener(mouseListener);
			}
			localScrollPane.setViewportView(localList);
			localScrollPane.setColumnHeaderView(new JLabel(localTitle));
    	}

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
        

		MouseListener treeMouseListener = new MouseAdapter() {
			 public void mousePressed(MouseEvent e) {
				 TreePath selPath = threadTree.getPathForLocation(e.getX(), e.getY());
				 if(selPath != null) {
					DefaultMutableTreeNode node =
					 (DefaultMutableTreeNode) selPath.getLastPathComponent();

					if (node != null) {
						DebuggerThread dt = threadModel.getNodeAsDebuggerThread(node);        

						if (dt != null)
							setSelectedThread(dt);				 	
					}
				 }
			 }
		 };
		 
		threadModel = debugger.getThreadTreeModel();
		threadModel.addTreeModelListener(this);
		
		threadTree = new JTree(threadModel);
		{
			threadTree.addTreeSelectionListener(this);		       
			threadTree.addMouseListener(treeMouseListener);
			threadTree.getSelectionModel().
						setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			threadTree.setVisibleRowCount(5);
			threadTree.setShowsRootHandles(false);
			threadTree.setRootVisible(false);
		}
										        
        JScrollPane threadScrollPane = new JScrollPane(threadTree);
        threadScrollPane.setColumnHeaderView(new JLabel(threadTitle));
        threadPanel.add(threadScrollPane, BorderLayout.CENTER);

		flipPanel = new JPanel();
		{
			flipPanel.setLayout(cardLayout = new CardLayout());
   
			flipPanel.add(splitPane, "split");
			JPanel tempPanel = new JPanel();
            JLabel infoLabel = new JLabel("<html><center>Thread is running.<br>Threads must be stopped to view details.</html>");
            infoLabel.setForeground(Color.gray);
			tempPanel.add(infoLabel);
			flipPanel.add(tempPanel, "blank");
		}

        JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                              threadPanel, flipPanel);

        mainPanel.setDividerSize(6);

        contentPane.add(mainPanel, BorderLayout.CENTER);

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event){
                Window win = (Window)event.getSource();
                win.setVisible(false);
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

	/**
	 * Create the debugger's menubar, all menus and items.
	 */
	private JMenuBar makeMenuBar()
	{
		JMenuBar menubar = new JMenuBar();
		JMenu menu = new JMenu(Config.getString("terminal.options"));
		JMenuItem item;

		systemThreadItem = new JCheckBoxMenuItem(new HideSystemThreadAction());
        systemThreadItem.setSelected(true);
        debugger.hideSystemThreads(true);
		menu.add(systemThreadItem);

		menu.add(new JSeparator());

		item = menu.add(new CloseAction());
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, SHORTCUT_MASK));

		menubar.add(menu);
		return menubar;
	}
    
    /**
     * Create a text & image button and add it to a panel.
     *
     * @param imgRsrcName    The name of the image resource for the button.
     * @param panel          The panel to add the button to.
     * @param margin         The margin around the button.
     */
    private JButton addButton(String imgRsrcName, String buttonText, JPanel panel)
    {
        JButton button;
        button = new JButton(buttonText, Config.getImageAsIcon(imgRsrcName));
        button.setVerticalTextPosition(AbstractButton.BOTTOM);
        button.setHorizontalTextPosition(AbstractButton.CENTER);
        button.setEnabled(false);

        button.addActionListener(this);
        panel.add(button);
        return button;
    }
    
    /**
     * Action to close the debugger window.
     */
	private class CloseAction extends AbstractAction
	{
		public CloseAction()
		{
			super(Config.getString("close"));
		}

		public void actionPerformed(ActionEvent e) {
			setVisible(false);
		}
	}
    
    /**
     * Action to enable/disable hiding of system threads. All this action
     * actually does is toggle an internal flag.
     */
    private class HideSystemThreadAction extends AbstractAction
    {
        public HideSystemThreadAction()
        {
            super(Config.getString("debugger.hideSystemThreads"));
        }

        public void actionPerformed(ActionEvent e) {
            debugger.hideSystemThreads(systemThreadItem.isSelected());
        }
    }
}
