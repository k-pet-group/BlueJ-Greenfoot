package bluej.debugmgr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import bluej.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.utility.*;
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
 * @version $Id: MethodDialog.java 2586 2004-06-10 13:33:02Z polle $
 */
public class MethodDialog extends CallDialog
	implements FocusListener
{
    static final int MD_CREATE = 0;
    static final int MD_CALL = 1;

    int dialogType;

    // Window Titles
    static final String wCreateTitle = Config.getString("pkgmgr.methodCall.titleCreate");
    static final String wCallRoutineTitle = Config.getString("pkgmgr.methodCall.titleCall");
    // MD_CREATE Specific
    static final String sNameOfInstance = Config.getString("pkgmgr.methodCall.namePrompt");
    static final String emptyFieldMsg = Config.getString("error.methodCall.emptyField");
    static final String illegalNameMsg = Config.getString("error.methodCall.illegalName");
    static final String duplicateNameMsg = Config.getString("error.methodCall.duplicateName");

    static final String commentSlash = "   ";

    private String methodName;
    private CallableView method;

    // Text Area
    private JPanel descPanel;

    private ParameterList parameterList;
    
    private JTextField instanceNameText;
    private JTextField focusedTextField;
    private JLabel callLabel;

    private CallHistory history;
    private boolean emptyField = false;
    private String defaultParamValue = "";
    
    public static class VarArgFactory implements ComponentFactory{        
        private List history;
        private MethodDialog dialog;
        public VarArgFactory(MethodDialog dialog, List history) {
            this.history=history;
            this.dialog = dialog;
        }        
        public Component createComponent(JButton addButton, JButton removeButton) {
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
            container.setBorder(BorderFactory.createEmptyBorder(2,0,2,0));
            return container;
        }        
    }
        
    public static class ParameterList {
        private List parameters;
        private boolean isVarArgs;
        
        public ParameterList(int initialSize, boolean isVarArgs) {
            parameters = new ArrayList(initialSize);
            this.isVarArgs = isVarArgs;
        }
        
        public JComboBox get(int index) {
            if(isVarArgs && index>=(parameters.size()-1)) {
                GrowableBox box = getGrowableBox();
                int boxIndex = index-parameters.size()+1;
                return (JComboBox) ((Container) box.getComponent(boxIndex)).getComponent(0);
            } else {
                return (JComboBox) parameters.get(index);
            }
        }        
        
        private GrowableBox getGrowableBox() {
            if(parameters.size() < 1) {
                return null;
            }
            Object c = parameters.get(parameters.size()-1);
            if(c instanceof GrowableBox) {
                return (GrowableBox) parameters.get(parameters.size()-1);
            } else {
                return null;
            }
        }
        
        public void addParameter(int index, JComboBox  component) {
            parameters.add(index, component);            
        }
        
        public void setVarArg(GrowableBox  component) {
            GrowableBox box = getGrowableBox();
            if(box != null) {
                parameters.remove(box);
            }
            parameters.add(component);
        }
        
        public int size() {
            if(isVarArgs) {
                return parameters.size() + getGrowableBox().getComponentCountWithoutEmpty() - 1;
            }
            else {
                return parameters.size();
            }
        }
        public void clear() {
            for (Iterator iter = parameters.iterator(); iter.hasNext();) {
                Object element =iter.next();
                if(isVarArgs && !iter.hasNext()) {
                    ((GrowableBox) element).clear();
                } else{
                    ((JComboBox) element).setSelectedIndex(0);                    
                }      
            }
        }        
    }

    public MethodDialog(PkgMgrFrame pmf, String className,
                        String instanceName, CallableView method)
    {
        super(pmf, "");

        Package pkg = pmf.getPackage();

        history = pkg.getCallHistory();

        // Find out the type of dialog
        if(method instanceof MethodView) {
            dialogType = MD_CALL;
            methodName = ((MethodView)method).getName();
        }
        else if (method instanceof ConstructorView) {
            dialogType = MD_CREATE;
        }

        makeDialog(className, instanceName, method);
    }
    
    /**
     * Set the visibility of the dialog, clearing parameter edit fields
     * and setting focus.
     */
    public void setVisible(boolean show)
    {
    	// reset error label message
    	setErrorMessage("");

    	if (show) {
            show();
    	    clearParameters();
            startObjectBenchListening();

      	    if(parameterList != null) {
                parameterList.get(0).getEditor().getEditorComponent().requestFocus();
      	    }
      	    else if(dialogType == MD_CREATE) {
                  instanceNameText.selectAll();
                  instanceNameText.requestFocus();
      	    }
    	}
    	else {
            stopObjectBenchListening();
            hide();
        }
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     *  Collects arguments and calls watcher objects (Invoker).
     */
    public void doOk()
    {
        String[] paramNames = getArgs();  // sets "emptyField"

        if(dialogType == MD_CREATE) {
            if(!JavaNames.isIdentifier(getNewInstanceName())) {
                setErrorMessage(illegalNameMsg);
                return;
            }
            if(getObjectBench().hasObject(getNewInstanceName())) {
                setErrorMessage(duplicateNameMsg);
                return;
            }
        }

        if(emptyField) {
            setErrorMessage(emptyFieldMsg);
            emptyField = false;
        }
        else {
            setWaitCursor(true);
            callWatcher(OK);
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
    public void setDescription(MultiLineLabel label)
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

        if(parameterList != null) {
            // Debug.message("params not null");
            args = new String[parameterList.size()];
            for(int i = 0; i < parameterList.size(); i++) {
                String arg = (String)parameterList.get(i).getEditor().getItem();
                if(arg == null || arg.trim().equals(""))
                    emptyField = true;                
                args[i] = arg;
            }
        }
        return args;
    }

    /**
     * Insert text into edit field (JComboBox) that has focus.
     */
    public void insertText(String text)
    {
        if(parameterList != null) {
            if(focusedTextField != null) {
                focusedTextField.setText(text);
                // bring to front after insertion, doesn't seem to work.
                this.show();
            }
        }
    }

    /**
     * setInstanceName - set the name of the instance shown in the label
     *  for method call dialogs.
     */
    public void setInstanceName(String instanceName)
    {
        setCallLabel(instanceName, methodName);
    }

    /**
     * setNewInstanceName - set the field for the new instance name for
     *  contruction dialogs.
     */
    public void setNewInstanceName(String name)
    {
        instanceNameText.setText(name);
    }

    /**
     * getNewInstanceName - get the contents of the instance name field.
     */
    public String getNewInstanceName()
    {
        if(instanceNameText == null)
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
    public Class[] getArgTypes(boolean varArgsExpanded) {
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
     * Workaround for udating model problems with JComboBox.
     * Updates CallHistory and resets model to updated Vectors.  Ugly and
     * brutal but corrects problems with JComboBox update problems.
     */
    public void updateParameters()
    {
        if(parameterList != null) {
            Class[] paramClasses = getArgTypes(true);
            for(int i = 0; i < parameterList.size(); i++) {
                history.addCall(paramClasses[i],
                                (String)parameterList.get(i).getEditor().getItem());
                List historyList = history.getHistory(paramClasses[i]);
                parameterList.get(i).setModel(new DefaultComboBoxModel(historyList.toArray()));
                parameterList.get(i).insertItemAt(defaultParamValue, 0);
            }
        }
    }

    /**
     * Clear parameters of any param entry fields
     *
     */
    private void clearParameters()
    {
        if(parameterList != null) {
            parameterList.clear(); 
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
        if(fe.getComponent() instanceof JTextField) {
            focusedTextField = (JTextField)fe.getComponent();
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
        // Debug.message(" Focus Lost: " + fe.paramString());
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

                case MD_CALL:
            	    makeCallDialog(className, instanceName, method, centerPanel);
            	    break;

                case MD_CREATE:
            	    makeCreateDialog(className, instanceName, method, centerPanel);
            	    break;

                default:	// error!
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
            JPanel panel) {
        JPanel tmpPanel;
        setTitle(wCallRoutineTitle);
        MethodView methView = (MethodView) method;
        tmpPanel = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
        tmpPanel.setLayout(gridBag);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        callLabel = new JLabel("", JLabel.RIGHT);
        
        if (method.isStatic())
            setCallLabel(className, methodName);
        else
            setCallLabel(instanceName, methodName);
        if (methView.isMain())
            defaultParamValue = "{ }";
        
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
        JPanel tmpPanel;
        setTitle(wCreateTitle);

        JLabel instName = new JLabel(sNameOfInstance);
        instanceNameText = new JTextField(instanceName, 16);
        // treat 'return' in text field as OK
        instanceNameText.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    doOk();
                }
            });
        instanceNameText.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent fe)
                {
                    ((JTextField)(fe.getComponent())).selectAll();
                }
                public void focusLost(FocusEvent fe) { }
            });
        
        if(method.hasParameters()) {            
            tmpPanel = new JPanel();
            GridBagLayout gridBag = new GridBagLayout();
            tmpPanel.setLayout(gridBag);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(2,2,2,2);

            gridBag.setConstraints(instName, constraints);
            tmpPanel.add(instName);

            constraints.gridx = 1;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            gridBag.setConstraints(instanceNameText, constraints);
            tmpPanel.add(instanceNameText);

            JLabel name = new JLabel("new " + className , JLabel.RIGHT);
            constraints.gridwidth = 1;
            constraints.gridx = 0;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.fill = GridBagConstraints.NONE;
            gridBag.setConstraints(name,constraints);
            tmpPanel.add(name);            
            
            constraints.gridy = 1;
            constraints.anchor = GridBagConstraints.WEST;

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
          
            JPanel parameterPanel = createParameterPanel();
            tmpPanel.add(parameterPanel,constraints);

            constraints.gridx = 3;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            JPanel filler = new JPanel();
            gridBag.setConstraints(filler, constraints);
            tmpPanel.add(filler);
        }
        else {
            tmpPanel = new JPanel();
            tmpPanel.add(instName);
            tmpPanel.add(instanceNameText);
        }
        tmpPanel.setBorder(BorderFactory.createEmptyBorder(BlueJTheme.generalSpacingWidth,
                                                           0,
                                                           BlueJTheme.generalSpacingWidth,
                                                           0));
        panel.add("North", tmpPanel);
    } // makeCreateDialog
    
    
    private JPanel createParameterPanel() {
    	Class[] paramClasses = getArgTypes(false);
        JPanel tmpPanel = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
        tmpPanel.setLayout(gridBag);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2,2,2,2);        
        JLabel startParenthesis = new JLabel(" (");
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.NORTHEAST;
        tmpPanel.add(startParenthesis, constraints);
        
        parameterList = new ParameterList(paramClasses.length, method.isVarArgs());

        for (int i = 0; i < paramClasses.length; i++) {
            constraints.gridx = 1;
            constraints.anchor = GridBagConstraints.WEST;

            constraints.gridy = i;
            
            if(method.isVarArgs() && i == (paramClasses.length - 1)) {
                List historyList = history.getHistory(paramClasses[i].getComponentType());
                GrowableBox component = new GrowableBox(new VarArgFactory(this,historyList), BoxLayout.Y_AXIS);
                
                //We want the dialog to resize when new args are added
                component.addComponentListener(new ComponentListener() {
                    public void componentResized(ComponentEvent e) {
                        MethodDialog.this.pack();
                    }
                    public void componentMoved(ComponentEvent e) {
                    }
                    public void componentShown(ComponentEvent e) {
                    }
                    public void componentHidden(ComponentEvent e) {
                    }});
                parameterList.setVarArg((GrowableBox)component);
                gridBag.setConstraints(component, constraints);
                tmpPanel.add(component);
                constraints.gridheight=1;
            } else {
                List historyList = history.getHistory(paramClasses[i]);
                JComboBox component = createComboBox(historyList);
                parameterList.addParameter(i, component);
                int oldHeight = constraints.gridheight;
                gridBag.setConstraints(component, constraints);
                tmpPanel.add(component);
            }                
            
            //parse method signature for param fields, there may be a better way
        	String[] paramNames = null;
        	if(method.hasParameters())
        	    paramNames = parseParamNames(method.getLongDesc());
        	
            constraints.gridx = 2;
            JLabel eol = new JLabel(",  " + commentSlash + paramNames[i], JLabel.LEFT);
            if (i == (paramClasses.length - 1)) {
                if(paramClasses.length == 1) {
                    eol.setText(")");
                } else {
                    JLabel lastType =  new JLabel(commentSlash + paramNames[i]);   
                    //HACK this border should be calculated properly
                    lastType.setBorder(BorderFactory.createEmptyBorder(7,0,0,0));
                    constraints.anchor=GridBagConstraints.NORTH;
                    tmpPanel.add(lastType, constraints);
                    //HACK and this border should be calculated properly too
                    eol.setBorder(BorderFactory.createEmptyBorder(0,0,7,0));
                    eol.setText(")  ");
                    
                }
            }
            constraints.anchor=GridBagConstraints.SOUTHWEST;
            gridBag.setConstraints(eol,constraints);
            tmpPanel.add(eol);
        }
        return tmpPanel;
    }

    /**
     * Set the text of the label showing the call to be made.
     */
    private void setCallLabel(String instanceName, String methodName)
    {
        if (callLabel != null)
            callLabel.setText(JavaNames.stripPrefix(instanceName) +
                              "." + methodName);
    }
    
    private JComboBox createComboBox(List history) {
        JComboBox component = new JComboBox(history.toArray());       
        component.insertItemAt(defaultParamValue, 0);
        component.setEditable(true);
        // treat 'return' in text field as OK
        component.getEditor().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    doOk();
                }
            });
        // add FocusListener for text insertion
        ((JTextField) component.getEditor().getEditorComponent()).addFocusListener(this);
        return component;
    }

    /**
     * parseParamNames - parses a long description of a method and returns
     * the argument types and names as an array of Strings.
     *
     * @return an array of Strings representing method arguments.
     */
    public String[] parseParamNames(String longMethodName)
    {
        String[] argNames = null;
        StringTokenizer tokenizer = new StringTokenizer(longMethodName,",()",true);
        String arg = "";
        boolean inArg = false;
        List args = new ArrayList();

        while( tokenizer.hasMoreTokens()){
            String token = tokenizer.nextToken();

            if (token.equals("(")){
                inArg = true;
                arg = "";
            }
            else if (token.equals(",")){
                args.add(arg);
                inArg = true;
                arg = "";
            }
            else if (token.equals(")")){
                args.add(arg);
                inArg = false;
            }
            else {
                if(inArg)
                    arg += token;
            }
        }
        //Debug.message("params = " + args.toString());
        argNames = new String[args.size()];
        for(int i = 0; i < args.size(); i++)
	    argNames[i] = ((String)args.get(i)).trim();

        return argNames;
    } // parseParamNames
}
