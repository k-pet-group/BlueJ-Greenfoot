package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.border.Border;

/**
 * @author  Andrew Patterson  
 * @version $Id: AssertPanel.java 1626 2003-02-11 01:46:35Z ajp $
 */
public class AssertPanel extends JPanel
{
    JTabbedPane assertionTabs;
    JPanel standardPanel;
    JPanel freeformPanel;

    public AssertPanel()
    {
        standardPanel = new JPanel();
        { 
            standardPanel.add(new JLabel("result is"));
            JComboBox eq = new JComboBox();
            eq.addItem("equal to");
            eq.addItem("same as");
            eq.addItem("not null");
            standardPanel.add(eq);
            standardPanel.add(new JTextField(20));
        }

        freeformPanel = new JPanel();
        { 
            freeformPanel.setLayout(new BoxLayout(freeformPanel, BoxLayout.Y_AXIS));
            freeformPanel.add(new JLabel("Free form assertions use the identifier 'result' to"));
            freeformPanel.add(new JLabel("refer to rhe method result"));
            freeformPanel.add(new JLabel("assert that"));
            freeformPanel.add(new JTextField(20));
            freeformPanel.add(new JLabel("is true"));
        }

        assertionTabs = new JTabbedPane();
        assertionTabs.addTab("Standard Assertions", null, standardPanel);
//        assertionTabs.addTab("Free Form Assertions", null, freeformPanel);

        add(assertionTabs, BorderLayout.CENTER);

    }
}
