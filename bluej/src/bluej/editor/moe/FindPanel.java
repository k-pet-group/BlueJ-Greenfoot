package bluej.editor.moe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;


public class FindPanel extends JPanel implements ActionListener, KeyListener {

    private MoeEditor editor;

    private JPanel body;
    private DBox findBody;
    private JPanel closeBody;
    private JLabel findLabel;
    private JButton closeButton;
    private JTextField findTField;
    private JButton previousButton;
    private JButton nextButton;
    private JButton replaceWithButton;
    private JCheckBox matchCaseCheckBox;
    //private JCheckBox highlightAllBox;
    //private BasicArrowButton prevArrowButton;
    //private BasicArrowButton nextArrowButton;

    private final static String CLOSE_BUTTON_NAME ="closeBtn";
    private final static String INPUT_QUERY_NAME ="queryText";
    private final static String PREVIOUS_BUTTON_NAME ="prevBtn";
    private final static String NEXT__BUTTON_NAME ="nextBtn";
    private final static String REPLACE_WITH_BUTTON_NAME ="replaceWithBtn";
    //private final static String REPLACE_CHECKBOX="replaceCheckBox";

    private String searchString=""; 
    private static Font findFont;

    /**
     * Constructor that creates and displays the different elements of the Find Panel
     */
    public FindPanel() {
        super();
        findFont=new Font(PrefMgr.getStandardFont().getFontName(), PrefMgr.getStandardFont().getSize(), PrefMgr.getStandardFont().getSize());
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.black));

        initDisplay();
        
        setFindDisplay();
        setCaseCheckDisplay();
        setCloseDisplay();
        setPrevNextDisplay();
        setReplaceDisplay();
        
        addDisplayElements();     
    }
    
    /**
     * Initialise the structure for the display panel
     */
    private void initDisplay()
    {
        body = new JPanel(new BorderLayout()); // one row, many columns
        body.setBackground(MoeEditor.infoColor);
        //body.setBorder(new EmptyBorder(3,6,3,4));
        body.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));
        body.setName("FindPanelBody");
        
        //findOptions=new JPanel(new GridLayout(1, 7));
        findBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        findBody.setName("FindPanelFindBody");

        closeBody=new JPanel(new GridLayout(1,5));
        closeBody.setBackground(MoeEditor.infoColor);
        closeBody.setName("FindPanelFindBody");
    }

    /**
     * Initialise find buttons and labels
     */
    private void setFindDisplay()
    {
        findLabel = new JLabel();
        findLabel.setText("Find: ");
        findLabel.setFont(findFont);

        findTField=new JTextField(10);
        findTField.setFont(findFont);
        setSearchString("");
        findTField.addKeyListener(this);
        findTField.setName(INPUT_QUERY_NAME);

    }
    
    /**
     * Initialise the previous and next buttons
     */
    private void setPrevNextDisplay()
    {
        previousButton=new JButton();
        previousButton.addActionListener(this);
        previousButton.setName(PREVIOUS_BUTTON_NAME);
        previousButton.setText("Prev");
        previousButton.setEnabled(false);
        previousButton.setFont(findFont);
        
//        prevArrowButton=new BasicArrowButton(BasicArrowButton.WEST);
//        prevArrowButton.addActionListener(this);
//        prevArrowButton.setName(PREVIOUS_BUTTON_NAME);
//        prevArrowButton.setText("Prev");
//        prevArrowButton.setEnabled(false);
        
        nextButton=new JButton();
        nextButton.addActionListener(this);
        nextButton.setName(NEXT__BUTTON_NAME);
        nextButton.setText("Next");
        nextButton.setEnabled(false);
        nextButton.setFont(findFont);
        
//        nextArrowButton=new BasicArrowButton(BasicArrowButton.EAST);
//        nextArrowButton.addActionListener(this);
//        nextArrowButton.setName(NEXT__BUTTON_NAME);
//        nextArrowButton.setText("Next");
//        nextArrowButton.setEnabled(false);

    }
    
    /**
     * Initialise the check case check box
     */
    private void setCaseCheckDisplay()
    {
        matchCaseCheckBox=new JCheckBox();
        matchCaseCheckBox.setText("Match Case");
        matchCaseCheckBox.setSelected(false);
        matchCaseCheckBox.setFont(findFont);

        //highlightAllBox=new JCheckBox();
        //highlightAllBox.setText("Highlight All");
        //highlightAllBox.setSelected(false);
    }
       
    /**
     * Initialise the close button
     */
    private void setCloseDisplay()
    {
        closeButton=new JButton();
        closeButton.setText("Done");
        closeButton.addActionListener(this);
        closeButton.setName(CLOSE_BUTTON_NAME); 
        closeButton.setFont(findFont);

    }
    
    /**
     * Initialise the buttons and labels for replace functionality
     */
    private void setReplaceDisplay()
    {       
        replaceWithButton=new JButton();
        replaceWithButton.addActionListener(this);
        replaceWithButton.setName(REPLACE_WITH_BUTTON_NAME);
        replaceWithButton.setText("Replace");
        replaceWithButton.setEnabled(true);
        replaceWithButton.setFont(findFont);
    }
    
    /**
     * Adds the different elements into the display
     */
    private void addDisplayElements()
    {
        findBody.add(findLabel);
        findBody.add(findTField);
        
        findBody.add(previousButton);
        findBody.add(nextButton);
        findBody.add(replaceWithButton);
        findBody.add(matchCaseCheckBox);
        //findBody.addSpacer(500);
        //findBody.add(closeButton);

        closeBody.add(closeButton);

        body.add(findBody, BorderLayout.WEST);
        body.add(closeBody, BorderLayout.EAST);

        add(body);
    }
    
    /**
     * Performs the required action dependent on source of action event 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) 
    {
        JComponent src = (JComponent) e.getSource();
        if(src.getName() == CLOSE_BUTTON_NAME){
            this.setVisible(false);
            return;
        }
        if (src.getName()==NEXT__BUTTON_NAME){           
            //editor.findString(getSearchString(), false, !matchCaseCheckBox.isSelected(), false, true);
            editor.findStringSelect(getSearchString(), false, !matchCaseCheckBox.isSelected(), false, true, true);
        }
        if (src.getName()==PREVIOUS_BUTTON_NAME){
            editor.findString(getSearchString(), true, !matchCaseCheckBox.isSelected(), false, true);
        }
        if (src.getName()==REPLACE_WITH_BUTTON_NAME){
            replace();
        }
        if (src.getName()==INPUT_QUERY_NAME){
            find(src);
        }
    }

    /** 
     * Handle the key-pressed event from the text field. 
     */
    public void keyPressed(KeyEvent e) {

    }

    /**
     * Handle the key-released event from the text field. 
     */
    public void keyReleased(KeyEvent e) 
    {
        boolean doFind=false;
        JComponent src = (JComponent) e.getSource();
        if (src.getName()== INPUT_QUERY_NAME){
            //check there has been a legitimate change in the search criteria
            if (getSearchString()!=null){
                if (!getSearchString().equals(((JTextField)src).getText()))
                    doFind=true;
            }
            //if it is the first letter of the search
            else if (((JTextField)src).getText().length()>0)
                doFind=true;
            if (doFind)
                find(src);
        }
    }

    /**
     * Display or remove the visibility of the find panel 
     */
    public void displayFindPanel(String selection, boolean visible)
    { 
        if (!visible){
            this.setVisible(false);
            return;
        }
        setSearchString(selection);
        updateDisplay();
        this.setVisible(true);
        findTField.requestFocus();

    }

    public void setEditor(MoeEditor editor) 
    {
        this.editor = editor;
    }

    public String getSearchString() 
    {
        return searchString;
    }

    /**
     * Sets the text in the textfield and resets the searchString to the new text
     * @param searchString
     */
    public void setSearchString(String searchString) 
    {
        findTField.setText(searchString);
        this.searchString = searchString;
    }

    /**
     * Enable (previous and next) buttons if there is a valid search string.
     * If not these buttons should be disabled
     */
    private void updateDisplay()
    {
        boolean validSearch=false;

        if(getSearchString() != null && getSearchString().length() > 0) 
            validSearch=true;

        if (validSearch){
            previousButton.setEnabled(true);
            nextButton.setEnabled(true);
            //prevArrowButton.setEnabled(true);
            //nextArrowButton.setEnabled(true);
        }
        else{
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            //prevArrowButton.setEnabled(false);
            //nextArrowButton.setEnabled(false);
        }
        findTField.requestFocus();
    }

    /**
     * When the editor finds a value it needs to reset the caret to before the search string
     * This ensures that the find will continue to find including what has already 
     * just been found. This is required as partial finds are done.
     * @param src JTextField is the source and focus is reset there and searchQuery is reset
     */
    private void setFindValues(JComponent src)
    {
        //just to ensure it is setback sufficiently, use the original length of the search string
        if (getSearchString()!=null){
            int length=getSearchString().length();
            //editor.setCaretBack(length);
        }
        //now get and reset fields
        JTextField query=(JTextField)src;
        setSearchString(query.getText());
        findTField.requestFocus();   
    }

    /**
     * Replaces selected text with the contents of the replaceField and return
     * next instance of the searchString.
     */
    private void replace()
    {
        editor.replace();
    }

    /**
     * Replace all instances of the search String with a replacement.
     * -check for valid search criteria
     * - TODO: get initial cursor pos
     * -start at beginning
     * -do initial find
     * -replace until not found, no wrapping!
     * -print out number of replacements (?)
     * -TODO: return cursor/caret to original place
     */
    private void replaceAll(String replaceString)
    {
        String searchString = getSearchString();

        int count = 0;

        while(editor.doFind(searchString, false, false, false)) {
            editor.insertText(smartFormat(editor.getSelectedText(), replaceString), true);
            count++;
        }

        if(count > 0)
            //editor.writeMessage("Replaced " + count + " instances of " + searchString);
            editor.writeMessage(Config.getString("editor.replaceAll.replaced") +
                    count + Config.getString("editor.replaceAll.intancesOf") + 
                    searchString);
        else
            //editor.writeMessage("String " + searchString + " not found. Nothing replaced.");
            editor.writeMessage(Config.getString("editor.replaceAll.string") + 
                    searchString + Config.getString("editor.replaceAll.notFoundNothingReplaced"));
    }

    
    /**
     * Replace all instances of the search String with a replacement.
     * -check for valid search criteria
     * - TODO: get initial cursor pos
     * -start at beginning
     * -do initial find
     * -replace until not found, no wrapping!
     * -print out number of replacements (?)
     * -TODO: return cursor/caret to original place
     */
    private void highlightAll(boolean ignoreCase, boolean wholeWord, boolean wrap)
    {
        String searchString = getSearchString();

        int count = 0;
        boolean select=false;

        System.out.println(" ");
        while(editor.doFindSelect(searchString, ignoreCase, wholeWord, wrap, select)) {
            //editor.insertText(smartFormat(editor.getSelectedText(), replaceString), true);
            //this ensures that only the first find is selected.
            select=false;
            System.out.println("count "+count+ " and position "+editor.getCaretPosition());            
            count++;
        }

        if(count > 0)
            //editor.writeMessage("Replaced " + count + " instances of " + searchString);
            editor.writeMessage(Config.getString("editor.replaceAll.replaced") +
                    count + Config.getString("editor.replaceAll.intancesOf") + 
                    searchString);
        else
            //editor.writeMessage("String " + searchString + " not found. Nothing replaced.");
            editor.writeMessage(Config.getString("editor.replaceAll.string") + 
                    searchString + Config.getString("editor.replaceAll.notFoundNothingReplaced"));
    }
    /**
     * Replace the text currently selected in the editor with
     */
    private String smartFormat(String original, String replacement)
    {
        return replacement;

    }

    private void find(JComponent src)
    {
        setFindValues(src);            
        find();
    }

    /**
     * Find requires the display to be reset (i.e button enabled/disabled) and calling the editor to find
     */
    private void find()
    {
        updateDisplay();
        editor.findStringSelect(getSearchString(), false, !matchCaseCheckBox.isSelected(), false, true, true);
        // highlightAll(!matchCaseCheckBox.isSelected(), false, true);
    }


    public void keyTyped(KeyEvent e) {
        
    }


}
