package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUIFramePropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Frame
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIFramePropertyDialog extends GUIPropertyDialog
{
    private GUIFramePropertyDialog thisDialog;
    private TextField tfTitle = new TextField(10);
    private Checkbox resizable = new Checkbox("", false);
    private Checkbox menubar = new Checkbox("Menubar", false);

    private Button layout = new Button("Define");
    private Button menuButton = new Button("Edit");

    private ButtonListener buttonListener = new ButtonListener();
    private CheckboxListener checkboxListener = new CheckboxListener();
    

     /**
       * Constructs a GUIFramePropertyDialog. It enables the user to edit the properties of a Frame.
       @param f Frame
       @param component The GUIFrame to be changed.
       @param componentStr A String that describes the component. "Frame"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIFramePropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
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


        specialPanel.add(new Label("Title:"), gbc1);
        specialPanel.add(tfTitle, gbc2);
        specialPanel.add(new Label("Layoutmanager:"), gbc1);
        specialPanel.add(layout, gbc2);
        specialPanel.add(new Label("Resizable:"), gbc1);
        specialPanel.add(resizable, gbc2);
        specialPanel.add(menubar, gbc1);
        specialPanel.add(menuButton,gbc2);
        
        tfTitle.setText(((GUIFrame)component).getTitle());
        if (((GUIFrame)component).isResizable())
            resizable.setState(true);
        if (((GUIFrame)component).getMenuBar()!=null)
            menubar.setState(true);
        else
            menuButton.setEnabled(false);

        layout.addActionListener(buttonListener);
        menuButton.addActionListener(buttonListener);
        menubar.addItemListener(checkboxListener);
            
        setTitle("Frame Properties");
        init();
    }


     /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
	((GUIFrame)component).setResizable(resizable.getState());
        ((GUIFrame)component).setTitle(tfTitle.getText());
        super.modifyComponent();
    }


    
    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
	    if (e.getActionCommand().equals("Define"))
		((GUIFrame)component).getGUILayout().showPropertiesDialog();
	    else if (e.getActionCommand().equals("Edit"))
		((GUIFrame)component).getGUIMenuBar().showPropertiesDialog(thisDialog);
        }
    }


    
    private class CheckboxListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
	    if (e.getItem().equals("Menubar"))
	    {
		if (e.getStateChange()==ItemEvent.DESELECTED)
		{
		    //Confirm deletion!
		    ((GUIFrame)component).removeMenuBar();
		    menuButton.setEnabled(false);
		}
		else
		{
		    ((GUIFrame)component).addMenuBar();
		    menuButton.setEnabled(true);
		    structureContainer.redraw();
		}
	    }
	}
    }
}
