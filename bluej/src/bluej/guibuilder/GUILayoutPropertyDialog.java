package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;
import bluej.guibuilder.graphics.Separator;
import bluej.guibuilder.graphics.IntegerField;
import java.util.Vector;


/**
 * GUILayoutPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of the layouts
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUILayoutPropertyDialog extends Dialog {
    
    private GridLayout gl = new GridLayout(4,2);
    private CardLayout cl = new CardLayout();
    private Panel layoutPanel = new Panel();
    
    private Panel specificPanel = new Panel();
    private Panel borderPanel = new Panel();
    private Panel gridPanel = new Panel();
    private Panel gridbagPanel = new Panel();
    private Panel cardPanel = new Panel();
    private Panel flowPanel = new Panel();
    private Panel buttonPanel = new Panel();
    
    private CardLayout specificLayout;

    // generic
    
    private IntegerField[] tfHgap = { new IntegerField(),new IntegerField(), new IntegerField(), new IntegerField()};
    private IntegerField[] tfVgap = { new IntegerField(), new IntegerField(), new IntegerField(), new IntegerField()};
    private Choice layoutChoice = new Choice();
    private LayoutListener layoutListener = new LayoutListener();
    
    // borderlayout
    
    // gridlayout
    private IntegerField tfRows = new IntegerField();
    private IntegerField tfColumns = new IntegerField();
    private TextFieldListener textFieldListener = new TextFieldListener();
    
    // gridbaglayout

    private Choice componentChoice = new Choice();
    private IntegerField tfGridx = new IntegerField();
    private IntegerField tfGridy = new IntegerField();
    private IntegerField tfGridwidth = new IntegerField();
    private IntegerField tfGridheight = new IntegerField();
    private Choice fillChoice = new Choice();
    private IntegerField tfIpadx = new IntegerField();
    private IntegerField tfIpady = new IntegerField();
    private Choice anchorChoice = new Choice();
    private TextField tfWeightx = new TextField();
    private TextField tfWeighty = new TextField();
    private IntegerField tfInsetBottom = new IntegerField();
    private IntegerField tfInsetLeft = new IntegerField();
    private IntegerField tfInsetRight = new IntegerField();
    private IntegerField tfInsetTop = new IntegerField();

    private ComponentChoiceListener componentChoiceListener = new ComponentChoiceListener();

    private String[] anchors = { "Center","North","NortEast","East","SouthEast","South","SouthWest","West","NortWest" 
    };
    private int[] anchorsInt = { GridBagConstraints.CENTER,GridBagConstraints.NORTH,GridBagConstraints.NORTHEAST,GridBagConstraints.EAST,GridBagConstraints.SOUTHEAST,GridBagConstraints.SOUTH,GridBagConstraints.SOUTHWEST,GridBagConstraints.WEST,GridBagConstraints.NORTHWEST 
    };
    
    private Checkbox relativexcbx = new Checkbox("Gridx relative");
    private Checkbox relativeycbx = new Checkbox("Gridy relative");
    private ConstraintsTextFieldListener constraintsTextFieldListener = new ConstraintsTextFieldListener();
    private ConstraintsChoiceListener constraintsChoiceListener = new ConstraintsChoiceListener();
    private ConstraintsCheckboxListener constraintsCheckboxListener = new ConstraintsCheckboxListener();

    // CardLayout

    private Button firstButton = new Button("First");
    private Button prevButton = new Button("Previous");
    private Button nextButton = new Button("Next");
    private Button lastButton = new Button("Last");
    private Button newButton = new Button("New Card");
    private Button removeButton = new Button("Remove Button");
    private TextField tfName = new TextField();
    private CardButtonListener cardButtonListener = new CardButtonListener();
    
    // flowlayout

    private Choice alignmentChoice = new Choice();
    private AlignmentListener alignmentListener = new AlignmentListener();
    
    // for optimizing

    private GUICardLayout card;
    private GUIGridBagLayout gridbag;
    private GUIGridLayout grid;
    private GUIFlowLayout flow;
    private GUIBorderLayout border;
    
    // generic
    
    private Button acceptButton = new Button("Accept");
    private Button cancelButton = new Button("Cancel");
    private Button resetButton = new Button("Reset");
    private ButtonListener buttonListener = new ButtonListener();
    private GapListener gapListener = new GapListener();
    private Vector undovconstraints = new Vector();
    private Vector undovpositions = new Vector();
    private int undoHgap;
    private int undoVgap;
    private int undoAlignment;
    private int undorows;
    private int undocolumns;
    private GUIBuilderApp app;
    private Dimension[] dim;
    private GUIComponentLayoutNode componentLayout;
    private GUIComponent[] components;
    private int componentCount;
    private GUIComponentNormalNode componentNode;
    private StructureContainer structCont;
    private int current = 0;
    private int oldcurrent = 0;
    private int hgap = 0;
    private int vgap = 0;
    final static int BORDERLAYOUT = 0;
    
    final static int GRIDLAYOUT = 1;
    final static int CARDLAYOUT = 2;
    
    final static int FLOWLAYOUT = 3;
    final static int GRIDBAGLAYOUT = 4;
    
    private String[] layouts = { "BorderLayout","GridLayout","CardLayout","FlowLayout","GridBagLayout" 
    }
    ;
    


// 0 BorderLayout 1 GridLayout 2 CardLayout 3 FlowLayout 4 GridBagLayout


     /**
       * Constructs a GUILayoutPropertyDialog. It enables the user to edit the properties of the selected layout. Furthermore the layout can be changed.
       @param app The GUIBuilderApp application.
       @param componentLayout The GUIComponentLayoutNode to be changed.
       @param structCont The StructureContainer in which the component resides.
       @param componentNode The GUIComponentNormalNode in which the layout is used.
       */
    public GUILayoutPropertyDialog(GUIBuilderApp app,GUIComponentLayoutNode componentLayout,StructureContainer structCont, GUIComponentNormalNode componentNode)
    {
        super(app,true );
        this.componentLayout = componentLayout;
        this.componentNode = componentNode;
        this.app =app;
        this.structCont = structCont;
        
        setLayout(new BorderLayout());
        setTitle("Layout properties");
        prepareTopPanel();
        
        preparePanels();

        specificLayout = new CardLayout();
        specificPanel.setLayout(specificLayout);
        specificPanel.add(borderPanel,"BorderLayout");
        specificPanel.add(gridPanel,"GridLayout");
        specificPanel.add(cardPanel,"CardLayout");
        specificPanel.add(flowPanel,"FlowLayout");
        specificPanel.add(gridbagPanel,"GridBagLayout");
        
        componentCount = componentLayout.getGUIComponentCount();
        components = componentLayout.getGUIComponents();
       
        // choose the right panel
        if(componentLayout instanceof BorderLayout)
            buildBorderLayout(true);
        else if(componentLayout instanceof GridLayout)
            buildGridLayout(true);
        else if(componentLayout instanceof GridBagLayout)
            buildGridBagLayout(true);
        else if(componentLayout instanceof CardLayout)
            buildCardLayout(true);
        else if(componentLayout instanceof FlowLayout)
            buildFlowLayout(true);

        layoutChoice.addItemListener(layoutListener);

        acceptButton.addActionListener(buttonListener);
        cancelButton.addActionListener(buttonListener);
        resetButton.addActionListener(buttonListener);
        
        buttonPanel.add(acceptButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(cancelButton);
       
        add(specificPanel,"Center");
        add(buttonPanel,"South");
        
        pack();
        dim = new Dimension[5];
        dim[0] = borderPanel.getPreferredSize();
        dim[1] = gridPanel.getPreferredSize();
        dim[2] = cardPanel.getPreferredSize();
        dim[3] = flowPanel.getPreferredSize();
        dim[4] = gridbagPanel.getPreferredSize();
        
        firstButton.addActionListener(cardButtonListener);
        lastButton.addActionListener(cardButtonListener);
        prevButton.addActionListener(cardButtonListener);
        nextButton.addActionListener(cardButtonListener);
        newButton.addActionListener(cardButtonListener);
        removeButton.addActionListener(cardButtonListener);
        
        componentChoice.addItemListener(componentChoiceListener);
        tfGridx.addFocusListener(constraintsTextFieldListener);
        tfGridy.addFocusListener(constraintsTextFieldListener);
        tfWeightx.addFocusListener(constraintsTextFieldListener);
        tfWeighty.addFocusListener(constraintsTextFieldListener);
        tfGridwidth.addFocusListener(constraintsTextFieldListener);
        tfGridheight.addFocusListener(constraintsTextFieldListener);
        tfIpadx.addFocusListener(constraintsTextFieldListener);
        tfIpady.addFocusListener(constraintsTextFieldListener);
        tfInsetBottom.addFocusListener(constraintsTextFieldListener);
        tfInsetLeft.addFocusListener(constraintsTextFieldListener);
        tfInsetRight.addFocusListener(constraintsTextFieldListener);
        tfInsetTop.addFocusListener(constraintsTextFieldListener);
        anchorChoice.addItemListener(constraintsChoiceListener);
        fillChoice.addItemListener(constraintsChoiceListener);
        relativexcbx.addItemListener(constraintsCheckboxListener);
        relativeycbx.addItemListener(constraintsCheckboxListener);

        tfRows.addFocusListener(textFieldListener);
        tfColumns.addFocusListener(textFieldListener);

        tfHgap[BORDERLAYOUT].addFocusListener(gapListener);
        tfVgap[BORDERLAYOUT].addFocusListener(gapListener);
        tfHgap[GRIDLAYOUT].addFocusListener(gapListener);
        tfVgap[GRIDLAYOUT].addFocusListener(gapListener);
        tfHgap[FLOWLAYOUT].addFocusListener(gapListener);
        tfVgap[FLOWLAYOUT].addFocusListener(gapListener);
        tfHgap[CARDLAYOUT].addFocusListener(gapListener);
        tfVgap[CARDLAYOUT].addFocusListener(gapListener);

        alignmentChoice.addItemListener(alignmentListener);
        
        updateCardSize();
        
       
        show();
        
    }


    private void buildBorderLayout(boolean firsttime)
    {
            current = BORDERLAYOUT;
            border = (GUIBorderLayout)componentLayout;
            if(firsttime)
            {
                hgap = border.getHgap();
                vgap = border.getVgap();
                undoHgap = hgap;
                undoVgap = vgap;
                for(int i = 0 ; i < componentCount ; i++)
                {
                    undovpositions.addElement(border.getConstraints(components[i]));
                }
                    
            }
            else
            {
                hgap = undoHgap;
                vgap = undoVgap;
                border.setHgap(hgap);
                border.setVgap(vgap);
            }
            tfHgap[current].setValue(hgap);
            tfVgap[current].setValue(vgap);
            layoutChoice.select("BorderLayout");
            specificLayout.show(specificPanel,"BorderLayout");
    }


    private void buildGridLayout(boolean firsttime)
    {
        current = GRIDLAYOUT;
        grid = (GUIGridLayout)componentLayout;
        if(firsttime)
        {
            hgap = grid.getHgap();
            vgap = grid.getVgap();
            undoHgap = hgap;
            undoVgap = vgap;
            undorows = grid.getRows();
            undocolumns = grid.getColumns();
        }
        else
        {
            hgap = undoHgap;
            vgap = undoVgap;
            grid.setHgap(hgap);
            grid.setVgap(vgap);
            grid.setRows(undorows);
            grid.setColumns(undocolumns);
            grid.adjustContainers();
        }
        tfHgap[current].setValue(hgap);
        tfVgap[current].setValue(vgap);
        layoutChoice.select("GridLayout");
        tfRows.setValue(grid.getRows());
        tfColumns.setValue(grid.getColumns());
        specificLayout.show(specificPanel,"GridLayout");
            
    }


    private void buildGridBagLayout(boolean firsttime)
    {
        gridbag = (GUIGridBagLayout)componentLayout;
        current = GRIDBAGLAYOUT;
        
        layoutChoice.select("GridBagLayout");
        specificLayout.show(specificPanel,"GridBagLayout");
        for(int i = 0 ; i < componentCount ; i++)
        {
            componentChoice.add(components[i].getName());
        }
        if(firsttime)
            for(int i = 0 ; i < componentCount ; i++)
                undovconstraints.addElement(gridbag.getConstraints(components[i]));
            
            
    }

    
    private void buildCardLayout(boolean firsttime)
    {
        current = CARDLAYOUT;
        card = (GUICardLayout)componentLayout;
        if(firsttime)
        {
            hgap = card.getHgap();
            vgap = card.getVgap();
            undoHgap = hgap;
            undoVgap = vgap;
        }
        else
        {
            hgap = undoHgap;
            vgap = undoVgap;
            card.setHgap(hgap);
            card.setVgap(vgap);
        }
        tfHgap[current].setValue(hgap);
        tfVgap[current].setValue(vgap);
        layoutChoice.select("CardLayout");
        specificLayout.show(specificPanel,"CardLayout");
        tfName.setText(card.getVisibleName());
    }

    
    private void buildFlowLayout(boolean firsttime)
    {
        current = FLOWLAYOUT;
        flow = (GUIFlowLayout)componentLayout;
        if(firsttime)
        {
            hgap = flow.getHgap();
            vgap = flow.getVgap();
            undoHgap = hgap;
            undoVgap = vgap;
            undoAlignment = flow.getAlignment();
        }
        else
        {
            hgap = undoHgap;
            vgap = undoVgap;
            flow.setAlignment(undoAlignment);
            flow.setHgap(hgap);
            flow.setVgap(vgap);
        }
        tfHgap[current].setValue(hgap);
        tfVgap[current].setValue(vgap);
        int align = flow.getAlignment();
        if(align == FlowLayout.LEFT)
            alignmentChoice.select("Left");
        else if(align == FlowLayout.RIGHT)
            alignmentChoice.select("Right");
        else if(align == FlowLayout.CENTER)
            alignmentChoice.select("Center");
        layoutChoice.select("FlowLayout");
        specificLayout.show(specificPanel,"FlowLayout");
    }

    
    private void preparePanels()
    {

        // borderLayout
        borderPanel.setLayout(new GridLayout(2,2));
        borderPanel.add(new Label("Horizontal Gap"));
        borderPanel.add(tfHgap[BORDERLAYOUT]);
        borderPanel.add(new Label("Vertical Gap"));
        borderPanel.add(tfVgap[BORDERLAYOUT]);
        // gridPanel
        gridPanel.setLayout(new GridLayout(4,2));
        gridPanel.add(new Label("Horizontal Gap"));
        gridPanel.add(tfHgap[GRIDLAYOUT]);
        gridPanel.add(new Label("Vertical Gap"));
        gridPanel.add(tfVgap[GRIDLAYOUT]);
        gridPanel.add(new Label("Rows:"));
        gridPanel.add(tfRows);
        gridPanel.add(new Label("Columns:"));
        gridPanel.add(tfColumns);

        // gridbagPanel
        gridbagPanel.setLayout(new GridBagLayout());
        GridBagConstraints upper = new GridBagConstraints();
        upper.gridx = 0;
        upper.gridwidth = 1;
        gridbagPanel.add(new Label("Component"),upper);
        upper.gridx++;
        gridbagPanel.add(componentChoice,upper);
        upper.gridx = 0;
        upper.gridwidth = 4;
        gridbagPanel.add(new Label("Constraints"),upper);
        upper.gridwidth = 2;
        gridbagPanel.add(relativexcbx,upper);
        upper.gridx=2;
        gridbagPanel.add(relativeycbx,upper);
        upper.gridx = 0;
        upper.gridwidth = 1;
        upper.fill = GridBagConstraints.HORIZONTAL;
        gridbagPanel.add(new Label("Gridx",Label.LEFT),upper);
        upper.gridx = 1;
        gridbagPanel.add(tfGridx,upper);
        upper.gridx = 2;
        gridbagPanel.add(new Label("Gridy",Label.LEFT),upper);
        upper.gridx = 3;
        gridbagPanel.add(tfGridy,upper);
        upper.gridx = 0;
        gridbagPanel.add(new Label("Gridwidth",Label.LEFT),upper);
        upper.gridx = 1;
        gridbagPanel.add(tfGridwidth,upper);
        upper.gridx = 2;
        gridbagPanel.add(new Label("GridHeigth",Label.LEFT),upper);
        upper.gridx = 3;
        gridbagPanel.add(tfGridheight,upper);
        upper.gridx = 0;
        gridbagPanel.add(new Label("Ipadx",Label.LEFT),upper);
        upper.gridx = 1;
        gridbagPanel.add(tfIpadx,upper);
        upper.gridx = 2;
        gridbagPanel.add(new Label("Ipady",Label.LEFT),upper);
        upper.gridx = 3;
        gridbagPanel.add(tfIpady,upper);
        upper.gridx = 0;
        gridbagPanel.add(new Label("Weightx",Label.LEFT),upper);
        upper.gridx = 1;
        gridbagPanel.add(tfWeightx,upper);
        upper.gridx = 2;
        gridbagPanel.add(new Label("Weighty",Label.LEFT),upper);
        upper.gridx = 3;
        gridbagPanel.add(tfWeighty,upper);
        upper.gridx = 0;
        gridbagPanel.add(new Label("Inset Bottom",Label.LEFT),upper);
        upper.gridx = 1;
        gridbagPanel.add(tfInsetBottom,upper);
        upper.gridx = 2;
        gridbagPanel.add(new Label("Inset Left",Label.LEFT),upper);
        upper.gridx = 3;
        gridbagPanel.add(tfInsetLeft,upper);
        upper.gridx = 0;
        gridbagPanel.add(new Label("Inset Right",Label.LEFT),upper);
        upper.gridx = 1;
        gridbagPanel.add(tfInsetRight,upper);
        upper.gridx = 2;
        gridbagPanel.add(new Label("Inset Top",Label.LEFT),upper);
        upper.gridx = 3;
        gridbagPanel.add(tfInsetTop,upper);
        upper.gridx = 0;
        gridbagPanel.add(new Label("Fill"),upper);
        upper.gridx = 1;
        gridbagPanel.add(fillChoice,upper);
        upper.gridx = 2;
        gridbagPanel.add(new Label("Anchor"),upper);
        upper.gridx = 3;
        gridbagPanel.add(anchorChoice,upper);
        
        fillChoice.add("None");
        fillChoice.add("Horizontal");
        fillChoice.add("Vertical");
        fillChoice.add("Both");
        
        for(int i = 0 ; i < anchors.length ; i++)
            anchorChoice.add(anchors[i]);
        
        // CardPanel
        
        cardPanel.setLayout(new GridLayout(6,2));
        cardPanel.add(new Label("Horizontal Gap"));
        cardPanel.add(tfHgap[CARDLAYOUT]);
        cardPanel.add(new Label("Vertical Gap"));
        cardPanel.add(tfVgap[CARDLAYOUT]);
        cardPanel.add(prevButton);
        cardPanel.add(nextButton);
        cardPanel.add(firstButton);
        cardPanel.add(lastButton);
        cardPanel.add(newButton);
        cardPanel.add(removeButton);
        cardPanel.add(new Label("Name:"));
        cardPanel.add(tfName);
        tfName.setEditable(false);
        
        // FlowPanel
        flowPanel.setLayout(new GridLayout(3,2));
        flowPanel.add(new Label("Horizontal Gap"));
        flowPanel.add(tfHgap[FLOWLAYOUT]);
        flowPanel.add(new Label("Vertical Gap"));
        flowPanel.add(tfVgap[FLOWLAYOUT]);
        flowPanel.add(new Label("Alignment"));
        flowPanel.add(alignmentChoice);
        alignmentChoice.add("Left");
        alignmentChoice.add("Center");
        alignmentChoice.add("Right");
        
    }

    
    private void prepareTopPanel()
    {
        layoutPanel.add(new Label("Layout"));
        layoutPanel.add(layoutChoice);
        add(layoutPanel,"North");
        layoutChoice.add("BorderLayout");
        layoutChoice.add("GridLayout");
        layoutChoice.add("CardLayout");
        layoutChoice.add("FlowLayout");
        layoutChoice.add("GridBagLayout");

    }
    

    private void updateCardSize()
    {
        current = layoutChoice.getSelectedIndex();
        specificLayout.show(specificPanel,layoutChoice.getSelectedItem());
        
        Dimension dimLayout = layoutPanel.getPreferredSize();
        Dimension dimButton = buttonPanel.getPreferredSize();
        
        int width = dimLayout.width;
        if(dimButton.width > width)
            width = dimButton.width;
        if(dim[current].width > width)
            width = dim[current].width;
        setSize(width+40,dimLayout.height+dimButton.height+dim[current].height+40);
        validate();
    }


    private void toFlow()
    {
        int alignment = 0;
        
        if(alignmentChoice.getSelectedItem().equals("Left"))
            alignment = FlowLayout.LEFT;
        else if(alignmentChoice.getSelectedItem().equals("Right"))
            alignment = FlowLayout.RIGHT;
        else if(alignmentChoice.getSelectedItem().equals("Center"))
            alignment = FlowLayout.CENTER;
            
        flow = new GUIFlowLayout(componentNode,structCont,app);
        flow.setAlignment(alignment);
        flow.setHgap(hgap);
        flow.setVgap(vgap);
        tfHgap[current].setValue(hgap);
        tfVgap[current].setValue(vgap);
        componentNode.removeLayout();
        componentNode.setGUILayout(flow);
        for(int i = 0 ; i < componentCount ; i++)
            componentNode.add(components[i]);
        structCont.redraw();
    }

    
    private void toGrid()
    {
        double  squareroot = Math.sqrt(componentCount);
        if(squareroot == 0)
            squareroot = 1;
        int columns = (int)squareroot +1;
        int rows = (int)Math.ceil(componentCount/columns)+1;
        if(rows == 0)
            rows = 1;
        if(componentCount == 0)
        {
            columns = 1;
            rows = 1;
        }
            
        grid = new GUIGridLayout(rows,columns,componentNode,structCont,app);
        grid.setHgap(hgap);
        grid.setVgap(vgap);
        tfHgap[current].setValue(hgap);
        tfVgap[current].setValue(vgap);
        tfRows.setValue(rows);
        tfColumns.setValue(columns);
        componentNode.removeLayout();
        componentNode.setGUILayout(grid);
        for(int i = 0 ; i < componentCount ; i++)
            componentNode.add(components[i]);
        structCont.redraw();
    }

    
    private void toCard()
    {
        card = new GUICardLayout(componentNode,structCont,app);
        card.setHgap(hgap);
        card.setVgap(vgap);
        tfHgap[current].setValue(hgap);
        tfVgap[current].setValue(vgap);
        componentNode.removeLayout();
        componentNode.setGUILayout(card);
        for(int i = 0 ; i < componentCount ; i++)
            card.addGUIComponent(components[i],components[i].getName());
        if(components[0]!=null)
        {
            card.show((Container)componentNode,components[0].getName());
            tfName.setText(components[0].getName());
        }
            
        structCont.redraw();
    }
    
    
    private void toBorder()
    {
        border = new GUIBorderLayout(componentNode,structCont,app);
        String[] constraints = { "North","East","South","Center","West" 
        };
        border.setHgap(hgap);
        border.setVgap(vgap);
        tfHgap[current].setValue(hgap);
        tfVgap[current].setValue(vgap);
        
        componentNode.removeLayout();
        componentNode.setGUILayout(border);
        for(int i = 0 ; i < componentCount ; i++)
        {
            componentNode.add(components[i],constraints[i]);
         }
         structCont.redraw();
    }

    
    private void toGridBag()
    {
        gridbag = new GUIGridBagLayout(componentNode,structCont,app);
        componentNode.removeLayout();
        componentNode.setGUILayout(gridbag);
        for(int i = 0 ; i < componentCount ; i++)
        {
            componentChoice.add(components[i].getName());
            componentNode.add(components[i]);
        }
        if(components[0]!=null)
            updateConstraints(components[0].getName());
        structCont.redraw();
    }


    private void updateConstraints(String componentName)
    {
        for(int i = 0 ; i < componentCount ; i++)
        {
            if(components[i].getName().equals(componentName))
            {
                GridBagConstraints gbc = gridbag.getConstraints(components[i]);
                if(gbc.gridx == GridBagConstraints.RELATIVE)
                {
                    relativexcbx.setState(true);
                    tfGridx.setEditable(false);
                }
                else
                {
                    relativexcbx.setState(false);
                    tfGridx.setValue(gbc.gridx);
                    tfGridx.setEditable(true);
                }
                if(gbc.gridy == GridBagConstraints.RELATIVE)
                {
                    relativeycbx.setState(true);
                    tfGridy.setEditable(false);
                }
                else
                {
                    relativeycbx.setState(false);
                    tfGridy.setValue(gbc.gridy);
                    tfGridy.setEditable(true);
                }

                tfGridwidth.setValue(gbc.gridwidth);
                tfGridheight.setValue(gbc.gridheight);
                tfWeightx.setText(Double.toString(gbc.weightx));
                tfWeighty.setText(Double.toString(gbc.weighty));
                for(int t = 0 ; t < anchors.length ; t++)
                    if(gbc.anchor == anchorsInt[t])
                        anchorChoice.select(anchors[t]);
                if(gbc.fill == GridBagConstraints.NONE)
                    fillChoice.select("None");
                else if(gbc.fill == GridBagConstraints.HORIZONTAL)
                    fillChoice.select("Horizontal");
                else if(gbc.fill == GridBagConstraints.VERTICAL)
                    fillChoice.select("Vertical");
                else if(gbc.fill == GridBagConstraints.BOTH)
                    fillChoice.select("Both");
                tfInsetBottom.setValue(gbc.insets.bottom);
                tfInsetLeft.setValue(gbc.insets.left);
                tfInsetRight.setValue(gbc.insets.right);
                tfInsetTop.setValue(gbc.insets.top);
                tfIpadx.setValue(gbc.ipadx);
                tfIpady.setValue(gbc.ipady);
                return;
            }
        }
    }

    
    private void undoLayout()
    {
        componentNode.removeLayout();
        componentNode.setGUILayout(componentLayout);
        //((Container)componentNode).removeAll();
        if(componentLayout instanceof GridBagLayout)
        {
            for(int i = 0 ; i < componentCount ;i++)
                componentNode.add(components[i],(GridBagConstraints)undovconstraints.elementAt(i));
            
            buildGridBagLayout(false);
        }
        else if(componentLayout instanceof BorderLayout)
        {
            for(int i = 0 ; i < componentCount ; i++)
                componentNode.add(components[i],undovpositions.elementAt(i));
            buildBorderLayout(false);
        }
        else if(componentLayout instanceof CardLayout)
        {
            card.first((Container)componentNode);
	    ((Container)componentNode).removeAll();

            for(int i = 0 ; i < componentCount ; i++)
            {
                componentNode.add(components[i],components[i].getName());
		((Component)components[i]).setVisible(true);
            }
            buildCardLayout(false);
            card.first((Container)componentNode);
        }
        else
        {
            for(int i=0 ; i < componentCount ; i++)
            {
                componentNode.add(components[i]);
            }
            
            if(componentLayout instanceof GridLayout)
                buildGridLayout(false);
            else if(componentLayout instanceof FlowLayout)
            {
                buildFlowLayout(false);
                int c = ((Container)componentNode).getComponentCount();
            }
        }
        structCont.redraw();
    }


    private void updateComponent()
    {
        String componentStr = componentChoice.getSelectedItem();
        GUIComponent cmp = null;
        for(int i = 0 ; i < componentCount ; i++)
            if(components[i].getName().equals(componentStr))
            {
                cmp = components[i];
                break;
            }
        GridBagConstraints gbc = new GridBagConstraints();
        if(relativexcbx.getState() == true)
            gbc.gridx = GridBagConstraints.RELATIVE;
        else
            gbc.gridx = tfGridx.getValue();
        if(relativeycbx.getState() == true)
            gbc.gridy = GridBagConstraints.RELATIVE;
        else
            gbc.gridy = tfGridy.getValue();
        gbc.gridwidth = tfGridwidth.getValue();
        gbc.gridheight = tfGridheight.getValue();
        gbc.anchor = anchorsInt[anchorChoice.getSelectedIndex()];
        String fillstring = fillChoice.getSelectedItem();
        if(fillstring.equals("None"))
            gbc.fill = GridBagConstraints.NONE;
        else if(fillstring.equals("Horizontal"))
            gbc.fill = GridBagConstraints.HORIZONTAL;
        else if(fillstring.equals("Vertical"))
            gbc.fill = GridBagConstraints.VERTICAL;
        else if(fillstring.equals("Both"))
            gbc.fill = GridBagConstraints.BOTH;
        Insets insets = new Insets(tfInsetTop.getValue(),tfInsetLeft.getValue(),tfInsetBottom.getValue(),tfInsetRight.getValue());
        gbc.ipadx = tfIpadx.getValue();
        gbc.ipady = tfIpady.getValue();
        try
        {
            gbc.weightx = new Double(tfWeightx.getText()).doubleValue();
        }
        catch(NumberFormatException e)
        {
            gbc.weightx = 0.0;
            tfWeightx.setText("0.0");
        }
        try
        {
            gbc.weighty = new Double(tfWeighty.getText()).doubleValue();
        }
        catch(NumberFormatException e)
        {
            gbc.weighty = 0.0;
            tfWeighty.setText("0.0");
        }
        gridbag.setConstraints((Component)(((GUIConcreteComponent)cmp).getContainer()),gbc);

        structCont.redraw();
    }

    
    private class ComponentChoiceListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            updateConstraints(componentChoice.getSelectedItem());
        }
    }


    
    private class ConstraintsTextFieldListener implements FocusListener
    {
        public void focusGained(FocusEvent e)
        {
        }
        public void focusLost(FocusEvent e)
        {
                updateComponent();
        }
    }


    
    private class ConstraintsChoiceListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
                updateComponent();
        }
    }
    private class ConstraintsCheckboxListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            if(e.getSource().equals(relativexcbx))
            {
                if(relativexcbx.getState())
                {
                    tfGridx.setEditable(false);
                }
                else
                    tfGridx.setEditable(true);
            }
            else
            {
                if(relativeycbx.getState())
                {
                    tfGridy.setEditable(false);
                }
                else
                    tfGridy.setEditable(true);
            }
            updateComponent();
        }
    }

    
    
    private class TextFieldListener implements FocusListener
    {
        public void focusGained(FocusEvent e)
        {
        }
        public void focusLost(FocusEvent e)
        {
            int rows = tfRows.getValue();
            int columns = tfColumns.getValue();
            
            if(rows < 1)
            {
                rows = 1;
                tfRows.setValue(1);
            }
            if(columns < 1)
            {
                columns = 1;
                tfColumns.setValue(1);
            }
            grid.setRows(rows);
            grid.setColumns(columns);
            grid.adjustContainers();
            grid.setValidAddPosition();
            structCont.redraw();
                
        }
    }
    
    private class GapListener implements FocusListener
    {
        public void focusGained(FocusEvent e)
        {
        }
        public void focusLost(FocusEvent e)
        {
            if(current!=GRIDBAGLAYOUT)
            {
                hgap = tfHgap[current].getValue();
                vgap = tfVgap[current].getValue();
                if(current == CARDLAYOUT)
                {
                    card.setHgap(hgap);
                    card.setVgap(vgap);
                }
                else if(current == GRIDLAYOUT)
                {
                    grid.setHgap(hgap);
                    grid.setVgap(vgap);
                }
                else if(current == FLOWLAYOUT)
                {
                    flow.setHgap(hgap);
                    flow.setVgap(vgap);
                }
                else if(current == BORDERLAYOUT)
                {
                    border.setHgap(hgap);
                    border.setVgap(vgap);
                }
                structCont.redraw();
                        
            }
        }
    }
    
    
    
    private class CardButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            Component source = (Component)e.getSource();
                
            if(source.equals(firstButton))
            {
                card.first((Container)componentNode);
                tfName.setText(card.getVisibleName());
            }
            else if(source.equals(lastButton))
            {
                card.last((Container)componentNode);
                tfName.setText(card.getVisibleName());
            }
            else if(source.equals(prevButton))
            {
                card.previous((Container)componentNode);
                tfName.setText(card.getVisibleName());
            }
            else if(source.equals(nextButton))
            {
                card.next((Container)componentNode);
                tfName.setText(card.getVisibleName());
            }
            else if(source.equals(newButton))
            {
                GUIPanel gp = new GUIPanel(componentNode,structCont,app);
                card.addGUIComponent(gp,gp.getName());
                gp.setGUILayout((GUIComponentLayoutNode)(new GUIFlowLayout ((GUIComponentNode)gp,structCont,app)));
                card.show((Container)componentNode,gp.getName());
                structCont.redraw();
                tfName.setText(gp.getName());
                
            }
            else if(source.equals(removeButton))
            {
                GUIComponent cmp = card.getVisibleComponent();
                if(cmp!=null)
                {
                    card.previous((Container)componentNode);
                    tfName.setText(card.getVisibleName());
                    card.deleteChild(cmp);
                }
            }
                
        }
    }


    
    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
            {
                if(e.getSource().equals(acceptButton))
                {
                    dispose();
                    
                }
                else if(e.getSource().equals(cancelButton))
                {
                    undoLayout();
                    dispose();
                }
                else
                {
                    undoLayout();
                }
                
            }
    }


    
    private class LayoutListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            oldcurrent = current;
            String layoutString = layoutChoice.getSelectedItem();
            if(current!=GRIDBAGLAYOUT)
            {
                hgap = tfHgap[current].getValue();
                vgap = tfVgap[current].getValue();
            }
                
            if(layoutString.equals("FlowLayout") && oldcurrent!=FLOWLAYOUT)
            {
                updateCardSize();
                toFlow();
            }
            else if(layoutString.equals("GridLayout") && oldcurrent!=GRIDLAYOUT)
            {
                updateCardSize();
                toGrid();
            }
            else if(layoutString.equals("CardLayout") && oldcurrent!=CARDLAYOUT)
            {
                updateCardSize();
                toCard();
            }
            else if(layoutString.equals("BorderLayout") && oldcurrent!=BORDERLAYOUT)
            {
                if(componentCount > 5)
                {
                    MessageBox mb = new MessageBox(app,0,"Maximum elements int BorderLayout is 5!","Message");
                    layoutChoice.select(layouts[current]);
                }
                else
                {
                    updateCardSize();
                    toBorder();
                }
                        
            }
            else if(layoutString.equals("GridBagLayout") && oldcurrent!=GRIDBAGLAYOUT)
            {
                updateCardSize();
                toGridBag();
            }

        }
    }

    
    
    private class AlignmentListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            if(alignmentChoice.getSelectedItem().equals("Left"))
            {
                flow.setAlignment(FlowLayout.LEFT);
            }
            else if(alignmentChoice.getSelectedItem().equals("Right"))
            {
                flow.setAlignment(FlowLayout.RIGHT);
            }
            else if(alignmentChoice.getSelectedItem().equals("Center"))
            {
                flow.setAlignment(FlowLayout.CENTER);
            }
            ((Container)componentNode).doLayout();
            ((Container)componentNode).validate();
        }
    }
} // GUIPropertyDialog
