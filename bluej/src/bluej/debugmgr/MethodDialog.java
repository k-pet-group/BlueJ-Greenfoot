/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.gentype.GenTypeArray;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.TextType;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.ComponentFactory;
import bluej.utility.GrowableBox;
import bluej.utility.JavaNames;
import bluej.utility.MultiLineLabel;
import bluej.views.*;


/**
 * This dialog is used for an interactive method call. The call
 * can be an object creation or an invocation of an object method.
 * A new instance of this dialog is created for each method.
 *
 * @author  Michael Kolling
 * @author  Bruce Quig
 * @author  Poul Henriksen <polle@mip.sdu.dk>
 *
 * @version $Id: MethodDialog.java 6163 2009-02-19 18:09:55Z polle $
 */
public class MethodDialog extends CallDialog implements FocusListener
{
    private static final Insets INSETS = new Insets(2, 2, 2, 2);
    private static final int MD_CREATE = 0;
    private static final int MD_CALL = 1;

    private int dialogType;
    private boolean listeningObjects; // listening on the object bench
    private boolean okCalled;
    private boolean rawObject;

    // Window Titles
    static final String wCreateTitle = Config.getString("pkgmgr.methodCall.titleCreate");
    static final String wCallRoutineTitle = Config.getString("pkgmgr.methodCall.titleCall");
    // MD_CREATE Specific
    static final String sNameOfInstance = Config.getString("pkgmgr.methodCall.namePrompt");
    static final String sTypeParameters = Config.getString("pkgmgr.methodCall.typeParametersPrompt");
    static final String sTypeParameter = Config.getString("pkgmgr.methodCall.typeParameterPrompt");
    static final String emptyFieldMsg = Config.getString("error.methodCall.emptyField");
    static final String emptyTypeFieldMsg = Config.getString("error.methodCall.emptyTypeField");
    static final String illegalNameMsg = Config.getString("error.methodCall.illegalName");
    static final String duplicateNameMsg = Config.getString("error.methodCall.duplicateName");

    static final String commentSlash = "   ";

    private String methodName;
    private CallableView method;
    private Map typeParameterMap;

    // Text Area
    private JPanel descPanel;

    private ParameterList parameterList;
    private ParameterList typeParameterList;

    private JTextField instanceNameText;
    private JTextField focusedTextField;
    private JLabel callLabel;

    private CallHistory history;
    private String defaultParamValue = "";

    public static class VarArgFactory implements ComponentFactory
    {
        private List history;
        private MethodDialog dialog;

        public VarArgFactory(MethodDialog dialog, List history) {
            this.history = history;
            this.dialog = dialog;
        }

        public void setHistory(List history) {
            this.history = history;
        }
        
        public JComponent createComponent(JButton addButton, JButton removeButton)
        {
            Box container = new Box(BoxLayout.X_AXIS);
            JComboBox comboBox = dialog.createComboBox(history);
            comboBox.setSelectedIndex(0);
            container.add(comboBox);
            container.add(Box.createHorizontalStrut(5));
            container.add(new JLabel(" , "));
            container.add(Box.createHorizontalStrut(5));
            container.add(addButton);
            container.add(Box.createHorizontalStrut(5));
            container.add(removeButton);
            return container;
        }
    }

    /**
     * Class that holds the components for  a list of parameters. 
     * That is: the actual parameter component and the formal type of the parameter.
     * @author Poul Henriksen <polle@mip.sdu.dk>
     * @version $Id: MethodDialog.java 6163 2009-02-19 18:09:55Z polle $
     */
    public static class ParameterList
    {
        private List parameters;
        private List types;
        private boolean isVarArgs;
        private String defaultParamValue;

        public ParameterList(int initialSize, String defaultParamValue, boolean isVarArgs) 
        {            
            parameters = new ArrayList(initialSize);
            types = new ArrayList(initialSize);
            this.defaultParamValue = defaultParamValue;
            this.isVarArgs = isVarArgs;
        }

        public JComboBox getParameter(int index)
        {
            if (isVarArgs && index >= (parameters.size() - 1)) {
                GrowableBox box = getGrowableBox();
                int boxIndex = index - parameters.size() + 1;
                return (JComboBox) ((Container) box.getComponent(boxIndex)).getComponent(0);
            } else {
                return (JComboBox) parameters.get(index);
            }
        }

