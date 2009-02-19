/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import javax.swing.FocusManager;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;		// all the GUI components
//import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import java.util.Properties;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.FixedMultiLineLabel;

/**
** Dialog to display user functions. The dialog displays function names,
** help text and key bindings.
**
** @author Michael Kolling
**
**/

public final class FunctionDialog extends EscapeDialog

implements ActionListener, ListSelectionListener, ItemListener
{
    // -------- CONSTANTS --------

    static final String title = Config.getString("editor.functions.title");
    static final String close = Config.getString("close");
    static final String defaultsLabel = Config.getString("editor.functions.defaults");
    static final String categoriesLabel = Config.getString("editor.functions.categories");
    static final String keyLabel = Config.getString("editor.functions.keys");
    static final String addKeyLabel = Config.getString("editor.functions.addkey");
    static final String delKeyLabel = Config.getString("editor.functions.delkey");

    // -------- INSTANCE VARIABLES --------

    private FocusManager focusMgr;
    private JButton defaultsButton;
    private JButton closeButton;
    private JButton addKeyButton;
    private JButton delKeyButton;
    private JComboBox categoryMenu;
    private JList functionList;
    private JList keyList;
    private FixedMultiLineLabel helpLabel;

    private MoeActions actions;		// The Moe action manager
    private Action currentAction;       // the action currently selected
    private KeyStroke[] currentKeys;    // key strokes currently displayed

    private Action[] functions;		// all user functions
    private int[] categoryIndex;	// an array of indexes into "functions"
    private int firstDisplayedFunc;	// index of first function in list
    private Properties help;

    // ------------- METHODS --------------

    public FunctionDialog(JFrame parent, Action[] actiontable, 
                          String[] categories, int[] categoryIndex)
    {
        super(parent, title, true);
        focusMgr = FocusManager.getCurrentManager();
        actions = MoeActions.getActions(null);
        currentAction = null;
        currentKeys = null;
        functions = actiontable;
        this.categoryIndex = categoryIndex;
        makeDialog(categories);
        openHelpFile();
    }

    /**
     * Handle click on Close button.
     */
    private void handleClose()
    {
        removeKeyListener();
        if(!actions.save())
            DialogManager.showError(this, "cannot-save-keys");
        setVisible(false);
    }

    /**
     * Handle click on Defaults button.
     */
    private void handleDefaults()
    {
        int answer = DialogManager.askQuestion(this, "default-keys");
        if(answer == 0) {
            actions.setDefaultKeyBindings();
            handleFuncListSelect();
        }
    }

    /**
     * Handle click in functions list.
     */
    private void handleFuncListSelect()
    {
        int index = functionList.getSelectedIndex();
        if(index == -1)
            return;	// deselection event - ignore

        // find selected action

        currentAction = functions[firstDisplayedFunc + index];

        // display keys and help text

        updateKeyList(currentAction);
        String helpText = 
            getHelpText((String)currentAction.getValue(Action.NAME));
        helpLabel.setText(helpText);
    }

    /**
     * Handle click in key bindings list.
     */
    private void handleKeyListSelect()
    {
        delKeyButton.setEnabled(true);
    }

    /**
     * Handle click on Add Key button.
     */
    private void handleAddKey()
    {
        helpLabel.setText(getHelpText("press-key"));
        addKeyListener();
    }

    /**
     * Handle click on Delete Key button.
     */
    private void handleDelKey()
    {
        if(currentKeys == null)
            return;             // something went wrong here...

        int index = keyList.getSelectedIndex();
        if(index == -1)
            return;             // deselection event - ignore

        actions.removeKeyStrokeBinding(currentKeys[index]);
        updateKeyList(currentAction);
    }

    /**
     * Display key bindings in the key list
     */
    private void updateKeyList(Action action)
    {
        currentKeys = actions.getKeyStrokesForAction(action);
        if(currentKeys == null)
            clearKeyList();
        else {
            String[] keyStrings = getKeyStrings(currentKeys);
            keyList.setListData(keyStrings);
            delKeyButton.setEnabled(false);
        }
        addKeyButton.setEnabled(true);
    }

    /**
     * Translate KeyStrokes into String representation.
     */
    private String[] getKeyStrings(KeyStroke[] keys)
    {
        String[] keyStrings = new String[keys.length];
        for(int i = 0; i < keys.length; i++) {
            int modifiers = keys[i].getModifiers();
            keyStrings[i] = KeyEvent.getKeyModifiersText(modifiers);
            if(keyStrings[i].length() > 0)
                keyStrings[i] += "+";
            keyStrings[i] += KeyEvent.getKeyText(keys[i].getKeyCode());
        }
        return keyStrings;
    }

    private void clearKeyList()
    {
        keyList.setListData(new String[0]);
    }

    private void clearHelpText()
    {
        helpLabel.setText(null);
    }

    private void openHelpFile()
    {
        help = Config.getMoeHelp();
    }

    private String getHelpText(String function)
    {
        if(help == null)
            return null;
        return help.getProperty(function);
    }

    private void addKeyListener()
    {
        FocusManager.setCurrentManager(new KeyCatcher());
    }

    private void removeKeyListener()
    {
        FocusManager.setCurrentManager(focusMgr);
    }

    // ======== EVENT HANDLING INTERFACES =========

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
        else if(src == addKeyButton)
            handleAddKey();
        else if(src == delKeyButton)
            handleDelKey();
    }

    // ----- ItemListener interface -----

    /**
     * The selected item in the category menu has changed.
     */
    public void itemStateChanged(ItemEvent evt)
    {
        int selected = categoryMenu.getSelectedIndex();

        firstDisplayedFunc = categoryIndex[selected];
        int lastFunc = categoryIndex[selected + 1];

        String[] names = new String[lastFunc - firstDisplayedFunc];

        for(int i = firstDisplayedFunc; i < lastFunc; i++) {
            names[i-firstDisplayedFunc] = 
                (String)functions[i].getValue(Action.NAME);
        }
        functionList.setListData(names);
        clearKeyList();
        clearHelpText();
        addKeyButton.setEnabled(false);
        delKeyButton.setEnabled(false);
        currentAction = null;
        currentKeys = null;
    }

    // ----- ListSelectionListener interface -----

    /**
     * The selected item in a list has changed.
     */
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

    private void makeDialog(String[] categories)
    {
        JPanel mainPanel = (JPanel)getContentPane();  // has BorderLayout
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // create help text area at bottom

        JPanel helpPanel = new JPanel(new GridLayout());
        helpPanel.setBorder(BorderFactory.createCompoundBorder(
                               BorderFactory.createEmptyBorder(10,0,0,0),
                               BorderFactory.createLineBorder(Color.black)));
        helpLabel = new FixedMultiLineLabel(4);
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

        funcPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel categoryPanel = new JPanel();

        JLabel label = new JLabel(categoriesLabel);
        categoryPanel.add(label);
        categoryMenu = new JComboBox();
        categoryMenu.addItemListener(this);
        for(int i=0; i<categories.length; i++)
            categoryMenu.addItem(categories[i]);
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
        DialogManager.centreDialog(this);
    }


    class KeyCatcher extends FocusManager {

        public void processKeyEvent(Component focusedComponent,
                                    KeyEvent e) 
        { 
            if(e.getID() != KeyEvent.KEY_PRESSED)
                return;

            int keyCode = e.getKeyCode();

            if(keyCode == KeyEvent.VK_CAPS_LOCK ||    // the keys we want to ignore...
               keyCode == KeyEvent.VK_SHIFT ||
               keyCode == KeyEvent.VK_CONTROL ||
               keyCode == KeyEvent.VK_META ||
               keyCode == KeyEvent.VK_ALT ||
               keyCode == KeyEvent.VK_ALT_GRAPH ||
               keyCode == KeyEvent.VK_COMPOSE ||
               keyCode == KeyEvent.VK_NUM_LOCK ||
               keyCode == KeyEvent.VK_SCROLL_LOCK ||
               keyCode == KeyEvent.VK_UNDEFINED
               )
                return;

            if(currentAction == null)
                Debug.message("FunctionDialog: currentAction is null...");
            else {
                KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
                if(isPrintable(key, e))
                    helpLabel.setText(getHelpText("cannot-redefine"));
                else {
                    actions.addActionForKeyStroke(key, currentAction);
                    handleFuncListSelect();
                }
            }
            e.consume();
            removeKeyListener();
        }

        private boolean isPrintable(KeyStroke key, KeyEvent e)
        {
            // all control and alt keys are non-printable
            int modifiers = key.getModifiers();
            if(modifiers != 0 && modifiers != Event.SHIFT_MASK)
                return false;

            // action keys are non-printable
            if(e.isActionKey())
                return false;

            // some action keys that the above function not recognises
            int keyCode = e.getKeyCode();
            if(keyCode == KeyEvent.VK_BACK_SPACE ||
               keyCode == KeyEvent.VK_DELETE ||
               keyCode == KeyEvent.VK_ENTER ||
               keyCode == KeyEvent.VK_TAB ||
               keyCode == KeyEvent.VK_ESCAPE)
                return false;

            // otherwise it's printable
            return true;
        }

        public void focusNextComponent(Component c) {}
        public void focusPreviousComponent(Component c) {}
    }

}  // end class FunctionDialog
