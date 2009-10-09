package bluej.editor.moe;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.border.EmptyBorder;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;


public class FindPanel extends JPanel implements ActionListener, KeyListener {

    private MoeEditor editor;

    private JPanel body;
    private DBox findBody;
    private JPanel replaceBody;
    private JLabel findLabel;
    private JLabel replaceLabel;
    private JButton closeButton;
    private JButton clearQueryButton;
    private JButton clearReplaceButton;
    private JTextField findTField;
    private JTextField replaceTField;
    private JButton previousButton;
    private JButton nextButton;
    private JButton replaceWithButton;
    private JButton replaceAllWithButton;
    private JCheckBox matchCaseCheckBox;
    //private JCheckBox highlightAllBox;
    //private JCheckBox replaceEnabled;
    //private BasicArrowButton prevArrowButton;
    //private BasicArrowButton nextArrowButton;

    private final static String CLOSE_BUTTON_NAME ="closeBtn";
    private final static String INPUT_QUERY_NAME ="queryText";
    private final static String REPLACE_TEXT_NAME ="replaceText";
    private final static String CLEAR_INPUT_QUERY_NAME ="clearQueryText";
    private final static String PREVIOUS_BUTTON_NAME ="prevBtn";
    private final static String NEXT__BUTTON_NAME ="nextBtn";
    private final static String REPLACE_WITH_BUTTON_NAME ="replaceWithBtn";
    private final static String REPLACE_ALL_WITH_BUTTON_NAME ="replaceAllWithBtn";
    private final static String CLEAR_REPLACE_BUTTON_NAME ="clearReplaceText";
    //private final static String REPLACE_CHECKBOX="replaceCheckBox";

    private String searchString=""; 
    private String replaceString="";

