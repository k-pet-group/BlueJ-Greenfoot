/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014,2016  Michael Kolling and John Rosenberg
 
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.role.StdClassRole;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A window that displays the static fields in an class.
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 * @version $Id: ClassInspector.java 16047 2016-06-17 14:29:12Z nccb $
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

   

    /**
     * Note: 'pkg' may be null if getEnabled is false.
     *  
     */
    public ClassInspector(DebuggerClass clss, InspectorManager inspectorManager, Package pkg, InvokerRecord ir, final Window parent)
    {
        super(inspectorManager, pkg, ir);

        myClass = clss;

        final ClassInspector insp = this;

        makeFrame();
        update();
        /*
        if (parent instanceof Inspector) {
            DialogManager.tileWindow(insp, parent);
        }
        else {
            DialogManager.centreWindow(insp, parent);
        }
        */
        installListenersForMoveDrag();
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
        header.getChildren().add(new Separator(Orientation.HORIZONTAL));
        
        // Create the main panel (field list, Get/Inspect buttons)

        BorderPane mainPanel = new BorderPane();

        mainPanel.setCenter(fieldList);
        Label lab = new Label("  " + noFieldsMsg);
        fieldList.setPlaceholder(lab);

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
        BorderPane contentPane = new BorderPane();
        contentPane.setTop(header);
        contentPane.setCenter(mainPanel);
        contentPane.setBottom(buttonPanel);

        button.setDefaultButton(true);
        setScene(new Scene(contentPane));
    }

    /**
     * True if this inspector is used to display a method call result.
     */
    protected boolean showingResult()
    {
        return false;
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
        DebuggerField field = myClass.getStaticField(slot);
        if (field.isReferenceType() && ! field.isNull()) {
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

    /**
     * Intialise additional inspector panels.
     */
    protected void initInspectors(JTabbedPane inspTabs)
    {
    // not supported for class inspectors.
    }

    protected int getPreferredRows()
    {
        return 8;
    }
}
