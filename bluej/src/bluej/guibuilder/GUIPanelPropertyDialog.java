package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUIPanelPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Panel
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIPanelPropertyDialog extends GUIPropertyDialog
{
    private GUIPanelPropertyDialog thisDialog;
    private Button layout = new Button("Define");
    private ButtonListener buttonListener = new ButtonListener();
    
     /**
       * Constructs a GUIPanelPropertyDialog. This enables the user to edit the properties of a Panel.
       @param f Frame
       @param component The GUIPanel to be changed.
       @param componentStr A String that describes the component. "Panel"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIPanelPropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
    {
        super(f,(GUIComponent)component,componentStr,structCont);
        thisDialog = this;
        
        specialPanel.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	GridBagConstraints gbc2 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;

        specialPanel.add(new Label("Layoutmanager:"), gbc1);
        specialPanel.add(layout, gbc2);
        
        layout.addActionListener(buttonListener);
        setTitle("Panel Properties");
        init();
    }

    /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
	super.modifyComponent();
    }
    
    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
	    ((GUIPanel)component).getGUILayout().showPropertiesDialog();
	}
    }
    

}
