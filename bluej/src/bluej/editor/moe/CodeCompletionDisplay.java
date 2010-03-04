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


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLEditorKit;

import bluej.parser.SourceLocation;
import bluej.parser.lexer.LocatableToken;
import bluej.utility.JavaUtils;


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
    private JEditorPane methodDescription; 
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
        populatePanel();
        makePanel();
        editor=ed;
        
        addWindowFocusListener(new WindowFocusListener() {

            public void windowGainedFocus(WindowEvent e)
            {
                methodList.requestFocusInWindow();
                editor.currentTextPane.getCaret().setVisible(true);
            }

            public void windowLostFocus(WindowEvent e)
            {
                dispose();
            }
        });
    }

    /**
     * Creates a component with a main panel (list of available methods & values)
     * and a text area where the description of the chosen value is displayed
     */
    private void makePanel()
    {
        GridLayout gridL=new GridLayout(1, 2);
        pane = (JComponent) getContentPane();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(gridL);

        // create area for method names
        JPanel methodPanel = new JPanel();

        // create function description area     
        methodDescription = new JEditorPane();
        methodDescription.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        methodDescription.setEditable(false);
        
        methodDescription.setEditorKit(new HTMLEditorKit());
        methodDescription.setEditable(false);
        //methodDescription.addHyperlinkListener(this);
        InputMap inputMap = new InputMap() {
            public Object get(KeyStroke keyStroke)
            {
                // Define no action for up/down, which allows the parent scroll
                // pane to process the keys instead. This means the view will scroll,
                // rather than just moving an invisible cursor.
                Object action = super.get(keyStroke);
                if ("caret-up".equals(action) || "caret-down".equals(action)) {
                    return null;
                }
                return action;
            }
        };
        inputMap.setParent(methodDescription.getInputMap(JComponent.WHEN_FOCUSED));
        methodDescription.setInputMap(JComponent.WHEN_FOCUSED, inputMap);
        
        methodList = new JList(methodsAvailable);
        methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        methodList.addListSelectionListener(this);
        methodList.setSelectedIndex(selectedValue);
        methodList.addMouseListener(this);
        methodList.requestFocusInWindow();
        
        // Set a standard height/width
        Font mlFont = methodList.getFont();
        FontMetrics metrics = methodList.getFontMetrics(mlFont);
        Dimension size = new Dimension(metrics.charWidth('m') * 30, metrics.getHeight() * 15);

        JScrollPane scrollPane;
        scrollPane = new JScrollPane(methodList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(size);
        methodPanel.add(scrollPane);
        
        scrollPane = new JScrollPane(methodDescription);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(size);

        mainPanel.add(methodPanel);
        mainPanel.add(scrollPane);
        
        pane.add(mainPanel); 

        inputMap = new ComponentInputMap(pane.getRootPane());
        inputMap.setParent(pane.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW));

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
        inputMap.put(keyStroke, "escapeAction");
        keyStroke=KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        inputMap.put(keyStroke, "completeAction");
        
        pane.getRootPane().setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);
        
        ActionMap actionMap = new ActionMap();
        actionMap.put("escapeAction", new AbstractAction() {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        actionMap.put("completeAction", new AbstractAction(){ 
            public void actionPerformed(ActionEvent e)
            {
                codeComplete();
            }
        });
        pane.getRootPane().setActionMap(actionMap);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setUndecorated(true);
        pack();
    }

    //once off call when the panel is initialised as it will not be changing
    private void populatePanel()
    {  
        for (int i=0;i <values.length; i++ ){
            methodsAvailable[i]=values[i].getDisplayName()+" : "+
            values[i].getReturnType()+" - "+values[i].getDeclaringClass();
            methodDescrs[i]=values[i].getJavadoc();
        }
    }

    /**
     * codeComplete prints the selected text in the editor
     */
    private void codeComplete()
    {
        String completion = values[selectedValue].getCompletionText();
        String completionSel = values[selectedValue].getCompletionTextSel();
        String completionPost = values[selectedValue].getCompletionTextPost();
        
        if (location != null) {
            SourceLocation begin = new SourceLocation(location.getLine(), location.getColumn());
            SourceLocation end = new SourceLocation(location.getEndLine(), location.getEndColumn());
            editor.setSelection(begin, end);
        }
        
        editor.insertText(completion, false);
        SourceLocation selLoc = editor.getCaretLocation();
        editor.insertText(completionSel, false);
        editor.insertText(completionPost, true);
        editor.setSelection(selLoc.getLine(), selLoc.getColumn(), completionSel.length());
        
        setVisible(false);
    }

    // ---------------- MouseListener -------------------
    
    /*
     * A double click results in a completion.
     */
    public void mouseClicked(MouseEvent e)
    {
        int count=e.getClickCount();
        if (count==2){
            codeComplete();
        }
    }

    public void mouseEntered(MouseEvent e) { }

    public void mouseExited(MouseEvent e) { }

    public void mousePressed(MouseEvent e) { }

    public void mouseReleased(MouseEvent e) { }

    // ---------------- ListSelectionListener -------------------
    
    /* (non-Javadoc)
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) 
    {
        selectedValue = methodList.getSelectedIndex();
        String jdHtml = methodDescrs[selectedValue];
        if (jdHtml != null) {
            jdHtml = JavaUtils.javadocToHtml(jdHtml);
        }
        
        methodDescription.setText(jdHtml);
        methodDescription.setCaretPosition(0); // scroll to top
    }
    
}
