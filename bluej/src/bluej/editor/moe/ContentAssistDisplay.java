package bluej.editor.moe;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class ContentAssistDisplay extends /*EscapeDialog*/ JWindow implements ActionListener, 
ListSelectionListener, FocusListener, KeyListener {


    private String[] methodsAvailable;
    private String[] methodDescrs;

    private JList methodList;
    private JTextArea methodDescription; 
    private int selectedMethod=0;
    private AssistContent[] methods=
    { new AssistContent("toString","Methodreturn", "MethodClass", "this is a test of toString"), 
            new AssistContent("methodName", "returntest1","classtest2","descrTest3"),
            new AssistContent("methodName1", "returntest11","classtest21","descrTest31"),
            new AssistContent("methodName2", "returntest12","classtest22","descrTest32"),
            new AssistContent("methodName3", "returntest13","classtest23","descrTest33"),
            new AssistContent("methodName4", "returntest14","classtest24","descrTest34"),
            new AssistContent("methodName5", "returntest15","classtest25","descrTest35"),
            new AssistContent("methodName6", "returntest16","classtest26","descrTest36"),
            new AssistContent("methodName7", "returntest17","classtest27","descrTest37"),
            new AssistContent("methodName8", "returntest18","classtest28","descrTest38"),
            new AssistContent("methodName9", "returntest19","classtest29","descrTest39"),
            new AssistContent("methodNamea", "returntest1a","classtest2a","descrTest3a")};

    private int selectedValue=0;
    private MoeEditor editor;

    private String text="";
    private Container pane;

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

    }

    public ContentAssistDisplay(Frame owner) {
        super(owner);

        editor=(MoeEditor)owner;
        methodsAvailable=new String[methods.length];
        methodDescrs=new String[methods.length];
        populateMethods();
        makeDialog();

    }

    private void makeDialog()
    {
        GridLayout gridL=new GridLayout(1, 2);
        pane=getContentPane();
        pane.addKeyListener(this);
        
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
        //mainPanel.addKeyListener(this);

        pane.add(mainPanel);
        pack();
    }

    public int getSelectedMethod() {
        return selectedMethod;
    }

    public void setSelectedMethod(int selectedMethod) {
        this.selectedMethod = selectedMethod;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        methodDescription.setText(methodDescrs[methodList.getSelectedIndex()]);
        selectedValue=methodList.getSelectedIndex();
    }

    //once off call when the panel is initialised as it will not be changing
    private void populateMethods(){  
        for (int i=0;i <methods.length; i++ ){
            methodsAvailable[i]=methods[i].getContentName()+" : "+
            methods[i].getContentReturnType()+" - "+methods[i].getContentClass();
            methodDescrs[i]=methods[i].getContentDString();
        }

    }

    @Override
    public void focusGained(FocusEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void focusLost(FocusEvent e) {
        System.out.println("focus lost...");
        // TODO Auto-generated method stub
        //pane.setVisible(false);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // TODO Auto-generated method stub  
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
        System.out.println("pressed "+e.getKeyChar());
        System.out.println("pressed code "+e.getKeyCode());
        if (e.getKeyCode()==KeyEvent.VK_ESCAPE)
            this.setVisible(false);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
        
    }

}
