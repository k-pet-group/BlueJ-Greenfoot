package bluej.editor.moe.autocomplete;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.reflect.*;


/**
 * A MoeDropDownList is a JWindow that simply displays a list of
 * MoeDropDownItems. The items that the list must display should be specified
 * upon construction.
 *
 * The MoeAutoCompleteManager should make sure that the
 * editor always retains the focus and that this window never gets the focus.
 * As this window should never have the focus the MoeAutoCompleteManager needs
 * to determine when the up and down cursor keys are pressed and call the
 * moveSelectionUp() and moveSelectionDown() methods respectively.
 *
 * Items can be selected using the mouse but the MoeAutoCompleteManager must
 * make sure that it retains the focus when this occurs.  The auto complete
 * manager can register itself as a MoeDropDownListMouseListener using the
 * addMoeDropDownListMouseListener method.  This will enable the auto-complete
 * manager to be notified every time an item is clicked or double clicked upon
 * in the list.
 *
 * The MoeAutoCompleteManager can reduce the number of items in the list by
 * calling the selectBestMatch method.  This method instructs the list to only
 * show the items that begin with the given String.  The very best match will
 * be the first item in the list and this will be selected.
 *
 * The MoeAutoCompleteManager can determine whether an item is selected in the
 * list using the itemSelected method.  It can also get the selected
 * MoeDropDownItem by calling the getSelectedItem method.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class MoeDropDownList extends JWindow implements MouseListener {

    private VariableLengthList lst;
    private ArrayList lstSelectionListeners = new ArrayList();



    /**
     * Constructs a new invisible MoeDropDownList that initially contains
     * every MoeDropDownItem in the specified ArrayList that begins with the
     * specified initialMatch String.  The given ArrayList must only contain
     * MoeDropDownItems to prevent errors from occurring.
     *
     * @param items an ArrayList containing every MoeDropDownItem
     *              to be initially displayed.
     * @param initialMatch the list will initially show all the items
     *                     that begin with this String.
     * @param owner the frame from which the window is displayed (MoeEditor)
     * @throws IllegalArgumentException if items is null/ empty
     *                                  or the owner is null
     *         NoInitialMatchException if none of the items begin
     *                                 with initialMatch
     */
    public MoeDropDownList(ArrayList items, String initialMatch, JFrame owner)
                           throws IllegalArgumentException,
                                  NoInitialMatchException{

        //The following line sets the owner frame for this window. It will
        //throw an illegal argument exception if the owner is null.
        super(owner);

        if(items==null) throw new IllegalArgumentException("Can't construct a MoeDropDownList because items is null.");
        if(items.size()<1) throw new IllegalArgumentException("Can't construct a MoeDropDownList because items is empty.");

        String initialMatch2 = "";
        if(initialMatch != null) initialMatch2 = initialMatch;

        //Create the list for the specified class
        lst = new VariableLengthList(items, initialMatch2);
        if(lst.getNumberOfMatches()<1){
            String msg = "There were no initial matches for " + initialMatch;
            throw new NoInitialMatchException(msg);
        }

        lst.setCellRenderer(new MoeDropDownListCellRenderer());

        //Create a scroller for the list and add the scroller
        //to the content pane
        final JScrollPane scroller = new JScrollPane(lst);
        Container cont = getContentPane();
        cont.add(scroller);

        //Set the size of the window so that the list has
        //seven visible items.
        setSize(300, 300);

        //The following line ensures that the window can gain the focus
        setFocusableWindowState(true);

        //Register the internal mouse listener for the code list
        lst.addMouseListener(this);

    }

    public void dispose(){
        lst.removeMouseListener(this);
        lstSelectionListeners.clear();
        super.dispose();
    }


    /**
     * Returns whether an item is currently selected in the drop down list.
     *
     * @return whether an item is selected in the drop down list
     */
    public boolean itemSelected(){
        return !lst.isSelectionEmpty();
    }



    /**
     * Returns the MoeDropDownItem object for the selected item
     *
     * @return the MoeDropDownItem object for the selected item.  This
     *         will be null if no item is selected.
     */
    public MoeDropDownItem getSelectedItem(){
        Object o = lst.getSelectedValue();
        if(o==null){
            return null;
        }
        else{
            return (MoeDropDownItem) o;
        }
    }



    /**
     * Returns whether a method is currently selected in the drop down list.
     *
     * @return whether a method is selected in the drop down list
     */
    public boolean methodSelected(){
        if(itemSelected()){
            return (getSelectedItem() instanceof MoeDropDownMethod);
        }
        else{
            return false;
        }
    }



    /**
     * This selects the previous item in the list. If the first item
     * is already selected it will remain selected.
     */
    public void moveSelectionUp(){
        lst.moveSelectionUp();
    }

    /**
     * This selects the next item in the list. If the last item
     * is already selected it will remain selected.
     */
    public void moveSelectionDown(){
        lst.moveSelectionDown();
    }


    /**
     * This scrolls the list up by the number of visible items
     */
    public void pageUp(){
        lst.pageUp();
    }

    /**
     * This scrolls the list down by the number of visible items
     */
    public void pageDown(){
        lst.pageDown();
    }

    /**
     * This selects the first item in the list and
     * ensures that it is visible
     */
    public void selectFirst(){
        lst.selectFirst();
    }

    /**
     * This selects the last item in the list and
     * ensures that it is visible
     */
    public void selectLast(){
        lst.selectLast();
    }

    /**
     * This method displays all the MoeDropDownItem's in the list
     * that begin with the string supplied.  If a null or empty String
     * is supplied the list will display all the MoeDropDownItems
     * but none of the items will be selected.  If the string has
     * a length that is greater than zero the first item in the list
     * will be selected.  If the string doesn't match any items the
     * list will be empty.  The method returns the number of items
     * that are in the list / match the given String.
     *
     * @param code this is the String that is used to select the best match
     * @return the number of items that are in the list and that begin
     *         with the supplied String
     */
    public int selectBestMatch(String code){
       return lst.selectBestMatch(code);
    }


    /**
     * This returns the number of items in
     * the list which is the number of matches.
     */
    public int getNumberOfMatches(){
        return lst.getNumberOfMatches();
    }


    /**
     * This method can be used to add a MoeDropDownListMouseListener.
     * Every MoeDropDownListMouseListener that is added using this method
     * will be informed of single and double clicks on the list.
     *
     * @param listener the MoeDropDownListMouseListener to add.
     */
    public void addMoeDropDownListMouseListener
                (MoeDropDownListMouseListener listener){

        lstSelectionListeners.add(listener);
    }


    /**
     * This method can be used to remove a MoeDropDownListMouseListener.
     * The specified MoeDropDownListMouseListener will no longer be
     * informed of single and double clicks on the list.
     *
     * @param listener the MoeDropDownListMouseListener to be removed.
     */
    public void removeMoeDropDownListMouseListener
                (MoeDropDownListMouseListener listener){

        lstSelectionListeners.remove(listener);
    }



