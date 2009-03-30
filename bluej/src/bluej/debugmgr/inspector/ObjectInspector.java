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
package bluej.debugmgr.inspector;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.DialogManager;

/**
 * A window that displays the fields in an object or a method return value.
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 * @author Bruce Quig
 * @version $Id: ObjectInspector.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ObjectInspector extends Inspector
{
    // === static variables ===

    protected final static String inspectTitle = Config.getString("debugger.inspector.object.title");

    // === instance variables ===
    
    /** A reference to the object being inspected */
    protected DebuggerObject obj;

    /**
     * Name of the object, as it appears on the object bench, or null if the
     * object being inspected is not on the object bench
     */
    protected String objName;

    protected boolean queryArrayElementSelected = false;

    /**
     * array of Integers representing the array indexes from a large array that
     * have been selected for viewing
     */
    //protected TreeSet arraySet = null;

    /**
     * list which is built when viewing an array that records the object slot
     * corresponding to each array index
     */
    protected List<Integer> indexToSlotList = null; 

    /**
     *  Note: 'pkg' may be null if 'ir' is null.
     * 
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to
     * @param ir
     *            the InvokerRecord explaining how we created this result/object
     *            if null, the "get" button is permanently disabled
     * @param parent
     *            The parent frame of this frame
     */
    public ObjectInspector(DebuggerObject obj, InspectorManager inspectorManager, String name, Package pkg, InvokerRecord ir, final JFrame parent)
    {
        super(inspectorManager, pkg, ir);

        this.obj = obj;
        this.objName = name;

        final ObjectInspector thisInspector = this;
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                makeFrame();
                pack();
                if (parent instanceof Inspector) {
                    DialogManager.tileWindow(thisInspector, parent);
                }
                else {
                    DialogManager.centreWindow(thisInspector, parent);
                }
            }
        });
    }

    /**
     * Build the GUI
     *  
     */
    protected void makeFrame()
    {
        setTitle(inspectTitle);
        setBorder(BlueJTheme.getRoundedShadowBorder());

        // Create the header

        JComponent header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        String className = obj.getStrippedGenClassName();

        String fullTitle = null;
        if(objName != null) {
            fullTitle = objName + " : " + className;   
        }
        else {
            fullTitle = " : " + className;
        }
        JLabel headerLabel = new JLabel(fullTitle, JLabel.CENTER) {
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                int ascent = g.getFontMetrics().getAscent() + 1;
                g.drawLine(0, ascent, this.getWidth(), ascent);
            }
        };
        headerLabel.setAlignmentX(0.5f);
        header.add(headerLabel);
        header.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        header.add(new JSeparator());

        // Create the main panel (field list, Get/Inspect buttons)

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);

        JScrollPane scrollPane = createFieldListScrollPane();
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel inspectAndGetButtons = createInspectAndGetButtons();
        mainPanel.add(inspectAndGetButtons, BorderLayout.EAST);

        Insets insets = BlueJTheme.generalBorderWithStatusBar.getBorderInsets(mainPanel);
        mainPanel.setBorder(new EmptyBorder(insets));

        // create bottom button pane with "Close" button

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

        JPanel buttonPanel;
        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        JButton button = createCloseButton();
        buttonPanel.add(button, BorderLayout.EAST);
        JButton classButton = new JButton(showClassLabel);
        classButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                showClass();
            }
        });
        buttonPanel.add(classButton, BorderLayout.WEST);

        bottomPanel.add(buttonPanel);

        // add the components

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(header, BorderLayout.NORTH);
        contentPane.add(mainPanel, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(button);
    }

    /**
     * True if this inspector is used to display a method call result.
     */
    protected Object[] getListData()
    {
        // if is an array (we potentially will compress the array if it is
        // large)
        if (obj.isArray()) {
            return compressArrayList(obj.getInstanceFields(true)).toArray(new Object[0]);
        }
        else {
            return obj.getInstanceFields(true).toArray(new Object[0]);
        }
    }

    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
        // add index to slot method for truncated arrays
        if (obj.isArray()) {
            slot = indexToSlot(slot);
            // if selection is the first field containing array length
            // we treat as special case and do nothing more
            if (slot == ARRAY_LENGTH_SLOT_VALUE) {
                setCurrentObj(null, null, null);
                setButtonsEnabled(false, false);
                return;
            }
        }

        queryArrayElementSelected = (slot == (ARRAY_QUERY_SLOT_VALUE));

        // for array compression..
        if (queryArrayElementSelected) { // "..." in Array inspector
            setCurrentObj(null, null, null); //  selected
            // check to see if elements are objects,
            // using the first item in the array
            if (obj.instanceFieldIsObject(0)) {
                setButtonsEnabled(true, false);
            }
            else {
                setButtonsEnabled(false, false);
            }
        }
        else if (obj.instanceFieldIsObject(slot)) {
            setCurrentObj(obj.getInstanceFieldObject(slot), obj.getInstanceFieldName(slot), obj.getInstanceFieldType(slot));

            if (obj.instanceFieldIsPublic(slot)) {
                setButtonsEnabled(true, true);
            }
            else {
                setButtonsEnabled(true, false);
            }
        }
        else {
            setCurrentObj(null, null, null);
            setButtonsEnabled(false, false);
        }
    }

    /**
     * Show the inspector for the class of an object.
     */
    protected void showClass()
    {
        inspectorManager.getClassInspectorInstance(obj.getClassRef(), pkg, this);
    }

    /**
     * We are about to inspect an object - prepare.
     */
    protected void prepareInspection()
    {
        // if need to query array element
        if (queryArrayElementSelected) {
            selectArrayElement();
        }
    } 

    /**
     * Remove this inspector.
     */
    protected void remove()
    {
        if(inspectorManager != null) {
            inspectorManager.removeInspector(obj);
        }
    }

    /**
     * Shows a dialog to select array element for inspection
     */
    private void selectArrayElement()
    {
        String response = DialogManager.askString(this, "ask-index");

        if (response != null) {
            try {
                int slot = Integer.parseInt(response);

                // check if within bounds of array
                if (slot >= 0 && slot < obj.getInstanceFieldCount()) {
                    // if its an object set as current object
                    if (obj.instanceFieldIsObject(slot)) {
                        setCurrentObj(obj.getInstanceFieldObject(slot), obj.getInstanceFieldName(slot), obj.getInstanceFieldType(slot));
                        setButtonsEnabled(true, false);
                    }
                    else {
                        // it is not an object - a primitive, so lets
                        // just display it in the array list display
                        setButtonsEnabled(false, false);
                        //arraySet.add(new Integer(slot));
                        update();
                    }
                }
                else { // not within array bounds
                    DialogManager.showError(this, "out-of-bounds");
                }
            }
            catch (NumberFormatException e) {
                // input could not be parsed, eg. non integer value
                setCurrentObj(null, null, null);
                DialogManager.showError(this, "cannot-access-element");
            }
        }
        else {
            // set current object to null to avoid re-inspection of
            // previously selected wildcard
            setCurrentObj(null, null, null);
        }
    }

    private final static int VISIBLE_ARRAY_START = 40; // show at least the
                                                       // first 40 elements
    private final static int VISIBLE_ARRAY_TAIL = 5; // and the last five
                                                     // elements

    private final static int ARRAY_QUERY_SLOT_VALUE = -2; // signal marker of
                                                          // the [...] slot in
                                                          // our
    private final static int ARRAY_LENGTH_SLOT_VALUE = -1; // marker for having
                                                           // selected the slot
                                                           // containing array
                                                           // length

    /**
     * Compress a potentially large array into a more displayable shortened
     * form.
     * 
     * Compresses an array field name list to a maximum of VISIBLE_ARRAY_START
     * which are guaranteed to be displayed at the start, then some [..]
     * expansion slots, followed by VISIBLE_ARRAY_TAIL elements from the end of
     * the array. When a selected element is chosen indexToSlot allows the
     * selection to be converted to the original array element position.
     * 
     * @param fullArrayFieldList
     *            the full field list for an array
     * @return the compressed array
     */
    private List<String> compressArrayList(List<String> fullArrayFieldList)
    {
        // mimic the public length field that arrays possess
        // according to the java spec...
        fullArrayFieldList.add(0, ("int length = " + fullArrayFieldList.size()));
        indexToSlotList = new LinkedList<Integer>();
        indexToSlotList.add(0, new Integer(ARRAY_LENGTH_SLOT_VALUE));

        // the +1 here is due to the fact that if we do not have at least one
        // more than
        // the sum of start elements and tail elements, then there is no point
        // in displaying
        // the ... elements because there would be no elements for them to
        // reveal
        if (fullArrayFieldList.size() > (VISIBLE_ARRAY_START + VISIBLE_ARRAY_TAIL + 2)) {

            // the destination list
            List<String> newArray = new ArrayList<String>();
            for (int i = 0; i <= VISIBLE_ARRAY_START; i++) {
                // first 40 elements are displayed as per normal
                newArray.add(fullArrayFieldList.get(i));
                if (i < VISIBLE_ARRAY_START)
                    indexToSlotList.add(new Integer(i));
            }

            // now the first of our expansion slots
            newArray.add("[...]");
            indexToSlotList.add(new Integer(ARRAY_QUERY_SLOT_VALUE));

            for (int i = VISIBLE_ARRAY_TAIL; i > 0; i--) {
                // last 5 elements are displayed
                newArray.add(fullArrayFieldList.get(fullArrayFieldList.size() - i));
                // slot is offset by one due to length field being included in
                // fullArrayFieldList therefore we add 1 to compensate
                indexToSlotList.add(new Integer(fullArrayFieldList.size() - (i + 1)));
            }
            return newArray;
        }
        else {
            for (int i = 0; i < fullArrayFieldList.size(); i++) {
                indexToSlotList.add(new Integer(i));
            }
            return fullArrayFieldList;
        }
    }

    /**
     * Converts list index position to that of array element position in arrays.
     * Uses the List built in compressArrayList to do the mapping.
     * 
     * @param listIndexPosition
     *            the position selected in the list
     * @return the translated index of field array element
     */
    private int indexToSlot(int listIndexPosition)
    {
        Integer slot = indexToSlotList.get(listIndexPosition);

        return slot.intValue();
    }

    protected int getPreferredRows()
    {
        return 8;
    }
}
