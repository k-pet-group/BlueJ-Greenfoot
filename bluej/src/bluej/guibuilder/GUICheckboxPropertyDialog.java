package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUICheckboxPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Checkbox
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUICheckboxPropertyDialog extends GUIPropertyDialog
{
    private GUIBuilderApp app;
    private GUICheckbox component;
    private StructureContainer structCont;
    private TextField text = new TextField();
    private Button groupButton = new Button("Define");
    private ButtonHandler buttonHandler = new ButtonHandler();


    /**
       * Constructs a GUICheckboxPropertyDialog. It enables the user to edit the properties of a Checkbox.
       @param f Frame
       @param component The GUICheckbox to be changed.
       @param componentStr A String that describes the component. "Checkbox"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUICheckboxPropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
    {
	super(f,(GUIComponent)component,componentStr,structCont);

	app = (GUIBuilderApp)f;
	this.component = (GUICheckbox)component;
	this.structCont = structCont;

	specialPanel.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	GridBagConstraints gbc2 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;

	specialPanel.add(new Label("Label:"),gbc1);
	specialPanel.add(text, gbc2);
	text.setText(((GUICheckbox)component).getLabel());
	specialPanel.add(new Label("CheckboxGroup:"), gbc1);
	groupButton.addActionListener(buttonHandler);
	specialPanel.add(groupButton, gbc2);

	setTitle("Checkbox Properties");
	init();
    }



    /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
	((GUICheckbox)component).setLabel(text.getText());
	super.modifyComponent();
    }



    private class ButtonHandler implements ActionListener
    {
	public void actionPerformed (ActionEvent e)
	{
	    CheckboxGroupDialog dialog = new CheckboxGroupDialog(app, component, structCont);
	    dialog.pack();
	    dialog.show();
	}
    }
}
