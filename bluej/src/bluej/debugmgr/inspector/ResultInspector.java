/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.debugmgr.inspector;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.ExpressionInformation;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.JavaUtils;
import bluej.utility.javafx.FXFormattedPrintWriter;
import bluej.utility.javafx.JavaFXUtil;
import bluej.views.Comment;
import bluej.views.MethodView;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.StageStyle;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A window that displays a method return value.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.FXPlatform)
public class ResultInspector extends Inspector
{

    // === static variables ===

    protected final static String resultTitle = Config.getString("debugger.inspector.result.title");
    protected final static String returnedString = Config.getString("debugger.inspector.result.returned");

    // === instance variables ===

    protected DebuggerObject obj;
    protected String objName; // name on the object bench

    private ExpressionInformation expressionInformation;
    private JavaType resultType; // static result type
    private VBox contentPane;


    /**
     * Note: 'pkg' may be null if 'ir' is null.
     * 
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to
     * @param ir
     *            the InvokerRecord explaining how we created this result/object
     *            if null, the "get" button is permanently disabled
     * @param info
     *            The expression used to create the object (ie. the method call
     *            information)
     */
    public ResultInspector(DebuggerObject obj, InspectorManager inspectorManager, String name,
            Package pkg, InvokerRecord ir, ExpressionInformation info)
    {
        super(inspectorManager, pkg, ir, StageStyle.DECORATED);

        expressionInformation = info;
        this.obj = obj;
        this.objName = name;

        calcResultType();

        makeFrame();
        update();
    }

    /**
     * Determine the expected static type of the result.
     */
    private void calcResultType()
    {
        GenTypeClass instanceType = expressionInformation.getInstanceType();
        // We know it's a MethodView, as we don't inspect the result of a
        // constructor!
        MethodView methodView = (MethodView) expressionInformation.getMethodView();
        Method m = methodView.getMethod();

        // Find the expected return type
        JavaType methodReturnType = methodView.getGenericReturnType();

        // TODO: infer type of generic parameters based on the actual
        // arguments passed to the method.
        // For now, use the base type of the any generic type parameters
        if (methodReturnType instanceof GenTypeParameter) {
            
            // The return type may contain type parameters. First, get the
            // type parameters of the object:
            Map<String,GenTypeParameter> tparmap;
            if (instanceType != null)
                tparmap = instanceType.mapToSuper(m.getDeclaringClass().getName()).getMap();
            else
                tparmap = new HashMap<String,GenTypeParameter>();
            
            // It's possible the mapping result is a raw type.
            if (tparmap == null) {
                resultType = JavaUtils.getJavaUtils().getRawReturnType(m);
                return;
            }
            
            // Then put in the type parameters from the method itself,
            // if there are any (ie. if the method is a generic method).
            // Tpars from the method override those from the instance.
            List<GenTypeDeclTpar> tpars = JavaUtils.getJavaUtils().getTypeParams(m);
            tparmap.putAll(JavaUtils.TParamsToMap(tpars));
            
            methodReturnType = methodReturnType.mapTparsToTypes(tparmap).getUpperBound();
        }

        resultType = methodReturnType;
    }

    @Override
    protected boolean shouldAutoUpdate()
    {
        return false;
    }

    /**
     * Returns a single string representing the return value.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    protected List<FieldInfo> getListData()
    {
        String fieldString;
        DebuggerField resultField = obj.getField(0);
        if (!resultType.isPrimitive()) {
            DebuggerObject resultObject = resultField.getValueObject(resultType);
            if (!resultObject.isNullObject()) {
                fieldString = resultObject.getGenType().toString(true);
            }
            else {
                fieldString = resultType.toString(true);
            }
        }
        else {
            fieldString = resultField.getType().toString(true);
        }
        
        List<FieldInfo> rlist = new ArrayList<FieldInfo>(1);
        rlist.add(new FieldInfo(fieldString, resultField.getValueString()));
        return rlist;
    }

    /**
     * Build the GUI
     */
    protected void makeFrame()
    {
        setTitle(resultTitle);
        BlueJTheme.setWindowIconFX(this);

        // Create the header
        Pane header = new VBox();
        
        Comment comment = expressionInformation.getComment();
        FXFormattedPrintWriter commentLabelPrintWriter = new FXFormattedPrintWriter();
        comment.print(commentLabelPrintWriter);
        Node commentLabel = commentLabelPrintWriter.getNode();
        header.getChildren().add(commentLabel);
        Label sig = new Label(expressionInformation.getSignature());

        header.getChildren().add(sig);
        JavaFXUtil.addStyleClass(sig, "inspector-header", "inspector-result-header");

        // Create the main part that shows the expression and the result

        BorderPane mainPanel = new BorderPane();
        VBox result = new VBox();
        JavaFXUtil.addStyleClass(result, "inspector-result-details");

        String expressionDisplay = expressionInformation.getExpression();
        final Node expression = new TextFlow(new Text(expressionDisplay + " " + returnedString));
        JavaFXUtil.addStyleClass(expression, "inspector-result-details-header");
        ContextMenu copyPopup = new ContextMenu();
        copyPopup.getItems().add(JavaFXUtil.makeMenuItem(Config.getString("editor.copyLabel"),() ->
            {
                Clipboard.getSystemClipboard().setContent(Collections.singletonMap(DataFormat.PLAIN_TEXT, expressionDisplay));
            }
        , null));
        expression.setOnContextMenuRequested(e -> copyPopup.show(expression, e.getScreenX(), e.getScreenY()));

        result.getChildren().add(expression);
        
        result.getChildren().add(fieldList);

        mainPanel.setCenter(result);

        mainPanel.setRight(createInspectAndGetButtons());
        // create bottom button pane with "Close" button
        BorderPane buttonPanel = new BorderPane();
        Button button = createCloseButton();
        buttonPanel.setRight(button);

        contentPane = new VBox(header, mainPanel, buttonPanel);
        Config.addDialogStylesheets(contentPane);
        JavaFXUtil.addStyleClass(contentPane, "inspector", "inspector-result");

        button.setDefaultButton(true);
        setScene(new Scene(contentPane));
    }

    @Override
    public Region getContent()
    {
        return contentPane;
    }

    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
        DebuggerField field = obj.getInstanceField(0);
        if (field.isReferenceType() && ! field.isNull()) {
            // Don't use the name, since it is meaningless anyway (it is always "result")
            setCurrentObj(field.getValueObject(resultType), null, resultType.toString(false));
            setButtonsEnabled(true, true);
        }
        else {
            setCurrentObj(null, null, null);
            setButtonsEnabled(false, false);
        }
    }

    @Override
    protected void doInspect()
    {
        if (selectedField != null) {
            boolean isPublic = !getButton.isDisable();
            inspectorManager.getInspectorInstance(selectedField, selectedFieldName, pkg, isPublic ? ir : null, this, null);
        }
    }
    
    /**
     * Remove this inspector.
     */
    protected void remove()
    {
        if(inspectorManager != null) {
            inspectorManager.removeInspector(obj);
        }
    }

    /**
     * return a String with the result.
     * 
     * @return The Result value
     */
    public String getResult()
    {
        DebuggerField resultField = obj.getField(0);
        
        String result = resultField.getType() + " " + resultField.getName() + " = " + resultField.getValueString();
        return result;
    }

    protected int getPreferredRows()
    {
        return 2;
    }
    
    protected void doGet()
    {
        if (selectedField != null) {
            GenTypeClass resultClass = resultType.asClass();
            pkg.getEditor().raisePutOnBenchEvent(this, selectedField, resultClass, ir, true, Optional.empty());
        }
    }
}
