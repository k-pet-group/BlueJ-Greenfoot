/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2013,2014  Michael Kolling and John Rosenberg 
 
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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.utility.JavaNames;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.LabelPrintWriter;
import bluej.views.TypeParamView;
import bluej.views.View;

public class ConstructorDialog extends CallDialog
{
    // Window Titles
    private static final String appName = Config.getApplicationName(); 
    private static final String wCreateTitle = appName + ":  " + Config.getString("pkgmgr.methodCall.titleCreate");
    // MD_CREATE Specific
    static final String sNameOfInstance = Config.getString("pkgmgr.methodCall.namePrompt");
    static final String sTypeParameters = Config.getString("pkgmgr.methodCall.typeParametersPrompt");
    static final String sTypeParameter = Config.getString("pkgmgr.methodCall.typeParameterPrompt");
    static final String illegalNameMsg = Config.getString("error.methodCall.illegalName");
    static final String duplicateNameMsg = Config.getString("error.methodCall.duplicateName");

    private JTextField instanceNameText;
    private ConstructorView constructor;

    private boolean okCalled;

    /**
     * MethodDialog constructor.
     * 
     * @param parentFrame  The parent window for the dialog
     * @param ob           The object bench to listen for object selection on
     * @param callHistory  The call history tracker
     * @param initialName  The initial (suggested) instance name
     * @param method       The constructor or method being used
     * @param typeMap      The mapping of type parameter names to runtime types
     *                     (a Map of String -> GenType).
     */
    public ConstructorDialog(JFrame parentFrame, ObjectBenchInterface ob, CallHistory callHistory,
            String initialName, ConstructorView constructor)
    {
        super(parentFrame, ob, "");

        history = callHistory;

        this.constructor = constructor;
        makeDialog(constructor.getClassName(), initialName);
        setInstanceInfo(initialName);
    }
    
    /*
     * @see bluej.debugmgr.CallDialog#makeDialogInternal(java.lang.String, java.lang.String, javax.swing.JPanel)
     */
    @Override
    protected void makeDialogInternal(String className, String instanceName, JPanel centerPanel)
    {
        makeCreateDialog(className, instanceName, constructor, centerPanel);
    }
    
