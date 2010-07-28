/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import bluej.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.utility.*;
import bluej.views.*;

/**
 * This dialog allows selection of classes and their static methods from
 * available libraries. When a constructor or static method is selected
 * it can be invoked.
 *
 * @author  Michael Kolling
 */
public class LibraryCallDialog extends EscapeDialog
	implements ActionListener, ListSelectionListener
{
    private static final String[] clickHere = {
        "    ",
        "    " + Config.getString("callLibraryDialog.clickHere1"),
        "    " + Config.getString("callLibraryDialog.clickHere2"),
    };

    private static final String[] classNotFound = {
        "    ",
        "    " + Config.getString("callLibraryDialog.classNotFound1"),
        "    " + Config.getString("callLibraryDialog.classNotFound2"),
    };

    private JComboBox classField;
    private JList methodList;
    private JButton docButton;
    private JButton okButton;
    private JButton cancelButton;

    private ClassHistory history;
    private Package pkg;
    private CallableView viewToCall;
    private List<CallableView> currentViews;      // views currently displayed in list

    public LibraryCallDialog(PkgMgrFrame pmf)
    {
        super(pmf, Config.getString("callLibraryDialog.title"), false);
        pkg = pmf.getPackage();
        currentViews = new ArrayList<CallableView>();
        viewToCall = null;
        history = ClassHistory.getClassHistory(10);
        makeDialog();
    }


    /**
     * Set the visibility of the dialog
     */
    public void setVisible(boolean show)
    {
    	super.setVisible(show);
    	if (show) {
            okButton.setEnabled(false);
            classField.setModel(new DefaultComboBoxModel(history.getHistory().toArray()));
            classSelected();
            classField.requestFocus();
    	}
    }

    /**
     * Process action events
     */
    public void actionPerformed(ActionEvent event)
    {
        Object eventSource = event.getSource();
        if(eventSource == classField)
            classSelected();
        else if (eventSource == docButton)
            showDocumentation();
        else if (eventSource == okButton)
            doOk();
        else if (eventSource == cancelButton)
            doCancel();
    }

    /**
     * Show the javadoc documentation for the selected class in a browser.
     */
    private void showDocumentation()
    {
        String className = (String)classField.getEditor().getItem();
        
        // Assume unqualified classes are in java.lang
        if(className.indexOf('.') == -1)
            className = "java.lang." + className;
        
        Utility.showClassDocumentation(className, "#constructor_summary");
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     *  Collects arguments and calls watcher objects (Invoker).
     */
    private void doOk()
    {
        if(viewToCall == null)   // not a method - help text selected
            return;

        history.add((String)classField.getEditor().getItem());
        setVisible(false);
        pkg.getEditor().raiseMethodCallEvent(pkg, viewToCall);
    }

    /**
     * Process a "Cancel" event to cancel a call.
     * Makes dialog invisible.
     */
    private void doCancel()
    {
        setVisible(false);
    }

    /**
     * A class was selected in the class selection box. Try to load that
     * class. If successful, display its constructors and methods. Otherwise
     * clear the method list and return.
     */
    private void classSelected()
    {
        Class<?> cl = null;
        currentViews.clear();
        viewToCall = null;
        okButton.setEnabled(false);

        String className = (String)classField.getEditor().getItem();

        if(className.length() == 0) {
            displayTextInClassList(clickHere);
            return;
        }

        boolean loaded;
        try {
            ClassLoader loader = pkg.getProject().getClassLoader();
            cl = loader.loadClass(className);
            loaded = true;
        }
        catch(Exception exc) {
            loaded = false;
        }
        if (!loaded) {   // try for unqualified names in java.lang
            try {
                ClassLoader loader = pkg.getProject().getClassLoader();
                cl = loader.loadClass("java.lang."+className);
            }
            catch(Exception exc) {
                displayTextInClassList(classNotFound);
                return;
            }
        }
        displayMethodsForClass(cl);
    }

    /**
     * Given a class, display its constructors and methods in the method list.
     */
    private void displayMethodsForClass(Class<?> cl)
    {
        View classView = View.getView(cl);
        ViewFilter filter;
        List<String> list = new ArrayList<String>();

        ConstructorView[] constructors = classView.getConstructors();
        
        // Determine visibility of package private / protected members
        int visibilityMod = ViewFilter.PUBLIC;
        if (classView.getPackageName().equals(pkg.getQualifiedName()))
            visibilityMod = ViewFilter.PACKAGE;

        filter = new ViewFilter(ViewFilter.INSTANCE | visibilityMod);
        addMethods(list, constructors, filter);

        MethodView[] methods = classView.getAllMethods();
        filter = new ViewFilter(ViewFilter.STATIC | visibilityMod);
        addMethods(list, methods, filter);

        methodList.setListData(list.toArray());
        methodList.clearSelection();
        methodList.setEnabled(true);
        docButton.setEnabled(true);
    }

    /**
     * Display a message that the current class was not found.
     */
    private void displayTextInClassList(String[] text)
    {
        methodList.setListData(text);
        methodList.setEnabled(false);
        docButton.setEnabled(false);
    }

    /**
     * Add some methods, filtered by a given view filter, to a list.
     */
    public void addMethods(List<String> list, CallableView[] methods,
                            ViewFilter filter)
    {
        for(int i = 0; i < methods.length; i++) {
            if(filter.accept(methods[i])) {
                currentViews.add(methods[i]);
                list.add(methods[i].getShortDesc());
                //list.add(methods[i].toString());  // includes public static
            }
        }
    }

    // ----- ListSelectionListener interface -----

    /**
     *  A list item was selected. This can be either in the thread list,
     *  the stack list, or one of the variable lists.
     */
    public void valueChanged(ListSelectionEvent event)
    {
        if(event.getValueIsAdjusting())  // ignore mouse down, dragging, etc.
            return;

        int index = methodList.getSelectedIndex();
        if(index == -1)
            return;

        String text = (String)methodList.getSelectedValue();
        if(text.charAt(0) == ' ')
            return;

        viewToCall = currentViews.get(index);
        okButton.setEnabled(true);
    }

    // ----- end of ListSelectionListener interface -----

    /**
     * Build the Swing dialog.
     */
    private void makeDialog()
    {
        JPanel contentPane = (JPanel)getContentPane();

        JPanel classPanel = new JPanel(new BorderLayout(4,6));
        {
            classPanel.add(new JLabel(
                  Config.getString("callLibraryDialog.classLabel")),
                  BorderLayout.WEST);

            classField = new JComboBox(history.getHistory().toArray());
            classField.setEditable(true);
            classField.setMaximumRowCount(10);
            JTextField textField = (JTextField)classField.getEditor().getEditorComponent();
            textField.setColumns(16);
            classField.addActionListener(this);
            classPanel.add(classField, BorderLayout.CENTER);

            docButton = new JButton(Config.getString("callLibraryDialog.docButton"));
            docButton.addActionListener(this);
            docButton.setEnabled(false);
            classPanel.add(docButton, BorderLayout.EAST);
        }

        // create the centre Panel
        JPanel centrePanel = new JPanel(new BorderLayout());
        {
            methodList = new JList();
            methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            methodList.addListSelectionListener(this);
            methodList.setVisibleRowCount(8);
            JScrollPane methodScrollPane = new JScrollPane(methodList);
            methodScrollPane.setColumnHeaderView(new JLabel(
                 Config.getString("callLibraryDialog.listHeading")));

            MouseListener mouseListener = new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            doOk();
                        }
                    }
                };
            methodList.addMouseListener(mouseListener);

            centrePanel.add(methodScrollPane, BorderLayout.CENTER);

        }

        // create the Button Panel
        JPanel buttonPanel = new JPanel();
        {
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

            okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(this);
            
            cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(this);
            
            DialogManager.addOKCancelButtons(buttonPanel, okButton, cancelButton);

            getRootPane().setDefaultButton(okButton);
            okButton.setEnabled(false);
        }

        //contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setLayout(new BorderLayout(6,6));
        contentPane.setBorder(BlueJTheme.generalBorder);

        contentPane.add(classPanel, BorderLayout.NORTH);
        contentPane.add(centrePanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        pack();
        DialogManager.centreDialog(this);

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    setVisible(false);
                }
            });
    }
}
