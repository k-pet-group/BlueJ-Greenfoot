package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;


/**
 * MenuListenerDialog.java
 * A Class for showing a Dialog to choose Listeners for a Menu
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class MenuListenerDialog extends Dialog
{
    private Button newButton = new Button("New Listener");
    private Button addButton = new Button("Add Listener");
    private Button removeButton = new Button("Remove Listener");
    private List listenerList = new List();
    private List totalListenerList = new List();
    
    private Button closeButton = new Button("Close");
    private ButtonListener buttonListener = new ButtonListener();
    private ChoiceListener itemListener = new ChoiceListener();
    
    private TextField nametf = new TextField();
    private Label reflbl = new Label();
    private Label status = new Label();
    private Choice typesChoice = new Choice();
    
    private Enumeration typesEnum;
    private ListenerContainer listenerContainer;
    private String componentStr;
    private NewDialog newDialog;
    private Vector typeVector = new Vector();
    
    private Vector listenerVector;
    private GUIMenuLeafComponent component;
    private ListenerPair listenerPair;


    /**
     * Constructs a MenuListenerDialog.
     *
     * @param g	            The GUIBuilderApp application.
     * @param component     The GUIComponent to add/remove listeners to/from.
     * @param componentStr  A String describing the type of the component.
     * @param structCont    The StructureContainer containing the component.
     */
    public MenuListenerDialog(Frame g,GUIMenuLeafComponent component, String componentStr,StructureContainer structureContainer)
    {
        super(g, "Listeners", true);
        this.componentStr = componentStr;
        this.component = component;
        
        typesChoice.setEnabled(false);
        
        listenerContainer = structureContainer.getListenerContainer();
        newDialog = new NewDialog(g);
        
        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);
        
        typesEnum = listenerContainer.getListenerTypes(componentStr);
        String type = new String();
        while(typesEnum.hasMoreElements())
        {
            type = (String)typesEnum.nextElement();
            typesChoice.add(type);
            typeVector.addElement(type);
        }
        updateTotalListenerList(typesChoice.getSelectedItem());
        listenerVector = component.getListenerVector();
        for(int i = 0 ; i < listenerVector.size() ; i++)
            listenerList.add(((ListenerPair)listenerVector.elementAt(i)).name);
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.gridx = 1;
        gbc1.gridy = 3;
        
        gbc1.gridheight = 3;
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridwidth = 3;
        gbc2.gridy = 0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        
        GridBagConstraints gbcButtons = new GridBagConstraints();
        gbcButtons.gridx = 0;
        gbcButtons.gridy = 4;
        GridBagConstraints gbcMiddle = new GridBagConstraints();
        gbcMiddle.gridy = 2;
        gbcMiddle.fill = GridBagConstraints.HORIZONTAL;
        
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridwidth = 2;
        
        add(new Label("All "),gbcLabel);
        add(typesChoice);
        
        gbc2.gridy++;
        
        add(totalListenerList,gbc2);
        add(new Label("Listeners associated:"),gbcMiddle);

        gbc2.gridy = 3;
        add(listenerList,gbc2);
        add(newButton,gbcButtons);
        gbcButtons.gridx++;
        add(addButton,gbcButtons);
        gbcButtons.gridx++;
        add(removeButton,gbcButtons);
        gbcButtons.gridx++;
        gbc2.gridy = 5;
        add(closeButton,gbc2);
        gbc2.gridy++;
        add(status,gbc2);
        newButton.addActionListener(buttonListener);
        addButton.addActionListener(buttonListener);
        removeButton.addActionListener(buttonListener);
        closeButton.addActionListener(buttonListener);
        typesChoice.addItemListener(itemListener);
        totalListenerList.addItemListener(itemListener);
        listenerList.addItemListener(itemListener);
    }

    // update the totallistenerList with all possible Listeners of type
    private void updateTotalListenerList(String type)
    {
        Enumeration tmp = listenerContainer.getListeners(componentStr,type);
        totalListenerList.removeAll();
        while(tmp.hasMoreElements())
        {
            totalListenerList.add((String)tmp.nextElement());
        }
    }


    // update the list containing the currently assigned listeners
    public void updateListenerList()
    {
        String item = new String();
        listenerList.removeAll();
            
        for(int i=0 ; i<listenerVector.size() ; i++)
        {
            listenerPair = (ListenerPair)listenerVector.elementAt(i);
            item = listenerPair.name + ":"+listenerPair.type;
            listenerList.addItem(item);
        }
    }
    
                
                
    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource().equals(addButton))
            {
                if(totalListenerList.getSelectedIndex()==-1) // No LIstener selected
                    status.setText("You must select a listener to add");
                else
                {
                    String type = typesChoice.getSelectedItem();
                    String name = totalListenerList.getSelectedItem();
                    
                    listenerPair = new ListenerPair(name,type);
                    listenerContainer.incReference(componentStr,listenerPair);
                    listenerVector.addElement(listenerPair);
                    updateListenerList();
                }
                                
            }
            else if(e.getSource().equals(closeButton))
            {
                dispose();
            }			
            else if(e.getSource().equals(newButton))
            {
                newDialog.setModal(true);
                newDialog.pack();
                newDialog.show();
            }
            else 		// delete Listener	
            {
                if(listenerList.getSelectedIndex()!=-1) // remove an assigned listener from the component
                {
                    String item = listenerList.getSelectedItem();
                    String type = new String(item.substring(0,item.indexOf("\t")-1));
                    String name = new String(item.substring(item.indexOf(":")+1));
                    totalListenerList.getSelectedItem();
                    listenerContainer.decReference(componentStr,listenerPair);
                    listenerVector.removeElementAt(listenerList.getSelectedIndex());
                    updateListenerList();
                                
                }
                else if(totalListenerList.getSelectedIndex()!=-1) // delete a listener if not used by other components
                {
                    String type = typesChoice.getSelectedItem();
                    String name = totalListenerList.getSelectedItem();
                    listenerPair = new ListenerPair(name,type);
                    
                    boolean deleted = listenerContainer.deleteListener(componentStr,listenerPair);
                    if(deleted)
                    {
                        status.setText("Listener deleted");
                        updateTotalListenerList(typesChoice.getSelectedItem());
                    }
                    else
                        status.setText("Listener used by a component");
                }
                else // no Listener selected
                    status.setText("You must select a listener to remove");
                            
            }
        }
    }



    private class ChoiceListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            if(e.getSource()==typesChoice)
            {
                updateTotalListenerList(typesChoice.getSelectedItem());
            }
            else if(e.getSource()==totalListenerList)
            {
                listenerList.deselect(listenerList.getSelectedIndex());
            }
            else
                totalListenerList.deselect(totalListenerList.getSelectedIndex());
                // only one list selected at any time
        }
    }
    
    private class NewDialog extends Dialog implements ActionListener
    {
        private Button addOkButton = new Button("OK");
        private Button addCancelButton = new Button("Cancel");
        private TextField text = new TextField();
        private Choice newTypesChoice = new Choice();
        
        public NewDialog(Frame f)
        {
            super(f);
            setLayout(new GridLayout(3,2));
            add(new Label("Type:"));
            Enumeration tmp = listenerContainer.getListenerTypes(componentStr);
            while(tmp.hasMoreElements())
                newTypesChoice.add((String)tmp.nextElement());
            add(newTypesChoice);
            
            add(new Label("Name: "));
            add(text);
            add(addOkButton);
            add(addCancelButton);
            addOkButton.addActionListener(this);
            addCancelButton.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource().equals(addOkButton)) // add a LIstener to the Listener container
            {
                listenerPair = new ListenerPair(text.getText(),newTypesChoice.getSelectedItem());
                listenerContainer.addListener(componentStr,listenerPair);
                listenerVector.addElement(listenerPair);
                listenerContainer.incReference(componentStr,listenerPair);
                updateTotalListenerList(typesChoice.getSelectedItem());
                updateListenerList();
                dispose();
            }
            else
            {
                dispose();
            }
        }
            
    }
        
        
}