        public JComponent getParameterComponent(int index)
        {
            return (JComponent) parameters.get(index);
        }

        public String getType(int index)
        {
            if (isVarArgs && index >= (parameters.size() - 1)) {
                return (String) types.get(types.size() - 1);
            } else {
                return (String) types.get(index);
            }
        }

        private GrowableBox getGrowableBox()
        {
            if (parameters.size() < 1) {
                return null;
            }
            Object c = parameters.get(parameters.size() - 1);
            if (c instanceof GrowableBox) {
                return (GrowableBox) parameters.get(parameters.size() - 1);
            } else {
                return null;
            }
        }

        public void addParameter(int index, JComboBox component, String type)
        {
            parameters.add(index, component);
            types.add(index, type);
        }

        public void setVarArg(GrowableBox component, String type)
        {
            GrowableBox box = getGrowableBox();
            if (box != null) {
                parameters.remove(box);
            }
            parameters.add(component);
            types.add(type);
        }

        public int size()
        {
            if (isVarArgs) {
                return parameters.size() + getGrowableBox().getComponentCountWithoutEmpty() - 1;
            } else {
                return parameters.size();
            }
        }

        public void clear()
        {
            for (Iterator iter = parameters.iterator(); iter.hasNext();) {
                Object element = iter.next();
                if (isVarArgs && !iter.hasNext()) {
                    ((GrowableBox) element).clear();
                } else {
                    ((JComboBox) element).setSelectedIndex(0);
                }
            }
        }

        /**
         * Set the history for the given element.
         * 
         * @param i
         * @param historyList
         */
        public void setHistory(int i, List historyList)
        {
            if(historyList == null) {
                return;
            }
            else if (isVarArgs && i >= (parameters.size() - 1)) {
                GrowableBox varArgs = getGrowableBox();
                VarArgFactory factory = (VarArgFactory) varArgs.getComponentFactory();
                factory.setHistory(historyList);
            } else {
                getParameter(i).setModel(new DefaultComboBoxModel(historyList.toArray()));
                getParameter(i).insertItemAt(defaultParamValue, 0);
            }
        }
    }

    /**
     * MethodDialog constructor.
     * 
     * @param pmf          The assosciated PkgMgrFrame instance
     * @param instanceName The initial instance name (for a constructor dialog)
     *                     or the object instance on which the method is being called
     * @param method       The constructor or method being used
     * @param typeMap      The mapping of type parameter names (as they appear
     *                     in the method declaration) to runtime types
     *                     (a Map of String -> GenType).
     */
    public MethodDialog(PkgMgrFrame pmf, String instanceName, CallableView method, Map typeMap) {
        super(pmf, pmf.getObjectBench(), "");

        Package pkg = pmf.getPackage();

        history = pkg.getCallHistory();

        // Find out the type of dialog
        if (method instanceof MethodView) {
            dialogType = MD_CALL;
            methodName = ((MethodView) method).getName();
        }
        else if (method instanceof ConstructorView) {
            dialogType = MD_CREATE;
        }

        makeDialog(method.getClassName(), instanceName, method);
        setInstanceInfo(instanceName, typeMap);
    }

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
    public MethodDialog(JFrame parentFrame, ObjectBenchInterface ob, CallHistory callHistory, String instanceName, CallableView method, Map typeMap)
    {
        super(parentFrame, ob, "");

        history = callHistory;

        // Find out the type of dialog
        if (method instanceof MethodView) {
            dialogType = MD_CALL;
            methodName = ((MethodView) method).getName();
        }
        else if (method instanceof ConstructorView) {
            dialogType = MD_CREATE;
        }

        makeDialog(method.getClassName(), instanceName, method);
        setInstanceInfo(instanceName, typeMap);
    }


