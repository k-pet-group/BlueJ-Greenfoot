package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUIDialogPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Dialog
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIDialogPropertyDialog extends GUIPropertyDialog
{
    private GUIDialogPropertyDialog thisDialog;
    private TextField tfTitle = new TextField(10);
    private Checkbox resizable = new Checkbox("", false);
    private Checkbox modal = new Checkbox("",false);
    private Button layout = new Button("Define");
    private ButtonListener buttonListener = new ButtonListener();


     /**
       * Constructs a GUIDialogPropertyDialog. It enables the user to edit the properties of a Dialog.
       @param f Frame
       @param component The Dialog to be changed.
       @param componentStr A String that describes the component. "Dialog"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIDialogPropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
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
        specialPanel.add(new Label("Modal:"), gbc1);
        specialPanel.add(modal, gbc2);

//        tfTitle.setText(((GUIDialog)component).getTitle());
//        if (((GUIDialog)component).isResizable())
            resizable.setState(true);
        if(((GUIDialog)component).isGUIModal())
            modal.setState(true);

        layout.addActionListener(buttonListener);
	setTitle("Dialog Properties");
        init();
    }


    /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
//	((GUIDialog)component).setResizable(resizable.getState());
        ((GUIDialog)component).setGUIModal(modal.getState());
//        ((GUIDialog)component).setTitle(tfTitle.getText());
        super.modifyComponent();
    }



    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
	    ((GUIDialog)component).getGUILayout().showPropertiesDialog();
        }
    }
}
