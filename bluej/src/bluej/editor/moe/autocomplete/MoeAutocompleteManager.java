package bluej.editor.moe.autocomplete;

import bluej.*;
import bluej.editor.moe.*;
import bluej.editor.moe.autocomplete.parser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.text.*;



/**
 * This class is designed to manage all the auto-complete functionality and
 * keeps the complexities away from the MoeEditor class.  A single
 * MoeEditorWindow should construct one and only one AutoCompleteManager.
 * Each MoeEditor Frame should construct its manager by passing a reference
 * to itself to the constructor.  The MoeEditor should also pass a reference
 * to the file that is being edited and a reference to the JEditorPane that
 * contains the source code.  When a manager is constructed several listeners
 * are set up to listen for events in the JEditorPane and the MoeEditor Frame.
 * This class implements the MoeDropDownListMouseListener interface in order
 * to determine when a MoeDropDownItem is double clicked upon.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */

public class MoeAutocompleteManager
             implements MoeDropDownListMouseListener,
                        ParserListener,
                        DocumentListener,
                        WindowFocusListener,
                        WindowListener,
                        ComponentListener,
                        CaretListener,
                        KeyListener,
                        ChangeListener{

    /**
     * Stores the location of the project root
     * for the file that is being edited in
     * the MoeEditor.
     */
    private File projectRoot;

    /**
     * Stores a reference to the MoeEditor
     * that created this MoeAutocompleteManager.
     */
    private MoeEditor moeEditorFrame = null;

    /**
     * Stores a reference to the JEditorPane
     * in the MoeEditor window.
     */
    private JEditorPane sourcePane = null;

    /**
     * Stores a reference to the underlying syntax
     * document of the JEditorPane.
     */
    private MoeSyntaxDocument document;

    /**
     * Stores a reference to the single parser
     * for this MoeAutocompleteManager.
     */
    private Parser parser;

    /**
     * A reference to the current MoeDropDownList.
     */
    private MoeDropDownList mddList = null;

    /**
     * Stores the position of the last typed
     * dot character that started the parsing.
     */
    private Position autoCompleteDotPosition;

    /**
     * Stores the position where the auto-complete region ends.
     * The text that the user has typed whilst the list has
     * been visible will be found between autoCompleteDotPosition
     * and autoCompleteEndPosition.
     */
    private Position autoCompleteEndPosition;

    /**
     * Stores the position of the open bracket for the last
     * method that was auto-completed.
     */
    private Position lastCompleteOpenBracketPosition = null;

    /**
     * Stores the position of the close bracket for the last
     * method that was auto-completed.
     */
    private Position lastCompleteCloseBracketPosition = null;

    /**
     * Stores the actual MoeDropDownMethod object for the last
     * method that was auto-completed.  Used to get parameters
     * and return type for the tool tip.
     */
    private MoeDropDownMethod lastCompletedMethod = null;

    /**
     * A reference to the single MoeToolTipManager for
     * this class.  Used to control showing, hiding
     * and updating of the bold argument of a tool tip.
     */
    private MoeToolTipManager toolTipMgr;



    //Private fields for configuration
    private boolean autoCompleteEnabled = false;
    private boolean toolTipsEnabled = false;
    private boolean dotCompleteEnabled = false;
    private boolean enterCompleteEnabled = false;
    private boolean spaceCompleteEnabled = false;
    private boolean tabCompleteEnabled = false;
    private boolean openBracketCompleteEnabled = false;
    private boolean closeBracketCompleteEnabled = false;
    private boolean mathematicalOperatorCompleteEnabled = false;
    private int toolTipFontSize;
    private int moeDropDownListHeight;
    private int moeDropDownListWidth;
    private int osBottomMargin;
    private int osRightMargin;
    private int osLeftMargin;

    //Some default values to be used if the properties
    //aren't in the moe.defs file.
    private int DEFAULT_TOOL_TIP_FONT_SIZE = 4;
    private int DEFAULT_MOE_DROP_DOWN_LIST_HEIGHT = 300;
    private int DEFAULT_MOE_DROP_DOWN_LIST_WIDTH = 130;



    /**
     * This is the constructor for an AutoCompleteManager.
     * It requires a reference to the MoeEditor Frame in order
     * to determine when it is closed, moved or minimized.
     * The reference to the JEditorPane is required so that
     * this manager can listen for events as changes are
     * made to the source code.
     *
     * @param file The file that is being edited in the MoeEditor window
     * @param moeEditorFrame The Frame of the editor that this
     *                       object is managing
     * @param sourcePane The JEditorPane of the editor that this
     *                   object is managing.
     */
    public MoeAutocompleteManager(File file,
                                  MoeEditor moeEditorFrame,
                                  JEditorPane sourcePane){

        this.moeEditorFrame = moeEditorFrame;
        this.sourcePane = sourcePane;

        getConfiguration();
        printConfiguration();

        if(autoCompleteEnabled){

            moeEditorFrame.addWindowFocusListener(this);
            moeEditorFrame.addWindowListener(this);
            moeEditorFrame.addComponentListener(this);


            document = (MoeSyntaxDocument) sourcePane.getDocument();
            document.addDocumentListener(this);
            sourcePane.addKeyListener(this);
            sourcePane.addCaretListener(this);

            //Get the viewport for the sourcePane
            //and register this as the ChangeListener
            Component comp = sourcePane.getParent();
            if(comp instanceof JViewport){
                JViewport viewPort = (JViewport) comp;
                viewPort.addChangeListener(this);
            }

            toolTipMgr = new MoeToolTipManager(sourcePane, toolTipFontSize);

            //Attempt to get the top most project directory
            //for the current file.  This will not work
            //if there is more than one project open at the
            //current time.  This is not satisfactory but it
            //is a reasonable fix for the moment
            bluej.pkgmgr.Project proj = bluej.pkgmgr.Project.getProject();
            if(proj==null){
                projectRoot = file.getParentFile();
                //Got Dir Of File Instead Of Project Root PROBLEM!!!!!!
            }
            else{
                projectRoot = proj.getProjectDir();
                //Got Proper Project Root
            }

            //Create the parser for this MoeAutoCompleteManager
            parser = new Parser(document, projectRoot);
            //Register this object as a listener for the parser.
            parser.addParserListener(this);
        }
    }



    /**
     * This method gets all the configuration settings for the
     * AutoCompleteManager from the moe.defs properties file
     * that is stored in the lib directory of BlueJ.
     */
    private void getConfiguration(){

        Properties editorProps = Config.moe_props;

        String autoCompleteMode = editorProps.getProperty("autocomplete","off");
        autoCompleteEnabled = convertToBoolean(autoCompleteMode);

        String toolTipMode = editorProps.getProperty("tooltips","off");
        toolTipsEnabled = convertToBoolean(toolTipMode);

        String dotComplete = editorProps.getProperty("dot.complete","off");
        dotCompleteEnabled = convertToBoolean(dotComplete);

        String enterComplete = editorProps.getProperty("enter.complete","off");
        enterCompleteEnabled = convertToBoolean(enterComplete);

        String spaceComplete = editorProps.getProperty("space.complete","off");
        spaceCompleteEnabled = convertToBoolean(spaceComplete);

        String tabComplete = editorProps.getProperty("tab.complete","off");
        tabCompleteEnabled = convertToBoolean(tabComplete);

        String openBracketComplete = editorProps.getProperty("open.bracket.complete","off");
        openBracketCompleteEnabled = convertToBoolean(openBracketComplete);

        String closeBracketComplete = editorProps.getProperty("close.bracket.complete","off");
        closeBracketCompleteEnabled = convertToBoolean(closeBracketComplete);

        String mathematicalOperatorComplete = editorProps.getProperty("mathematical.operator.complete","off");
        mathematicalOperatorCompleteEnabled = convertToBoolean(mathematicalOperatorComplete);

        String strToolTipFontSize = editorProps.getProperty("tool.tip.font.size", new Integer(DEFAULT_TOOL_TIP_FONT_SIZE).toString());
        toolTipFontSize = convertToInt(strToolTipFontSize, DEFAULT_TOOL_TIP_FONT_SIZE);

        String strMoeDropDownListHeight = editorProps.getProperty("list.height", new Integer(DEFAULT_MOE_DROP_DOWN_LIST_HEIGHT).toString());
        moeDropDownListHeight = convertToInt(strMoeDropDownListHeight, DEFAULT_MOE_DROP_DOWN_LIST_HEIGHT);

        String strMoeDropDownListWidth = editorProps.getProperty("list.width", new Integer(DEFAULT_MOE_DROP_DOWN_LIST_WIDTH).toString());
        moeDropDownListWidth = convertToInt(strMoeDropDownListWidth, DEFAULT_MOE_DROP_DOWN_LIST_WIDTH);

        String strOSBottomMargin = editorProps.getProperty("os.bottom.margin", "0");
        osBottomMargin = convertToInt(strOSBottomMargin, 0);

        String strOSRightMargin = editorProps.getProperty("os.right.margin", "0");
        osRightMargin = convertToInt(strOSRightMargin, 0);

        String strOSLeftMargin = editorProps.getProperty("os.left.margin", "0");
        osLeftMargin = convertToInt(strOSLeftMargin, 0);

    }



    /**
     * This method prints the configuration of the MoeAutoCompleteManager.
     * This should be used for debugging purposes only.
     */
    private void printConfiguration(){
        Debug.printConfigMsg("autoCompleteEnabled=" + autoCompleteEnabled);
        Debug.printConfigMsg("toolTipsEnabled=" + toolTipsEnabled);
        Debug.printConfigMsg("dotCompleteEnabled=" + dotCompleteEnabled);
        Debug.printConfigMsg("enterCompleteEnabled=" + enterCompleteEnabled);
        Debug.printConfigMsg("spaceCompleteEnabled=" + spaceCompleteEnabled);
        Debug.printConfigMsg("tabCompleteEnabled=" + tabCompleteEnabled);
        Debug.printConfigMsg("openBracketCompleteEnabled=" + openBracketCompleteEnabled);
        Debug.printConfigMsg("closeBracketCompleteEnabled=" + closeBracketCompleteEnabled);
        Debug.printConfigMsg("mathematicalOperatorCompleteEnabled=" + mathematicalOperatorCompleteEnabled);
        Debug.printConfigMsg("toolTipFontSize=" + toolTipFontSize);
        Debug.printConfigMsg("moeDropDownListHeight=" + moeDropDownListHeight);
        Debug.printConfigMsg("moeDropDownListWidth=" + moeDropDownListWidth);
        Debug.printConfigMsg("osBottomMargin=" + osBottomMargin);
        Debug.printConfigMsg("osRightMargin=" + osRightMargin);
        Debug.printConfigMsg("osLeftMargin=" + osLeftMargin);
    }



    /**
     * This method returns a boolean value of a String.
     * The method returns true if the String contains
     * on, yes, y or true.  If it doesn't the method
     * returns false.  The search is case insensitive.
     *
     * @param s The string to be converted to a boolean.
     * @return the boolean value of the given String.
     */
    private boolean convertToBoolean(String s){
        if(s==null){
            return false;
        }
        else{
            String t = s.trim();
            if(t.equalsIgnoreCase("on") ||
               t.equalsIgnoreCase("yes") ||
               t.equalsIgnoreCase("y") ||
               t.equalsIgnoreCase("true")){
                return true;
            }
            else{
                return false;
            }
        }
    }



    /**
     * This method returns the integer value of the given String.
     * If the String doesn't contain an integer the default value
     * is returned.
     *
     * @param s The String to be converted to an int.
     * @param def The default value to be returned if the String
     *            doesn't contain an integer.
     * @return The integer value for the given String.
     */
    private int convertToInt(String s, int def){
        int val;
        try{
            val = Integer.parseInt(s);
            return val;
        }
        catch(NumberFormatException e){
            return def;
        }
    }


    /**
     * This method should be called after a dot has been pressed and
     * before the parsing begins.  It sets the initial auto-complete
     * region for a typed dot
     *
     * @param dotPos the offset of the dot in the document.
     */
    private void setDotPosition(int dotPos) throws BadLocationException{
        autoCompleteDotPosition = document.createPosition(dotPos);
        autoCompleteEndPosition = document.createPosition(dotPos+1);
    }



    /**
     * This method shows a MoeDropDown list at the given Position in the
     * document.  The MoeDropDownList will not be shown if the items
     * ArrayList is null or doesn't contain any items.  It will also
     * not be shown if the specified dotPosition is null.
     * The method determines the screen location of the given
     * position in the document and in normal circumstances will display
     * the list just below the dot character.  If there isn't enough room
     * below the line the list will be shown above the line instead. This is
     * determined by seeing whether the bottom of the list would overlap
     * the bottom margin of the operating system.  The list will also be
     * positioned to the furthest right point if it cannot be positioned
     * in its normal location without disappearing off the edge of screen.
     * The MoeDropDownList will initially be populated with all the items
     * in the ArrayList that begin with the initial match.  If no matches
     * are initially found the list will not be shown.
     * The ArrayList must only contain MoeDropDownItems and the list will
     * not be shown if the ArrayList is empty.
     *
     * @param items An ArrayList of MoeDropDownItems to be shown in a
     *              MoeDropDownList.
     * @param initialMatch only the items that begin with this string
     *                     are initially displayed in the list.  The list
     *                     will not be displayed if none of the items begin
     *                     with this String.
     * @param dotPosition A Position object representing the location of
     *                    the dot in the document.
     */
    private void showList(ArrayList items,
                          String initialMatch,
                          Position dotPosition){

        if(items==null || dotPosition==null) return;
        if(items.size() < 1) return;

        try{
            Rectangle dotRectangle =
                getScreenLocationOfOffset(dotPosition.getOffset());

            if(dotRectangle!=null){
                Point screenLocation =
                    new Point(dotRectangle.x + 3,
                              dotRectangle.y + dotRectangle.height);

                Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();

                if(screenLocation.getY() + moeDropDownListHeight >
                   screenSize.height - osBottomMargin){

                    screenLocation =
                        new Point(screenLocation.x,
                                  dotRectangle.y - moeDropDownListHeight);

                }

                if(screenLocation.getX() + moeDropDownListWidth >
                   screenSize.width - osRightMargin){

                    screenLocation =
                        new Point(screenSize.width - moeDropDownListWidth -
                                  osRightMargin, screenLocation.y);
                }

                if(screenLocation.getX() < osLeftMargin){

                    screenLocation =
                        new Point(osLeftMargin, screenLocation.y);
                }

                showList(items, initialMatch, screenLocation);
            }
        }
        catch(BadLocationException ble){
        }
    }



    /**
     * This method shows a MoeDropDown list at the given location on the
     * screen.  This method should not be called directly.  Use the
     * showList(ArrayList items, Position dotPosition) variant instead.
     *
     * @param items An ArrayList of MoeDropDownItems to be shown in a
     *              MoeDropDownList.
     * @param initialMatch only the items that begin with this string
     *                     are initially displayed in the list.  The list
     *                     will not be displayed if none of the items begin
     *                     with this String.
     * @param screenLocation The screen location where the top left of
     *                       the list should be positioned.
     */
    private void showList(ArrayList items, String initialMatch, Point screenLocation){

        if(items==null || screenLocation==null) return;
        if(items.size() < 1) return;

        if(mddList!=null) hideList();

        try{
            mddList = new MoeDropDownList(items, initialMatch, moeEditorFrame);

            mddList.addWindowListener(this);
            mddList.addWindowFocusListener(this);
            mddList.setLocation(screenLocation);
            mddList.setSize(moeDropDownListWidth, moeDropDownListHeight);
            mddList.toFront();
            mddList.addMoeDropDownListMouseListener(this);
            mddList.setVisible(true);
        }
        catch(NoInitialMatchException e){
            //No point showing an empty list
            mddList=null;
            Debug.printAutoCompleteInfo(e.getMessage());
        }
    }



    /**
     * This method hides the MoeDropDownList if it is visible.
     */
    private void hideList(){
        if(mddList!=null){
            mddList.removeWindowFocusListener(this);
            mddList.removeWindowListener(this);
            mddList.removeMoeDropDownListMouseListener(this);
            mddList.hide();
            mddList.setVisible(false);
            mddList.dispose();
            mddList=null;
        }

        //THIS COULD BE A MAJOR MEMORY PROBLEM
        //OWNED WINDOWS ARRAY KEEPS INCREASING IN SIZE
        Debug.printOwnedWindowsMsg("Owned Windows =" + moeEditorFrame.getOwnedWindows().length);
    }



    /**
     * This method can be used to determine whether a MoeDropDownList
     * is currently being shown.
     *
     * @return whether a MoeDropDownList is currently being shown.
     */
    private boolean listVisible(){
        return mddList!=null;
    }



    /**
     * This method can be used to auto-complete the users chosen item.
     * The insertable code will be calculated from the specified
     * MoeDropDownItem.  If the item is a MoeDropDownMethod the
     * insertable code will also include the open and close brackets
     * for the parameters section.  If the method takes no arguments
     * the cursor will be positioned after the close bracket.  If the
     * method takes one or more arguments the cursor will be positioned
     * between the brackets.  If the chosen item is a MoeDropDownField
     * or a MoeDropDownPackage no brackets will be inserted.  This method
     * takes the spaceBeforeCursor argument in order to support completion
     * by using the space bar.  If this is true an extra space is inserted
     * before the location of the cursor during the auto complete.
     *
     * @param mddItem The users chosen MoeDropDownItem to be used in the
     *                auto-complete process.
     * @param spaceBeforeCursor whether an extra space should be included
     *                          in the auto-complete text before the cursor.
     * @throws BadLocationException if the method tries to auto-complete
     *                              in an invalid location.
     */
    private void autoComplete(MoeDropDownItem mddItem, boolean spaceBeforeCursor)
                              throws BadLocationException{
        String maybeSpace = "";
        if(spaceBeforeCursor) maybeSpace = " ";

        if(mddItem instanceof MoeDropDownMethod){
            MoeDropDownMethod mddMethod = (MoeDropDownMethod) mddItem;
            int args = mddMethod.getNoOfArguments();
            if(args==0){
                autoComplete(mddMethod, mddMethod.getInsertableCode() + "()" + maybeSpace,"");
            }
            else if(args>=1){
                autoComplete(mddMethod, mddMethod.getInsertableCode() + "(" + maybeSpace,")");
            }
        }
        else{
            autoComplete(mddItem, mddItem.getInsertableCode() + maybeSpace,"");
        }
    }


    /**
     * This method does the main work for the auto-complete process.
     * In most circumstances this method will be called from the
     * autoComplete(MoeDropDownItem mddItem, boolean spaceBeforeCursor)
     * method but this method can be called directly in a few specialised
     * cases.  For this method the auto-complete code should be broken
     * up into 2 sections called beforeCursor and afterCursor.  This
     * provides an intuitive way of specifying where the cursor should be
     * placed after the auto-complete process.  The cursor is simply
     * placed after the beforeCursor String in the document and before
     * the afterCursor String in the document. The MoeDropDownItem
     * that is given to this method is not used to determine the
     * code to be inserted into the document.  It is simply  used to
     * determine when a method is being completed and sets up the tool
     * tip functionality for the arguments of the method.
     *
     * @param mddItem The users chosen MoeDropDownItem.
     * @param beforeCursor first half of insertable code.
     * @param afterCursor second half of insertable code.
     */
    private void autoComplete(MoeDropDownItem mddItem, String beforeCursor, String afterCursor)
                              throws BadLocationException{

        if(autoCompleteDotPosition==null) throw new BadLocationException("Start of auto-complete region not set", -1);
        if(beforeCursor==null) beforeCursor = "";
        if(afterCursor==null) afterCursor = "";

        //Determine the region in the document that needs to
        //be replaced with the auto-complete text.
        int start = autoCompleteDotPosition.getOffset() + 1;
        int end = autoCompleteEndPosition.getOffset();

        //Calculate the auto-complete text.
        String code = beforeCursor + afterCursor;

        //Replace the code that the user has typed with the
        //auto-complete text.
        replace(start, end, code);

        //Position the caret so that it appears between the
        //beforeCursor text and the afterCursor text.
        sourcePane.setCaretPosition(start+beforeCursor.length());

        //Let's hide the drop down list
        hideList();

        //Standard auto-complete functionality done at this point


        if(mddItem instanceof MoeDropDownMethod){
            //We have just auto-completed a method
            //so we need to determine the region in
            //the document where the tool tip should
            //be displayed.  We need to find out the
            //location of the open bracket and
            //possibly the close bracket.

            //Store the last auto-completed method
            lastCompletedMethod = (MoeDropDownMethod) mddItem;

            //Determine the locations of the brackets
            int openBracketPos = code.indexOf("(");
            int closeBracketPos = code.indexOf(")");

            if(openBracketPos>=0){

                //Calculate the position of the open bracket in the document
                lastCompleteOpenBracketPosition =
                    document.createPosition(start + openBracketPos);

                if(closeBracketPos >= 0){
                    //Calculate the position of the close bracket in the document
                    lastCompleteCloseBracketPosition =
                        document.createPosition(start + closeBracketPos);
                }
                else{
                    //A close bracket was not inserted into the document
                    //but we need to specify the position where the
                    //tool tip should be hidden if the cursor goes
                    //beyond that point.
                    lastCompleteCloseBracketPosition =
                        document.createPosition(start + openBracketPos + 1);
                }
            }
            else{
                //No open bracket was present in the auto-complete
                //text.  This should never occur but let's be
                //safe and prevent a tool tip from showing.
                lastCompleteOpenBracketPosition = null;
                lastCompleteCloseBracketPosition = null;
            }

            //Force the tool tip to be shown if the caret is
            //positioned between the positions of the open
            //and close brackets.
            if(toolTipMgr.toolTipVisible()) toolTipMgr.hideToolTip();
            showHideOrUpdateToolTip();

        }
    }


    /**
     * This method replaces a region of text in the document
     * with the specified String.  The replace region is
     * specified using two integers, start and end.
     *
     * @param start The start offset of the region that needs to be replaced.
     * @param end The end offset of the region that needs to be replaced.
     * @param txt The 'replace with' String.
     */
    private void replace(int start, int end, String txt)
                         throws BadLocationException {
        document.removeDocumentListener(this);
        int l = end - start;
        document.remove(start, l);
        document.insertString(start, txt, null);
        document.addDocumentListener(this);
    }



    /**
     * This method returns the line number of the line
     * that the caret is currently on.
     *
     * @return the current line number.
     */
    private int getCurrentLineNumber(){
        Element map = document.getDefaultRootElement();
        int currentPosition = sourcePane.getCaretPosition();
        int line = map.getElementIndex(currentPosition);
        return line;
    }



    /**
     * This method returns the line number for a given offset
     * in the document.
     */
    private int getLineNumberForOffset(int offset){
        Element map = document.getDefaultRootElement();
        return map.getElementIndex(offset);
    }



//--DocumentListener interface implementations --------------------------------

    public void insertUpdate(DocumentEvent e) {
        documentChanged(e);
    }

    public void removeUpdate(DocumentEvent e) {
        documentChanged(e);
    }

    public void changedUpdate(DocumentEvent e) {
        documentChanged(e);
    }

//-----------------------------------------------------------------------------

    /**
     * This method is called whenever a change is made to the contents
     * of the document.  This is a very important method as it
     * controls when a MoeDropDownList is shown, hidden or has
     * it's best matches changed.
     *
     * @param e the document event
     */
    private void documentChanged(DocumentEvent e) {

        //Get the offset within the document of the start of the change.
        int pos = e.getOffset();

        String character = null;

        try{
            character = document.getText(pos, 1);  //The last character typed.
        }
        catch(BadLocationException ble){}


        if(listVisible()){
            //A drop down list is visible
            //Remember that autoCompleteDotPosition is the location of
            //the dot for the displayed drop down list.

            if(pos > autoCompleteDotPosition.getOffset()){

                if(autoCompleteEndPosition!=null){

                    String manuallyEnteredTxt = getTextInAutoCompleteRegion();
                    if(manuallyEnteredTxt==null) manuallyEnteredTxt="";

                    Debug.printAutoCompleteInfo
                        ("manuallyEnteredTxt=[" + manuallyEnteredTxt + "]");

                    int matchingItems =
                        mddList.selectBestMatch(manuallyEnteredTxt);

                    if(matchingItems==0) hideList();
                }
            }
        }
        else{
            //A drop down list is not visible
            if(character!=null && character.equals(".")){
                dotPressedWhenListNotVisible(pos);
            }
        }
    }



    /**
     * This method returns the text in the auto-complete region.
     * This text is what the user has typed whilst the
     * MoeDropDownList has been displayed.
     *
     * @return the text in the auto-complete region.
     *         Null if the region is not set or a BadLocationException
     *         is thrown.
     */
    private String getTextInAutoCompleteRegion(){

        if(autoCompleteDotPosition==null) return null;
        if(autoCompleteEndPosition==null) return null;

        int start = autoCompleteDotPosition.getOffset() + 1;
        int end = autoCompleteEndPosition.getOffset();
        int len = end - start;

        try{
             return document.getText(start, len);
        }
        catch(BadLocationException ble){
            return null;
        }
    }



    /**
     * This method is called whenever a dot is pressed and
     * a MoeDropDownList is not visible.  This method
     * activates the parsing and attempts to resolve the
     * type of the previous expression.  This class
     * implements the ParserListener interface and is
     * notified when the type is established or not
     * established.  This method shouldn't really
     * start the parsing if the dot is inserted in
     * a comment or String.  Can the MoeSyntaxDocument
     * be used to determine this?
     *
     * @param dotPos the location of the inserted dot.
     */
    private void dotPressedWhenListNotVisible(int dotPos){
        if(parser.isParsing()) return;

        try{
            setDotPosition(dotPos);
            //Parse the file and attempt to resolve the class type
            parser.parseAndDetermineType(dotPos);
        }
        catch(BadLocationException e){
        }
    }



//--ParserListener interface implementations ----------------------------------

    /**
     * This method is called by the parser when a type has
     * been successfully determined.
     *
     * @param cls A Class object representing the type of the expression
     * @param items An ArrayList containing the MoeDropDownItems for
     *              the expression.
     */
    public void typeEstablished(Class cls, ArrayList items){

        if(cls==null) return;
        if(!caretInAutoCompleteRegion()) return;

        int dotPosOffset = autoCompleteDotPosition.getOffset();

        Debug.printParserResultsMessage("DETERMINED CLASS");
        Debug.printParserResultsMessage("foundClass=" + cls);

        if(!cls.isPrimitive()){
            //ArrayList items = getMoeDropDownItems(cls, false);
            showList(items, getTextInAutoCompleteRegion(), autoCompleteDotPosition);
        }


    }


    /**
     * This method is called by the parser when the type cannot
     * be determined.
     */
    public void typeNotEstablished(){

        if(!caretInAutoCompleteRegion()) return;

        Debug.printParserResultsMessage("COULD NOT DETERMINE CLASS TYPE");

        //The type of the expression was not determined but it may be
        //the start of a package statement.
        AvailablePackages pkgs = new AvailablePackages(moeEditorFrame.getProjectClassLoader());

        // Get a String array containing all the package roots
        // (ie java, javax, org)
        String[] pkgRoots = pkgs.getPackageRoots();

        //See if any of the package roots match what has been
        //typed before the dot character.

        int dotPosOffset = autoCompleteDotPosition.getOffset();

        for(int i=0; i<pkgRoots.length; i++){
            String pkgRoot = pkgRoots[i];
            if(identifierMatchBeforeOffset(dotPosOffset, pkgRoot)){
                //We have a match.  Show all the packages that begin
                //with the package root in a MoeDropDownList.
                ArrayList items =
                    pkgs.getMoeDropDownPackagesWithRoot(pkgRoot);

                showList(items,
                         getTextInAutoCompleteRegion(),
                         autoCompleteDotPosition);

                break;
            }
        }
    }
//-----------------------------------------------------------------------------



    /**
     * This method looks at the contents of the document to see whether the
     * given identifier appears before the given offset.  This method will only
     * return true if there is a match and the character before the match is
     * not a valid identifier character.
     *
     * @param offset the position in the document that we want to match before.
     * @param identifier the identifier we are looking for before the given
     *                   offset
     * @return true if the identifier appears before the given offset.
     */
    private boolean identifierMatchBeforeOffset(int offset, String identifier){
        try{
            int len = identifier.length();
            int startOffset = offset - len;
            String typed = document.getText(startOffset, len);
            if(typed==null) return false;

            if(typed.equals(identifier)){
                //We've found a match but we need to make
                //sure that the character before the match
                //is not a valid identifier character.
                int beforeMatch = startOffset - 1;
                if(beforeMatch>=0){
                    String charBeforeMatch = document.getText(beforeMatch, 1);
                    char c = charBeforeMatch.charAt(0);
                    if(Character.isUnicodeIdentifierStart(c) ||
                       Character.isJavaIdentifierPart(c) ||
                       Character.isJavaIdentifierStart(c)){
                        return false;
                    }
                    else{
                        return true;
                    }

                }
                else{
                    //There isn't a character before the match
                    return true;
                }

            }
            else{
                return false;
            }
        }
        catch(BadLocationException e){
            return false;
        }
    }



//--MoeDropDownListMouseListener interface implementations --------------------

    /**
     * This method is called whenever the user clicks on an item
     * in the MoeDropDownList.
     *
     * @param mddItem The MoeDropDownItem that was clicked upon.
     */
    public void mouseClicked(MoeDropDownItem mddItem){}


    /**
     * This method is called whenever the user clicks on an item
     * in the MoeDropDownList.
     *
     * @param mddItem The MoeDropDownItem that was clicked upon.
     */
    public void mouseDoubleClicked(MoeDropDownItem mddItem){
        try{
            autoComplete(mddItem, false);
        }
        catch(BadLocationException exception){
        }
    }

//-----------------------------------------------------------------------------



    /**
     * This method can be used to determine if the cursor is in
     * the auto-complete region.
     *
     * @return true if the cursor is in the auto-complete region.
     */
    private boolean caretInAutoCompleteRegion(){
        if(autoCompleteDotPosition==null) return false;
        if(autoCompleteEndPosition==null) return false;

        int caretPos = sourcePane.getCaretPosition();
        int caretLine = getLineNumberForOffset(caretPos);
        int dotLine = getLineNumberForOffset(autoCompleteDotPosition.getOffset());
        if(!(caretLine==dotLine)) return false;

        return (caretPos > autoCompleteDotPosition.getOffset() &&
               caretPos<=autoCompleteEndPosition.getOffset());
    }



//--CaretListener interface implementations -----------------------------------

    /** This method implements the CaretListener interface and is called
      * whenever the caret position is changed within the source code.
      * If the list is visible and the caret has been moved outside of
      * the auto complete region the list is hidden.  If the list is not
      * visible a tool tip will be shown if the caret is positioned
      * between the argument brackets of the last auto-completed method.
      * If the list is not visible but a tool tip is being shown the
      * tool tip will disappear if the caret is no longer situated
      * between the argument brackets of the last auto-completed method.
      *
      * @param e the caret event
      */
    public void caretUpdate(CaretEvent e){

        if(listVisible()){
            // The drop down list is visible
            // We need to check to make sure that the caret is still
            // within the auto complete region.  If it isn't we should
            // hide the list.
            if(autoCompleteDotPosition!=null &&
               autoCompleteEndPosition!=null){

                if(!caretInAutoCompleteRegion()){
                    hideList();
                }
            }
        }
        else{
            // The drop down list is not visible.  The following call
            // will show the tool tip if the cursor is between the
            // argument brackets of the last auto-completed method.
            // The call will also hide a visible tool tip if the
            // cursor is not between the argument brackets of the
            // last auto-completed method.
            showHideOrUpdateToolTip();
        }
    }

//-----------------------------------------------------------------------------



    /**
     * This method clears all information about the previously
     * completed method.  It discards the positions of the
     * opening and closing brackets and the actual MoeDropDownMethod
     * that was selected.  This will prevent a tool tip from
     * being displayed until another method is auto-completed.
     */
    private void clearLastAutocompleteData(){
        lastCompleteOpenBracketPosition=null;
        lastCompleteCloseBracketPosition=null;
        lastCompletedMethod = null;
    }



    /**
     * This method can be used to determine whether the information
     * stored about the previously auto-completed method is still valid.
     * It will return false if the information has been cleared using
     * the clearLastAutocompleteData method.  It will also return
     * false if the position where the opening bracket should be
     * no longer contains an opening bracket.  This can occur
     * if the user deletes the bracket or replaces it with another
     * character.
     *
     * @return true if the last auto-completed method is known and an
     *              opening bracket is still found at the expected place.
     */
    private boolean lastAutcompleteDataValid(){

        if(lastCompleteOpenBracketPosition!=null &&
           lastCompletedMethod!=null){

            try{
                int pos = lastCompleteOpenBracketPosition.getOffset();
                String openBracket = document.getText(pos, 1);

                if (openBracket.equals("(")){
                    return true;
                }
                else{
                    return false;
                }

            }
            catch(BadLocationException e){
                return false;
            }
        }
        else{
            return false;
        }
    }



    /**
     * This method is normally called when the position of the caret changes
     * in the document.  It is responsible for hiding and showing the
     * tool tip for the last method that was completed.  It is also
     * responsible for determining the current argument that is expected
     * and highlights the required argument in the tool tip bold.  If the
     * tool tips are disabled this method will simply return without
     * doing anything.
     *
     * The method initially checks to see whether the information stored
     * about the brackets of the last auto-completed method is still
     * correct.  If this information is invalid any visible tool tip
     * will be hidden.  If the bracket information is correct but the
     * cursor is not situated between the brackets any visible
     * tool tip will be hidden.  If the bracket information is correct
     * and the cursor is between the brackets the tool tip will
     * be shown with the expected argument highlighted in bold.
     */
    private void showHideOrUpdateToolTip(){
        if(!toolTipsEnabled) return;

        if(lastAutcompleteDataValid()){

            int caretPos = sourcePane.getCaretPosition();

            if(caretPos > lastCompleteOpenBracketPosition.getOffset()  &&
               caretPos <= lastCompleteCloseBracketPosition.getOffset()){

                //The caret is between the argument brackets
                //of the last auto-completed method

                //Determine the offset of the open bracket
                int openBracketOffset =
                    lastCompleteOpenBracketPosition.getOffset();
                int argumentStartOffset = openBracketOffset + 1;

                try{
                    //Attempt to find the matching close bracket
                    //for the opening argument bracket.
                    int closeBracketOffset =
                        TextUtilities.findMatchingBracket
                                      (document, openBracketOffset);

                    if(closeBracketOffset > 0){
                        //We have found the matching close bracket
                        lastCompleteCloseBracketPosition =
                            document.createPosition(closeBracketOffset);
                        if(caretPos > closeBracketOffset){
                            if(toolTipMgr.toolTipVisible()){
                                toolTipMgr.hideToolTip();
                                return;
                            }
                        }
                    }

                    //Work out which argument is expected by counting
                    //the number of argument separating commas
                    //between the opening bracket and the cursor
                    String textBetweenOpenBracketAndCaret =
                        document.getText(argumentStartOffset,
                                         caretPos-argumentStartOffset);

                    int argNo = countArgumentSeparatorCommas
                                (textBetweenOpenBracketAndCaret);

                    //Determine the area on the screen where the open
                    //bracket is located and use this to calculate
                    //where the tool tip should be positioned.
                    Rectangle openBracketRectangle =
                        getScreenLocationOfOffset(openBracketOffset);
                    Point toolTipPosition =
                        new Point(openBracketRectangle.x + 5,
                                  openBracketRectangle.y -
                                  toolTipMgr.getToolTipHeight());

                    //Show the tool tip if it isn't visible.
                    //If it is already visible just update
                    //the bold argument.
                    if(toolTipMgr.toolTipVisible()){
                        toolTipMgr.setBoldArgument(argNo);
                    }
                    else{
                        String[] args =
                            lastCompletedMethod.getArgumentDescriptions();

                        String returnType =
                            lastCompletedMethod.getReturnType();

                        toolTipMgr.showToolTip(toolTipPosition,
                                               args,
                                               returnType,
                                               argNo);
                    }
                }
                catch(BadLocationException e){
                    //Hide the tool tip if it is visible.
                    if(toolTipMgr.toolTipVisible()) toolTipMgr.hideToolTip();
                }

            }
            else{
                //The bracket information is still valid but the cursor is
                //not positioned between the brackets.
                if(toolTipMgr.toolTipVisible()) toolTipMgr.hideToolTip();
            }
        }
        else{
            //The bracket information contains invalid data so completely
            //clear the data and hide the tool tip if it is visible.
            clearLastAutocompleteData();
            if(toolTipMgr.toolTipVisible()) toolTipMgr.hideToolTip();
        }
    }



    /**
     *  This method is able to count the argument separators in
     *  a given String.  It will ignore all commas that are
     *  enclosed within brackets at all levels of nesting.
     *  The String given to this method should typically
     *  be the String between the open bracket of the
     *  parameters section of a method and the cursor.  The
     *  String should not start with an open bracket
     *  and end with a close bracket because no argument
     *  separators will be counted in this case.
     *
     *  "getInt(getInt(7,7))  ,  6  ,  getString(1,5) , 6"
     *  will give the result of 3 as the String contains
     *  3 commas that separate the arguments.
     *
     * @param s A string that contains arguments separated by commas.
     * @return the number of commas in the String that are argument separators.
     */
    private int countArgumentSeparatorCommas(String s){
        if(s==null){
            return 0;
        }
        else{
            int count = 0;
            int level = 0;
            for(int i=0; i<s.length(); i++){
                char c = s.charAt(i);
                if(c=='(' || c=='[' || c=='{'){
                    level++;
                }
                else if(c==')' || c==']' || c== '}'){
                    level--;
                }
                else if(c==','){
                    if(level==0) count++;
                }
            }
            return count;
        }
    }



    /**
     * This method can be used to determine the screen location
     * of a character at a specified offset in the document. This
     * method originally returned a Point object but it has been
     * changed to a rectangle so that we also get the height of
     * the line.  This is necessary because the font size can be
     * changed and this obviously increases the height of the
     * line.  When calculating the position of a MoeDropDownList
     * the y position should be calculated by adding the
     * ypos of the returned rectangle onto the height of the
     * returned rectangle.
     *
     * @param offset the offset in the document that you
     *               wish to know the screen location of.
     * @return a Rectangle object that represents the screen
     *         location of the specified offset in the document.
     * @throws BadLocationException if the specified offset is an
     *                              invalid location in the document.
     */
    private Rectangle getScreenLocationOfOffset(int offset)
                      throws BadLocationException{

        Point a = sourcePane.getLocationOnScreen();
        TextUI ui = sourcePane.getUI();
        Rectangle r = ui.modelToView(sourcePane, offset);
        Point b = r.getLocation();

        return new Rectangle(a.x + b.x, a.y + b.y, r.width, r.height);
    }



//--KeyListener interface implementations -------------------------------------

    private boolean disableOpenBracket = false;
    private boolean disableCloseBracket = false;

    /**
     * This method implements the KeyListener interface and is called
     * whenever a key is pressed and the sourcePane has the focus.
     * It is responsible for detecting when any of the auto-complete
     * keys are pressed and for providing control functionality
     * for the MoeDropDownList.  In many cases the key event is consumed
     * so that the default effect of the key doesn't actually get applied
     * to the JEditorPane.  That way we can provide our own customised
     * functionality for certain keys.  Normally for example the up and
     * down arrow keys move the cursor up and down.  When the list is
     * visible however the events are consumed and we make the selected
     * item in the list scroll up and down instead.  This method is
     * very important for the auto-complete functionality because it
     * detects when any of the auto-complete keys are pressed.
     *
     * @param e the KeyEvent
     */
    public void keyPressed(KeyEvent e){

        if(listVisible()){
            //The list is visible
            if(e.getKeyChar() == '.'){

                Debug.printAutoCompleteInfo("Dot Key Pressed Whilst List Visible");

                if(mddList.itemSelected()){

                    if(dotCompleteEnabled){
                        MoeDropDownItem mddItem = mddList.getSelectedItem();

                        if(mddItem instanceof MoeDropDownMethod){
                            MoeDropDownMethod mddMethod = (MoeDropDownMethod) mddItem;
                            if(mddMethod.getNoOfArguments() >= 1){
                                return;
                            }
                            else{
                                hideList();
                                try{autoComplete(mddItem, false);}
                                catch(BadLocationException exception){}
                            }
                        }
                        else if(mddItem instanceof MoeDropDownField){
                            hideList();
                            try{autoComplete(mddItem, false);}
                            catch(BadLocationException exception){}
                        }
                    }
                    else{
                        hideList();
                    }
                }
            }
            else if(e.getKeyCode() == e.VK_ESCAPE){
                hideList();
            }
            else if( e.getKeyCode() == e.VK_ENTER){
                //Auto-Complete if an item is selected
                //Otherwise let a new line be inserted
                //by not consuming the key event.
                if(enterCompleteEnabled && mddList.itemSelected()){
                    e.consume();
                    autoCompleteKeyPressed();
                }
            }
            else if( e.getKeyCode() == e.VK_SPACE){

                if(spaceCompleteEnabled && mddList.itemSelected()){
                    e.consume();
                    autoCompleteKeyPressed();
                }
            }
            else if( e.getKeyCode() == e.VK_TAB){
                //Auto-Complete if an item is selected
                //Otherwise let a tab character be inserted
                //by not consuming the key event.
                if(tabCompleteEnabled && mddList.itemSelected()){
                    e.consume();
                    autoCompleteKeyPressed();
                }
            }
            else if(e.getKeyChar() == '('){

                if(openBracketCompleteEnabled){
                    if(mddList.methodSelected()){
                        disableOpenBracket = true;
                        e.consume();
                        openBracketPressed();
                    }
                }
            }
            else if(e.getKeyChar() == ')'){

                if(closeBracketCompleteEnabled && mddList.methodSelected()){
                    disableCloseBracket = true;
                    e.consume();
                    closeBracketPressed();
                }
            }
            else if(e.getKeyChar() == '=' ||
                    e.getKeyChar() == '+' ||
                    e.getKeyChar() == '-' ||
                    e.getKeyChar() == '*' ||
                    e.getKeyChar() == '/'){
                if(mathematicalOperatorCompleteEnabled){
                    e.consume();
                    mathematicalOperatorKeyPressed();
                }
            }
            else if(e.getKeyCode() == e.VK_UP){
                e.consume();
                mddList.moveSelectionUp();
            }
            else if(e.getKeyCode() == e.VK_DOWN){
                e.consume();
                mddList.moveSelectionDown();
            }
            else if(e.getKeyCode() == e.VK_PAGE_UP){
                e.consume();
                mddList.pageUp();
            }
            else if(e.getKeyCode() == e.VK_PAGE_DOWN){
                e.consume();
                mddList.pageDown();
            }
            else if(e.getKeyCode() == e.VK_HOME){
                e.consume();
                mddList.selectFirst();
            }
            else if(e.getKeyCode() == e.VK_END){
                e.consume();
                mddList.selectLast();
            }
        }
        else{
            //The list is not visible
            disableOpenBracket = false;
            disableCloseBracket = false;

            if(e.getKeyCode() == e.VK_ESCAPE){
                //If a code completion tool tip is visible hide it.
                if(toolTipMgr.toolTipVisible()){
                    //Clear the last auto completed data to
                    //prevent the tool tip from showing
                    //again when the caret is moved.
                    clearLastAutocompleteData();
                    toolTipMgr.hideToolTip();
                }
            }
        }

    }

    /**
     * This method prevents brackets from being inserted
     * into the source code when disableOpenBracket /
     * disableCloseBracket are true.
     *
     * @param e the KeyEvent
     */
    public void keyReleased(KeyEvent e){
        if(disableOpenBracket){
            if(e.getKeyChar() == '('){
                e.consume();
            }
        }
        if(disableCloseBracket){
            if(e.getKeyChar() == ')'){
                e.consume();
            }
        }
    }

    /**
     * This method prevents brackets from being inserted
     * into the source code when disableOpenBracket /
     * disableCloseBracket are true.
     *
     * @param e the KeyEvent
     */
    public void keyTyped(KeyEvent e){
        if(disableOpenBracket){
            if(e.getKeyChar() == '('){
                e.consume();
            }
        }
        if(disableCloseBracket){
            if(e.getKeyChar() == ')'){
                e.consume();
            }
        }
    }

//-----------------------------------------------------------------------------


    /**
     * This method call can be used for the majority of
     * key presses that trigger the auto-complete functionality.
     * Some keys however require different implementations and
     * cannot use this method (ie '(', ')' and mathematical
     * operator keys.
     */
    private void autoCompleteKeyPressed(){

        if(mddList.itemSelected()){

            MoeDropDownItem mddItem = mddList.getSelectedItem();
            hideList();

            try{autoComplete(mddItem, false);}
            catch(BadLocationException exception){
            }
        }
    }


    /**
     * This method provides the auto-complete functionality when
     * an open bracket '(' is pressed.
     */
    private void openBracketPressed(){
        if(mddList.itemSelected()){
            MoeDropDownItem mddItem = mddList.getSelectedItem();
            if(mddItem instanceof MoeDropDownMethod){
                try{autoComplete(mddItem, mddItem.getInsertableCode() + "(", "");}
                catch(BadLocationException exception){}
            }
        }
    }

    /**
     * This method provides the auto-complete functionality when
     * a close bracket ')' is pressed.
     */
    private void closeBracketPressed(){
        autoCompleteKeyPressed();
    }

    /**
     * This method provides the auto-complete functionality when
     * a mathematical operator key ('+', '-', '*', '/', '=') is
     * pressed.  These keys will only auto-complete for fields
     * and methods that take no arguments.
     */
    private void mathematicalOperatorKeyPressed(){
        if(mddList.itemSelected()){
            MoeDropDownItem mddItem = mddList.getSelectedItem();
            if(mddItem instanceof MoeDropDownField){
                autoCompleteKeyPressed();
            }
            else if(mddItem instanceof MoeDropDownMethod){
                MoeDropDownMethod mddMethod = (MoeDropDownMethod) mddItem;
                if(mddMethod.getNoOfArguments()==0){
                    autoCompleteKeyPressed();
                }
            }
        }
    }





//--WindowFocusListener interface implementations -----------------------------

    /**
     * This method implements the WindowFocusListener interface and is called
     * whenever the MoeDropDownList or the MoeEditor window gain the focus.
     *
     * @param e the WindowEvent that is used to detect
     *          which window has gained the focus
     */
    public void windowGainedFocus(WindowEvent e){

        if(e.getWindow() == mddList){
            //The MoeDropDownList has gained the focus
            //We must give the focus back to the sourcePane
            //so that the user can continue to type,
            //control the list and the auto-complete
            //functionality.
            Debug.printWindowEventMessage("Moe Drop Down List Has Gained The Focus");
            sourcePane.requestFocus();
        }
        else if(e.getWindow() == moeEditorFrame){
            Debug.printWindowEventMessage("Moe Editor Has Gained The Focus");
        }
    }


    /**
     * This method implements the WindowFocusListener interface and is called
     * whenever the MoeDropDownList or the MoeEditor loose the focus.
     *
     * @param e the WindowEvent that is used to detect
     *          which window has lost the focus
     */
    public void windowLostFocus(WindowEvent e){

        if(e.getWindow() == mddList){
            Debug.printWindowEventMessage("Moe Drop Down List Has Lost The Focus");
        }
        else if(e.getWindow() == moeEditorFrame){

            if(e.getOppositeWindow() == mddList){
                Debug.printWindowEventMessage("Moe Editor Has Lost Its Focus And The mddList Has Gained The Focus");
            }
            else{
                Debug.printWindowEventMessage("The Moe Editor Has Lost Its Focus And Some Other Window Has Gained The Focus");

                //Hide the list if it's visible
                if(listVisible()){
                    hideList();
                }

                //Hide the tool tip if its visible
                if(toolTipMgr.toolTipVisible()) toolTipMgr.hideToolTip();

            }

        }
    }

//-----------------------------------------------------------------------------




//--WindowListener interface implementations ----------------------------------

    /**
     * This method implements the WindowListener interface and is
     * called when either the MoeEditor window or a MoeDropDownList
     * is opened.
     *
     * @param e the WindowEvent.
     */
    public void windowOpened(WindowEvent e){}

    /**
     * This method implements the WindowListener interface and is
     * called when either the MoeEditor window or a MoeDropDownList
     * is closing.
     *
     * @param e the WindowEvent.
     */
    public void windowClosing(WindowEvent e){
        if(e.getWindow() == moeEditorFrame){
            Debug.printWindowEventMessage("Moe Editor Closing");
            hideList();
        }
        else if(e.getWindow() instanceof MoeDropDownList){
            Debug.printWindowEventMessage("Moe Drop Down List Closing");
        }

    }

    /**
     * This method implements the WindowListener interface and is
     * called when either the MoeEditor window or a MoeDropDownList
     * has been closed.
     *
     * @param e the WindowEvent.
     */
    public void windowClosed(WindowEvent e){
        if(e.getWindow() == moeEditorFrame){
            Debug.printWindowEventMessage("Moe Editor Closed");
            hideList();
        }
        else if (e.getWindow() instanceof MoeDropDownList){
            Debug.printWindowEventMessage("Moe Drop Down List Closed");
        }

    }

    public void windowIconified(WindowEvent e){}
    public void windowDeiconified(WindowEvent e){}
    public void windowActivated(WindowEvent e){}
    public void windowDeactivated(WindowEvent e){}

//-----------------------------------------------------------------------------



//--ComponentListener interface implementations -------------------------------

    /**
     * This method implements the ComponentListener interface and is
     * called when the MoeEditor window is moved.  If a list or
     * tool tip is visible they are hidden.
     *
     * @param e the WindowEvent.
     */
    public void componentMoved(ComponentEvent e){
        if(e.getComponent() == moeEditorFrame){
            if(listVisible()) hideList();
            if(toolTipMgr.toolTipVisible()) toolTipMgr.hideToolTip();
        }
    }

    /**
     * This method implements the ComponentListener interface and is
     * called when the MoeEditor window is resized.  If a list or
     * tool tip is visible they are hidden.
     *
     * @param e the WindowEvent.
     */
    public void componentResized(ComponentEvent e){
        if(e.getComponent() == moeEditorFrame){
            if(listVisible()) hideList();
            if(toolTipMgr.toolTipVisible()) toolTipMgr.hideToolTip();
        }
    }

    /**
     * This method implements the ComponentListener interface and is
     * called when the MoeEditor window is shown.  This method
     * hides any of the MoeEditor's owned windows to fix the bug
     * where old MoeDropDownList's are re-shown when a MoeEditor
     * window is closed and re-opened. The moeEditorFrame.getOwnedWindows()
     * array KEEPS INCREASING IN SIZE.  THIS REALLY NEEDS TO BE FIXED.
     *
     * @param e the WindowEvent.
     */
    public void componentShown(ComponentEvent e){
        if(e.getComponent() == moeEditorFrame){
            Debug.printWindowEventMessage("Moe Editor Made Visible");

            //Make sure that non of the editors owned windows get
            //re-displayed.  Is there a better way of doing this.
            //Can we prevent the editors owned windows array
            //from continually increasing in size.
            Window[] ownedWins = moeEditorFrame.getOwnedWindows();
            for(int i=0; i<ownedWins.length; i++){
                ownedWins[i].hide();
                ownedWins[i].dispose();
            }
        }
    }

    /**
     * This method implements the ComponentListener interface and is
     * called when the MoeEditor window is hidden.
     *
     * @param e the WindowEvent.
     */
    public void componentHidden(ComponentEvent e){
        if(e.getComponent() == moeEditorFrame){
            Debug.printWindowEventMessage("Moe Editor Hidden");
            hideList();
        }
    }

//-----------------------------------------------------------------------------


//--ChangeListener interface implementations ----------------------------------

    /**
     * This method is called whenever the viewport of the
     * JEditorPane changes.
     *
     * @param e the ChangeEvent
     */
    public void stateChanged(ChangeEvent e){
        if(listVisible()) hideList();
        if(toolTipMgr.toolTipVisible()) toolTipMgr.hideToolTip();
    }

//-----------------------------------------------------------------------------
}
