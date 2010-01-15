/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 

 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.editor.moe;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
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

import bluej.parser.SourceLocation;
import bluej.parser.lexer.LocatableToken;


/**
 * Code completion panel for the Moe editor.
 * 
 * @author Marion Zalk
 */
public class CodeCompletionDisplay extends JFrame 
    implements ListSelectionListener, MouseListener
{
    private MoeEditor editor;
    private String[] methodsAvailable;
    private String[] methodDescrs;
    private LocatableToken location;
    private AssistContent[] values;

    private JList methodList;
    private JTextArea methodDescription; 
    private int selectedValue=0;

    private JComponent pane;

    /**
     * Construct a code completion display panel, for the given editor and with the given
     * suggestions. The location specifies the partial identifier entered by the user before
     * requesting suggestions (if any - it may be null).
     */
    public CodeCompletionDisplay(MoeEditor ed, AssistContent[] values, LocatableToken location) 
    {
        this.values=values;
        this.location = location;
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

        addWindowFocusListener(new WindowFocusListener() {

            public void windowGainedFocus(WindowEvent e)
            {
                methodList.requestFocusInWindow();
                editor.currentTextPane.getCaret().setVisible(true);
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
        methodList.addMouseListener(this);
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
                setVisible(false);
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
                setVisible(false);
            }
        });

        //setLocationRelativeTo(location);
        this.setUndecorated(true);
        pack();
    }

    public void valueChanged(ListSelectionEvent e) 
    {
        int index=methodList.getSelectedIndex();
        if (index==0)
            index=selectedValue;
        methodDescription.setText(methodDescrs[index]);
        this.selectedValue = methodList.getSelectedIndex();
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

    /**
     * codeComplete prints the selected text in the editor
     */
    private void codeComplete()
    {
        String selected = values[selectedValue].getContentName();
        
        //editor.codeComplete(values[selectedValue].getContentName());
        if (location == null) {
            editor.insertText(selected, false);
        }
        else {
            SourceLocation begin = new SourceLocation(location.getLine(), location.getColumn());
            SourceLocation end = new SourceLocation(location.getEndLine(), location.getEndColumn());
            editor.setSelection(begin, end);
            editor.insertText(selected, false);
        }
        
        setVisible(false);
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
