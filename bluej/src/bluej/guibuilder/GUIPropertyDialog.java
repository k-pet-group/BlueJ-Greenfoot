package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;
import javablue.GUIGraphics.Separator;


/**
 * GUIPropertyDialog.java
 *
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIPropertyDialog extends Dialog {
    private GridBagLayout gb = new GridBagLayout();
    private Panel p = new Panel();
    private Panel buttonPanel = new Panel();
    private Panel basicPanel = new Panel();

    /**
     *
     * This Panel is one that any subclass of the dialog should
     * when showing the specific properties for that component.
     * After this Panel has initialized the init method can be called.
     *
     * @see GUIPropertyDialog#init
     */
    protected Panel specialPanel = new Panel();
    
    private TextField tfName = new TextField();
    private Button fg = new Button();
    private Button bg = new Button();
    private Choice fontChoice = new Choice();
    private Choice sizeChoice = new Choice();
    private Choice faceChoice = new Choice();
    private TextField samplefield = new TextField("ABCabc123");
    private Font font;
    private Choice cursorChoice = new Choice();
    private Button listenerButton = new Button("Define");
    private Choice levelChoice = new Choice();
    
    private Button okButton = new Button("OK");
    private Button cancelButton = new Button("Cancel");
    
    private ColorButtonListener colorbuttonListener = new ColorButtonListener();
    private FontListener fontListener = new FontListener();
    
    private ButtonListener buttonListener = new ButtonListener();
    
    private ColorDialog colorDialog;
    private Frame frame;
    private StringBuffer ftext = new StringBuffer();
    private StringBuffer btext = new StringBuffer();
    private ListenerDialog listenerDialog;
    private String componentStr = new String();
    /**
     * This structureContainer needs to be referred to in subclasses.
     *
     */
    protected StructureContainer structureContainer;
    private ComponentDescriptor componentDescriptor;
    
      private int[] cursors ={ Cursor.CROSSHAIR_CURSOR,Cursor.DEFAULT_CURSOR,Cursor.E_RESIZE_CURSOR,Cursor.HAND_CURSOR,Cursor.MOVE_CURSOR,Cursor.N_RESIZE_CURSOR,Cursor.NE_RESIZE_CURSOR,Cursor.NW_RESIZE_CURSOR,Cursor.S_RESIZE_CURSOR,Cursor.SE_RESIZE_CURSOR,Cursor.SW_RESIZE_CURSOR,Cursor.TEXT_CURSOR,Cursor.W_RESIZE_CURSOR,Cursor.WAIT_CURSOR 
    };
    
    /**
     * This component needs to be referred to in subclasses.
     *
     */
    protected GUIComponent component;


     /**
       * Constructs a GUIPropertyDialog. This class mainly invoked from one of its subclasses.
       The protected member specialPanel should be used by the subclass to show
       the specific properties of the class.
       
       @param f Frame
       @param component The GUIComponent to be changed.
       @param componentStr A String that describes the component.
       @param structCont The StructureContainer in which the component resides.
       */
    public GUIPropertyDialog(Frame f,GUIComponent component,String componentStr,StructureContainer structureContainer) {
        super(f,true );
        this.component = component;
        this.componentStr = componentStr;
        this.structureContainer = structureContainer;
        this.componentDescriptor = ((GUIConcreteComponent)component).getComponentDescriptor();
    
        frame = f;
        ftext = new StringBuffer(componentDescriptor.getColorString());
        btext = new StringBuffer(componentDescriptor.getBcolorString());
        
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.HORIZONTAL;
        
        GridBagConstraints gbc2 = new GridBagConstraints();
	gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
	gbc2.weightx = 1;
        
        p.setLayout(gb);
        
        p.add(new Label("Name:",Label.LEFT),gbc1);

        tfName.setText(component.getName());
        p.add(tfName,gbc2);

        p.add(new Label("Foreground:",Label.LEFT),gbc1);
        
        p.add(fg,gbc2);
        fg.setBackground(((Component)component).getForeground());
        p.add(new Label("Background:",Label.LEFT),gbc1);

        p.add(bg,gbc2);
        bg.setBackground(((Component)component).getBackground());
        p.add(new Label("Font:",Label.LEFT),gbc1);
        p.add(fontChoice,gbc2);
        String[] fonts = java.awt.Toolkit.getDefaultToolkit().getFontList();
        for(int i = 0 ; i < fonts.length ; i++)
            fontChoice.add(fonts[i]);
        Font currentFont = ((Component)component).getFont();
        fontChoice.select(currentFont.getName());
        font = ((Component)component).getFont();
        p.add(new Label("Size:",Label.LEFT),gbc1);
        p.add(sizeChoice,gbc2);
        int[] sizes = { 8,10,12,14,16,18,24,36 };
        int size = (((Component)component).getFont()).getSize();
        int i = 0 ; 
        while(sizes[i]<size)
        {
            sizeChoice.add(""+sizes[i]);
            i++;
        }
        sizeChoice.add(""+size);
        if(sizes[i]==size)
            i++;
        for(; i < sizes.length ; i++)
            sizeChoice.add(""+sizes[i]);
        sizeChoice.select(""+size);

        p.add(new Label("Face:",Label.LEFT),gbc1);
        p.add(faceChoice,gbc2);
        faceChoice.add("Plain");
        faceChoice.add("Bold");
        faceChoice.add("Italics");
        faceChoice.add("Bold & Italics");
        faceChoice.select("Plain");
        
        if(currentFont.isBold())
            faceChoice.select("Bold");
        if(currentFont.isItalic())
            faceChoice.select("Italics");
        if(currentFont.isBold() && currentFont.isItalic())
            faceChoice.select("Bold & Italics");
        

        samplefield.setFont(currentFont);
        
	p.add(new Label("Sample:"), gbc1);
        p.add(samplefield,gbc2);
        
        p.add(new Label("Cursor: ",Label.LEFT),gbc1);
        p.add(cursorChoice,gbc2);
        cursorChoice.add("CROSSHAIR_CURSOR");
        cursorChoice.add("DEFAULT_CURSOR"); 
        cursorChoice.add("E_RESIZE_CURSOR"); 
        cursorChoice.add("HAND_CURSOR"); 
        cursorChoice.add("MOVE_CURSOR"); 
        cursorChoice.add("N_RESIZE_CURSOR"); 
        cursorChoice.add("NE_RESIZE_CURSOR"); 
        cursorChoice.add("NW_RESIZE_CURSOR"); 
        cursorChoice.add("S_RESIZE_CURSOR"); 
        cursorChoice.add("SE_RESIZE_CURSOR"); 
        cursorChoice.add("SW_RESIZE_CURSOR"); 
        cursorChoice.add("TEXT_CURSOR"); 
        cursorChoice.add("W_RESIZE_CURSOR"); 
        cursorChoice.add("WAIT_CURSOR"); 
        setCursorChoice();
        
	p.add(new Label("Listeners:"), gbc1);
        p.add(listenerButton,gbc2);

	p.add(new Label("Code level:"), gbc1);
	levelChoice.addItem("Class");
	levelChoice.addItem("Method");
	levelChoice.addItem("On the fly");
	levelChoice.select(componentDescriptor.getInitLevel());
	p.add(levelChoice, gbc2);

        p.add(new Separator(),gbc2);
        
        // add Listeners
        fg.addActionListener(colorbuttonListener);
        bg.addActionListener(colorbuttonListener);
        fontChoice.addItemListener(fontListener);
        sizeChoice.addItemListener(fontListener);
        faceChoice.addItemListener(fontListener);
        
        listenerButton.addActionListener(buttonListener);
        
        okButton.addActionListener(buttonListener);
        cancelButton.addActionListener(buttonListener);
        
        basicPanel.setLayout(new BorderLayout());
        buttonPanel.setLayout(new GridBagLayout());

	Panel commandPanel = new Panel();
	commandPanel.add(okButton);
	commandPanel.add(cancelButton);

        buttonPanel.add(new Separator(),gbc2);
	buttonPanel.add(commandPanel, gbc2);

        basicPanel.add(p,"North");
        basicPanel.add(specialPanel,"Center");

        setLayout(new BorderLayout());
        add(basicPanel,"Center");
        add(buttonPanel,"South");
    }

    /**
       * Modify the component, so that the changes in the component becomes persistent.
       Should be called by the overrided method int the subclass.
       */
    public void modifyComponent()
    {
        component.setName(tfName.getText());
        ((Component)component).setForeground(fg.getBackground());
        ((Component)component).setBackground(bg.getBackground());
        componentDescriptor.setColorString(ftext.toString());
        componentDescriptor.setBcolorString(btext.toString());
        componentDescriptor.setInitLevel(levelChoice.getSelectedIndex());
        int selected = cursorChoice.getSelectedIndex();
        
        ((Component)component).setCursor(new Cursor(cursors[selected]));
        ((Component)component).setFont(font);
    }

     /**
       * Prepares and shows the dialog. Should be called after the specialpanel is initialized.
       */
    public void init()
    {
        pack();

        Dimension dim = specialPanel.getPreferredSize();
        int width = dim.width;
        dim = basicPanel.getPreferredSize();
        if(dim.width > width)
            width = dim.width;

        
        basicPanel.setSize(width,dim.height);
        setSize(getPreferredSize());
        
	setLocation(((Component)component).getLocationOnScreen());
            
        validate();

        show();
        
    }

    private void setCursorChoice()
    {
        Cursor tmp = ((Component)component).getCursor();
        int tmpInt = tmp.getType();
        for(int i = 0 ; i < cursors.length ; i++)
            if(cursors[i] == tmpInt)
                cursorChoice.select(i);
            
    }


    
    private class ColorButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource().equals(fg))
            {
                Color newColor = ((Component)component).getForeground() ;
                componentDescriptor.getColorString();
                ColorCap colorCap = new ColorCap(newColor);

                if(componentDescriptor.getColorString().indexOf("System") ==-1)
                {
                    colorDialog = new ColorDialog(frame,colorCap,ftext,false);
                }
                else
                {
                    colorDialog = new ColorDialog(frame,colorCap,ftext,true);
                }
                newColor = colorCap.getColor();
                fg.setBackground(newColor);
                    
            }
            else
            {
                Color newColor = ((Component)component).getBackground() ;
                btext = new StringBuffer(componentDescriptor.getBcolorString());
                ColorCap colorCap = new ColorCap(newColor);
                    
                if(componentDescriptor.getBcolorString().indexOf("System") ==-1)
                    colorDialog = new ColorDialog(frame,colorCap,btext,false);
                else
                    colorDialog = new ColorDialog(frame,colorCap,btext,true);
                newColor = colorCap.getColor();
                bg.setBackground(newColor);

            }
        }
    }



    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource().equals(listenerButton))
            {
                listenerDialog = new ListenerDialog((GUIBuilderApp)frame,(GUIConcreteComponent)component,componentStr,structureContainer);
                listenerDialog.pack();
                listenerDialog.show();
            }
            else if(e.getSource().equals(okButton))
            {
                modifyComponent();
                dispose();
            }
            else
            {
                dispose();
            }
        }
    }


    
    private class FontListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            int style = 0;
            int selected = faceChoice.getSelectedIndex();
            if(selected == 0)
                style = Font.PLAIN;
            else if(selected == 1)
                style = Font.BOLD;
            else if(selected == 2)
                style = Font.ITALIC;
            else if(selected == 3)
                style = Font.BOLD|Font.ITALIC;
            font = new Font(fontChoice.getSelectedItem(),style ,Integer.parseInt(sizeChoice.getSelectedItem()));

            samplefield.setFont(font);
            samplefield.setSize(samplefield.getPreferredSize());
            validate();
        }
    }
    
            
                    
 
                       
                    
} // GUIPropertyDialog