//--MouseListener interface implementations -----------------------------------

     /**
      * This method is called whenever the user single clicks or double clicks
      * on the list.  In both cases the MoeDropDownListMouseListeners
      * are informed of the action.  Other classes are only interested
      * in the MoeDropDownItem that was clicked or double clicked upon.
      * They do not need to know about the index of the selected item
      * or the location of the click(s).
      */
    public void mouseClicked(MouseEvent e) {

        int index = lst.locationToIndex(e.getPoint());
        MoeDropDownItem mddItem = (MoeDropDownItem) lst.getSelectedValue();

        Iterator it = lstSelectionListeners.iterator();

        while(it.hasNext()){
            MoeDropDownListMouseListener l =
                (MoeDropDownListMouseListener) it.next();
            if(e.getClickCount() == 1) {
                l.mouseClicked(mddItem);
            }
            else if(e.getClickCount() == 2) {
                l.mouseDoubleClicked(mddItem);
            }
        }
    }

    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mousePressed(MouseEvent e){}
    public void mouseReleased(MouseEvent e){}

//-----------------------------------------------------------------------------




    /**
     * This private inner class extends JList and provides
     * an ability for the number of items to be reduced
     * and expanded by looking for the best matches)
     */
    private class VariableLengthList extends JList{

        /**
         * An ArrayList containing every single MoeDropDownItem
         * that can be displayed in the list.
         */
        private ArrayList allItems;

        /** All the items that are currently being displayed in the list. */
        private Object[] lstData;



        /**
         * This constructs a VariableLengthList that
         * can display every item in the items ArrayList
         * or a subset of these items. The items ArrayList
         * must only contain MoeDropDownItems.
         *
         * @param items an ArrayList containing the maximum amount
         *              of items that this list can display
         */
        public VariableLengthList(ArrayList items, String initialMatch){
            this.allItems = items;
            Collections.sort(allItems);
            selectBestMatch(initialMatch);
        }


        /**
         * This method selects the item above the currently
         * selected item.  If no item is selected the first item
         * in the list is selected instead.  If the first item
         * in the list is selected before this method is called
         * the selection will remain the same.
         */
        public void moveSelectionUp(){
            int selected = getSelectedIndex();
            if(selected==-1){
                //Select the first item
                setSelectedIndex(0);
            }
            else if(selected==0){
                //The top most item is selected so we can't go up
                //Just make sure the top most index is visible
                ensureIndexIsVisible(selected);
            }
            else if(selected >= 1){
                selected --;
                setSelectedIndex(selected);
                ensureIndexIsVisible(selected);
            }
        }



        /**
         * This method selects the item below the currently
         * selected item.  If no item is selected the last item
         * in the list is selected instead.  If the last item
         * in the list is selected before this method is called
         * the selection will remain the same.
         */
        public void moveSelectionDown(){
            int selected = getSelectedIndex();
            if(selected==-1){
                //Select the first item
                setSelectedIndex(0);
            }
            else if(selected == lstData.length-1){
                //The bottom most item is selected so we can't go down
                //Just make sure the bottom most index is visible
                ensureIndexIsVisible(selected);
            }
            else if(selected >= 0 && selected < lstData.length-1 ){
                selected ++;
                setSelectedIndex(selected);
                ensureIndexIsVisible(selected);
            }
        }



        /**
         * This scrolls the list up by the number of visible items
         */
        public void pageUp(){
            int visible = getLastVisibleIndex() - getFirstVisibleIndex();
            int step = visible + 1;
            if(step<1) step=1;

            int selected = getSelectedIndex();
            if(selected==-1){
                //Select the first item
                setSelectedIndex(0);
            }
            else if(selected==0){
                //The top most item is selected so we can't go up
                //Just make sure the top most index is visible
                ensureIndexIsVisible(selected);
            }
            else{
                selected = selected - step;
                if(selected<0) selected = 0;
                setSelectedIndex(selected);
                ensureIndexIsVisible(selected);
            }
        }



        /**
         * This scrolls the list down by the number of visible items
         */
        public void pageDown(){
            int visible = getLastVisibleIndex() - getFirstVisibleIndex();
            int step = visible + 1;
            if(step<1) step=1;

            int selected = getSelectedIndex();
            if(selected==-1){
                //Select the first item
                setSelectedIndex(0);
            }
            else if(selected == lstData.length-1){
                //The bottom most item is selected so we can't go down
                //Just make sure the bottom most index is visible
                ensureIndexIsVisible(selected);
            }
            else{
                selected = selected + step;
                if(selected > lstData.length-1) selected = lstData.length-1;
                setSelectedIndex(selected);
                ensureIndexIsVisible(selected);
            }
        }



        /**
         * This selects the first item in the list and
         * ensures that it is visible
         */
        public void selectFirst(){
            if(lstData.length>=1){
                setSelectedIndex(0);
                ensureIndexIsVisible(0);
            }
        }



        /**
         * This selects the last item in the list and
         * ensures that it is visible
         */
        public void selectLast(){
            if(lstData.length>=1){
                setSelectedIndex(lstData.length-1);
                ensureIndexIsVisible(lstData.length-1);
            }
        }



        /**
         * This method reduces the number of items in the list
         * so that every item begins with the code String.
         * The very first item in the list will be the best
         * match and will become the selected item.  The
         * method returns the new amount of items that are in
         * the list
         *
         * @param code the String to use to get the best matches.
         * @return the new amount of items that are in the list.
         */
        public int selectBestMatch(String code){
            if(code==null) code="";
            ArrayList minItems = getMatchingItems(code);
            lstData = minItems.toArray();
            setListData(lstData);
            if(minItems.size() >= 1 && code.length()>0) setSelectedIndex(0);
            return minItems.size();
        }



        /**
         * Returns the number of items in the list
         *
         * @return the number of items in the list
         */
        public int getNumberOfMatches(){
            return getModel().getSize();
        }



        /**
         * This method returns an ArrayList containing all the items
         * that match the given String.
         *
         * @param code the String to use to get the best matches.
         * @return an ArrayList containing matching items.
         */
        private ArrayList getMatchingItems(String code){

            if(code.length()==0) return allItems;

            ArrayList reducedItems = new ArrayList();
            Iterator it = allItems.iterator();

            while(it.hasNext()){
                Object o = it.next();
                MoeDropDownItem item = (MoeDropDownItem) o;
                String insertableCode = item.getInsertableCode();
                int codeLength = code.length();

                if(codeLength <= insertableCode.length()){
                    if(insertableCode.substring(0, codeLength).equalsIgnoreCase(code)){
                        reducedItems.add(item);
                    }
                }
            }

            return reducedItems;
        }

    }

}