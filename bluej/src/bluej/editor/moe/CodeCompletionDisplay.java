/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2012  Michael Kolling and John Rosenberg 

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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
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
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLEditorKit;

import bluej.Config;
import bluej.parser.AssistContent;
import bluej.parser.SourceLocation;
import bluej.parser.lexer.LocatableToken;
import bluej.prefmgr.PrefMgr;
import bluej.utility.JavaUtils;


/**
 * Code completion panel for the Moe editor.
 * 
 * @author Marion Zalk
 */
public class CodeCompletionDisplay extends JFrame 
    implements ListSelectionListener, MouseListener
{
    private static final Color msgTextColor = new Color(200,170,100);

    private MoeEditor editor;
    private WindowListener editorListener;
    private AssistContent[] values;
    private String prefix;
    private String suggestionType;
    private SourceLocation prefixBegin;
    private SourceLocation prefixEnd;

    private JList methodList;
    private JEditorPane methodDescription; 

    private JComponent pane;

    /**
     * Construct a code completion display panel, for the given editor and with the given
     * suggestions. The location specifies the partial identifier entered by the user before
     * requesting suggestions (if any - it may be null).
     */
    public CodeCompletionDisplay(MoeEditor ed, String suggestionType, 
            AssistContent[] values, LocatableToken location) 
    {
        this.values=values;
        this.suggestionType = suggestionType;
        makePanel();
        editor=ed;
        
        if (location != null) {
            prefixBegin = new SourceLocation(location.getLine(), location.getColumn());
            prefixEnd = new SourceLocation(location.getEndLine(), location.getEndColumn());
            prefix = location.getText();
        }
        else {
            prefixBegin = editor.getCaretLocation();
            prefixEnd = prefixBegin;
            prefix = "";
        }

        populatePanel();

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e)
            {
                methodList.requestFocusInWindow();
                editor.getSourcePane().getCaret().setVisible(true);
            }

            @Override
            public void windowLostFocus(WindowEvent e)
            {
                doClose();
            }
        });
        
        editorListener = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e)
            {
                doClose();
            }
            
            @Override
            public void windowIconified(WindowEvent e)
            {
                doClose();
            }
        };
        
        ed.addWindowListener(editorListener);
    }

    /**
     * Close the code completion display window.
     */
    private void doClose()
    {
        editor.removeWindowListener(editorListener);
        dispose();
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
        Border mdBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(0, 10, 5, 10));
        methodDescription.setBorder(mdBorder);
        methodDescription.setEditable(false);
        methodDescription.setOpaque(false);
        
        methodDescription.setEditorKit(new HTMLEditorKit());
        methodDescription.setEditable(false);
        InputMap inputMap = new InputMap() {
            @Override
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
        // To make the gradient fill show up on the Nimbus look and feel,
        // we set the background with an alpha component of zero to get transparency
        // (see http://forums.java.net/jive/thread.jspa?messageID=267839)
        methodDescription.setBackground(new Color(0,0,0,0));
        methodDescription.setFont(methodDescription.getFont().deriveFont((float)PrefMgr.getEditorFontSize()));
        
        methodList = new JList();
        methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        methodList.addListSelectionListener(this);
        methodList.addMouseListener(this);
        methodList.requestFocusInWindow();
        methodList.setCellRenderer(new CodeCompleteCellRenderer(suggestionType));
        methodList.setOpaque(false);
        
        // To allow continued typing of method name prefix, we map keys to equivalent actions
        // within the editor. I.e. typing a key inserts that key character.
        inputMap = new InputMap() {
            @Override
            public Object get(final KeyStroke keyStroke)
            {
                if (keyStroke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return null;
                }
                if (keyStroke.getKeyChar() == 8 && keyStroke.getKeyEventType() == KeyEvent.KEY_TYPED) {
                    // keyChar 8 = backspace
                    return new AbstractAction() {
                        public void actionPerformed(ActionEvent e)
                        {
                            if (prefix.length() > 0) {
                                SourceLocation back = new SourceLocation(prefixEnd.getLine(), prefixEnd.getColumn() - 1);
                                editor.setSelection(back, prefixEnd);
                                editor.insertText("", false);
                                prefix = prefix.substring(0, prefix.length() - 1);
                                prefixEnd = editor.getCaretLocation();
                                updatePrefix();
                            }
                        }
                    };
                }
                Object actionName = super.get(keyStroke);
                if (actionName == null && keyStroke.getKeyEventType() == KeyEvent.KEY_TYPED) {
                    char keyChar = keyStroke.getKeyChar();
                    // Ignore < 32, and range between 0x7F and 0x9F which are unicode
                    // control characters. 0x7F is "delete".
                    // 0xFFFF is returned on Mac JDK7 for non-character events.
                    if (keyChar >= 32 && keyChar < 0x7F || keyChar > 0x9F && keyChar < 0xFFFF) {
                        return new AbstractAction() {
                            public void actionPerformed(ActionEvent e)
                            {
                                editor.insertText("" + keyStroke.getKeyChar(), false);
                                prefix += keyStroke.getKeyChar();
                                prefixEnd = editor.getCaretLocation();
                                updatePrefix();
                            }
                        };
                    }
                }
                return actionName;
            }
        };
        inputMap.setParent(methodList.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        methodList.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);

        ActionMap actionMap = new ActionMap() {
            @Override
            public Action get(Object key)
            {
                if (key instanceof Action) {
                    return (Action) key;
                }
                return super.get(key);
            }
        };
        actionMap.setParent(methodList.getActionMap());
        methodList.setActionMap(actionMap);
        
        // Set a standard height/width
        Font mlFont = methodList.getFont();
        mlFont = mlFont.deriveFont((float)PrefMgr.getEditorFontSize());
        FontMetrics metrics = methodList.getFontMetrics(mlFont);
        Dimension size = new Dimension(metrics.charWidth('m') * 30, metrics.getHeight() * 15);

        JScrollPane scrollPane;
        scrollPane = new GradientFillScrollPane(methodList, new Color(250,246,229), new Color(233,210,132));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(size);
        methodPanel.add(scrollPane);
        methodPanel.setFont(mlFont);
        
        scrollPane = new GradientFillScrollPane(methodDescription, new Color(250,246,229), new Color(240,220,140));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(size);

        mainPanel.add(methodPanel);
        mainPanel.add(scrollPane);
        
        pane.add(mainPanel); 

        inputMap = new InputMap();
        inputMap.setParent(pane.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
        inputMap.put(keyStroke, "escapeAction");
        keyStroke=KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        inputMap.put(keyStroke, "completeAction");
        
        pane.getRootPane().setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);
        
        actionMap = new ActionMap() {
            @Override
            public Action get(Object key)
            {
                if (key instanceof Action) {
                    return (Action) key;
                }
                return super.get(key);
            }
        };
        actionMap.put("escapeAction", new AbstractAction() {
            public void actionPerformed(ActionEvent e)
            {
                doClose();
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
        setGlassPane(getCodeCompleteGlassPane());
        pack();
    }

    private void updatePrefix()
    {
        Vector<AssistContent> listData = new Vector<AssistContent>();
        for (int i=0; i<values.length; i++ ) {
            if (values[i].getDisplayName().startsWith(prefix)) {
                listData.add(values[i]);
            }
        }
        methodList.setListData(listData);
        methodList.setSelectedIndex(0);

        getGlassPane().setVisible(listData.size() == 0);
    }
    
    /**
     * Populate the completion list.
     */
    private void populatePanel()
    {  
        updatePrefix();
    }

    /**
     * codeComplete prints the selected text in the editor
     */
    private void codeComplete()
    {
        AssistContent selected = (AssistContent) methodList.getSelectedValue();
        if (selected != null) {
            String completion = selected.getCompletionText();
            String completionSel = selected.getCompletionTextSel();
            String completionPost = selected.getCompletionTextPost();
            boolean hasParameters = selected.hasParameters();

            editor.setSelection(prefixBegin, prefixEnd);

            editor.insertText(completion, false);
            SourceLocation selLoc = editor.getCaretLocation();
            editor.insertText(completionSel, false);
            editor.insertText(completionPost, hasParameters);
            if (hasParameters) {
                editor.setSelection(selLoc.getLine(), selLoc.getColumn(), completionSel.length());
            }
        }
        
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
        AssistContent selected = (AssistContent) methodList.getSelectedValue();
        if (selected != null) {
            String jdHtml = selected.getJavadoc();
            if (jdHtml != null) {
                jdHtml = JavaUtils.javadocToHtml(jdHtml);
            }
            else {
                jdHtml = "";
            }
            
            String sig = escapeAngleBrackets(selected.getReturnType())
                       + " <b>" + escapeAngleBrackets(selected.getDisplayMethodName()) + "</b>"
                       + escapeAngleBrackets(selected.getDisplayMethodParams());
            
            jdHtml = "<h3>" + selected.getDeclaringClass() + "</h3>" + 
                "<blockquote><tt>" + sig + "</tt></blockquote><br>" +
                jdHtml;

            methodDescription.setText(jdHtml);
            methodDescription.setCaretPosition(0); // scroll to top
        }
        else {
            methodDescription.setText("");
        }
    }

    private static String escapeAngleBrackets(String sig)
    {
        return sig.replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * A JScrollPane variant that paints a gradient fill as the background.
     * 
     * Don't forget to setOpaque(false) on whatever is inside this pane.
     */
    private static class GradientFillScrollPane extends JScrollPane
    {
        private Color topColor;
        private Color bottomColor;
        
        private GradientFillScrollPane(Component view, Color topColor, Color bottomColor)
        {
            super(view);
            getViewport().setOpaque(false);
            this.topColor = topColor;
            this.bottomColor = bottomColor;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
         
            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D)g;
                
                int w = getWidth();
                int h = getHeight();
                 
                // Paint a gradient from top to bottom:
                GradientPaint gp = new GradientPaint(
                    0, 0, topColor,
                    0, h, bottomColor);
   
                Paint origPaint = g2d.getPaint();
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                g2d.setPaint(origPaint);
            }
        }
    }    

    // ===== the glass pane for messages on this component =====

    private CodeCompleteGlassPane myGlassPane;

    private CodeCompleteGlassPane getCodeCompleteGlassPane()
    {
        if(myGlassPane == null) {
            myGlassPane = new CodeCompleteGlassPane();
        }
        return myGlassPane;
    }

    /**
     * A glass pane which displays a "no matching completions" message. 
     */
    class CodeCompleteGlassPane extends JComponent
    {
        @Override
        protected void paintComponent(Graphics g)
        {
            Color origColor = g.getColor();
            Font origFont = g.getFont();
            g.setColor(msgTextColor);
            g.setFont(origFont.deriveFont(20f));
            g.drawString(Config.getString("editor.completion.noMatch"), 30, 60);
            g.setColor(origColor);
            g.setFont(origFont);
        }
    }
}
