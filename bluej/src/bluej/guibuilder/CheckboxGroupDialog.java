package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;


/**
 * A Dialog for choosing CheckboxGroups.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class CheckboxGroupDialog extends Dialog
{
    private Button newButton = new Button("New Group");
    private Button associateButton = new Button("Associate Group");
    private Button removeButton = new Button("Remove Group");
    private List groupList = new List();

    private Button closeButton = new Button("Close");
    private ButtonListener buttonListener = new ButtonListener();

    private TextField tfName = new TextField();
    private Label status = new Label("Status");

    private NewDialog newDialog;
    private CheckboxGroupContainer checkboxGroupContainer;
    private GUICheckbox component;


    /**
     * Constructs a ListenerDialog.
     *
     * @param g	            The GUIBuilderApp application.
     * @param component     The GUICheckbox to add/remove a CheckBoxGroup to/from.
     * @param structCont    The StructureContainer containing the component.
     */
    public CheckboxGroupDialog(GUIBuilderApp g,GUICheckbox component,StructureContainer structureContainer)
    {
        super(g, true);
        this.component = component;

        checkboxGroupContainer = structureContainer.getCheckboxGroupContainer();
        newDialog = new NewDialog(g);

        setTitle("CheckboxGroups");

        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);

        String tmp2 = component.getGroup();
        if(tmp2.equals(""))
            tfName.setText("None");
        else
            tfName.setText(component.getGroup());
        groupList.addItem("None");
        String tmp;
        Enumeration groups = checkboxGroupContainer.getGroups();
        while(groups.hasMoreElements())
        {
            tmp = (String)groups.nextElement();
            groupList.addItem(tmp);
        }

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

        add(new Label("All Groups"),gbc2);
        gbc2.gridy++;
        add(groupList,gbc2);
        add(new Label("Group associated:"),gbcMiddle);

        add(tfName,gbcMiddle);

        add(newButton,gbcButtons);
        gbcButtons.gridx++;

        add(associateButton,gbcButtons);
        gbcButtons.gridx++;

        add(removeButton,gbcButtons);
        gbcButtons.gridx++;

        gbc2.gridy = 5;

        add(closeButton,gbc2);
        gbc2.gridy++;

        add(status,gbc2);

        newButton.addActionListener(buttonListener);
        associateButton.addActionListener(buttonListener);
        removeButton.addActionListener(buttonListener);
        closeButton.addActionListener(buttonListener);
    }



    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource().equals(associateButton))
            {
                if(groupList.getSelectedIndex() == -1)
                {
                    status.setText("You must select a group");
                }
                else
                {
                    if(!tfName.getText().equals("None"))
                        checkboxGroupContainer.decReference(tfName.getText());
                    String tmp = groupList.getSelectedItem();
                    if(!tmp.equals("None"))
                    {
                        component.setGroup(tmp);
                        checkboxGroupContainer.incReference(tmp);
                    }
                    tfName.setText(tmp);
                }
            }
            else if(e.getSource().equals(closeButton))
            {
/*                if(!tfName.getText().equals("None"))
                    component.setCheckboxGroup(checkboxGroupContainer.getGUICheckboxGroup(tfName.getText()));
                else
                    component.setCheckboxGroup(null);*/
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
                if(groupList.getSelectedIndex()!=-1 && !groupList.getSelectedItem().equals("None"))
                {
                    String item = groupList.getSelectedItem();
                    if(!checkboxGroupContainer.deleteGroup(item))
                        status.setText("The group is used by another checkbox. It is not deleted");
                    else
                        groupList.remove(item);
                }
                else
                    status.setText("You must select a group to remove");

            }
        }

    }



    private class NewDialog extends Dialog implements ActionListener
    {
        private Button addOkButton = new Button("OK");
        private Button addCancelButton = new Button("Cancel");
        private TextField text = new TextField(10);

        public NewDialog(Frame f)
        {
            super(f);
            Panel p1 = new Panel();
            Panel p2 = new Panel();
            p1.add(new Label("Name: "));
            p1.add(text);
            p2.add(addOkButton);
            p2.add(addCancelButton);
            add(p1, "North");
            add(p2, "South");
            addOkButton.addActionListener(this);
            addCancelButton.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource().equals(addOkButton))
            {
                if(!text.getText().equals(""))
                {
                    checkboxGroupContainer.addGroup(text.getText());
                    groupList.addItem(text.getText());
                }
                dispose();
            }
            else
            {
                dispose();
            }
        }

    }


}

