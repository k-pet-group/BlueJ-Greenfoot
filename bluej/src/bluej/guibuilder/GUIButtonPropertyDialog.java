package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUIButtonPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Button
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIButtonPropertyDialog extends GUIPropertyDialog
{
    private TextField text = new TextField();


     /**
       * Constructs a GUIButtonPropertyDialog. It enables the user to edit the properties of a Button.
       @param f Frame
       @param component The GUIButton to be changed.
       @param componentStr A String that describes the component. "Button"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIButtonPropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
    {
        super(f,(GUIComponent)component,componentStr,structCont);

        specialPanel.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	GridBagConstraints gbc2 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;

        specialPanel.add(new Label("Label:"), gbc1);
        specialPanel.add(text, gbc2);
        text.setText(((GUIButton)component).getLabel());
        setTitle("Button Properties");
        init();
    }


     /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
        ((GUIButton)component).setLabel(text.getText());
        super.modifyComponent();
    }
}