    public FindPanel() {
        super();
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
    
    private void initDisplay()
    {
        body = new JPanel(new BorderLayout()); // one row, many columns
        body.setBackground(MoeEditor.infoColor);
        body.setBorder(new EmptyBorder(3,6,3,4));
        
        //findOptions=new JPanel(new GridLayout(1, 7));
        findBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        findBody.setBackground(MoeEditor.infoColor);

        replaceBody=new JPanel(new GridLayout(1,5));
        replaceBody.setBackground(MoeEditor.infoColor);
    }

    private void setFindDisplay()
    {
        findLabel = new JLabel();
        findLabel.setText("Find: ");

        findTField=new JTextField(10);
        setSearchString("");
        findTField.addKeyListener(this);
        findTField.setName(INPUT_QUERY_NAME);
        //findTField.set

        clearQueryButton=new JButton();
        clearQueryButton.setText("Clear Text");
        clearQueryButton.addActionListener(this);
        clearQueryButton.setName(CLEAR_INPUT_QUERY_NAME);
        clearQueryButton.setEnabled(false);
    }
    
    private void setPrevNextDisplay()
    {
        previousButton=new JButton();
        previousButton.addActionListener(this);
        previousButton.setName(PREVIOUS_BUTTON_NAME);
        previousButton.setText("Prev");
        previousButton.setEnabled(false);
        
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
        
//        nextArrowButton=new BasicArrowButton(BasicArrowButton.EAST);
//        nextArrowButton.addActionListener(this);
//        nextArrowButton.setName(NEXT__BUTTON_NAME);
//        nextArrowButton.setText("Next");
//        nextArrowButton.setEnabled(false);

    }
    
    private void setCaseCheckDisplay()
    {
        matchCaseCheckBox=new JCheckBox();
        matchCaseCheckBox.setText("Match Case");
        matchCaseCheckBox.setSelected(false);

        //highlightAllBox=new JCheckBox();
        //highlightAllBox.setText("Highlight All");
        //highlightAllBox.setSelected(false);
    }
    
    private void setCloseDisplay()
    {
        closeButton=new JButton();
        closeButton.setText("X");
        closeButton.addActionListener(this);
        closeButton.setName(CLOSE_BUTTON_NAME); 

    }
    
    private void setReplaceDisplay()
    {       
        replaceLabel=new JLabel();
        replaceLabel.setText("Replace: ");
        
        replaceTField=new JTextField();
        setReplaceString("");
        replaceTField.addKeyListener(this);
        replaceTField.setName(REPLACE_TEXT_NAME);
        
//        replaceEnabled=new JCheckBox();
//        replaceEnabled.setName(REPLACE_CHECKBOX);
//        replaceEnabled.setText("Enable Replace");

        clearReplaceButton=new JButton();
        clearReplaceButton.setText("Clear Text");
        clearReplaceButton.addActionListener(this);
        clearReplaceButton.setName(CLEAR_REPLACE_BUTTON_NAME);
        clearReplaceButton.setEnabled(false);

        replaceWithButton=new JButton();
        replaceWithButton.addActionListener(this);
        replaceWithButton.setName(REPLACE_WITH_BUTTON_NAME);
        replaceWithButton.setText("Replace With");
        replaceWithButton.setEnabled(false);

        replaceAllWithButton=new JButton();
        replaceAllWithButton.addActionListener(this);
        replaceAllWithButton.setName(REPLACE_ALL_WITH_BUTTON_NAME);
        replaceAllWithButton.setText("Replace All With");
        replaceAllWithButton.setEnabled(false);
    }
    
    private void addDisplayElements()
    {
        findBody.add(findLabel);
        findBody.add(findTField);
        findBody.add(clearQueryButton);
        
        findBody.add(previousButton);
        findBody.add(nextButton);

        findBody.add(matchCaseCheckBox);
        //findBody.addSpacer(500);
        //findBody.add(closeButton);

//        replaceBody.add(replaceLabel);
//        replaceBody.add(replaceTField);
//        replaceBody.add(clearReplaceButton);
//        replaceBody.add(replaceWithButton);
//        replaceBody.add(replaceAllWithButton);

        replaceBody.add(closeButton);

        body.add(findBody, BorderLayout.WEST);
        body.add(replaceBody, BorderLayout.EAST);

        add(body);
    }
    
    public void actionPerformed(ActionEvent e) 
    {
        JComponent src = (JComponent) e.getSource();
        if(src.getName() == CLOSE_BUTTON_NAME){
            this.setVisible(false);
            return;
        }
        if(src.getName()==CLEAR_INPUT_QUERY_NAME){
            setSearchString("");
            updateDisplay();
            //editor.resetCaretPoition();
        }
        if (src.getName()==NEXT__BUTTON_NAME){           
            editor.findString(getSearchString(), false, !matchCaseCheckBox.isSelected(), false, true);
        }
        if (src.getName()==PREVIOUS_BUTTON_NAME){
            editor.findString(getSearchString(), true, !matchCaseCheckBox.isSelected(), false, true);
        }
        if (src.getName()==REPLACE_WITH_BUTTON_NAME){
            replace();
        }
        if (src.getName()==REPLACE_ALL_WITH_BUTTON_NAME){
            replaceAll();
        }
        if (src.getName()==CLEAR_REPLACE_BUTTON_NAME){
            setReplaceString("");
            updateDisplay();
        }
    }

    /** Handle the key-pressed event from the text field. */
    public void keyPressed(KeyEvent e) {

    }

    /** Handle the key-released event from the text field. */
    public void keyReleased(KeyEvent e) 
    {
        JComponent src = (JComponent) e.getSource();
        if (src.getName()== INPUT_QUERY_NAME){
            find(src);
        }
        if (src.getName()==REPLACE_TEXT_NAME){
            setReplaceValue(src);
            updateDisplay();
        }
    }

    /**
     * Displaying the find panel
     *
     */
    public void displayFindPanel(String selection, boolean visible)
    { 
        if (!visible){
            this.setVisible(false);
            return;
        }
        updateDisplay();
        this.setVisible(true);

    }

    public void setEditor(MoeEditor editor) 
    {
        this.editor = editor;
    }

    public String getSearchString() 
    {
        return searchString;
    }

    public void setSearchString(String searchString) 
    {
        findTField.setText(searchString);
        this.searchString = searchString;
    }

    private void updateDisplay()
    {
        boolean validSearch=false;
        boolean validReplace=false;

        if(getSearchString() != null && getSearchString().length() > 0) 
            validSearch=true;
        if (getReplaceString()!=null && getReplaceString().length()>0)
            validReplace=true;

        if (validSearch){
            //replaceButton.setEnabled(true);
            previousButton.setEnabled(true);
            nextButton.setEnabled(true);
            //prevArrowButton.setEnabled(true);
            //nextArrowButton.setEnabled(true);
            clearQueryButton.setEnabled(true);
            if (validReplace){
                clearReplaceButton.setEnabled(true);
                replaceWithButton.setEnabled(true);
                replaceAllWithButton.setEnabled(true);
            }
        }
        else{
            replaceWithButton.setEnabled(false);
            replaceAllWithButton.setEnabled(false);
            clearReplaceButton.setEnabled(false);
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            //prevArrowButton.setEnabled(false);
            //nextArrowButton.setEnabled(false);
            clearQueryButton.setEnabled(false);
        }
    }

    private void setFindValues(JComponent src)
    {
        //just to ensure it is setback sufficiently, use the original length of the search string
        int length=getSearchString().length();
        editor.setCaretBack(length);

        //now get and reset fields
        JTextField query=(JTextField)src;
        setSearchString(query.getText());
        findTField.requestFocus();   
    }

    private void setReplaceValue(JComponent src)
    {      
        //now get and reset fields
        JTextField replace=(JTextField)src;
        setReplaceString(replace.getText());
        replaceTField.requestFocus();   
    }

    /**
     * replaces selected text with the contents of the replaceField and return
     * next instance of the searchString.
     */
    private void replace()
    {
        String replaceText = smartFormat(editor.getSelectedText(), replaceTField.getText());
        editor.insertText(replaceText, false);
        //sets it to the next string
        find();
    }

    private void replaceAll()
    {
        replaceAll(replaceTField.getText());
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
     * Replace the text currently selected in the editor with
     */
    private String smartFormat(String original, String replacement)
    {
        return replacement;

    }

    public String getReplaceString() 
    {
        return replaceString;
    }

    public void setReplaceString(String replaceString) 
    {
        this.replaceString = replaceString;
        replaceTField.setText(replaceString);
    }

    private void find(JComponent src)
    {
        setFindValues(src);            
        find();
    }

    private void find()
    {
        updateDisplay();
        editor.findString(getSearchString(), false, !matchCaseCheckBox.isSelected(), false, true);
    }


    public void keyTyped(KeyEvent e) {
        
    }


}
