package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUIListPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a List
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIListPropertyDialog extends GUIPropertyDialog
{
    List items = new List();
    private TextField text = new TextField();
    
    private Button addButton = new Button("Add Element");
    private Button insertButton = new Button("Insert Element");
    private Button removeButton = new Button("Remove Element");
    private Checkbox multiple = new Checkbox();
    
    private GUIList list;
    private ButtonListener buttonListener = new ButtonListener();

    /**
       * Constructs a GUIListPropertyDialog. It enables the user to edit the properties of a List.
       @param f Frame
       @param component The GUIList to be changed.
       @param componentStr A String that describes the component. "List"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIListPropertyDialog(Frame f, GUIComponent component,String componentStr,StructureContainer structCont)
    {
        super(f,(GUIComponent)component,componentStr,structCont);
        list = (GUIList)component;
        
        for(int i=0 ; i < list.getItemCount() ; i++)
            items.add(list.getItem(i));
            
        specialPanel.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	GridBagConstraints gbc2 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;

	Panel buttonPanel = new Panel(new GridBagLayout());
        buttonPanel.add(addButton, gbc2);
        buttonPanel.add(insertButton, gbc2);
        buttonPanel.add(removeButton, gbc2);
        buttonPanel.add(text, gbc2);

        specialPanel.add(buttonPanel, gbc1);
        gbc2.fill = GridBagConstraints.BOTH;
        specialPanel.add(items, gbc2);
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        specialPanel.add(new Label("Multi Selection:"), gbc1);
        specialPanel.add(multiple, gbc2);
            
        addButton.addActionListener(buttonListener);
        insertButton.addActionListener(buttonListener);
        removeButton.addActionListener(buttonListener);
        setTitle("List Properties");
        init();
    }

    /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
        list.removeAll();
        for(int i=0 ; i < items.getItemCount() ; i++)
            list.add(items.getItem(i));
        list.setMultipleMode(multiple.getState());
        super.modifyComponent();
    }


    
    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource().equals(addButton))
                items.add(text.getText());
            else if(items.getSelectedIndex()!=-1)
            {
                if(e.getSource().equals(removeButton))
                    items.remove(items.getSelectedIndex());
                else
                    items.add(text.getText(),items.getSelectedIndex());
            }
        }
    }
    
                    
                    
}
