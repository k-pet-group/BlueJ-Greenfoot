/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015,2016,2017,2019  Michael Kolling and John Rosenberg
 
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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import bluej.debugmgr.inspector.AssertPanel;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import bluej.Config;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.TextType;
import bluej.debugmgr.objectbench.ObjectBenchEvent;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.debugmgr.objectbench.ObjectBenchListener;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.dialog.DialogPaneAnimateError;
import bluej.views.CallableView;
import bluej.views.TypeParamView;
import bluej.views.View;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Superclass for interactive call dialogs (method calls or free
 * form calls.
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public abstract class CallDialog extends Dialog<Void>
    implements ObjectBenchListener
{
    protected static final String emptyFieldMsg = Config.getString("error.methodCall.emptyField");
    protected static final String emptyTypeFieldMsg = Config.getString("error.methodCall.emptyTypeField");

    private Label errorLabel;
    protected ParameterList parameterList;
    protected ParameterList typeParameterList;

    protected final ObjectBenchInterface bench;

    protected String defaultParamValue = "";
    
    // Text Area
    private Pane descPanel;
    protected TextField focusedTextField;
    
    protected CallHistory history;
    private DialogPaneAnimateError dialogPane;

    // Flag indicating if the dialog requires to show assertion (for tests).
    protected boolean showAssertion = false;
    // Type of the assertion's variable to evaluate.
    protected JavaType assertionEvalType = null;
    // The assertion panel to add to the dialog if required.
    private AssertPanel assertPanel = null;

    public CallDialog(Window parent, ObjectBenchInterface objectBench, String title)
    {
        initOwner(parent);
        setTitle(title);
        initModality(Modality.NONE);
        setResizable(true);
        bench = objectBench;
        bench.addObjectBenchListener(this);
    }

    /**
     * The Ok button was pressed.
     */
    public abstract void handleOK();

    /**
     * setWaitCursor - Sets the cursor to "wait" style cursor or back to default
     */
    public void setWaitCursor(boolean wait)
    {
        getDialogPane().setCursor(wait ? Cursor.WAIT : Cursor.DEFAULT);
    }

    /**
     * Set the enabled state of the OK button
     */
    public void setOKEnabled(boolean state)
    {
        dialogPane.getOKButton().setDisable(!state);
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

        String messageFinal = message;
        JavaFXUtil.runNowOrLater(() -> errorLabel.setText(messageFinal));
    }

    // ---- ObjectBenchListener interface ----

    /**
     * The object was selected interactively (by clicking
     * on it with the mouse pointer).
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void objectEvent(ObjectBenchEvent obe)
    {
        NamedValue value = obe.getValue();
        String name = value.getName();
        insertText(name);
    }

    /**
     * Returns the formal type parameters for the class that declares this method.
     * @return Array of typeParamViews
     */
    protected static TypeParamView[] getFormalTypeParams(CallableView callable)
    {
        View clazz = callable.getDeclaringView();
        return clazz.getTypeParams();        
    }

    /**
     * Creates a panel of parameters for a method.
     */
    protected Pane createParameterPanel(String prefix)
    {
        CallableView method = getCallableView();
        Class<?>[] paramClasses = getArgTypes(false);
        String[] paramNames = method.getParamNames();
        String[] paramTypes = method.getParamTypeStrings();

        parameterList = new ParameterList(paramClasses.length, defaultParamValue, f -> this.focusedTextField = f, this::fireOK);
        for (int i = 0; i < paramTypes.length; i++) {
            if (method.isVarArgs() && i == (paramClasses.length - 1)) {
                List<String> historyList = history.getHistory(paramClasses[i].getComponentType());
                parameterList.addVarArgsTypes(paramTypes[i], paramNames == null ? null : paramNames[i]);
                parameterList.setHistory(i, historyList);
            } 
            else {
                parameterList.addNormalParameter(paramTypes[i], paramNames == null ? null : paramNames[i], history.getHistory(paramClasses[i]));
            }
        }

        return createParameterPanel(prefix + "(", ")", parameterList);
    }

    // Fire the OK button.  This is not the same as directly calling doOK,
    // because fireOK only does anything if the OK button is enabled.
    protected void fireOK()
    {
        dialogPane.getOKButton().fire();
    }

    /**
     * Creates a panel of parameters.
     * 
     * @param startString The string prepended before the first parameter. Typically something like ( or <
     * @param endString The string appended after the last parameter. Typically something like ) or >
     * @param parameterList A list containing the components for the parameter panel
     * @return A pane containing the method signature's nodes, where the parameters are input boxes.
     */
    protected Pane createParameterPanel(String startString, String endString, ParameterList parameterList)
    {
        if (parameterList.actualCount() > 0)
        {
            GridPane parameterPanel = new GridPane();
            parameterPanel.getStyleClass().add("grid");

            Label startParenthesis = new Label(startString);
            JavaFXUtil.addStyleClass(startParenthesis,"call-label");
            parameterPanel.add(startParenthesis, 0, 0);

            for (int i = 0; i < parameterList.formalCount(); i++)
            {
                ObservableList<? extends Node> components = parameterList.getNodesForFormal(i);

                if (components.size() == 1)
                { // One component means it is not Varargs.
                    Node child = components.get(0);
                    parameterPanel.add(child, 1, i);
                }
                else
                { // Varargs.
                    GridPane varargsPane = new GridPane();
                    varargsPane.getStyleClass().add("grid");
                    varargsPane.setAlignment(Pos.BASELINE_LEFT);
                    GridPane.setValignment(varargsPane, VPos.BASELINE);
                    arrangeVarargsComponents(varargsPane, components);
                    components.addListener((ListChangeListener<Node>) c -> {
                        arrangeVarargsComponents(varargsPane, c.getList());
                        getDialogPane().getScene().getWindow().sizeToScene();
                    });

                    // Second column gets any extra width
                    ColumnConstraints column2 = new ColumnConstraints();
                    column2.setHgrow(Priority.ALWAYS);
                    varargsPane.getColumnConstraints().addAll(new ColumnConstraints(), column2, new ColumnConstraints(), new ColumnConstraints());

                    parameterPanel.add(varargsPane, 1, i);
                }

                Label type = new Label((i == (parameterList.formalCount() - 1)) ? endString : ",");
                JavaFXUtil.addStyleClass(type,"call-label");
                parameterPanel.add(type, 2, i);
            }


            // Second column gets any extra width 
            ColumnConstraints column2 = new ColumnConstraints();
            column2.setHgrow(Priority.ALWAYS);
            parameterPanel.getColumnConstraints().addAll(new ColumnConstraints(), column2, new ColumnConstraints());
            return parameterPanel;
        }
        else
        {
            // There is no parameter to show, we only construct a label with the start and end strings
            Label emptyPararms = new Label(startString + endString);
            JavaFXUtil.addStyleClass(emptyPararms,"call-label");
            return new Pane(emptyPararms);
        }
    }

    /**
     * Arranges the varags components on a grid pane.
     * Each row has the components of one parameter:
     *      '+' to add another one before,
     *      combobox as a field to enter values and
     *      'x' to delete it,
     * The last row will have an extra component:
     *      '+' to add another one after.
     *
     * Only the second column should be resizable.
     *
     * @param varargsPane The grid pane containing the components
     * @param components  The parameters' components
     */
    private void arrangeVarargsComponents(GridPane varargsPane, ObservableList<? extends Node> components)
    {
        varargsPane.getChildren().clear();
        int lastComponentIndex = components.size() - 1;
        for (int j = 0; j < lastComponentIndex; j++) {
            varargsPane.add(components.get(j), j % 3, j / 3);
        }
        varargsPane.add(components.get(lastComponentIndex), 3, (lastComponentIndex - 1) / 3);
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
                && parameterList.actualCount() >= params.length;
        if (hasVarArgs && varArgsExpanded) {
            int totalParams = parameterList.actualCount();
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
            args = new String[parameterList.actualCount()];
            for (int i = 0; i < parameterList.actualCount(); i++) {
                args[i] = parameterList.getActualParameter(i).getEditor().getText();    
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
            for (int i = 0; i < parameterList.actualCount(); i++) {
                String arg = parameterList.getActualParameter(i).getEditor().getText();
                if (arg == null || arg.trim().equals("")) {
                    JavaFXUtil.setPseudoclass("bj-dialog-error", true, parameterList.getActualParameter(i).getEditor());
                    return false;
                }
                JavaFXUtil.setPseudoclass("bj-dialog-error", false, parameterList.getActualParameter(i).getEditor());
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
            List<TextField> empties = new ArrayList<>();
            for (int i = 0; i < typeParameterList.actualCount(); i++) {
                TextField field = typeParameterList.getActualParameter(i).getEditor();
                JavaFXUtil.setPseudoclass("bj-dialog-error", false, field);
                String arg = field.getText();
                if (arg == null || arg.trim().equals("")) {
                    oneIsEmpty = true;
                    empties.add(field);
                }
                else {
                    oneIsTypedIn = true;
                }
                if (oneIsEmpty && oneIsTypedIn) {
                    empties.forEach(f -> JavaFXUtil.setPseudoclass("bj-dialog-error", true, field));
                    return false;
                }             
            }
        }
        return true;
    }
    
    /**
     * Build the dialog.
     *
     * @param centerPanel the main pane of this dialog.
     */
    protected void makeDialog(Pane centerPanel)
    {
        VBox dialogPanel = new VBox();
        JavaFXUtil.addStyleClass(dialogPanel, "call-dialog-content");
        descPanel = new VBox();
        errorLabel = JavaFXUtil.withStyleClass(new Label(" "), "dialog-error-label");
        dialogPanel.getChildren().addAll(descPanel, new Separator(), centerPanel, errorLabel);

        dialogPane = new DialogPaneAnimateError(errorLabel, () -> {});
        Config.addDialogStylesheets(dialogPane);
        setDialogPane(dialogPane);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        // add the assumption panel.
        // Note: this must be done *after* the dialog buttons are created
        // because when the assertion panel is created, it updates the GUI
        if (showAssertion)
        {
            // Add the assertion panel (when recording a test)
            assertPanel = new AssertPanel(assertionEvalType, this::setOKEnabled);
            dialogPanel.getChildren().add(assertPanel);
        }

        // The dialog does not get dismissed by OK, only by method call:
        dialogPane.getOKButton().addEventFilter(ActionEvent.ACTION, e -> {
            handleOK();
            e.consume();
        });

        getDialogPane().setContent(dialogPanel);

        setOnHidden(e -> bench.removeObjectBenchListener(this));
    }

    /**
     *  Gets the assertion statement.
     *  Returns null if no assertion is set.
     */
    protected void setAssertionRecord(Invoker invoker)
    {
        if (assertPanel != null && assertPanel.isAssertEnabled())
        {
            invoker.setAssertionStatement(assertPanel.getAssertStatement());
            Package pkg = invoker.getPackage();
            assertPanel.recordAssertion(pkg, () -> Optional.ofNullable(PkgMgrFrame.findFrame(pkg)).map(PkgMgrFrame::getTestIdentifier), invoker.getUniqueIRIdentifier());
        }
    }

    /**
     * setDescription - display a new description in the dialog
     */
    protected void setDescription(Node label)
    {
        descPanel.getChildren().setAll(label);
    }

    /**
     * Insert text into edit field (JComboBox) that has focus.
     */
    public void insertText(String text)
    {
        if (parameterList != null) {
            if (focusedTextField != null) {
                focusedTextField.setText(text);
                // bring to front after insertion
                ((Stage)focusedTextField.getScene().getWindow()).toFront();
                focusedTextField.requestFocus();
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    public void saveCallHistory()
    {
        if (parameterList != null) {
            Class<?>[] paramClasses = getArgTypes(true);
            //First we add all the current items into the historylist
            for (int i = 0; i < parameterList.actualCount(); i++) {
                history.addCall(paramClasses[i], parameterList.getActualParameter(i).getEditor().getText());
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
        String[] typeParams = new String[typeParameterList.actualCount()];
        for (int i = 0; i < typeParameterList.actualCount(); i++) {
            typeParams[i] = typeParameterList.getActualParameter(i).getEditor().getText();
            if (typeParams[i] == null || typeParams[i].equals("")) {
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
     */
    public JavaType[] getArgGenTypes(boolean varArgsExpanded)
    {
        CallableView method = getCallableView();
        boolean raw = targetIsRaw();
        
        // first construct a type parameter map which includes not only
        // type parameters from the declaring class, but also those from this
        // particular call
        Map<String,GenTypeParameter> typeParameterMap = getTargetTypeArgs();
        Map<String,GenTypeParameter> typeMap = new HashMap<>();
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
            int totalParams = parameterList.actualCount();
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
        if (parameterList.actualCount() < params.length) {
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
    
    protected boolean targetIsRaw()
    {
        return false;
    }
    
    protected Map<String,GenTypeParameter> getTargetTypeArgs()
    {
        return Collections.emptyMap();
    }
    
}
