package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;

/**
 * GUITextAreaPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a TextArea
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUITextAreaPropertyDialog extends GUIPropertyDialog
{
    private TextField text = new TextField();
    private Checkbox startColumnscbx = new Checkbox();
    private TextField startColumnstf = new TextField(3);
    private TextField startRowstf = new TextField(3);
    private Choice scrollbarsChoice = new Choice();
    private String[] scrollbarsString = { "Both","Vertical","Horizontal","None"};
    private int[] scrollbarVisibility = {TextArea.SCROLLBARS_BOTH,
		TextArea.SCROLLBARS_VERTICAL_ONLY,
		TextArea.SCROLLBARS_HORIZONTAL_ONLY,
		TextArea.SCROLLBARS_NONE};    
    
    private Checkbox isEditablecbx = new Checkbox();
    private GUITextArea guitextarea;
    private CheckboxListener checkboxListener = new CheckboxListener();

    
     /**
       * Constructs a GUITextAreaPropertyDialog. This enables the user to edit the properties of a TextArea.
       @param f Frame
       @param component The GUITextArea to be changed.
       @param componentStr A String that describes the component. "TextArea"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUITextAreaPropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
    {
        super(f,component,componentStr,structCont);

        guitextarea = (GUITextArea)component;
        for(int i = 0 ; i < scrollbarsString.length ; i++)
            scrollbarsChoice.add(scrollbarsString[i]);
        if(guitextarea.isDefined())
        {
            startColumnscbx.setState(true);
            startColumnstf.setText(""+guitextarea.getColumns());
            startRowstf.setText(""+guitextarea.getRows());
                
        }
        else
        {
            startColumnscbx.setState(false);
            startColumnstf.setEditable(false);
            startRowstf.setEditable(false);
        }
        isEditablecbx.setState(guitextarea.isEditable());

        specialPanel.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	GridBagConstraints gbc2 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;

        specialPanel.add(new Label("Predefined size:"), gbc1);
        specialPanel.add(startColumnscbx,gbc2);

	Panel colPanel = new Panel(new GridBagLayout());
	colPanel.add(new Label("Columns:"), gbc1);
        colPanel.add(startColumnstf, gbc2);

        specialPanel.add(new Label("Size:"), gbc1);
	specialPanel.add(colPanel, gbc2);

	Panel rowPanel = new Panel(new GridBagLayout());
	rowPanel.add(new Label("Rows:"), gbc1);
        rowPanel.add(startRowstf, gbc2);

        specialPanel.add(new Canvas(), gbc1);
	specialPanel.add(rowPanel, gbc2);

        specialPanel.add(new Label("Editable:"), gbc1);
        specialPanel.add(isEditablecbx, gbc2);
        specialPanel.add(new Label("Label:"), gbc1);
        specialPanel.add(text, gbc2);
        specialPanel.add(new Label("Scrollbars:"), gbc1);
        specialPanel.add(scrollbarsChoice, gbc2);

        int scrollState = guitextarea.getScrollbarVisibility();
        int i;
	for (i=0; scrollbarVisibility[i]!=scrollState; i++)
            ;
        scrollbarsChoice.select(i);
        
        text.setText(guitextarea.getText()); 
        startColumnscbx.addItemListener(checkboxListener);
        setTitle("TextArea Properties");
        
        init();
    }


    /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
	super.modifyComponent();

	guitextarea.setText(text.getText());
	guitextarea.setEditable(isEditablecbx.getState());

	if(startColumnscbx.getState()==true)
	{
	    guitextarea.setColumns(Integer.parseInt(startColumnstf.getText()));
	    guitextarea.setRows(Integer.parseInt(startRowstf.getText()));
	    guitextarea.setDefined(true);
	}
	else
	{
	    guitextarea.setDefined(false);
	}


	int index = scrollbarsChoice.getSelectedIndex();

	if (guitextarea.getScrollbarVisibility() != scrollbarVisibility[index])
	{
	    GUITextArea newComp = new GUITextArea(guitextarea.getText(), guitextarea.getRows(), guitextarea.getColumns(), scrollbarVisibility[index], null, null, null);
	    guitextarea.getComponentDescriptor().cloneComponent(newComp);
	    guitextarea.getContainer().setComponent(newComp);
	}
    }


    
    private class CheckboxListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            if(startColumnscbx.getState()==true)
            {
                startColumnstf.setEditable(true);
                startRowstf.setEditable(true);
            }
            else
            {
                startColumnstf.setEditable(false);
                startRowstf.setEditable(false);
            }
        }
    }
}
