package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;
import javablue.GUIGraphics.IntegerField;


/**
 * GUIScrollbarPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Scrollbar
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIScrollbarPropertyDialog extends GUIPropertyDialog
{
    private Choice orientation = new Choice();
    private IntegerField value = new IntegerField();
    private IntegerField visible = new IntegerField();
    private IntegerField minimum = new IntegerField();
    private IntegerField maximum = new IntegerField();
    private IntegerField blockincrement = new IntegerField();
    private IntegerField unitincrement = new IntegerField();
    private GUIScrollbar scrollbar;


    /**
       * Constructs a GUIScrollbarPropertyDialog. This enables the user to edit the proerties of a Scrollbar.
       @param f Frame
       @param component The GUIScrollbar to be changed.
       @param componentStr A String that describes the component. "Scrollbar"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIScrollbarPropertyDialog(Frame f, GUIComponent component,String componentStr,StructureContainer structCont)
    {
        super(f,(GUIComponent)component,componentStr,structCont);

        scrollbar = (GUIScrollbar)component;
        
        orientation.add("Horizontal");
        orientation.add("Vertical");
        if(scrollbar.getOrientation()==Scrollbar.VERTICAL)
            orientation.select("Vertical");
        else
            orientation.select("Horizontal");
            
        specialPanel.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	GridBagConstraints gbc2 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;

        specialPanel.add(new Label("Orientation:"), gbc1);
        specialPanel.add(orientation, gbc2);
        specialPanel.add(new Label("Value"), gbc1);
        specialPanel.add(value, gbc2);
        specialPanel.add(new Label("Visible"), gbc1);
        specialPanel.add(visible, gbc2);
        specialPanel.add(new Label("Minimum"), gbc1);
        specialPanel.add(minimum, gbc2);
        specialPanel.add(new Label("Maximum"), gbc1);
        specialPanel.add(maximum, gbc2);
        specialPanel.add(new Label("Blockincrement"), gbc1);
        specialPanel.add(blockincrement, gbc2);
        specialPanel.add(new Label("Unitincrement"), gbc1);
        specialPanel.add(unitincrement, gbc2);
            
        setTitle("Scrollbar Properties");

        value.setValue(scrollbar.getValue());
        visible.setValue(scrollbar.getVisibleAmount());
        minimum.setValue(scrollbar.getMinimum());
        maximum.setValue(scrollbar.getMaximum());
        blockincrement.setValue(scrollbar.getBlockIncrement());
        unitincrement.setValue(scrollbar.getUnitIncrement());
        init();
    }


    /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
        if(orientation.getSelectedItem().equals("Horizontal"))
            scrollbar.setOrientation(Scrollbar.HORIZONTAL);
        else
            scrollbar.setOrientation(Scrollbar.VERTICAL);
        scrollbar.setValues(value.getValue(),visible.getValue(),minimum.getValue(),maximum.getValue());
        scrollbar.setBlockIncrement(blockincrement.getValue());
        scrollbar.setUnitIncrement(unitincrement.getValue());
        
        super.modifyComponent();
    }

    

}
