package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUIScrollPanePropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a ScrollPane
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIScrollPanePropertyDialog extends GUIPropertyDialog
{
    private GUIScrollPanePropertyDialog thisDialog;
    private Button layout = new Button("Layout");
    private Choice scrollbarChoice = new Choice();
    private GUIScrollPane scrollPane;
    private int scrollbarState = 0;
    private String[] scrollbarsString = { "When needed", "Always", "Never" };
    
    private int[] scrollbarDisplayPolicy = { ScrollPane.SCROLLBARS_AS_NEEDED , ScrollPane.SCROLLBARS_ALWAYS, ScrollPane.SCROLLBARS_NEVER } ;
    
     /**
       * Constructs a GUIScrollPanePropertyDialog. This enables the user to edit the properties of a ScrollPane.
       * Notice, that it is not possible to set the layout of a ScrollPane
       @param f Frame
       @param component The GUIScrollPane to be changed.
       @param componentStr A String that describes the component. "ScrollPane"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIScrollPanePropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
    {
        super(f,(GUIComponent)component,componentStr,structCont);
        thisDialog = this;
        scrollPane = (GUIScrollPane)component;
        
        specialPanel.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	GridBagConstraints gbc2 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;

        specialPanel.add(new Label("Scrollbars:"), gbc1);
        specialPanel.add(scrollbarChoice, gbc2);
        for(int i = 0 ; i < scrollbarsString.length ; i++)
            scrollbarChoice.add(scrollbarsString[i]);
        
        // set the right scrollbar policy
        scrollbarState = scrollPane.getScrollbarDisplayPolicy();
        int i = 0;
        while(scrollbarDisplayPolicy[i]!=scrollbarState)
            i++;
        scrollbarChoice.select(scrollbarDisplayPolicy[i]);
        
        setTitle("ScrollPane Properties");
        init();
    }

    /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
        if(scrollbarState != scrollbarChoice.getSelectedIndex())
        {
            scrollPane.removeAll();
            GUIScrollPane newComp = new GUIScrollPane(scrollbarDisplayPolicy[scrollbarChoice.getSelectedIndex()],null,null,null);

            scrollPane.getComponentDescriptor().cloneComponent(newComp);
            GUIComponent tmp = scrollPane.getChild();
            scrollPane.getContainer().setComponent(newComp);
            if(tmp != null)
                newComp.add(tmp);
        }
        super.modifyComponent();
    }
}
