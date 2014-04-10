/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014  Michael Kolling and John Rosenberg 
 
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
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.TextType;
import bluej.debugmgr.objectbench.ObjectBenchEvent;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.debugmgr.objectbench.ObjectBenchListener;
import bluej.utility.ComponentFactory;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.GrowableBox;
import bluej.utility.MultiLineLabel;
import bluej.views.CallableView;
import bluej.views.TypeParamView;
import bluej.views.View;

/**
 * Superclass for interactive call dialogs (method calls or free
 * form calls.
 *
 * @author  Michael Kolling
 */
public abstract class CallDialog extends EscapeDialog
    implements ObjectBenchListener, FocusListener
{
    protected static final Insets INSETS = new Insets(2, 2, 2, 2);

    protected static final String emptyFieldMsg = Config.getString("error.methodCall.emptyField");
    protected static final String emptyTypeFieldMsg = Config.getString("error.methodCall.emptyTypeField");

    public static final int OK = 0;
    public static final int CANCEL = 1;

    private MultiLineLabel errorLabel;
    protected ParameterList parameterList;
    protected ParameterList typeParameterList;

    private ObjectBenchInterface bench;
    private CallDialogWatcher watcher;
    private boolean listeningObjects; // listening on the object bench

    protected JButton okButton;
    protected String defaultParamValue = "";
    
    // Text Area
    private JPanel descPanel;
    private JTextField focusedTextField;
    
    protected CallHistory history;

    public static class VarArgFactory implements ComponentFactory
    {
        private List<String> history;
        private CallDialog dialog;

        public VarArgFactory(CallDialog dialog, List<String> history)
        {
            this.history = history;
            this.dialog = dialog;
        }

        public void setHistory(List<String> history)
        {
            this.history = history;
        }
        
        @Override
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
     */
    public static class ParameterList
    {
        private List<JComponent> parameters;
        private List<String> types;
        private boolean isVarArgs;
        private String defaultParamValue;

        public ParameterList(int initialSize, String defaultParamValue, boolean isVarArgs) 
        {            
            parameters = new ArrayList<JComponent>(initialSize);
            types = new ArrayList<String>(initialSize);
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
            return parameters.get(index);
        }

        public String getType(int index)
        {
            if (isVarArgs && index >= (parameters.size() - 1)) {
                return types.get(types.size() - 1);
            } 
            else {
                return types.get(index);
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
            }
            else {
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
            }
            else {
                return parameters.size();
            }
        }

        public void clear()
        {
            for (Iterator<JComponent> iter = parameters.iterator(); iter.hasNext();) {
                JComponent element = iter.next();
                if (isVarArgs && !iter.hasNext()) {
                    ((GrowableBox) element).clear();
                }
                else {
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
        public void setHistory(int i, List<String> historyList)
        {
            if(historyList == null) {
                return;
            }
            else if (isVarArgs && i >= (parameters.size() - 1)) {
                GrowableBox varArgs = getGrowableBox();
                VarArgFactory factory = (VarArgFactory) varArgs.getComponentFactory();
                factory.setHistory(historyList);
            }
            else {
                getParameter(i).setModel(new DefaultComboBoxModel(historyList.toArray()));
                getParameter(i).insertItemAt(defaultParamValue, 0);
            }
        }
    }
    
    public CallDialog(JFrame parentFrame, ObjectBenchInterface objectBench, String title)
    {
        super(parentFrame, title, false);
        bench = objectBench;
    }

    /**
     * The Ok button was pressed.
     */
    public abstract void doOk();

    /**
     * The Cancel button was pressed.
     * Process a "Cancel" event to cancel a Constructor or Method call.
     * Makes dialog invisible.
     */
    public void doCancel()
    {
        callWatcher(CANCEL);
    }

    /**
     * Set a watcher for events of this dialog.
     */
    public void setWatcher(CallDialogWatcher w)
    {
        watcher = w;
    }

    /**
     * callWatcher - notify watcher of dialog events.
     */
    public void callWatcher(int event)
    {
        if (watcher != null) {
            watcher.callDialogEvent(this, event);
        }
    }

    /**
     * setWaitCursor - Sets the cursor to "wait" style cursor, using swing
     *  bug workaround at present
     */
    public void setWaitCursor(boolean wait)
    {
        if(wait) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Return the label to be used for showing error messages.
     */
    protected MultiLineLabel getErrorLabel()
    {
        if(errorLabel == null) {
            errorLabel = new MultiLineLabel("\n\n", LEFT_ALIGNMENT);
            errorLabel.setForeground(new Color(136,56,56));  // dark red
        }
        return errorLabel;
    }

    /**
     * Return the frame's object bench.
     */
    protected ObjectBenchInterface getObjectBench()
    {
        return bench;
    }

    /**
     * Start listening to object bench events.
     */
    protected void startObjectBenchListening()
    {
        if (!listeningObjects && bench != null) {
            bench.addObjectBenchListener(this);
            listeningObjects = true;
        }
    }

    /**
     * Stop listening to object bench events.
     */
    protected void stopObjectBenchListening()
    {
        if (listeningObjects && bench != null) {
            bench.removeObjectBenchListener(this);
            listeningObjects = false;
        }
    }

    /**
     * setMessage - Sets a status bar style message for the dialog mainly
     *  for reporting back compiler errors upon method calls.
     */
    public void setErrorMessage(String message)
    {
        // cut the "location: __SHELL3" bit from some error messages
        int index = message.indexOf("location:");
        if(index != -1) {
            message = message.substring(0,index-1);
        }

        errorLabel.setText(message);
        pack();
        invalidate();
        validate();
    }

    // ---- ObjectBenchListener interface ----

    /**
     * The object was selected interactively (by clicking
     * on it with the mouse pointer).
     */
    @Override
    public void objectEvent(ObjectBenchEvent obe)
    {
        NamedValue value = obe.getValue();
        insertText(value.getName());
    }

    /**
     * Build the Swing dialog. The top and center components
     * are supplied by the specific subclasses. This method
     * add the Ok and Cancel buttons.
     */
    protected void makeDialog(JComponent topComponent, JComponent centerComponent)
    {
        JPanel contentPane = (JPanel)getContentPane();

        // create the ok/cancel button panel
        JPanel buttonPanel = new JPanel();
        if (!Config.isRaspberryPi()) buttonPanel.setOpaque(false);
        {
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

            //JButton okButton = BlueJTheme.getOkButton();
            okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt)
                { 
                    doOk();
                }
            });

            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt)
                { 
                    doCancel(); 
                }
            });
                    
            DialogManager.addOKCancelButtons(buttonPanel, okButton, cancelButton);
            getRootPane().setDefaultButton(okButton);
        }

        //contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setLayout(new BorderLayout(6,6));
        contentPane.setBorder(BlueJTheme.generalBorder);

        if(topComponent != null) {
            contentPane.add(topComponent, BorderLayout.NORTH);
        }
        if(centerComponent != null) {
            contentPane.add(centerComponent, BorderLayout.CENTER);
        }
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        pack();
        DialogManager.centreDialog(this);

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event)
            {
                setVisible(false);
            }
        });
    }
    
    /**
     * Returns the formal type parameters for the class that declares this method.
     * @return Array of typeParamViews
     */
    public static TypeParamView[] getFormalTypeParams(CallableView callable)
    {
        View clazz = callable.getDeclaringView();
        return clazz.getTypeParams();        
    }
    
    /**
     * Calculates and returns the preferred height of a combobox.
     * 
     * @return Preferred height of a normal JComboBox
     */
    protected double getComboBoxHeight()
    {
        JComboBox comboBox = createComboBox(new ArrayList<String>());
        double comboHeight = comboBox.getPreferredSize().getHeight();
        return comboHeight;
    }

    protected JComboBox createComboBox(List<String> history)
    {        
        if(history == null) {
            history = new ArrayList<String>();
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
            @Override
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
     * Sets the preferred height of the component.
     * @param c The component for which to change the preferred height
     * @param height the new height
     */
    protected void setPreferredHeight(JComponent c, double height)
    {
        int lastTypeWidth = (int) c.getPreferredSize().getWidth();
        c.setPreferredSize(new Dimension(lastTypeWidth, (int) height));
    }

    /**
     * Creates a panel of parameters for a method
     */
    protected JPanel createParameterPanel()
    {
        CallableView method = getCallableView();
        Class<?>[] paramClasses = getArgTypes(false);
        String[] paramNames = method.getParamNames();
        String[] paramTypes = method.getParamTypeStrings();

        parameterList = new ParameterList(paramClasses.length, defaultParamValue, method.isVarArgs());
        for (int i = 0; i < paramTypes.length; i++) {
            String paramString = paramTypes[i];
            if(paramNames!=null) {
                paramString += " " + paramNames[i];
            }
            if (method.isVarArgs() && i == (paramClasses.length - 1)) {
                List<String> historyList = history.getHistory(paramClasses[i].getComponentType());
                GrowableBox component = new GrowableBox(new VarArgFactory(this, historyList),
                        BoxLayout.Y_AXIS, INSETS.top + INSETS.bottom);
                //We want the dialog to resize when new args are added
                component.addComponentListener(new ComponentListener() {
                    @Override
                    public void componentResized(ComponentEvent e)
                    {
                        CallDialog.this.pack();
                    }

                    @Override
                    public void componentMoved(ComponentEvent e)
                    {
                    }

                    @Override
                    public void componentShown(ComponentEvent e)
                    {
                    }

                    @Override
                    public void componentHidden(ComponentEvent e)
                    {
                    }
                });
                parameterList.setVarArg(component, paramString);
            } 
            else {
                List<String> historyList = history.getHistory(paramClasses[i]);
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
    protected JPanel createParameterPanel(String startString, String endString,
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

            JLabel eol = new JLabel(",", SwingConstants.LEFT);
            JLabel type = new JLabel(" " + parameterList.getType(i), SwingConstants.LEFT);
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
                    type.setLabelFor(component);
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
    public Class<?>[] getArgTypes(boolean varArgsExpanded)
    {
        CallableView method = getCallableView();
        Class<?>[] params = method.getParameters();
        boolean hasVarArgs = method.isVarArgs() && parameterList != null
                && parameterList.size() >= params.length;
        if (hasVarArgs && varArgsExpanded) {
            int totalParams = parameterList.size();
            Class<?>[] allParams = new Class[totalParams];
            System.arraycopy(params, 0, allParams, 0, params.length);
            Class<?> varArgType = params[params.length - 1].getComponentType();
            for (int i = params.length - 1; i < totalParams; i++) {
                allParams[i] = varArgType;
            }
            return allParams;
        }
        else {
            return params;
        }
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
                if (arg == null || arg.trim().equals("")) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Returns false if some of the typeParameter fields are empty.
     * That is: if one or more type parameters, but not all, are typed in
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
                }
                else {
                    oneIsTypedIn = true;
                }
                if (oneIsEmpty && oneIsTypedIn) {
                    return false;
                }             
            }
        }
        return true;
    }

    /**
     * Clear parameters of any param entry fields
     */
    protected void clearParameters()
    {
        if (parameterList != null) {
            parameterList.clear();
        }
        if (typeParameterList != null) {
            typeParameterList.clear();
        }
    }
    
    /**
     * Set the visibility of the dialog, clearing parameter edit fields
     * and setting focus.
     */
    @Override
    public void setVisible(boolean show)
    {
        if (! show) {
            stopObjectBenchListening();
        }
        super.setVisible(show);
    }

    /**
     * Build the Swing dialog.
     */
    protected void makeDialog(String className, String instanceName)
    {
        super.setContentPane(new JPanel() {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                
                Graphics2D g2d = (Graphics2D)g;
                int width = getWidth();
                int height = getHeight();
                
                if (!Config.isRaspberryPi()){
                    g2d.setPaint(new GradientPaint(width/4, 0, new Color(230,229,228),
                            width*3/4, height, new Color(191,186,178)));
                }else{
                    g2d.setPaint(new Color(214, 217, 223));
                }
                
                g2d.fillRect(0, 0, width, height);
            }
        });
        
        JPanel dialogPanel = new JPanel();
        if (!Config.isRaspberryPi()) dialogPanel.setOpaque(false);
        {
            descPanel = new JPanel();
            if (!Config.isRaspberryPi()) descPanel.setOpaque(false);
            {
                descPanel.setLayout(new BoxLayout(descPanel, BoxLayout.Y_AXIS));
                descPanel.setAlignmentX(LEFT_ALIGNMENT);
            }

            JPanel centerPanel = new JPanel();
            if (!Config.isRaspberryPi()) centerPanel.setOpaque(false);
            {
                centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
                centerPanel.setAlignmentX(LEFT_ALIGNMENT);
                //
                // Set dialog items depends on the Dialog type
                //
                makeDialogInternal(className, instanceName, centerPanel);
            }

            dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
            dialogPanel.setBorder(BlueJTheme.generalBorder);
            dialogPanel.add(descPanel);
            dialogPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(191,190,187));
            if (!Config.isRaspberryPi()) {
                sep.setBackground(new Color(0,0,0,0));
            }else {
                sep.setBackground(new Color(0,0,0));
            }
            dialogPanel.add(sep);
            dialogPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
            dialogPanel.add(centerPanel);
            dialogPanel.add(getErrorLabel());
        }
        makeDialog(null, dialogPanel);
    }

    /**
     * setDescription - display a new description in the dialog
     */
    protected void setDescription(MultiLineLabel label)
    {
        label.setAlignmentX(LEFT_ALIGNMENT);
        descPanel.removeAll();
        descPanel.add(label);
        if (!Config.isRaspberryPi()) label.setOpaque(false);
        invalidate();
        validate();
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
     * Workaround for udating model problems with JComboBox.
     * Updates CallHistory and resets model to updated Vectors.  Ugly and
     * brutal but corrects problems with JComboBox update problems.
     */
    public void updateParameters()
    {
        if (parameterList != null) {
            Class<?>[] paramClasses = getArgTypes(true);
            //First we add all the current items into the historylist
            for (int i = 0; i < parameterList.size(); i++) {
                history.addCall(paramClasses[i], (String) parameterList.getParameter(i).getEditor()
                        .getItem());                
            }
            //Then we update all the comboboxes
            for (int i = 0; i < parameterList.size(); i++) {                
                List<String> historyList = history.getHistory(paramClasses[i]);                
                parameterList.setHistory(i, historyList);                
            }
        }
        
        if (typeParameterList != null) {
            CallableView callable = getCallableView();
            TypeParamView[] formalTypeParams = getFormalTypeParams(callable);
            String[] typeParams = getTypeParams();
            //First we add all the current items into the historylist
            for (int i = 0; i < typeParams.length; i++) {
                history.addCall(formalTypeParams[i], typeParams[i]);
            }
            //Then we update all the comboboxes
            for (int i = 0; i < typeParams.length; i++) {
                List<String> historyList = history.getHistory(formalTypeParams[i]);
                typeParameterList.setHistory(i, historyList);                                
            }
        }
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
        CallableView method = getCallableView();
        boolean raw = targetIsRaw();
        
        // first construct a type parameter map which includes not only
        // type parameters from the declaring class, but also those from this
        // particular call
        Map<String,GenTypeParameter> typeParameterMap = getTargetTypeArgs();
        Map<String,GenTypeParameter> typeMap = new HashMap<String,GenTypeParameter>();
        if (typeParameterMap != null) {
            typeMap.putAll(typeParameterMap);
        }
        
        String [] typeParams = getTypeParams();
        TypeParamView[] formalTypeParamViews = getFormalTypeParams(method);                  
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
            params[i] = params[i].mapTparsToTypes(typeMap).getUpperBound();
        }
        
        // handle varargs expansion
        if (hasVarArgs(method, params) && varArgsExpanded) {
            int totalParams = parameterList.size();
            JavaType[] allParams = new JavaType[totalParams];
            System.arraycopy(params, 0, allParams, 0, params.length);
            JavaType varArgType = params[params.length - 1].getArrayComponent();
            for (int i = params.length - 1; i < totalParams; i++) {
                allParams[i] = varArgType;
            }
            return allParams;
        }
        else {
            return params;
        }
    }

    private boolean hasVarArgs(CallableView method, JavaType[] params)
    {
        if (!method.isVarArgs()) {
            return false;
        }
        if (parameterList == null) {
            return false;
        }
        if (parameterList.size() < params.length) {
            return false;
        }
        if (getArgs().length == 1 && isEmptyArg(getArgs()[0]) ) {
            return false;
        }
        return true;
    }

    private boolean isEmptyArg(String value)
    {
        String[] emptyArgs = {"{ }", "{}", ""};
        return  Arrays.asList(emptyArgs).contains(value.trim());
    }

    /**
     * Get the user-supplied instance name (constructor dialogs). Returns null for method dialogs.
     */
    protected String getNewInstanceName()
    {
        return null;
    }
    
    protected abstract CallableView getCallableView();
    
    protected abstract void makeDialogInternal(String className, String instanceName, JPanel centerPanel);
    
    protected boolean targetIsRaw()
    {
        return false;
    }
    
    protected Map<String,GenTypeParameter> getTargetTypeArgs()
    {
        return Collections.emptyMap();
    }
    
    // --- FocusListener interface ---

    /**
     * FocusEventListener method fired when watched component gains focus
     * Assigns focusedTextField to work around difficulties in JComboBox
     * firing focus gained events.
     */
    @Override
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
    @Override
    public void focusLost(FocusEvent fe)
    {
        //Debug.message(" Focus Lost: " + fe.paramString());
    }

    // --- end of FocusListener interface ---
}
