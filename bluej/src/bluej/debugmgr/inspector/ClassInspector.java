/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
import java.awt.EventQueue;
import java.awt.Insets;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;

/**
 * A window that displays the static fields in an class.
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 * @version $Id: ClassInspector.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ClassInspector extends Inspector
{
    // === static variables ===

    protected final static String CLASS_INSPECT_TITLE = Config.getString("debugger.inspector.class.title");
    protected final static String CLASS_NAME_LABEL = Config.getString("debugger.inspector.class.nameLabel");

    protected final static String ENUM_INSPECT_TITLE = Config.getString("debugger.inspector.enum.title");
    protected final static String ENUM_NAME_LABEL = Config.getString("debugger.inspector.enum.nameLabel");

    protected final static String INTERFACE_INSPECT_TITLE = Config.getString("debugger.inspector.interface.title");
    protected final static String INTERFACE_NAME_LABEL = Config.getString("debugger.inspector.interface.nameLabel");

    
    // === instance variables ===

    protected DebuggerClass myClass;

   

    /**
     * Note: 'pkg' may be null if getEnabled is false.
     *  
     */
    public ClassInspector(DebuggerClass clss, InspectorManager inspectorManager, Package pkg, InvokerRecord ir, final JFrame parent)
    {
        super(inspectorManager, pkg, ir);

        myClass = clss;

        final ClassInspector insp = this;
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                makeFrame();
                pack();
                if (parent instanceof Inspector) {
                    DialogManager.tileWindow(insp, parent);
                }
                else {
                    DialogManager.centreWindow(insp, parent);
                }
            }
        });
    }

    /**
     * Build the GUI
     */
    protected void makeFrame()
    {
        String className = JavaNames.stripPrefix(myClass.getName());
        String headerString = null;
        if(myClass.isEnum()) {
            setTitle(ENUM_INSPECT_TITLE);
            headerString = ENUM_NAME_LABEL + " " + className;
        } else if (myClass.isInterface()) {
            setTitle(INTERFACE_INSPECT_TITLE);
            headerString = INTERFACE_NAME_LABEL + " " + className;
        } else {
            setTitle(CLASS_INSPECT_TITLE);
            headerString = CLASS_NAME_LABEL + " " + className;
        }
        
        setBorder(BorderFactory.createCompoundBorder(BlueJTheme.getShadowBorder(), BorderFactory.createEmptyBorder(10, 10,
                10, 10)));

        // Create the header
        JComponent header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));        
        JLabel headerLabel = new JLabel(headerString);

        headerLabel.setAlignmentX(0.5f);
        header.add(headerLabel);
        header.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        header.add(new JSeparator());

        // Create the main panel (field list, Get/Inspect buttons)

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);

        JScrollPane scrollPane = createFieldListScrollPane();
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel inspectAndGetButtons = createInspectAndGetButtons();
        mainPanel.add(inspectAndGetButtons, BorderLayout.EAST);

        Insets insets = BlueJTheme.generalBorderWithStatusBar.getBorderInsets(mainPanel);
        mainPanel.setBorder(new EmptyBorder(insets));

        // create bottom button pane with "Close" button

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

        JPanel buttonPanel;
        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        JButton button = createCloseButton();
        buttonPanel.add(button, BorderLayout.EAST);

        bottomPanel.add(buttonPanel);

        // add the components
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(header, BorderLayout.NORTH);
        contentPane.add(mainPanel, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(button);
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
    protected Object[] getListData()
    {
        return myClass.getStaticFields(true).toArray(new Object[0]);
    }

    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
        if (myClass.staticFieldIsObject(slot)) {
            setCurrentObj(myClass.getStaticFieldObject(slot), myClass.getStaticFieldName(slot), myClass.getStaticFieldType(slot));

            if (myClass.staticFieldIsPublic(slot)) {
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
