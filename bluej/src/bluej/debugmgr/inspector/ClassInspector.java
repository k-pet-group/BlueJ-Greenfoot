/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.JavaNames;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A window that displays the static fields in an class.
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 */
@OnThread(Tag.FXPlatform)
public class ClassInspector extends Inspector
{
    // === static variables ===

    protected final static String CLASS_INSPECT_TITLE = Config.getString("debugger.inspector.class.title");
    protected final static String CLASS_NAME_LABEL = Config.getString("debugger.inspector.class.nameLabel");

    protected final static String ENUM_INSPECT_TITLE = Config.getString("debugger.inspector.enum.title");
    protected final static String ENUM_NAME_LABEL = Config.getString("debugger.inspector.enum.nameLabel");

    protected final static String INTERFACE_INSPECT_TITLE = Config.getString("debugger.inspector.interface.title");
    protected final static String INTERFACE_NAME_LABEL = Config.getString("debugger.inspector.interface.nameLabel");

    protected final static String noFieldsMsg = Config.getString("debugger.inspector.class.noFields");
    protected final static String numFields = Config.getString("debugger.inspector.numFields");
    
    // === instance variables ===

    protected DebuggerClass myClass;
    private VBox contentPane;


    /**
     * Note: 'pkg' may be null if getEnabled is false.
     *  
     */
    public ClassInspector(DebuggerClass clss, InspectorManager inspectorManager, Package pkg, InvokerRecord ir, final Window parent)
    {
        super(inspectorManager, pkg, ir, StageStyle.TRANSPARENT);

        myClass = clss;

        makeFrame();
        update();

        setMinWidth(500);
        setMinHeight(260);

        /*
        if (parent instanceof Inspector) {
            DialogManager.tileWindow(insp, parent);
        }
        else {
            DialogManager.centreWindow(insp, parent);
        }
        */
        installListenersForMoveDrag(8.0);
    }

    /**
     * Build the GUI
     */
    protected void makeFrame()
    {
        String className = JavaNames.stripPrefix(myClass.getName());
        String headerString = null;
        String suffix = " " + numFields + " " + getListData().size();
        if(myClass.isEnum()) {
            setTitle(ENUM_INSPECT_TITLE + " " + className + suffix);
            headerString = ENUM_NAME_LABEL + " " + className;
        } else if (myClass.isInterface()) {
            setTitle(INTERFACE_INSPECT_TITLE + " " + className + suffix);
            headerString = INTERFACE_NAME_LABEL + " " + className;
        } else {
            setTitle(CLASS_INSPECT_TITLE + " " + className + suffix);
            headerString = CLASS_NAME_LABEL + " " + className;
        }
        
        // Create the header
        Pane header = new VBox();
        Label headerLabel = new Label(headerString);
        
        header.getChildren().add(headerLabel);
        JavaFXUtil.addStyleClass(header, "inspector-header", "inspector-class-header");
        
        // Create the main panel (field list, Get/Inspect buttons)

        BorderPane mainPanel = new BorderPane();

        mainPanel.setCenter(fieldList);
        fieldList.setPlaceHolderText("  " + noFieldsMsg);

        mainPanel.setRight(createInspectAndGetButtons());

        // create bottom button pane with "Close" button
        BorderPane buttonPanel = new BorderPane();
        Button button = createCloseButton();
        buttonPanel.setRight(button);

        // add the components
        /*
        JPanel contentPane = new JPanel() {
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D)g;
                g2d.setPaint(new StdClassRole().getBackgroundPaint(getWidth(), getHeight()));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.BLACK);
                g2d.drawRect(0, 0, getWidth()-1, getHeight()-1);
            }
        };
        */
        contentPane = new VBox();
        contentPane.getChildren().addAll(header, mainPanel, buttonPanel);
        VBox.setVgrow(mainPanel, Priority.ALWAYS);
        JavaFXUtil.addStyleClass(contentPane, "inspector", "inspector-class");

        button.setDefaultButton(true);
        BorderPane root = new BorderPane(contentPane);
        root.setBackground(null);
        Scene scene = new Scene(root);
        scene.setFill(null);
        setScene(scene);
    }

    /**
     * True if this inspector is used to display a method call result.
     */
    protected boolean showingResult()
    {
        return false;
    }

    @Override
    protected boolean shouldAutoUpdate()
    {
        return Config.isGreenfoot();
    }

    /**
     * True if this inspector is used to display a method call result.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    protected List<FieldInfo> getListData()
    {
        List<DebuggerField> fields = myClass.getStaticFields();
        List<FieldInfo> fieldInfos = new ArrayList<FieldInfo>(fields.size());
        for (DebuggerField field : fields) {
            String desc = Inspector.fieldToString(field);
            String value = field.getValueString();
            fieldInfos.add(new FieldInfo(desc, value));
        }
        return fieldInfos;
    }

    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
        DebuggerField field = slot == -1 ? null : myClass.getStaticField(slot);
        if (field != null && field.isReferenceType() && ! field.isNull()) {
            setCurrentObj(field.getValueObject(null), field.getName(), field.getType().toString());

            if (Modifier.isPublic(field.getModifiers())) {
                setButtonsEnabled(true, true);
            }
            else {
                setButtonsEnabled(true, false);
            }
        }
        else {
            setCurrentObj(null, null, null);
            setButtonsEnabled(false, false);
        }
    }

    /**
     * Show the inspector for the class of an object.
     */
    protected void showClass()
    {
    // nothing to do here - this is the class already
    }

    /**
     * We are about to inspect an object - prepare.
     */
    protected void prepareInspection()
    {
    // nothing to do here
    }
    
    /**
     * Remove this inspector.
     */
    protected void remove()
    {
        if(inspectorManager != null) {
            inspectorManager.removeInspector(myClass);
        }
    }

    protected int getPreferredRows()
    {
        return 8;
    }

    @Override
    public Region getContent()
    {
        return contentPane;
    }
}
