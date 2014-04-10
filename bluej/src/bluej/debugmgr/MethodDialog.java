/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014  Michael Kolling and John Rosenberg 
 
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import bluej.Config;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.utility.JavaNames;
import bluej.views.CallableView;
import bluej.views.LabelPrintWriter;
import bluej.views.MethodView;


/**
 * This dialog is used for an interactive method call. The call
 * can be an object creation or an invocation of an object method.
 * A new instance of this dialog is created for each method.
 *
 * @author  Michael Kolling
 * @author  Bruce Quig
 * @author  Poul Henriksen <polle@mip.sdu.dk>
 */
public class MethodDialog extends CallDialog
{
    private boolean okCalled;
    private boolean rawObject;

    // Window Titles
    private static final String appName = Config.getApplicationName(); 
    static final String wCallRoutineTitle = appName + ":  " + Config.getString("pkgmgr.methodCall.titleCall");

    static final String commentSlash = "   ";

    private String methodName;
    private MethodView method;
    private Map<String,GenTypeParameter> typeParameterMap;

    private JLabel callLabel;



    /**
     * MethodDialog constructor.
     * 
     * @param parentFrame  The parent window for the dialog
     * @param ob           The object bench to listen for object selection on
     * @param callHistory  The call history tracker
     * @param instanceName The initial instance name (for a constructor dialog)
     *                     or the object instance on which the method is being called
     * @param method       The constructor or method being used
     * @param typeMap      The mapping of type parameter names to runtime types
     *                     (a Map of String -> GenType).
     */
    public MethodDialog(JFrame parentFrame, ObjectBenchInterface ob, CallHistory callHistory,
            String instanceName, MethodView method, Map<String,GenTypeParameter> typeMap)
    {
        super(parentFrame, ob, "");

        history = callHistory;

        // Find out the type of dialog
        methodName = method.getName();

        this.method = method;
        makeDialog(method.getClassName(), instanceName);
        setInstanceInfo(instanceName, typeMap);
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     * Collects arguments and calls watcher objects (Invoker).
     */
    @Override
    public void doOk()
    {
        if(!okCalled) {
            if (!parameterFieldsOk()) {
                setErrorMessage(emptyFieldMsg);            
            }
            else if(!typeParameterFieldsOk()) {
                setErrorMessage(emptyTypeFieldMsg);
            } 
            else {
                setWaitCursor(true);
                okButton.requestFocus();
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
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
     * setInstanceName - set the name of the instance shown in the label
     * for method call dialogs, or in the text field for construction dialogs,
     * and the assosciated type parameters.
     */
    public void setInstanceInfo(String instanceName, Map<String,GenTypeParameter> typeParams)
    {
        typeParameterMap = typeParams;
        rawObject = instanceName != null && typeParams == null;
        createDescription();
        
        // reset error label message
        setErrorMessage("");

        clearParameters();
        startObjectBenchListening();

        // focus requests have been wrapped in invokeLater method to resolve issues 
        // with focus confusion on Mac OSX (BlueJ 2.0, JDK 1.4.2)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                if (typeParameterList != null) {
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
        method.print(writer, typeParameterMap, 0);
        setDescription(writer.getLabel());
        setVisible(true);
    }
    
    /*
     * @see bluej.debugmgr.CallDialog#makeDialogInternal(java.lang.String, java.lang.String, javax.swing.JPanel)
     */
    @Override
    protected void makeDialogInternal(String className, String instanceName, JPanel centerPanel)
    {
        makeCallDialog(className, instanceName, method, centerPanel);
    }

    /**
     * makeCallDialog - create a dialog to make a method call
     */
    private void makeCallDialog(String className, String instanceName, CallableView method, JPanel panel)
    {
        JPanel tmpPanel;
        setTitle(wCallRoutineTitle);
        MethodView methView = (MethodView) method;
        tmpPanel = new JPanel();
        if (!Config.isRaspberryPi()) tmpPanel.setOpaque(false);
        GridBagLayout gridBag = new GridBagLayout();
        tmpPanel.setLayout(gridBag);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = INSETS;
        callLabel = new JLabel("", SwingConstants.RIGHT);
        if (method.isStatic()) {
            setCallLabel(className);
        }
        else {
            setCallLabel(instanceName);
        }
        if (methView.isMain()) {
            defaultParamValue = "{ }";
        }

        setPreferredHeight(callLabel, getComboBoxHeight());

        constraints.anchor = GridBagConstraints.NORTHWEST;
        gridBag.setConstraints(callLabel, constraints);
        tmpPanel.add(callLabel);
        JPanel parameterPanel = createParameterPanel();
        if (!Config.isRaspberryPi()) parameterPanel.setOpaque(false);
        constraints.gridy++;
        tmpPanel.add(parameterPanel, constraints);
        tmpPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tmpPanel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(tmpPanel);
    }

    /**
     * Set the text of the label showing the call to be made.
     */
    public void setCallLabel(String instanceName)
    {
        callLabel.setText(JavaNames.stripPrefix(instanceName) + "." + methodName);
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
    @Override
    public void setEnabled(boolean state)
    {
        okButton.setEnabled(state);
        super.setEnabled(state);
        if(state) {
            //reset ok called status when re-enabling dialog
            okCalled = false;    
        }
    }
    
    /*
     * @see bluej.debugmgr.CallDialog#getCallableView()
     */
    @Override
    protected CallableView getCallableView()
    {
        return method;
    }
    
    /*
     * @see bluej.debugmgr.CallDialog#targetIsRaw()
     */
    @Override
    protected boolean targetIsRaw()
    {
        return rawObject;
    }

    /*
     * @see bluej.debugmgr.CallDialog#getTargetTypeArgs()
     */
    @Override
    protected Map<String, GenTypeParameter> getTargetTypeArgs()
    {
        return typeParameterMap;
    }
}
