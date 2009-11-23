package bluej.editor.moe;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class ContentAssistDisplay extends JFrame implements ActionListener, 
ListSelectionListener, FocusListener {

    private String[] methodsAvailable;
    private String[] methodDescrs;
    private AssistContent[] values;

    private JList methodList;
    private JTextArea methodDescription; 
    private int selectedMethod=0;

    private int selectedValue=0;

    private JComponent pane;

    public ContentAssistDisplay(Frame owner, AssistContent[] values) 
    {
        this.values=values;
        methodsAvailable=new String[values.length];
        methodDescrs=new String[values.length];
        populateMethods();
        makePanel();

    }

    /*
     * Creates a component with a main panel (list of available methods & values)
     * and a text area where the description of the chosen value is displayed
     */
    private void makePanel()
    {
        GridLayout gridL=new GridLayout(1, 2);
        pane=(JComponent) getContentPane();
        
        //pane.addKeyListener(this);
        
        addWindowFocusListener(new WindowFocusListener() {
            
            public void windowGainedFocus(WindowEvent e)
            {
            }
            
            public void windowLostFocus(WindowEvent e)
            {
                setVisible(false);
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(gridL);

        // create area for method names
        JPanel methodPanel = new JPanel();
        methodList = new JList(methodsAvailable);
        methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        methodList.setSelectedIndex(selectedValue);
        methodList.addListSelectionListener(this);
        methodList.setVisibleRowCount(10);

        JScrollPane scrollPane;
        scrollPane = new JScrollPane(methodList);
        methodPanel.add(scrollPane);

        // create function description area     
        methodDescription=new JTextArea();
        methodDescription.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        methodDescription.setSize((int)methodPanel.getSize().getWidth(), (int)methodPanel.getSize().getHeight());
        methodDescription.setText(methodDescrs[selectedMethod]);

        mainPanel.add(methodPanel, BorderLayout.WEST);
        mainPanel.add(methodDescription, BorderLayout.EAST);
		mainPanel.addFocusListener(this);
        pane.add(mainPanel);        

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
        pane.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke ,"escapeAction");
        getRootPane().getActionMap().put("escapeAction", new AbstractAction(){ //$NON-NLS-1$
            public void actionPerformed(ActionEvent e)
            {
                close();
            }
        });
        
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) 
            {
                close();
            }
        });

        //setLocationRelativeTo(location);
        this.setUndecorated(true);
        pack();

    }

    public int getSelectedMethod() 
    {
        return selectedMethod;
    }

    public void setSelectedMethod(int selectedMethod) 
    {
        this.selectedMethod = selectedMethod;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) 
    {
        methodDescription.setText(methodDescrs[methodList.getSelectedIndex()]);
        selectedValue=methodList.getSelectedIndex();
    }

    //once off call when the panel is initialised as it will not be changing
    private void populateMethods()
    {  
        for (int i=0;i <values.length; i++ ){
            methodsAvailable[i]=values[i].getContentName()+" : "+
            values[i].getContentReturnType()+" - "+values[i].getContentClass();
            methodDescrs[i]=values[i].getContentDString();
        }

    }

    public void close()
    {
        this.setVisible(false);
    }

    @Override
    public void focusGained(FocusEvent e) 
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void focusLost(FocusEvent e) 
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        // TODO Auto-generated method stub

    }

}
