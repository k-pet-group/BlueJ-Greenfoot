package bluej.editor.moe;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
ListSelectionListener, FocusListener, MouseListener {

    private MoeEditor editor;
    private String[] methodsAvailable;
    private String[] methodDescrs;
    private AssistContent[] values;

    private JList methodList;
    private JTextArea methodDescription; 
    private int selectedValue=0;

    private JComponent pane;

    public ContentAssistDisplay(MoeEditor ed, AssistContent[] values) 
    {
        this.values=values;
        methodsAvailable=new String[values.length];
        methodDescrs=new String[values.length];
        populateMethods();
        makePanel();
        editor=ed;
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
                requestFocus();
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

        // create function description area     
        methodDescription=new JTextArea();
        methodDescription.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        methodDescription.setSize((int)methodPanel.getSize().getWidth(), (int)methodPanel.getSize().getHeight());
        if (methodDescrs.length >selectedValue)
            methodDescription.setText(methodDescrs[selectedValue]);

        methodList = new JList(methodsAvailable);
        methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        methodList.addListSelectionListener(this);
        methodList.setSelectedIndex(selectedValue);
        //methodList.ensureIndexIsVisible(selectedValue);
        methodList.setRequestFocusEnabled(true);
        methodList.addMouseListener(this);
        methodList.addFocusListener(this);
        methodList.requestFocusInWindow();
        methodList.setVisibleRowCount(10);

        JScrollPane scrollPane;
        scrollPane = new JScrollPane(methodList);
        methodPanel.add(scrollPane);

        mainPanel.add(methodPanel, BorderLayout.WEST);
        mainPanel.add(methodDescription, BorderLayout.EAST);
        
        pane.add(mainPanel);        

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
        pane.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke ,"escapeAction");
        getRootPane().getActionMap().put("escapeAction", new AbstractAction(){ 
            public void actionPerformed(ActionEvent e)
            {
                close();
            }
        });

        keyStroke=KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        pane.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke ,"completeAction");
        getRootPane().getActionMap().put("completeAction", new AbstractAction(){ 
            public void actionPerformed(ActionEvent e)
            {
                codeComplete();
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
        setSelectedValue(selectedValue); 
        pane.requestFocusInWindow();
    }

    public void valueChanged(ListSelectionEvent e) 
    {
        int index=methodList.getSelectedIndex();
        if (index==0)
            index=getSelectedValue();
        methodDescription.setText(methodDescrs[index]);
        setSelectedValue(methodList.getSelectedIndex());
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

    public int getSelectedValue() {
        return selectedValue;
    }

    public void setSelectedValue(int selectedValue) {
        this.selectedValue = selectedValue;
    }

    /**
     * close sets the window to invisible
     */
    private void close()
    {
        this.setVisible(false);
    }

    /**
     * codeComplete prints the selected text in the editor
     */
    private void codeComplete()
    {
        boolean success=editor.codeComplete(values[selectedValue].getContentName());
        if (success)
            close();
    }

    public void focusGained(FocusEvent e) 
    {

    }

    public void focusLost(FocusEvent e) 
    {

    }

    public void actionPerformed(ActionEvent e) 
    {

    }

    /**
     * mouseClicked listener for when the item is double clicked. This should result in a code completion
     */
    public void mouseClicked(MouseEvent e) {
        int count=e.getClickCount();
        if (count==2){
            codeComplete();
        }
    }


    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {

    }

}
