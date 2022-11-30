/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016  Michael Kolling and John Rosenberg
 
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
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.GrowableList;
import bluej.utility.javafx.HangingFlowPane;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Class that holds the components for  a list of parameters. 
 * That is: the actual parameter component and the formal type of the parameter.
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
@OnThread(Tag.FXPlatform)
public class ParameterList
{
    /**
     * The combo boxes for all non-vararg actual parameters.
     */
    private List<ComboBox<String>> parameters;
    /**
     * The actual varargs list at the end of the parameters.  Null if there
     * are no varargs.
     */
    private GrowableList<ComboBox<String>> varArgsList;
    /** The varargs history.  Only relevant if varArgsList != null */
    private ObservableList<String> varArgsHistory = FXCollections.observableArrayList();
    /**
     * The default parameter value
     */
    private String defaultParamValue;
    private final FXConsumer<TextField> setLastFocused;
    private final FXPlatformRunnable fireOK;

    public ParameterList(int initialSize, String defaultParamValue, FXConsumer<TextField> setLastFocused, FXPlatformRunnable fireOK) 
    {            
        parameters = new ArrayList<>(initialSize);
        this.defaultParamValue = defaultParamValue;
        this.setLastFocused = setLastFocused;
        this.fireOK = fireOK;
    }

    public ComboBox<String> getActualParameter(int index)
    {
        if (varArgsList != null && index >= parameters.size())
        {
            int varArgIndex = index - parameters.size();
            return varArgsList.getItem(varArgIndex);
        }
        else
        {
            return parameters.get(index);
        }
    }
    
    public ObservableList<? extends Node> getNodesForFormal(int index)
    {
        if (varArgsList != null && index == parameters.size())
            return varArgsList.getNodes();
        else
            return FXCollections.observableArrayList(parameters.get(index));
    }

    public void addNormalParameter(String paramType, String paramName, List<String> history)
    {
        String paramString = paramType + (paramName == null ? "" : " " + paramName);
        parameters.add(createComboBox(paramString, history == null ? null : FXCollections.observableArrayList(history)));
    }

    public int formalCount()
    {
        return parameters.size() + (varArgsList != null ? 1 : 0);
    }
    
    public int actualCount()
    {
        return parameters.size() + (varArgsList != null ? varArgsList.size() : 0);
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
        else if (varArgsList != null && i >= parameters.size()) {
            varArgsHistory.setAll(historyList);
        }
        else {
            getActualParameter(i).getItems().setAll(historyList);
            getActualParameter(i).getItems().add(0, defaultParamValue);
        }
    }

    protected ComboBox<String> createComboBox(String prompt, ObservableList<String> history)
    {
        if(history == null) {
            history = FXCollections.observableArrayList();
        }
        history.add(0, defaultParamValue);
        ComboBox<String> component = new ComboBox<>(history);
        component.setEditable(true);
        component.setPromptText(prompt);
        component.getEditor().setText(defaultParamValue);
        //TODO can't get this working...
        //if (!defaultParamValue.isEmpty())
        //    JavaFXUtil.addSelfRemovingListener(component.getEditor().focusedProperty(), b -> component.getEditor().selectAll());
        HangingFlowPane.setBreakBefore(component, true);
        // Can't do this from CSS:
        HangingFlowPane.setMargin(component, new Insets(0, 8, 0, 8));
        JavaFXUtil.addChangeListener(component.focusedProperty(), focused -> {
            if (focused.booleanValue())
                setLastFocused.accept(component.getEditor());
        });

        // treat 'return' in text field as OK
        component.getEditor().setOnAction(e -> fireOK.run());
        return component;
    }

    public void addVarArgsTypes(String paramType, String paramName)
    {
        String paramString = paramType + (paramName == null ? "" : " " + paramName);
        varArgsList = new GrowableList(() -> createComboBox(paramString, varArgsHistory));
    }
}