    /**
     * makeCreateDialog - create a dialog to create an object (including
     *  constructor call)
     */
    private void makeCreateDialog(String className, String instanceName, CallableView method,
            JPanel panel)
    {
        setTitle(wCreateTitle);

        JLabel instName = new JLabel(sNameOfInstance);
        instanceNameText = new JTextField(instanceName, 16);
        instName.setLabelFor(instanceNameText);
        // treat 'return' in text field as OK
        instanceNameText.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                doOk();
            }
        });
        instanceNameText.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent fe)
            {
                ((JTextField) (fe.getComponent())).selectAll();
            }

            public void focusLost(FocusEvent fe)
            {
            }
        });

        JPanel tmpPanel = new JPanel();
        if (!Config.isRaspberryPi()) tmpPanel.setOpaque(false);

        GridBagLayout gridBag = new GridBagLayout();
        tmpPanel.setLayout(gridBag);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = INSETS;
        constraints.gridy = 0;
        constraints.gridx = 0;
        gridBag.setConstraints(instName, constraints);
        if(!Config.isGreenfoot()) {
            tmpPanel.add(instName);
        }
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints(instanceNameText, constraints);
        if(!Config.isGreenfoot()) {
            tmpPanel.add(instanceNameText);
        }

        View clazz = method.getDeclaringView();
        if (clazz.isGeneric()) {
            JLabel name = null;
            if(getFormalTypeParams(constructor).length > 1) {
                name = new JLabel(sTypeParameters);
            } else {
                name = new JLabel(sTypeParameter);
            }
            constraints.gridwidth = 1;
            constraints.gridx = 0;
            constraints.gridy++;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            setPreferredHeight(name, getComboBoxHeight());
            gridBag.setConstraints(name, constraints);
            tmpPanel.add(name);

            JPanel typeParameterPanel = createTypeParameterPanel();
            if (!Config.isRaspberryPi()) typeParameterPanel.setOpaque(false);
            constraints.gridwidth = 1;
            constraints.gridx = 1;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            tmpPanel.add(typeParameterPanel, constraints);
        }

        if (method.hasParameters()) {
            JLabel name = new JLabel("new " + className, JLabel.RIGHT);
            constraints.gridwidth = 1;
            constraints.gridx = 0;
            constraints.gridy++;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.fill = GridBagConstraints.NONE;
            setPreferredHeight(name, getComboBoxHeight());
            gridBag.setConstraints(name, constraints);
            tmpPanel.add(name);

            constraints.anchor = GridBagConstraints.WEST;
            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JPanel parameterPanel = createParameterPanel();
            if (!Config.isRaspberryPi()) parameterPanel.setOpaque(false);
            tmpPanel.add(parameterPanel, constraints);

            constraints.gridx = 3;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            JPanel filler = new JPanel();
            if (!Config.isRaspberryPi()) filler.setOpaque(false);
            gridBag.setConstraints(filler, constraints);
            tmpPanel.add(filler);
        }

        tmpPanel.setBorder(BorderFactory.createEmptyBorder(BlueJTheme.generalSpacingWidth, 0,
                BlueJTheme.generalSpacingWidth, 0));
        panel.add(tmpPanel, BorderLayout.NORTH);
    } // makeCreateDialog

    /**
     * setInstanceName - set the name of the instance shown in the label
     * for method call dialogs, or in the text field for construction dialogs,
     * and the assosciated type parameters.
     */
    public void setInstanceInfo(String instanceName)
    {
        instanceNameText.setText(instanceName);
        createDescription();
        
        // reset error label message
        setErrorMessage("");

        clearParameters();
        startObjectBenchListening();

        // focus requests have been wrapped in invokeLater method to resolve issues 
        // with focus confusion on Mac OSX (BlueJ 2.0, JDK 1.4.2)
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                if (!Config.isGreenfoot()) {
                    instanceNameText.requestFocusInWindow();
                }
                else if (typeParameterList != null) {
                    typeParameterList.getParameter(0).getEditor().getEditorComponent().requestFocusInWindow();
                }
                else if (parameterList != null) {
                    parameterList.getParameter(0).getEditor().getEditorComponent().requestFocusInWindow();
                }
            }
        });
    }

    /**
     * Create the description. This includes the comments for the method
     * or constructor, together with its signature, and appears at the top
     * of the dialog.
     */
    private void createDescription()
    {
        LabelPrintWriter writer = new LabelPrintWriter();
        constructor.print(writer);
        setDescription(writer.getLabel());
        setVisible(true);
    }

    /**
     * Creates a panel of type parameters for a new object
     */
    private JPanel createTypeParameterPanel()
    {
        TypeParamView formalTypeParams[] = getFormalTypeParams(constructor);

        typeParameterList = new ParameterList(formalTypeParams.length, defaultParamValue, false);
        for (int i = 0; i < formalTypeParams.length; i++) {
            List<String> historyList = history.getHistory(formalTypeParams[i]);            
            JComboBox component = createComboBox(historyList);
            typeParameterList.addParameter(i, component, formalTypeParams[i].toString());
        }
        String startString = "<";
        String endString = ">";
        ParameterList superParamList = typeParameterList;
        return createParameterPanel(startString, endString, superParamList);
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     * Collects arguments and calls watcher objects (Invoker).
     */
    public void doOk()
    {
        if(!okCalled) {
            if (!JavaNames.isIdentifier(getNewInstanceName())) {
                setErrorMessage(illegalNameMsg);
                return;
            }
            ObjectBenchInterface ob = getObjectBench();
            if (ob != null && ob.hasObject(getNewInstanceName())) {
                setErrorMessage(duplicateNameMsg);
                return;
            }
            
            if (!parameterFieldsOk()) {
                setErrorMessage(emptyFieldMsg);            
            } else if (!typeParameterFieldsOk()) {     
                setErrorMessage(emptyTypeFieldMsg);
            } else {
                setWaitCursor(true);
                okButton.requestFocus();
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        callWatcher(OK);
                    }
                });
                okCalled = true;
            }
        }
    }

    /**
     * Redefined setEnabled method to ensure that OK button gets disabled.
     * As ActionListeners are also attached to combo boxes it can trigger 
     * more than one OK action as the default button also catches an 
     * action whther it has focus or not.
     * 
     * <p>Calling setEnabled on the Dialog alone does not prevent the default button 
     * from getting action events. We therefore explicitly call setEnabled on the 
     * default button (OK)
     * 
     * <p>The okCalled flag is used to prevent multiple rapid button presses before
     * the button and dialog are disabled.
     */
    public void setEnabled(boolean state)
    {
        okButton.setEnabled(state);
        super.setEnabled(state);
        if(state) {
            //reset ok called status when re-enabling dialog
            okCalled = false;    
        }
    }
    
    /**
     * getNewInstanceName - get the contents of the instance name field.
     */
    public String getNewInstanceName()
    {
        if (instanceNameText == null) {
            return "";
        }
        else {
            return instanceNameText.getText().trim();
        }
    }
    
    /*
     * @see bluej.debugmgr.CallDialog#getCallableView()
     */
    @Override
    protected CallableView getCallableView()
    {
        return constructor;
    }
}
