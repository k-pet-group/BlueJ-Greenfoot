package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;
import bluej.guibuilder.graphics.Separator;


/**
 * GUIMenuPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Menu
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIMenuPropertyDialog extends Dialog
{
    private GUIMenuComponent component;
    private StructureContainer structCont;
    private Frame frame;

    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constrLeft = new GridBagConstraints();
    private GridBagConstraints constrRight = new GridBagConstraints();

    private TextField nameField = new TextField(15);
    private TextField labelField = new TextField(15);
    private Choice shortcutChoice = new Choice();
    private Checkbox shiftCheckbox = new Checkbox("Shift");
    private Choice fontChoice = new Choice();
    private Choice sizeChoice = new Choice();
    private Choice faceChoice = new Choice();
    private TextField sampleField = new TextField("ABCabc123");
    private Button listenerButton = new Button("Define");
    private Button okButton = new Button("OK");
    private Button cancelButton = new Button("Cancel");
    private Font componentFont;
    private ButtonListener buttonListener = new ButtonListener();
    private FontListener fontListener = new FontListener();

     /**
       * Constructs a GUIMenuPropertyDialog. It is used to edit the menu properties int a frame.
       @param f Frame
       @param parentWinodw The parent Window of the menu.
       @param title The title of this dialog.
       @param component The GUIMenuComponent to be changed.
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIMenuPropertyDialog (Frame f, Window parentWindow, String title, GUIMenuComponent component, StructureContainer structCont)
    {
	super (f, title, true);
	this.frame = f;
	this.component = component;
	this.structCont = structCont;

	addWindowListener(new WindowAdapter() { public void 
		windowClosing(WindowEvent e) { onCancel(); } } );

	Panel north = new Panel(gridbag);

	constrLeft.fill = GridBagConstraints.HORIZONTAL;
	constrRight.fill = GridBagConstraints.HORIZONTAL;
	constrLeft.anchor = GridBagConstraints.NORTHWEST;
	constrRight.anchor = GridBagConstraints.NORTHWEST;
	constrLeft.weightx = 0;
	constrRight.weightx = 1;
	constrRight.gridwidth = GridBagConstraints.REMAINDER;

	nameField.setText(((MenuItem)component).getName());

	labelField.setText(((MenuItem)component).getLabel());

	shortcutChoice.add("None");
	for (int i=0; i<GUIMenuShortcuts.shortcuts.length; i++)
	    shortcutChoice.add(GUIMenuShortcuts.shortcuts[i].label);
	componentFont = ((MenuItem)component).getFont();
        String[] fonts = java.awt.Toolkit.getDefaultToolkit().getFontList();
        for(int i = 0 ; i < fonts.length ; i++)
            fontChoice.add(fonts[i]);
        fontChoice.select (componentFont.getName());
	fontChoice.addItemListener (fontListener);

        int[] sizes = { 8,10,12,14,16,18,24,36 };
        int size = componentFont.getSize();
        int i = 0; 
        while(sizes[i]<size && i<sizes.length)
        {
            sizeChoice.add(""+sizes[i++]);
        }
        sizeChoice.add(String.valueOf(size));
        if(sizes[i]==size)
            i++;
        for(; i < sizes.length ; i++)
            sizeChoice.add(""+sizes[i]);
        sizeChoice.select(String.valueOf(size));
	sizeChoice.addItemListener (fontListener);

	String[] faces = {"Plain", "Bold", "Italic", "Bold & Italic"};
	for (i=0; i<faces.length; i++)
	    faceChoice.add(faces[i]);
        if(componentFont.isBold() && componentFont.isItalic())
            faceChoice.select(faces[3]);
	else if(componentFont.isBold())
            faceChoice.select(faces[1]);
        else if(componentFont.isItalic())
            faceChoice.select(faces[2]);
	else
            faceChoice.select(faces[0]);
	faceChoice.addItemListener (fontListener);

        sampleField.setFont(componentFont);

	listenerButton.addActionListener (buttonListener);

	addPair(north, new Label ("Name:"), nameField);
	addPair(north, new Label ("Label:"), labelField);
	if (component instanceof GUIMenuLeafComponent)
	{
	    addPair(north, new Label ("Shortcut:"), shortcutChoice);
	    addPair(north, new Label ("Qualifier:"), shiftCheckbox);
	}
	addPair(north, new Label ("Font:"), fontChoice);
	addPair(north, new Label ("Size:"), sizeChoice);
	addPair(north, new Label ("Face:"), faceChoice);
	addPair(north, new Label ("Sample:"), sampleField);
	if (component instanceof GUIMenuLeafComponent)
	    addPair(north, new Label ("Listeners:"), listenerButton);

	Panel south = new Panel ();
	okButton.addActionListener(buttonListener);
	cancelButton.addActionListener(buttonListener);
	south.add(okButton);
	south.add(cancelButton);

	add(north, "North");
	add(south, "South");

	pack();

	Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	Dimension dim = getSize();
	Point position = parentWindow.getLocation();
	int x = position.x+20;
	int y = position.y+20;
	if (x+dim.width>screen.width)
	    x = screen.width-dim.width;
	if (x<0)
	    x = 0;
	if (y+dim.height>screen.height)
	    y = screen.height-dim.height;
	if (y<0)
	    y = 0;
	setLocation (x, y);

	show();
    }

    private void addPair(Panel panel, Component comp1, Component comp2)
    {
	gridbag.setConstraints(comp1, constrLeft);
	panel.add(comp1);
	gridbag.setConstraints(comp2, constrRight);
	panel.add(comp2);
    }

    private void onCancel()
    {
	dispose();
    }

    private void onOK()
    {
	((MenuItem)component).setName(nameField.getText());
	((MenuItem)component).setLabel(labelField.getText());
	if (shortcutChoice.getSelectedIndex()!=0)
	{
	    ((MenuItem)component).setShortcut(new MenuShortcut(GUIMenuShortcuts.shortcuts[shortcutChoice.getSelectedIndex()-1].keycode, shiftCheckbox.getState()));
	}
	else
	    ((MenuItem)component).deleteShortcut();
	((MenuItem)component).setFont(componentFont);

	dispose();
    }


    private class ButtonListener implements ActionListener
    {
	public void actionPerformed (ActionEvent e)
	{
	    if(e.getSource().equals(listenerButton))
	    {
		MenuListenerDialog listenerDialog = new MenuListenerDialog (frame, (GUIMenuLeafComponent)component, "MenuItem", structCont);
		listenerDialog.pack();

		Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		Dimension dim = listenerDialog.getSize();
		Point position = getLocation();
		int x = position.x+20;
		int y = position.y+20;
		if (x+dim.width>screen.width)
		    x = screen.width-dim.width;
		if (x<0)
		    x = 0;
		if (y+dim.height>screen.height)
		    y = screen.height-dim.height;
		if (y<0)
		    y = 0;
		listenerDialog.setLocation (x, y);

		listenerDialog.show();
	    }
	    else if(e.getSource().equals(cancelButton))
		onCancel();
	    else if(e.getSource().equals(okButton))
		onOK();
	}
    }



    private class FontListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
	    int[] styles = {Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD|Font.ITALIC};
	    int style = styles[faceChoice.getSelectedIndex()];

	    componentFont = new Font(fontChoice.getSelectedItem(), style, Integer.parseInt(sizeChoice.getSelectedItem()));

	    sampleField.setFont(componentFont);
	    sampleField.setSize(sampleField.getPreferredSize());

	    setResizable(true);
	    setSize(getPreferredSize());
	    setResizable(false);
	    validate();
	}
    }
    
}