    /**
     * Set the visibility of the dialog, clearing parameter edit fields
     * and setting focus.
     */
    public void setVisible(boolean show)
    {
        if (! show) {
            if(listeningObjects)
                stopObjectBenchListening();
            listeningObjects = false;
        }
        super.setVisible(show);
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     * Collects arguments and calls watcher objects (Invoker).
     */
    public void doOk()
    {
        // only process if 
        if(!okCalled) {
            if (dialogType == MD_CREATE) {
                if (!JavaNames.isIdentifier(getNewInstanceName())) {
                    setErrorMessage(illegalNameMsg);
                    return;
                }
                ObjectBenchInterface ob = getObjectBench();
                if (ob != null && ob.hasObject(getNewInstanceName())) {
                    setErrorMessage(duplicateNameMsg);
                    return;
                }
            }
            
            if (!parameterFieldsOk()) {
                setErrorMessage(emptyFieldMsg);            
            } else if(!typeParameterFieldsOk()) {     
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
     * Process a "Cancel" event to cancel a Constructor or Method call.
     * Makes dialog invisible.
     */
    public void doCancel()
    {
        callWatcher(CANCEL);
    }

    /**
     * setDescription - display a new description in the dialog
     */
    private void setDescription(MultiLineLabel label)
    {
        label.setAlignmentX(LEFT_ALIGNMENT);
        descPanel.removeAll();
        descPanel.add(label);
        invalidate();
        validate();
    }

    /**
     * Get arguments from param entry fields as array of strings.
     * May be null (if there are no arguments).
     */
    public String[] getArgs()
    {
        String[] args = null;

        if (parameterList != null) {
            args = new String[parameterList.size()];
            for (int i = 0; i < parameterList.size(); i++) {
                args[i] = (String) parameterList.getParameter(i).getEditor().getItem();    
            }
        }
        return args;
    }
    
    /**
     * Returns false if any of the parameter fields are empty
     */
    public boolean parameterFieldsOk()
    {        
        if (parameterList != null) {            
            for (int i = 0; i < parameterList.size(); i++) {
                String arg = (String) parameterList.getParameter(i).getEditor().getItem();
                if (arg == null || arg.trim().equals(""))
                    return false;                
            }
        }
        return true;
    }
    
    /**
     * Returns false if some of the typeParameter fields are empty.
     * That is: if more than one type parameter, but not all, is typed in
     */
    public boolean typeParameterFieldsOk()
    {        
        boolean oneIsTypedIn = false;
        boolean oneIsEmpty = false;
        if (typeParameterList != null) {            
            for (int i = 0; i < typeParameterList.size(); i++) {
                String arg = (String) typeParameterList.getParameter(i).getEditor().getItem();
                if (arg == null || arg.trim().equals("")) {
                    oneIsEmpty = true;                     
                } else {
                    oneIsTypedIn = true;
                }
                if(oneIsEmpty && oneIsTypedIn) {
                    return false;
                }                
            }
        }
        return true;
    }

    /**
     * For a generic class this will return the type parameters if any has been
     * typed in. Otherwise it will just return an empty array.
     * 
     * @return A String array containing the type parameters as typed by the
     *         user
     */
    public String[] getTypeParams()
    {
        if (typeParameterList == null) {
            return new String[0];
        }
        String[] typeParams = new String[typeParameterList.size()];
        for (int i = 0; i < typeParameterList.size(); i++) {
            typeParams[i] = (String) typeParameterList.getParameter(i).getEditor().getItem();
            if (typeParams[i].equals("")) {
                // more complete checking of parameters is done elsewhere
                return new String[0];
            }
        }
        return typeParams;
    }

    /**
     * Insert text into edit field (JComboBox) that has focus.
     */
    public void insertText(String text)
    {
        if (parameterList != null) {
            if (focusedTextField != null) {
                focusedTextField.setText(text);
                // bring to front after insertion, doesn't seem to work.
                this.setVisible(true);
            }
        }
    }

    /**
     * setInstanceName - set the name of the instance shown in the label
     * for method call dialogs, or in the text field for construction dialogs,
     * and the assosciated type parameters.
     */
    public void setInstanceInfo(String instanceName, Map typeParams)
    {
        if(dialogType == MD_CALL) {
            typeParameterMap = typeParams;
            setCallLabel(instanceName, methodName);
            rawObject = instanceName != null && typeParams == null;
        }
        else {
            instanceNameText.setText(instanceName);
            rawObject = false;
        }
        createDescription();
        
        // reset error label message
        setErrorMessage("");

        clearParameters();
        if(! listeningObjects) {
            startObjectBenchListening();
            listeningObjects = true;
        }
        setVisible(true);

        // focus requests have been wrapped in invokeLater method to resolve issues 
        // with focus confusion on Mac OSX (BlueJ 2.0, JDK 1.4.2)
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                if (parameterList != null) {
                    parameterList.getParameter(0).getEditor().getEditorComponent().requestFocusInWindow();
                } else if (dialogType == MD_CREATE) {
                    instanceNameText.selectAll();
                    instanceNameText.requestFocusInWindow();
                }
                
                if (typeParameterList != null) {
                    typeParameterList.getParameter(0).getEditor().getEditorComponent().requestFocusInWindow();
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
        if (dialogType == MD_CALL)
            ((MethodView) method).print(writer, typeParameterMap, 0);
        else
            method.print(writer);
        setDescription(writer.getLabel());
        setVisible(true);
    }

    /**
     * getNewInstanceName - get the contents of the instance name field.
     */
    public String getNewInstanceName()
    {
        if (instanceNameText == null)
            return "";
        else
            return instanceNameText.getText().trim();
    }


    /**
     * getArgTypes - Get an array with the classes of the parameters for this
     * method. "null" if there are no parameters. <br>
     * If varArgsExpanded is set to true, the varargs will be expanded to the
     * number of variable arguments that have been typed into the dialog. For
     * instance, if the only argument is a vararg of type String and two strings
     * has been typed in, this method will return an array of two String
     * classes.
     * 
     * @param varArgsExpanded
     *            if set to true, varargs will be expanded.
     */
    public Class[] getArgTypes(boolean varArgsExpanded)
    {
        Class[] params = method.getParameters();
        boolean hasVarArgs = method.isVarArgs() && parameterList != null
                && parameterList.size() >= params.length;
        if (hasVarArgs && varArgsExpanded) {
            int totalParams = parameterList.size();
            Class[] allParams = new Class[totalParams];
            System.arraycopy(params, 0, allParams, 0, params.length);
            Class varArgType = params[params.length - 1].getComponentType();
            for (int i = params.length - 1; i < totalParams; i++) {
                allParams[i] = varArgType;
            }
            return allParams;
        } else {
            return params;
        }
    }

    /**
     * getArgTypes - Get an array with the types of the parameters for this
     * method. This takes into account mapping from type parameter names to
     * their types as supplied in the constructor or setInstanceInfo call.<p>
     * 
     * If varArgsExpanded is set to true, the varargs will be expanded to the
     * number of variable arguments that have been typed into the dialog. For
     * instance, if the only argument is a vararg of type String and two
     * strings have been typed in, this method will return an array of two
     * String classes.
     * 
     * @param varArgsExpanded
     *            if set to true, varargs will be expanded.
     * @param raw
     *            if true, raw types will be returned
     */
    public JavaType[] getArgGenTypes(boolean varArgsExpanded)
    {
        boolean raw = rawObject;
        
        // first construct a type parameter map which includes not only
        // type parameters from the declaring class, but also those from this
        // particular call
        Map typeMap = new HashMap();
        if (typeParameterMap != null)
            typeMap.putAll(typeParameterMap);
        
        String [] typeParams = getTypeParams();
        TypeParamView[] formalTypeParamViews = getFormalTypeParams();                  
        int len = typeParams.length;
        for (int i = 0; i < len; i++) {
            TypeParamView view = formalTypeParamViews[i];
            GenTypeDeclTpar formalType = view.getParamType();
            JavaType actualType = new TextType(typeParams[i]);
            typeMap.put(formalType.getTparName(), actualType);
        }
        
        // Map type parameter names in arguments to the corresponding types
        JavaType[] params = method.getParamTypes(raw);
        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].mapTparsToTypes(typeMap);
        }
        
        // handle varargs expansion
        boolean hasVarArgs = method.isVarArgs() && parameterList != null
                && parameterList.size() >= params.length;
        if (hasVarArgs && varArgsExpanded) {
            int totalParams = parameterList.size();
            JavaType[] allParams = new JavaType[totalParams];
            System.arraycopy(params, 0, allParams, 0, params.length);
            JavaType varArgType = ((GenTypeArray)params[params.length - 1]).getArrayComponent();
            for (int i = params.length - 1; i < totalParams; i++) {
                allParams[i] = varArgType;
            }
            return allParams;
        } else {
            return params;
        }
    }
    
    /**
     * Returns the formal type parameters for the class that declares this method.
     * @return Array of typeParamViews
     */
    public TypeParamView[] getFormalTypeParams() {
        View clazz = method.getDeclaringView();
        return clazz.getTypeParams();        
    }
    
    /**
     * Workaround for udating model problems with JComboBox.
     * Updates CallHistory and resets model to updated Vectors.  Ugly and
     * brutal but corrects problems with JComboBox update problems.
     */
    public void updateParameters()
    {
        if (parameterList != null) {
            Class[] paramClasses = getArgTypes(true);
            //First we add all the current items into the historylist
            for (int i = 0; i < parameterList.size(); i++) {
                history.addCall(paramClasses[i], (String) parameterList.getParameter(i).getEditor()
                        .getItem());                
            }
            //Then we update all the comboboxes
            for (int i = 0; i < parameterList.size(); i++) {                
                List historyList = history.getHistory(paramClasses[i]);                
                parameterList.setHistory(i, historyList);                
            }
        }
        
        if (typeParameterList != null) {
            TypeParamView[] formalTypeParams = getFormalTypeParams();
            String[] typeParams = getTypeParams();
            //First we add all the current items into the historylist
            for (int i = 0; i < typeParams.length; i++) {
                history.addCall(formalTypeParams[i], typeParams[i]);
            }
            //Then we update all the comboboxes
            for (int i = 0; i < typeParams.length; i++) {
                List historyList = history.getHistory(formalTypeParams[i]);
                typeParameterList.setHistory(i, historyList);                                
            }
        }
    }

    /**
     * Clear parameters of any param entry fields
     *
     */
    private void clearParameters()
    {
        if (parameterList != null) {
            parameterList.clear();
        }
        if (typeParameterList != null) {
            typeParameterList.clear();
        }
    }

    // --- FocusListener interface ---

    /**
     * FocusEventListener method fired when watched component gains focus
     * Assigns focusedTextField to work around difficulties in JComboBox
     * firing focus gained events.
     */
    public void focusGained(FocusEvent fe)
    {
        if (fe.getComponent() instanceof JTextField) {
            focusedTextField = (JTextField) fe.getComponent();
            focusedTextField.selectAll();
        }
    }

    /**
     * FocusEventListener method fired when watched component loses focus
     * Does nothing at present except for debug message.
     *
     */
    public void focusLost(FocusEvent fe)
    {
        //Debug.message(" Focus Lost: " + fe.paramString());
    }

    // --- end of FocusListener interface ---

    /**
     * Build the Swing dialog.
     */
    private void makeDialog(String className, String instanceName, CallableView method)
    {
        this.method = method;
        JPanel dialogPanel = new JPanel();
        {
            descPanel = new JPanel();
            {
                descPanel.setLayout(new BoxLayout(descPanel, BoxLayout.Y_AXIS));
                descPanel.setAlignmentX(LEFT_ALIGNMENT);
            }

            JPanel centerPanel = new JPanel();
            {
                centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
                centerPanel.setAlignmentX(LEFT_ALIGNMENT);
                //
                // Set dialog items depends on the Dialog type
                //
                switch (dialogType) {

                    case MD_CALL :
                        makeCallDialog(className, instanceName, method, centerPanel);
                        break;

                    case MD_CREATE :
                        makeCreateDialog(className, instanceName, method, centerPanel);
                        break;

                    default : // error!
                        throw new Error("Invalid MethodDialog type " + dialogType);
                }
            }

            dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
            dialogPanel.setBorder(BlueJTheme.generalBorder);
            dialogPanel.add(descPanel);
            dialogPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
            dialogPanel.add(new JSeparator());
            dialogPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
            dialogPanel.add(centerPanel);
            dialogPanel.add(getErrorLabel());
        }
        super.makeDialog(null, dialogPanel);
    }


    /**
     * makeCallDialog - create a dialog to make a method call
     */
    private void makeCallDialog(String className, String instanceName, CallableView method,
            JPanel panel)
    {
        JPanel tmpPanel;
        setTitle(wCallRoutineTitle);
        MethodView methView = (MethodView) method;
        tmpPanel = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
        tmpPanel.setLayout(gridBag);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = INSETS;
        callLabel = new JLabel("", JLabel.RIGHT);

        if (method.isStatic())
            setCallLabel(className, methodName);
        else
            setCallLabel(instanceName, methodName);
        if (methView.isMain())
            defaultParamValue = "{ }";

        setPreferredHeight(callLabel, getComboBoxHeight());

        constraints.anchor = GridBagConstraints.NORTHWEST;
        gridBag.setConstraints(callLabel, constraints);
        tmpPanel.add(callLabel);
        JPanel parameterPanel = createParameterPanel();
        constraints.gridy++;
        tmpPanel.add(parameterPanel, constraints);
        tmpPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tmpPanel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(tmpPanel);
    } // makeCallDialog



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
            if(getFormalTypeParams().length > 1) {
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
            tmpPanel.add(parameterPanel, constraints);

            constraints.gridx = 3;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            JPanel filler = new JPanel();
            gridBag.setConstraints(filler, constraints);
            tmpPanel.add(filler);
        }

        tmpPanel.setBorder(BorderFactory.createEmptyBorder(BlueJTheme.generalSpacingWidth, 0,
                BlueJTheme.generalSpacingWidth, 0));
        panel.add(tmpPanel, BorderLayout.NORTH);
    } // makeCreateDialog


    /**
     * Creates a panel of type parameters for a new object
     * 
     */
    private JPanel createTypeParameterPanel()
    {
        TypeParamView formalTypeParams[] = getFormalTypeParams();

        typeParameterList = new ParameterList(formalTypeParams.length, defaultParamValue, false);
        for (int i = 0; i < formalTypeParams.length; i++) {
            List historyList = history.getHistory(formalTypeParams[i]);            
            JComboBox component = createComboBox(historyList);
            typeParameterList.addParameter(i, component, formalTypeParams[i].toString());
        }
        String startString = "<";
        String endString = ">";
        ParameterList superParamList = typeParameterList;
        return createParameterPanel(startString, endString, superParamList);
    }



    /**
     * Creates a panel of parameters for a method
     * 
     */
    private JPanel createParameterPanel()
    {
        Class[] paramClasses = getArgTypes(false);
        String[] paramNames = method.getParamNames();
        String[] paramTypes = method.getParamTypeStrings();

        parameterList = new ParameterList(paramClasses.length, defaultParamValue, method.isVarArgs());
        for (int i = 0; i < paramTypes.length; i++) {
            String paramString = paramTypes[i];
            if(paramNames!=null) {
                paramString += " " + paramNames[i];
            }
            if (method.isVarArgs() && i == (paramClasses.length - 1)) {
                List historyList = history.getHistory(paramClasses[i].getComponentType());
                GrowableBox component = new GrowableBox(new VarArgFactory(this, historyList),
                        BoxLayout.Y_AXIS, INSETS.top + INSETS.bottom);
                //We want the dialog to resize when new args are added
                component.addComponentListener(new ComponentListener() {
                    public void componentResized(ComponentEvent e)
                    {
                        MethodDialog.this.pack();
                    }

                    public void componentMoved(ComponentEvent e)
                    {
                    }

                    public void componentShown(ComponentEvent e)
                    {
                    }

                    public void componentHidden(ComponentEvent e)
                    {
                    }
                });
                parameterList.setVarArg(component, paramString);
            } else {
                List historyList = history.getHistory(paramClasses[i]);
                JComboBox component = createComboBox(historyList);
                parameterList.addParameter(i, component, paramString);
            }
        }

        return createParameterPanel("(", ")", parameterList);
    }

    /**
     * Creates a panel of parameters.
     * 
     * @param startString The string prepended before the first parameter. Typically something like ( or <
     * @param endString The string appended after the last parameter. Typically something like ) or >
     * @param parameterList A list containing the components for the parameter panel
     * @return
     */
    private JPanel createParameterPanel(String startString, String endString,
            ParameterList parameterList)
    {
        JPanel tmpPanel = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
        tmpPanel.setLayout(gridBag);


        JLabel startParenthesis = new JLabel(startString);
        double comboHeight = getComboBoxHeight();
        //we want a large parenthesis
        double parenthesisHeight = startParenthesis.getPreferredSize().getHeight();
        double parenthesisScale = comboHeight / parenthesisHeight;
        Font f = startParenthesis.getFont();
        Font parenthesisFont = f.deriveFont((float) (f.getSize() * parenthesisScale));
        startParenthesis.setFont(parenthesisFont);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = INSETS;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.NORTHEAST;
        tmpPanel.add(startParenthesis, constraints);

        for (int i = 0; i < parameterList.size(); i++) {
            constraints.gridx = 1;
            constraints.gridy = i;
            constraints.anchor = GridBagConstraints.WEST;

            JComponent component = parameterList.getParameterComponent(i);
            gridBag.setConstraints(component, constraints);
            tmpPanel.add(component);

            JLabel eol = new JLabel(",", JLabel.LEFT);
            JLabel type = new JLabel(" " + parameterList.getType(i), JLabel.LEFT);
            if (i == (parameterList.size() - 1)) {
                eol.setText(endString);
                eol.setFont(parenthesisFont);
                if (parameterList.size() == 1) {                    
                    type = null;
                } else {
                    setPreferredHeight(type, comboHeight);
                    constraints.anchor = GridBagConstraints.NORTH;
                }                
            }                      

            if(type!=null) {
	            constraints.gridx = 3;
	            tmpPanel.add(type, constraints);
            }            

            constraints.gridx = 2;
            setPreferredHeight(eol, comboHeight);
            constraints.anchor = GridBagConstraints.SOUTHWEST;
            gridBag.setConstraints(eol, constraints);
            tmpPanel.add(eol); 
        }
        return tmpPanel;
    }

    /**
     * Sets the preferred height of the component.
     * @param c The component for which to change the preferred height
     * @param height the new height
     */
    private void setPreferredHeight(JComponent c, double height)
    {
        int lastTypeWidth = (int) c.getPreferredSize().getWidth();
        c.setPreferredSize(new Dimension(lastTypeWidth, (int) height));
    }

    /**
     * Calculates and returns the preferred height of a combobox.
     * 
     * @return Preferred height of a normal JComboBox
     */
    private double getComboBoxHeight()
    {
        JComboBox comboBox = createComboBox(new ArrayList());
        double comboHeight = comboBox.getPreferredSize().getHeight();
        return comboHeight;
    }

    /**
     * Set the text of the label showing the call to be made.
     */
    private void setCallLabel(String instanceName, String methodName)
    {
        if (callLabel != null)
            callLabel.setText(JavaNames.stripPrefix(instanceName) + "." + methodName);
    }

    private JComboBox createComboBox(List history)
    {        
        if(history == null) {
            history = new ArrayList();
        }
        JComboBox component = new JComboBox(history.toArray());
        component.insertItemAt(defaultParamValue, 0);
        component.setEditable(true);
        
        Dimension prefSize = component.getPreferredSize();
        if (prefSize.width < 100) {
            // On MacOS (Leopard) the ComboBox is tiny. So we
            // explicitly set the width here.
            prefSize.width = 100;
            component.setPreferredSize(prefSize);            
        }
        // treat 'return' in text field as OK
        component.getEditor().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
               doOk();
            }
        });
        // add FocusListener for text insertion
        ((JTextField) component.getEditor().getEditorComponent()).addFocusListener(this);
        return component;
    }
    
    /**
     * Redefined setEnabled method to ensure that OK button gets disabled.
     * As ActionListeners are also attached to combo boxes it can trigger 
     * more than one OK action as the default button also catches an 
     * action whther it has focus or not.
     * 
     * Calling setEnabled on the Dialog alone does not prevent the default button 
     * from getting action events. We therefore explicitly call setEnabled on the 
     * default button (OK)
     * 
     * The okCalled flag is used to prevent multiple rapid button presses before
     * the button and dialog are disabled.
     * 
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
}
