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
package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.event.*;

import bluej.*;
import bluej.pkgmgr.target.role.AppletClassRole;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

/**
 * Dialog for generating HTML and running applets.
 * 
 * @author Bruce Quig
 */
public class RunAppletDialog extends EscapeDialog
    implements ListSelectionListener
{
    // Internationalisation
    static final String createWebPage = Config.getString("pkgmgr.runApplet.webPageLabel");
    static final String radioButtonText1 = Config.getString("pkgmgr.runApplet.webPage");
    static final String radioButtonText2 = Config.getString("pkgmgr.runApplet.appletviewer");
    static final String radioButtonText3 = Config.getString("pkgmgr.runApplet.webBrowser");
    static final String heightLbl = Config.getString("pkgmgr.runApplet.heightLbl");
    static final String widthLbl = Config.getString("pkgmgr.runApplet.widthLbl");
    static final String newParameterLbl = Config.getString("pkgmgr.runApplet.newParameterLbl");
    static final String appletParameterLbl = Config.getString("pkgmgr.runApplet.appletParameterLbl");
    static final String nameLbl = Config.getString("pkgmgr.runApplet.nameLbl");
    static final String valueLbl = Config.getString("pkgmgr.runApplet.valueLbl");

    public static final int EXEC_APPLETVIEWER = 0;
    public static final int EXEC_WEBBROWSER = 1;
    public static final int GENERATE_PAGE_ONLY = 2;

    private String webPageName;

    private JList parameterList;
    private DefaultListModel appletParameters;
    private JTextField paramNameField;
    private JTextField paramValueField;
    private JTextField heightField;
    private JTextField widthField;
    private JButton addButton;
    private JButton deleteButton;
    private JFrame parent;
    private JRadioButton generateWebPage;
    private JRadioButton runAppletViewer;
    private JRadioButton runWebBrowser;

    private boolean ok; // result: which button?

    public RunAppletDialog(JFrame parent, String appletClassName)
    {
        super(parent, Config.getString("pkgmgr.runApplet.title"), true);
        this.parent = parent;
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E)
            {
                ok = false;
                setVisible(false);
            }
        });
        JPanel mainPanel = (JPanel) getContentPane(); // has BorderLayout
        mainPanel.setBorder(BlueJTheme.dialogBorder);

        appletParameters = new DefaultListModel();
        webPageName = appletClassName + AppletClassRole.HTML_EXTENSION;

        // button panel at bottom of dialog
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton button;
        buttonPanel.add(button = BlueJTheme.getOkButton());
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                doOK();
            }
        });
        getRootPane().setDefaultButton(button);
        buttonPanel.add(button = BlueJTheme.getCancelButton());
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                doCancel();
            }
        });
        getContentPane().add("South", buttonPanel);

        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints gridConstraints = new GridBagConstraints();
        gridConstraints.insets = new Insets(5, 5, 5, 5);
        JPanel webPanel = new JPanel();

        // Radio Button group for execution options
        ButtonGroup bGroup = new ButtonGroup();
        JPanel radioPanel = new JPanel();
        radioPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        radioPanel.setLayout(new GridLayout(3, 1));
        generateWebPage = new JRadioButton(radioButtonText1, false);
        radioPanel.add(generateWebPage);
        bGroup.add(generateWebPage);
        runAppletViewer = new JRadioButton(radioButtonText2, true);
        radioPanel.add(runAppletViewer);
        bGroup.add(runAppletViewer);
        runWebBrowser = new JRadioButton(radioButtonText3, false);
        radioPanel.add(runWebBrowser);
        bGroup.add(runWebBrowser);
        getContentPane().add("North", radioPanel);

        webPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.darkGray),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        gridConstraints.weightx = 0;
        gridConstraints.weighty = 0;
        addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel(heightLbl), 0, 1, 1, 1,
                GridBagConstraints.EAST);

        gridConstraints.fill = GridBagConstraints.HORIZONTAL;
        heightField = new JTextField(5);
        gridConstraints.weightx = 1.0;
        gridConstraints.weighty = 1.0;
        addGridBagComponent(webPanel, gridBag, gridConstraints, heightField, 1, 1, 1, 1, GridBagConstraints.WEST);

        gridConstraints.fill = GridBagConstraints.NONE;
        gridConstraints.weightx = 0;
        gridConstraints.weighty = 0;
        addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel(widthLbl), 2, 1, 1, 1,
                GridBagConstraints.EAST);

        gridConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridConstraints.weightx = 1.0;
        gridConstraints.weighty = 1.0;
        widthField = new JTextField(5);
        addGridBagComponent(webPanel, gridBag, gridConstraints, widthField, 3, 1, 1, 1, GridBagConstraints.WEST);

        gridConstraints.fill = GridBagConstraints.NONE;
        addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel(newParameterLbl), 5, 2, 1, 1,
                GridBagConstraints.CENTER);

        parameterList = new JList(appletParameters);
        parameterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        parameterList.setModel(appletParameters);
        parameterList.addListSelectionListener(this);
        JScrollPane parameterScroller = new JScrollPane(parameterList);
        parameterScroller.setColumnHeaderView(new JLabel(appletParameterLbl, JLabel.CENTER));
        
        gridConstraints.fill = GridBagConstraints.BOTH;
        addGridBagComponent(webPanel, gridBag, gridConstraints, parameterScroller, 0, 2, 4, 4,
                GridBagConstraints.CENTER);

        gridConstraints.fill = GridBagConstraints.NONE;
        addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel(nameLbl), 4, 3, 1, 1,
                GridBagConstraints.EAST);

        gridConstraints.weightx = 1.0;
        gridConstraints.weighty = 1.0;
        gridConstraints.fill = GridBagConstraints.HORIZONTAL;
        paramNameField = new JTextField(16);
        addGridBagComponent(webPanel, gridBag, gridConstraints, paramNameField, 5, 3, 1, 1, GridBagConstraints.WEST);

        gridConstraints.weightx = 0;
        gridConstraints.weighty = 0;
        gridConstraints.fill = GridBagConstraints.NONE;
        addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel(valueLbl), 4, 4, 1, 1,
                GridBagConstraints.EAST);

        gridConstraints.weightx = 1.0;
        gridConstraints.weighty = 1.0;
        gridConstraints.fill = GridBagConstraints.HORIZONTAL;
        paramValueField = new JTextField(16);
        addGridBagComponent(webPanel, gridBag, gridConstraints, paramValueField, 5, 4, 1, 1, GridBagConstraints.WEST);

        gridConstraints.weightx = 0;
        gridConstraints.weighty = 0;
        gridConstraints.fill = GridBagConstraints.NONE;
        deleteButton = new JButton(Config.getString("classmgr.delete"));
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                doDelete();
            }
        });
 
        deleteButton.setEnabled(false);

        addButton = new JButton(Config.getString("classmgr.add"));
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                doAdd();
            }
        });
        addButton.setEnabled(true);

        JPanel addDeletePanel = new JPanel();
        addDeletePanel.add(deleteButton);
        addDeletePanel.add(addButton);
        addGridBagComponent(webPanel, gridBag, gridConstraints, addDeletePanel, 5, 5, 1, 1, GridBagConstraints.CENTER);
        
        getContentPane().add("Center", webPanel);

        DialogManager.centreDialog(this);
    }

    /**
     * Method to simplify adding components to a gridbag layout and modify
     * constraints
     * 
     * @param container
     *            the container the component is to be added to
     * @param layout
     *            the GridBagLayout object to be used
     * @param constraints
     *            the GridBagConstraints object being used
     * @param component
     *            the component to be added
     * @param gridx
     *            x coordinate for component starting position
     * @param gridy
     *            y coordinate for component starting position
     * @param gridWidth
     *            number of grid cells for width of component
     * @param gridHeight
     *            number of grid cells for height of component
     * @param anchor
     *            the alignment of component within grid cell
     */
    private void addGridBagComponent(Container container, GridBagLayout layout, GridBagConstraints constraints,
            Component component, int gridx, int gridy, int gridWidth, int gridHeight, int anchor)
    {
        constraints.gridx = gridx;
        constraints.gridy = gridy;
        constraints.gridwidth = gridWidth;
        constraints.gridheight = gridHeight;
        constraints.anchor = anchor;
        layout.setConstraints(component, constraints);
        // check that this layout has not already been set
        if (!container.getLayout().equals(layout))
            container.setLayout(layout);
        container.add(component);
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if cancelled.
     */
    public boolean display()
    {
        ok = false;
        pack();
        setVisible(true);
        return ok;
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if cancelled.
     */
    public String getWebPageName()
    {
        return webPageName;
    }

    /**
     * Add a parameter to the applet parameter list.
     */
    public void addAppletParameter()
    {

        AppletParam param = new AppletParam(paramNameField.getText(), paramValueField.getText());

        int index = appletParameters.indexOf(param);
        if (index == -1)
            appletParameters.addElement(param);
        else
            appletParameters.set(index, param);

        clearInput();

    }

    /**
     * clear the input fields
     */
    private void clearInput()
    {
        paramNameField.setText("");
        paramValueField.setText("");
    }

    /**
     * Clear list selection and input fields. Based on the assumption we don't
     * want them next time.
     */
    private void prepareForClosure()
    {
        clearInput();
        parameterList.getSelectionModel().clearSelection();
    }

    /**
     * Close action when OK is pressed.
     */
    public void doOK()
    {
        if (!checkFieldsAreValid()) {
            DialogManager.showError(parent, "applet-height-width");
        }
        else { // collect information from fields
            ok = true;
            prepareForClosure();
            setVisible(false);
        }
    }

    /**
     * Close action when Cancel is pressed.
     */
    public void doCancel()
    {
        ok = false;
        prepareForClosure();
        setVisible(false);
    }

    public void doAdd()
    {
        if (!paramNameField.getText().equals("") && !paramValueField.getText().equals("")) {
            addAppletParameter();
            parameterList.getSelectionModel().clearSelection();
            paramNameField.requestFocus();
        }
    }

    public void doDelete()
    {
        appletParameters.remove(parameterList.getSelectedIndex());
        clearInput();
        deleteButton.setEnabled(false);
    }

    /**
     * Check that required fields have entries. There is no checking the
     * validity of what is entered.
     * 
     * @return true if both width and height fields are not empty
     */
    public boolean checkFieldsAreValid()
    {
        return (!widthField.getText().equals("") && !heightField.getText().equals(""));
    }

    /**
     * Returns height of height text field.
     * 
     * @return height of applet as a String
     */
    public String getAppletHeight()
    {
        return heightField.getText();
    }

    /**
     * Returns value of width text field.
     * 
     * @return width of Applet as a String
     */
    public String getAppletWidth()
    {
        return widthField.getText();
    }

    /**
     * sets value of height text field.
     * 
     * @param height
     *            value to set in field
     */
    public void setAppletHeight(int height)
    {
        heightField.setText(String.valueOf(height));
    }

    /**
     * sets value of width text field.
     * 
     * @param width
     *            value to set in field
     */
    public void setAppletWidth(int width)
    {
        widthField.setText(String.valueOf(width));
    }

    /**
     * Returns applet parameters.
     * 
     * @return applet parameters as an array of Strings or null if no parameters
     */
    public String[] getAppletParameters()
    {
        String[] paramStringArray = new String[appletParameters.size()];
        Enumeration<?> e = appletParameters.elements();
        for (int i = 0; e.hasMoreElements() && i < paramStringArray.length; i++) {
            AppletParam ap = (AppletParam) e.nextElement();
            paramStringArray[i] = ap.toString();
        }
        return paramStringArray;
    }

    /**
     * Set applet parameters as an array of Strings or null if no parameters
     */
    public void setAppletParameters(String[] parameters)
    {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                appletParameters.addElement(new AppletParam(parameters[i]));
            }
        }
    }

    /**
     * Returns an int representing the radio button chosen for execution option.
     * 
     * @return int representing index of radio button selected
     */
    public int getAppletExecutionOption()
    {
        if (runAppletViewer.isSelected()) {
            return EXEC_APPLETVIEWER;
        }
        else if (runWebBrowser.isSelected()) {
            return EXEC_WEBBROWSER;
        }
        else {
            return GENERATE_PAGE_ONLY;
        }
    }

    // ----- ListSelectionListener interface -----

    /**
     * The value of the list selection has changed.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        //if(e.getValueIsAdjusting()) // ignore mouse down, dragging, etc.
        //    return;
        if (!parameterList.isSelectionEmpty()) {
            deleteButton.setEnabled(true);
            AppletParam param = (AppletParam) parameterList.getSelectedValue();
            paramNameField.setText(param.getParamName());
            paramValueField.setText(param.getParamValue());
        }

    }

    // ----- end of ListSelectionListener interface -----

}
