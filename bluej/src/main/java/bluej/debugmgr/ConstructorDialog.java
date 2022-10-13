/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2013,2014,2015,2016,2017,2019  Michael Kolling and John Rosenberg
 
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

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import bluej.Config;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.utility.JavaNames;
import bluej.utility.javafx.FXFormattedPrintWriter;
import bluej.utility.javafx.JavaFXUtil;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.TypeParamView;
import bluej.views.View;
import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.FXPlatform)
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

    private final TextField instanceNameText;
    private final ConstructorView constructor;
    private final Invoker invoker;

    /**
     * MethodDialog constructor.
     * 
     * @param parentFrame  The parent window for the dialog
     * @param ob           The object bench to listen for object selection on
     * @param callHistory  The call history tracker
     * @param initialName  The initial (suggested) instance name
     * @param constructor  The constructor or method being used
     * @param invoker      The object invoked the constructor
     *
     */
    public ConstructorDialog(Window parentFrame, ObjectBenchInterface ob, CallHistory callHistory,
                             String initialName, ConstructorView constructor, Invoker invoker)
    {
        super(parentFrame, ob, "");
        this.invoker = invoker;

        history = callHistory;

        this.constructor = constructor;
        setTitle(wCreateTitle);

        Label instName = new Label(sNameOfInstance);
        instName.setAlignment(Pos.BASELINE_LEFT);
        instanceNameText = new TextField(initialName);
        instanceNameText.setPrefColumnCount(16);
        instanceNameText.setAlignment(Pos.BASELINE_LEFT);
        instName.setLabelFor(instanceNameText);
        // treat 'return' in text field as OK
        instanceNameText.setOnAction(e1 -> fireOK());

        Pane tmpPanel = new VBox();
        JavaFXUtil.addStyleClass(tmpPanel, "constructor-dialog-fields");

        if(!Config.isGreenfoot()) {
            HBox hBox = new HBox(instName, instanceNameText);
            hBox.setAlignment(Pos.BASELINE_LEFT);
            JavaFXUtil.addStyleClass(hBox, "constructor-instance-name-row");
            tmpPanel.getChildren().add(hBox);
        }

        View clazz = this.constructor.getDeclaringView();
        if (clazz.isGeneric()) {
            String name;
            if(getFormalTypeParams(this.constructor).length > 1) {
                name = sTypeParameters + "  ";
            } else {
                name = sTypeParameter + "  ";
            }

            Pane typeParameterPanel = createTypeParameterPanel(name);
            tmpPanel.getChildren().add(typeParameterPanel);
        }

        if (this.constructor.hasParameters()) {
            Pane parameterPanel = createParameterPanel("new " + constructor.getClassName());
            tmpPanel.getChildren().add(parameterPanel);
        }

        makeDialog(tmpPanel);
        instanceNameText.setText(initialName);
        FXFormattedPrintWriter writer = new FXFormattedPrintWriter();
        this.constructor.print(writer);
        setDescription(writer.getNode());

        setOnShown(e -> {
            if (typeParameterList != null)
            {
                typeParameterList.getActualParameter(0).getEditor().requestFocus();
            }
            else if (parameterList != null)
            {
                parameterList.getActualParameter(0).getEditor().requestFocus();
            }
            else
            {
                instanceNameText.requestFocus();
            }
        });
    }

    /**
     * Creates a panel of type parameters for a new object
     */
    private Pane createTypeParameterPanel(String prefix)
    {
        TypeParamView formalTypeParams[] = getFormalTypeParams(constructor);

        typeParameterList = new ParameterList(formalTypeParams.length, defaultParamValue, f -> this.focusedTextField = f, this::fireOK);
        for (TypeParamView formalTypeParam : formalTypeParams)
        {
            typeParameterList.addNormalParameter(formalTypeParam.toString(), null, history.getHistory(formalTypeParam));
        }
        String startString = prefix + "<";
        String endString = ">";
        ParameterList superParamList = typeParameterList;
        return createParameterPanel(startString, endString, superParamList);
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     * Collects arguments and calls watcher objects (Invoker).
     */
    public void handleOK()
    {
        String newInstanceName = getNewInstanceName();
        if (!JavaNames.isIdentifier(newInstanceName)) {
            setErrorMessage(illegalNameMsg);
            JavaFXUtil.setPseudoclass("bj-dialog-error", true, instanceNameText);
            return;
        }
        boolean alreadyOnBench = bench != null && bench.hasObject(newInstanceName);
        if (alreadyOnBench)
        {
            setErrorMessage(duplicateNameMsg);
            JavaFXUtil.setPseudoclass("bj-dialog-error", true, instanceNameText);
            return;
        }
        JavaFXUtil.setPseudoclass("bj-dialog-error", false, instanceNameText);

        if (!parameterFieldsOk())
        {
            setErrorMessage(emptyFieldMsg);
        }
        else if (!typeParameterFieldsOk())
        {
            setErrorMessage(emptyTypeFieldMsg);
        }
        else
        {
            setWaitCursor(true);
            invoker.callDialogOK();
        }
    }
    
    /**
     * getNewInstanceName - get the contents of the instance name field.
     */
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
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
