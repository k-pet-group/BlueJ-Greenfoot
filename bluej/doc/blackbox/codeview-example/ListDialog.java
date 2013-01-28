/*
 * Blackbox Code Viewer sample
 * Copyright (c) 2012, Neil Brown
 */

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class ListDialog<T>
{
    private ArrayList<T> choices;
    
    private JDialog dlg;

    private boolean okPressed;

    private T selection;

    public ListDialog(String prompt, ArrayList<T> choices)
    {
        this.choices = choices;
        this.okPressed = false;
        
        dlg = new JDialog();
        dlg.setModal(true);
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        content.add(new JLabel(prompt));
        final JList list = new JList(choices.toArray());
        content.add(list);
        final JButton button = new JButton(new AbstractAction("Ok") {
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("Button pressed");
                okPressed = true;
                dlg.setVisible(false);
            }
        });
        content.add(button);
        button.setEnabled(false);
        
        list.addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                selection = (T)list.getSelectedValue();
                button.setEnabled(selection != null);
            }
        });
        
        dlg.setContentPane(content);
        dlg.pack();
    }
    
    public boolean showDialog()
    {
        System.out.println("Showing dialog");
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        return okPressed;
    }
    
    public T getSelection()
    {
        return selection;
    }
    
    public static <U> U ask(String prompt, ArrayList<U> choices)
    {
        ListDialog<U> dlg = new ListDialog<U>(prompt, choices);
        
        if (dlg.showDialog())
            return dlg.getSelection();
        else
            return null;
    }
}
