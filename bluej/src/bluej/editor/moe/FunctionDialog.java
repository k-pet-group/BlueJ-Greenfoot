package bluej.editor.moe;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;		// all the GUI components
//import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.MultiLineLabel;

/**
 ** Dialog to display user functions. The dialog displays function names,
 ** help text and key bindings.
 **
 ** @author Michael Kolling
 **
 **/

public final class FunctionDialog extends JDialog

    implements ActionListener, ListSelectionListener, ItemListener
{
  // -------- CONSTANTS --------

    static final String close = Config.getString("close");
    static final String defaultsLabel = Config.getString("editor.functions.defaults");
    static final String categoriesLabel = Config.getString("editor.functions.categories");
    static final String keyLabel = Config.getString("editor.functions.keys");
    static final String addKeyLabel = Config.getString("editor.functions.addkey");
    static final String delKeyLabel = Config.getString("editor.functions.delkey");


  
  // -------- INSTANCE VARIABLES --------

    JButton defaultsButton;
    JButton closeButton;
    JButton addKeyButton;
    JButton delKeyButton;
    JComboBox categoryMenu;
    JList functionList;
    JList keyList;
    MultiLineLabel helpLabel;

    Vector functions;
    MoeActions actions;

  // ------------- METHODS --------------

    public FunctionDialog(Hashtable actiontable)
    {
	super(null, "Editor Functions", true);
	actions = MoeActions.getActions(null);;
	functions = new Vector(actiontable.size());
	for (Enumeration e = actiontable.keys(); e.hasMoreElements() ;)
	    functions.addElement(e.nextElement());
	makeDialog();
    }

    /**
     * 
     */
    //public void xx(String s)
    //{
    //}

    /**
     * Handle click on Close button.
     */
    private void handleClose()
    {
	setVisible(false);
    }

    /**
     * Handle click on Defaults button.
     */
    private void handleDefaults()
    {
    }

    /**
     * Handle click in functions list.
     */
    private void handleFuncListSelect()
    {
	int index = functionList.getSelectedIndex();
	Debug.message("func: " + index);
	String funcName = (String)functions.elementAt(index);
	KeyStroke[] keys = actions.getKeyStrokesForName(funcName);
	if(keys == null) {
	    keyList.setListData(new String[0]);
	}
	else {
	    keyList.setListData(keys);
	    addKeyButton.setEnabled(false); // should be true once implemented
	    delKeyButton.setEnabled(false);
	}
    }

    /**
     * Handle click in key bindings list.
     */
    private void handleKeyListSelect()
    {
	//delKeyButton.setEnabled(true);
    }

    /**
     * Handle click on Add Key button.
     */
    private void handleAddKey()
    {
    }

    /**
     * Handle click on Delete Key button.
     */
    private void handleDelKey()
    {
    }

    // ----- ActionListener interface -----

    /**
     * A button was pressed. Find out which one and do the appropriate
     * thing.
     */
    public void actionPerformed(ActionEvent event)
    {
	Object src = event.getSource();

	if(src == closeButton)
	    handleClose();
	else if(src == defaultsButton)
	    handleDefaults();
    }

    // ----- ItemListener interface -----

    public void itemStateChanged(ItemEvent evt)
    {
	functionList.setListData(functions);
	// ...there is only one category at the moment...
    }

    // ----- ListSelectionListener interface -----

    public void valueChanged(ListSelectionEvent event)
    {
	if(event.getValueIsAdjusting())  // ignore mouse down, dragging, etc.
	    return;

	Object src = event.getSource();

	if(src == functionList)
	    handleFuncListSelect();
	else if(src == keyList)
	    handleKeyListSelect();
    }
    
    // ----- end of ListSelectionListener interface -----

    private void makeDialog()
    {
	JPanel mainPanel = (JPanel)getContentPane();  // has BorderLayout
	mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

	// create help text area at bottom

	JPanel helpPanel = new JPanel(new GridLayout());
	helpPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(10,0,0,0),
				BorderFactory.createLineBorder(Color.black)));
	helpLabel = new MultiLineLabel("\nTest text\n\n  ", JLabel.LEFT);
	helpLabel.setBackground(MoeEditor.infoColor);
	helpPanel.add(helpLabel);
	mainPanel.add(helpPanel, BorderLayout.SOUTH);

	// create control area on right (key bindings and buttons)

	JPanel controlPanel = new JPanel(new BorderLayout());

	    // create area for main buttons (close, defaults)

	    JPanel buttonPanel = new JPanel();
	    buttonPanel.setLayout(new GridLayout(0,1,5,5));

		closeButton = new JButton(close);
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);

		defaultsButton = new JButton(defaultsLabel);
		defaultsButton.addActionListener(this);
		buttonPanel.add(defaultsButton);

		JPanel buttonFramePanel = new JPanel();
		buttonFramePanel.setLayout(new BorderLayout(0,0));
		//buttonFramePanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
		buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);

	    controlPanel.add(buttonFramePanel, BorderLayout.EAST);

	    // create area for key bindings

	    JPanel keyPanel = new JPanel();
	    keyPanel.setLayout(new BorderLayout());
	    keyPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(),
				keyLabel));

		keyList = new JList();
		keyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		keyList.setPrototypeCellValue("shift-ctrl-delete");	
		keyList.addListSelectionListener(this);
		keyList.setVisibleRowCount(4);
		JScrollPane scrollPane;
		scrollPane = new JScrollPane(keyList);
		keyPanel.add(scrollPane, BorderLayout.CENTER);
	    
		JPanel keyButtonPanel = new JPanel();
		    addKeyButton = new JButton(addKeyLabel);
		    addKeyButton.addActionListener(this);
		    addKeyButton.setMargin(new Insets(2,2,2,2));
		    keyButtonPanel.add(addKeyButton);

		    delKeyButton = new JButton(delKeyLabel);
		    delKeyButton.addActionListener(this);
		    delKeyButton.setMargin(new Insets(2,2,2,2));
		    keyButtonPanel.add(delKeyButton);

		keyPanel.add(keyButtonPanel, BorderLayout.SOUTH);

	    controlPanel.add(keyPanel, BorderLayout.SOUTH);

	mainPanel.add(controlPanel, BorderLayout.EAST);

	// create function list area

	JPanel funcPanel = new JPanel(new BorderLayout());
	funcPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));

 	    functionList = new JList();
	    functionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    functionList.addListSelectionListener(this);
	    functionList.setVisibleRowCount(12);
	    scrollPane = new JScrollPane(functionList);
	    //scrollPane.setColumnHeaderView(new JLabel(localTitle));
	    
	    funcPanel.add(scrollPane, BorderLayout.CENTER);

	    JPanel categoryPanel = new JPanel();

		JLabel label = new JLabel(categoriesLabel);
		categoryPanel.add(label);
		categoryMenu = new JComboBox();
		categoryMenu.addItemListener(this);
		categoryMenu.addItem("All functions");
	        categoryPanel.add(categoryMenu);

	    funcPanel.add(categoryPanel, BorderLayout.NORTH);

	mainPanel.add(funcPanel, BorderLayout.CENTER);
	getRootPane().setDefaultButton(closeButton);

	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E) {
		setVisible(false);
	    }
	});

	pack();
    }

}  // end class Finder
