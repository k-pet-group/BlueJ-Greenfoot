/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

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
package bluej.editor.moe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Event;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.Config;
import bluej.prefmgr.PrefPanelListener;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

/**
 * KeyBindingsPanel panel for the key bindings in preferences
 * @author Marion Zalk
 *
 */
public class KeyBindingsPanel extends JPanel implements ActionListener, ListSelectionListener, ItemListener, PrefPanelListener
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
    private JButton addKeyButton;
    private JButton delKeyButton;
    private JComboBox categoryMenu;
    private JList functionList;
    private JList keyList;
    private JTextArea helpLabel;

    private MoeActions actions;     // The Moe action manager
    private Action currentAction;       // the action currently selected
    private KeyStroke[] currentKeys;    // key strokes currently displayed

    private int firstDisplayedFunc; // index of first function in list
    private Properties help;
    private Action[] functions;     // all user functions
    private String[] categories;
    private int[] categoryIndex;   // an array of indexes into "functions"

    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        if(src == defaultsButton)
            handleDefaults();
        else if(src == addKeyButton)
            handleAddKey();
        else if(src == delKeyButton)
            handleDelKey();

    }


    public void valueChanged(ListSelectionEvent event) {
        if(event.getValueIsAdjusting())  // ignore mouse down, dragging, etc.
            return;

        Object src = event.getSource();

        if(src == functionList)
            handleFuncListSelect();
        else if(src == keyList)
            handleKeyListSelect();

    }

    public void itemStateChanged(ItemEvent e) {
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

    public JPanel makePanel(){
        GridLayout gridL=new GridLayout(1, 2);
        JPanel mainPanel = new JPanel(gridL);  // has BorderLayout
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        // create function list area
        JPanel funcPanel = new JPanel(new BorderLayout());
        funcPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        functionList = new JList();
        functionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        functionList.addListSelectionListener(this);
        functionList.setVisibleRowCount(12);
        JScrollPane scrollPane = new JScrollPane(functionList);
        funcPanel.add(scrollPane);

        JPanel categoryPanel = new JPanel();
        JLabel label = new JLabel(categoriesLabel);
        categoryPanel.add(label);
        categoryMenu = new JComboBox();
        categoryPanel.add(categoryMenu);
        funcPanel.add(categoryPanel, BorderLayout.NORTH);

        // create control area on right (key bindings and buttons)
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        // create area for key bindings
        JPanel keyPanel = new JPanel(new BorderLayout());
        JLabel kLabel=new JLabel(keyLabel);
        kLabel.setPreferredSize(categoryMenu.getPreferredSize());
        keyPanel.add(kLabel , BorderLayout.NORTH);
        keyList = new JList();
        keyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyList.setPrototypeCellValue("shift-ctrl-delete"); 
        keyList.setVisibleRowCount(4);
        scrollPane = new JScrollPane(keyList);
        keyPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel keyButtonPanel = new JPanel();
        addKeyButton = new JButton(addKeyLabel);
        addKeyButton.setMargin(new Insets(2,2,2,2));
        keyButtonPanel.add(addKeyButton);

        delKeyButton = new JButton(delKeyLabel);
        delKeyButton.setMargin(new Insets(2,2,2,2));
        keyButtonPanel.add(delKeyButton);

        defaultsButton = new JButton(defaultsLabel);
        keyButtonPanel.add(defaultsButton);
        keyPanel.add(keyButtonPanel, BorderLayout.SOUTH);
        controlPanel.add(keyPanel);

        // create help text area at bottom
        JPanel helpPanel = new JPanel(new GridLayout());
        helpPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10,0,0,0),
                BorderFactory.createLineBorder(Color.black)));
        helpLabel=new JTextArea();
        helpLabel.setRows(6);
        helpLabel.setLineWrap(true);
        helpLabel.setWrapStyleWord(true);
        helpLabel.setBackground(MoeEditor.infoColor);
        helpPanel.add(helpLabel);
        controlPanel.add(helpPanel,BorderLayout.SOUTH);

        mainPanel.add(funcPanel);
        mainPanel.add(controlPanel);
        updateDispay();

        return mainPanel;
    }

    public void beginEditing() {
       
    }

    public void commitEditing() {
       handleClose();
    }

    public void revertEditing() {
        
    }

    /**
     * 
     * @param actiontable
     * @param categories
     * @param categoryIndex
     */
    public void setActionValues(Action[] actiontable, 
            String[] categories, int[] categoryIndex){
        this.categories=categories;
        functions = actiontable;
        this.categoryIndex=categoryIndex;
    }

    /**
     * 
     */
    public void updateDispay()
    {
        categoryMenu.addItemListener(this);
        delKeyButton.addActionListener(this);
        addKeyButton.addActionListener(this);
        keyList.addListSelectionListener(this);
        defaultsButton.addActionListener(this);
        focusMgr = FocusManager.getCurrentManager();
        openHelpFile();
        for(int i=0; i<categories.length; i++)
            categoryMenu.addItem(categories[i]);
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
            return; // deselection event - ignore

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
        String helpText=help.getProperty(function);
        //need to check if null first as some items do not have helpText/helpText is empty
        if (helpText!=null && helpText.length()>0) {
            //remove the /n from the text 
            helpText=helpText.replaceAll("\n", "");
            helpText=helpText.trim();
        }
        return helpText;
    }

    private void addKeyListener()
    {
        FocusManager.setCurrentManager(new KeyCatcher());
    }

    private void removeKeyListener()
    {
        FocusManager.setCurrentManager(focusMgr);
    }


    public KeyBindingsPanel() {
        super();
        actions = MoeActions.getActions(new JEditorPane());
        setActionValues(actions.getActionTable(), actions.getCategories(), actions.getCategoryIndex());
    }

}
