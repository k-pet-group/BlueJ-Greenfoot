
/**
 *  Write a description of class TestInspector here.
 *
 *@author     (your name)
 *@version    (a version number or a date)
 */
//package bluej.inspector;

import java.awt.*;
import javax.swing.*;
import java.awt.image.*;
import com.sun.jdi.*;
import java.awt.event.*;
import bluej.debugger.*;
import bluej.debugger.jdi.*;
import bluej.Config;

public class ListInspector extends bluej.debugger.Inspector
{
    JList list;
    JButton inspectBtn, getBtn;
    private static String inspectLabel = Config.getString("debugger.objectviewer.inspect");
    private static String getLabel = Config.getString("debugger.objectviewer.get");

    public String[] getInspectedClassnames()
    {
        String[] ic = {
                "java.util.List"};
        return ic;
    }

    public String getInspectorTitle()
    {
        return "List";
    }

    public boolean initialize(bluej.debugger.DebuggerObject objParm)
    {
        boolean initOK = super.initialize(objParm);
        if (!initOK || obj.getObjectReference() == null)
        {
            return false;
        }
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(Config.generalBorder);
        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel classNameLabel = new JLabel("( " + "java.util.List" + " )  " + ((ClassType) obj.getObjectReference().referenceType()).name());
        titlePanel.add(classNameLabel, BorderLayout.CENTER);
        panel.add(titlePanel, BorderLayout.NORTH);
        // Create panel with "inspect" and "get" buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 1));

        inspectBtn = new JButton(inspectLabel);
        inspectBtn.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                {
                    fireInspectEvent(JdiObject.getDebuggerObject(((ObjectReference) list.getSelectedValue())));
                }
            });
        buttonPanel.add(inspectBtn);

        getBtn = new JButton(getLabel);
        getBtn.setEnabled(false);
        //getBtn.addActionListener(this);
        buttonPanel.add(getBtn);

        JPanel buttonFramePanel = new JPanel();
        buttonFramePanel.setLayout(new BorderLayout(0, 0));
        buttonFramePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(buttonFramePanel, BorderLayout.EAST);

        list = new JList();
        java.awt.event.MouseListener mouseListener =
            new java.awt.event.MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    // monitor for double clicks
                    if (e.getClickCount() == 2)
                    {
                        fireInspectEvent(JdiObject.getDebuggerObject(((ObjectReference) list.getSelectedValue())));
                    }
                }
            };
        list.addMouseListener(mouseListener);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);
        refresh();
        return true;
    }

    public void refresh()
    {
        ArrayReference theList = ((ArrayReference) obj.invokeMethod(
                "toArray", "()[Ljava/lang/Object;", new java.util.Vector()));
        Object[] listarray;
        if (theList.length() == 0)
        {
            listarray = new Object[0];
        }
        else
        {
            listarray = theList.getValues().toArray();
        }
        int cur = Math.max(0, list.getSelectedIndex());  // Retain current selection
        list.setListData(listarray);
        if (listarray.length == 0)
        {
            inspectBtn.setEnabled(false);
        }
        else
        {
            inspectBtn.setEnabled(true);
            list.setSelectedIndex(cur);
        }
        return;
    }
}
