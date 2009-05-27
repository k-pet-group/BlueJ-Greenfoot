/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.*;
import bluej.debugger.DebuggerThreadTreeModel.SyncMechanism;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

/**
 * Window for controlling the debugger
 *
 * @author  Michael Kolling
 * @version $Id: ExecControls.java 6353 2009-05-27 04:26:36Z marionz $
 */
public class ExecControls extends JFrame
    implements ListSelectionListener, TreeSelectionListener, TreeModelListener
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
	
    
    private JSplitPane mainPanel;
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
    
    // A flag to keep track of whether a stack frame selection was performed
    // explicitly via the gui or as a result of a debugger event
    private boolean autoSelectionEvent = false; 
    

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
    private void listDoubleClick(MouseEvent event)
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
	 * Checks to make sure that a particular thread is
     * selected in the thread tree. Often when we get to this,
     * the thread in question should already be selected so
     * in that case we should not cause any more events, or
     * we'll end in a cycle.
	 * 
	 * If the thread is already selected, this method
	 * will ensure that the status details are up to date.
	 * 
	 * @param  dt  the thread to hilight in the thread
	 *             tree and whose status we want to display.
	 */
	public void makeSureThreadIsSelected(final DebuggerThread dt)
	{
        TreePath tp = threadModel.findNodeForThread(dt);
	    
	    if (tp != null) {
	        if (!tp.equals(threadTree.getSelectionPath())) {
	            threadTree.clearSelection();
	            threadTree.addSelectionPath(tp);
	        }
	    }
	    else {
	        Debug.message("Thread " + dt + " no longer available for selection");
	    }
        
        // There seems to be a swing glitch causing the thread-tree scrollpane
        // to be reduced to a very small size by the divider. Doing a paint
        // here seems to fix it.
        mainPanel.paintImmediately(0,0,mainPanel.getSize().width,mainPanel.getSize().height);
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

		if (dt == null) {
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
     * These details include showing the threads stack, and displaying 
     * the details for the top stack frome.
     */
    private void setThreadDetails()
    {
        stackList.setFixedCellWidth(-1);
        List stack = selectedThread.getStack();
        if(stack.size() > 0) {
            stackList.setListData(stack.toArray(new Object[0]));
			// show details of top frame
            autoSelectionEvent = true;
			selectStackFrame(0);
            autoSelectionEvent = false;
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
        // if the UI isn't up to date, make sure the correct frame is
        // selected in the list
        if (stackList.getSelectedIndex() != index)
            stackList.setSelectedIndex(index);

        else if (index >= 0) {
            setStackFrameDetails(index);
            selectedThread.setSelectedFrame(index);
                
            if (! autoSelectionEvent)
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
            ObjectInspector viewer = project.getInspectorInstance(currentClass.getStaticFieldObject(index),
                                          null,
                                          null, null, this);
        }
    }

    /**
     * Display an object inspector for an object in an instance field.
     */
    private void viewInstanceField(int index)
    {
        if(currentObject.instanceFieldIsObject(index)) {
            ObjectInspector viewer = project.getInspectorInstance(currentObject.getInstanceFieldObject(index),
                                          null,
                                          null, null, this);
        }
    }

    /**
     * Display an object inspector for an object in a local variable.
     */
    private void viewLocalVar(int index)
    {
        if(selectedThread.varIsObject(currentFrame, index)) {
            ObjectInspector viewer = project.getInspectorInstance(selectedThread.getStackObject(currentFrame, index),
                           null,
                           null, null, this);
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

			stopButton = addButton(new StopAction(), buttonBox);
			stepButton = addButton(new StepAction(), buttonBox);
			stepIntoButton = addButton(new StepIntoAction(), buttonBox);
			continueButton = addButton(new ContinueAction(), buttonBox);

			// terminate is always on
			terminateButton = addButton(new TerminateAction(), buttonBox);
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
			JLabel lbl = new JLabel(staticTitle);
            lbl.setOpaque(true);
            staticScrollPane.setColumnHeaderView(lbl);
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
            JLabel lbl = new JLabel(instanceTitle);
            lbl.setOpaque(true);
			instanceScrollPane.setColumnHeaderView(lbl);
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
            JLabel lbl = new JLabel(localTitle);
            lbl.setOpaque(true);
			localScrollPane.setColumnHeaderView(lbl);
    	}

        // Create variable display area

        JSplitPane innerVarPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                 staticScrollPane, instanceScrollPane);
        innerVarPane.setDividerSize(6);
        innerVarPane.setBorder(null);

        JSplitPane varPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                            innerVarPane, localScrollPane);
        varPane.setDividerSize(6);
        varPane.setBorder(null);

        // Create stack listing panel

        stackList = new JList(new DefaultListModel());
        stackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stackList.addListSelectionListener(this);
        stackList.setFixedCellWidth(150);
        JScrollPane stackScrollPane = new JScrollPane(stackList);
        JLabel lbl = new JLabel(stackTitle);
        lbl.setOpaque(true);
        stackScrollPane.setColumnHeaderView(lbl);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                              stackScrollPane, varPane);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);

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
        threadModel.setSyncMechanism(new SyncMechanism() {
            public void invokeLater(Runnable r)
            {
                if(EventQueue.isDispatchThread())
                    r.run();
                else
                    EventQueue.invokeLater(r);
            }
        });
		threadModel.addTreeModelListener(this);
		
		threadTree = new JTree(threadModel);
		{
			threadTree.getSelectionModel().
						setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			threadTree.setVisibleRowCount(5);
			threadTree.setShowsRootHandles(false);
			threadTree.setRootVisible(false);
            threadTree.addTreeSelectionListener(this);             
            threadTree.addMouseListener(treeMouseListener);
		}
										        
        JScrollPane threadScrollPane = new JScrollPane(threadTree);
        lbl = new JLabel(threadTitle);
        lbl.setOpaque(true);
        threadScrollPane.setColumnHeaderView(lbl);
        threadPanel.add(threadScrollPane, BorderLayout.CENTER);
        //threadPanel.setMinimumSize(new Dimension(100,100));

		flipPanel = new JPanel();
		{
			flipPanel.setLayout(cardLayout = new CardLayout());
   
			flipPanel.add(splitPane, "split");
			JPanel tempPanel = new JPanel();
            JLabel infoLabel = new JLabel(Config.getString("debugger.threadRunning"));
            infoLabel.setForeground(Color.gray);
			tempPanel.add(infoLabel);
			flipPanel.add(tempPanel, "blank");
		}

        /* JSplitPane */ mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
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

		menubar.add(menu);
		return menubar;
	}
    
    /**
     * Create a text & image button and add it to a panel.
     * 
     * @param action
     *            The assosciated Action (with text, icon, action, etc).
     * @param panel
     *            The panel to add the button to.
     */
    private JButton addButton(Action action, JPanel panel)
    {
        JButton button = new JButton(action);
        button.setVerticalTextPosition(AbstractButton.BOTTOM);
        button.setHorizontalTextPosition(AbstractButton.CENTER);
        button.setEnabled(false);
        panel.add(button);
        return button;
    }
    
    /**
     * Action to halt the selected thread.
     */
    private class StopAction extends AbstractAction
    {
        public StopAction()
        {
            super(haltButtonText, Config.getImageAsIcon("image.debug.stop"));
        }
        
        public void actionPerformed(ActionEvent e)
        {
            if (selectedThread == null)
                return;
            clearThreadDetails();
            if (!selectedThread.isSuspended()) {
                selectedThread.halt();
            }
        }
    }
        
    /**
     * Action to step through the code.
     */
    private class StepAction extends AbstractAction
    {
        public StepAction()
        {
            super(stepButtonText, Config.getImageAsIcon("image.debug.step"));
        }
        
        public void actionPerformed(ActionEvent e)
        {
            if (selectedThread == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (selectedThread.isSuspended()) {
                selectedThread.step();
            }
            project.updateInspectors();
        }
    }
    
    /**
     * Action to "step into" the code.
     */
    private class StepIntoAction extends AbstractAction
    {
        public StepIntoAction()
        {
            super(stepIntoButtonText, Config.getImageAsIcon("image.debug.step_into"));
        }
        
        public void actionPerformed(ActionEvent e)
        {
            if (selectedThread == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (selectedThread.isSuspended()) {
                selectedThread.stepInto();
            }
        }
    }
    
    /**
     * Action to continue a halted thread. 
     */
    private class ContinueAction extends AbstractAction
    {
        public ContinueAction()
        {
            super(continueButtonText, Config.getImageAsIcon("image.debug.continue"));
        }
        
        public void actionPerformed(ActionEvent e)
        {
            if (selectedThread == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (selectedThread.isSuspended()) {
                selectedThread.cont();
            }
        }
    }
    
    /**
     * Action to terminate the program, restart the VM.
     */
    private class TerminateAction extends AbstractAction
    {
        public TerminateAction()
        {
            super(terminateButtonText, Config.getImageAsIcon("image.debug.terminate"));           
        }
        
        public void actionPerformed(ActionEvent e)
        {
            try {
                // throws an illegal state exception
                // if we press this whilst we are already
                // restarting the remote VM
                project.restartVM();
            }
            catch (IllegalStateException ise) { }
        }
    }
    
    /**
     * Action to close the debugger window.
     */
	private class CloseAction extends AbstractAction
	{
		public CloseAction()
		{
			super(Config.getString("close"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, SHORTCUT_MASK));
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
    
    /** 
     * This method provides the user with an elegant way to decide on how to proceed if there is
     * a debugger running in the background (Bug#138)
     * returns boolean to whether it should continue processing original request
     * (dependent on user choice)
     */
    public boolean processDebuggerState(){
    	//whether or not the original action should be executed. This is dependent on the response from the user
    	boolean processCallingAction=false;
    	//only need to give user warnings if debugger is already initiated, double check status
	    if (debugger.getStatus()==Debugger.IDLE || debugger.getStatus()==Debugger.NOTREADY){
	    	processCallingAction=true;
	    	return processCallingAction;
	    }
	    int numResponses=4;
	    int response=DialogManager.askQuestion(this, "debugger-running-options", numResponses);
       	switch (response) {
            case 0:  //Terminate
                //this should reset the debug VM, and ideally run the recently 
           	 	//invoked code immediately once the new debug VM has started
           	 try {
                    project.restartVM();
                    processCallingAction=true;
                }
                catch (IllegalStateException ise) { }
                break;
            case 1: //Continue
           	  if (selectedThread == null)
                     return processCallingAction;
                 clearThreadDetails();
                 project.removeStepMarks();
                 if (selectedThread.isSuspended()) {
                     selectedThread.cont();
                 }
                break;
            case 2: //Open Debugger: This will make the debugger visible
            	setVisible(true);                   	
                break;
            case 3://Cancel
            	break;
            default:
                //... If we get here, something is wrong.
                JOptionPane.showMessageDialog(null, "Unexpected response " + response);
            	
        }  
       	return processCallingAction;
   	
   }
    
  
     
}
