package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUILabelPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Label
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUILabelPropertyDialog extends GUIPropertyDialog
{
    private Choice align = new Choice();
    private TextField text = new TextField();

    /**
       * Constructs a GUILabelPropertyDialog. It enables the user to edit the properties of a Label.
       @param f Frame
       @param component The GUILabel to be changed.
       @param componentStr A String that describes the component. "Label"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUILabelPropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
    {
        super(f,component,componentStr,structCont);
        align.add("Left");
        align.add("Center");
        align.add("Right");

        specialPanel.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	GridBagConstraints gbc2 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;

        specialPanel.add(new Label("Alignment"), gbc1);
        specialPanel.add(align, gbc2);
        specialPanel.add(new Label("Label:"), gbc1);
        specialPanel.add(text, gbc2);
        text.setText(((GUILabel)component).getText());
        setTitle("Label Properties");
            
        init();
    }

     /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
        String str = new String(align.getSelectedItem());
        if(str.equals("Left"))
            ((GUILabel)component).setAlignment(Label.LEFT);
        else if(str.equals("Center"))
            ((GUILabel)component).setAlignment(Label.CENTER);
        else if(str.equals("Right"))
            ((GUILabel)component).setAlignment(Label.RIGHT);
        ((GUILabel)component).setText(text.getText());
        super.modifyComponent();
    }
}
