package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;

/**
 * GUITextFieldPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a TextField
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUITextFieldPropertyDialog extends GUIPropertyDialog
{
    private TextField text = new TextField();
    private Checkbox startColumnscbx = new Checkbox();
    private TextField startColumnstf = new TextField();
    private Checkbox isEditablecbx = new Checkbox();
    private GUITextField guitextfield;
    private CheckboxListener checkboxListener = new CheckboxListener();
    
    /**
       * Constructs a GUITextFieldPropertyDialog. It enables the user to edit the properties of a TextField.
       @param f Frame
       @param component The GUITextField to be changed.
       @param componentStr A String that describes the component. "TextField"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUITextFieldPropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
    {
	super(f,(GUIComponent)component,componentStr,structCont);
	guitextfield = (GUITextField)component;

	if(guitextfield.isDefined())
	{
	    startColumnscbx.setState(true);
	    startColumnstf.setText(""+(guitextfield.getColumns()));
	}
	else
	{
	    startColumnscbx.setState(false);
	    startColumnstf.setEditable(false);
	}
	isEditablecbx.setState(guitextfield.isEditable());

	specialPanel.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	GridBagConstraints gbc2 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;


	specialPanel.add(new Label("Predefined size:"), gbc1);
	specialPanel.add(startColumnscbx, gbc2);
	specialPanel.add(new Label("Columns"), gbc1);
	specialPanel.add(startColumnstf, gbc2);
	specialPanel.add(new Label("Editable:"),gbc1);
	specialPanel.add(isEditablecbx, gbc2);
	specialPanel.add(new Label("Label:"), gbc1);
	specialPanel.add(text, gbc2);

	text.setText(guitextfield.getText()); 
	startColumnscbx.addItemListener(checkboxListener);
	setTitle("TextField Properties");

	init();
    }


    /**
      * Modify the component, so that the changes in the component becomes persistent.
      */
    public void modifyComponent()
    {
	guitextfield.setText(text.getText());
	guitextfield.setEditable(isEditablecbx.getState());
	if(startColumnscbx.getState()==true)
	{
	    guitextfield.setColumns(Integer.parseInt(startColumnstf.getText()));
	    guitextfield.setDefined(true);
	}
	else
	{
	    guitextfield.setDefined(false);
	}
	super.modifyComponent();
    }


    
    private class CheckboxListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
	{
            if(startColumnscbx.getState()==true)
	    {
		startColumnstf.setEditable(true);
	    }
	    else
	    {
		startColumnstf.setEditable(false);
	    }
	}
    }
}
