package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUIChoicePropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Choice
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIChoicePropertyDialog extends GUIPropertyDialog
{
    private List items = new List();
    private TextField text = new TextField();
    private Button addButton = new Button("Add Element");
    private Button insertButton = new Button("Insert Element");
    private Button removeButton = new Button("Remove Element");
    private GUIChoice list;
    private ButtonListener buttonListener = new ButtonListener();
    private String componentStr = new String();


     /**
       * Constructs a GUIChoicePropertyDialog. It enables the user to edit the properties of a Choice.
       @param f Frame
       @param component The GUIChoice to be changed.
       @param componentStr A String that describes the component. "Choice"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIChoicePropertyDialog(Frame f, GUIComponent component,String componentStr,StructureContainer structCont)
    {
        super(f,(GUIComponent)component,componentStr,structCont);
        list = (GUIChoice)component;
        this.componentStr = componentStr;
        
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
        
        addButton.addActionListener(buttonListener);
        insertButton.addActionListener(buttonListener);
        removeButton.addActionListener(buttonListener);
        setTitle("Choice Properties");
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
        super.modifyComponent();
    }


    
    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource().equals(addButton))
            {
                items.add(text.getText());
            }
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
