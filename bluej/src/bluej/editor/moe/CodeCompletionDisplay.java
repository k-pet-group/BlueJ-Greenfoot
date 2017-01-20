/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2012,2014,2015,2016,2017  Michael Kolling and John Rosenberg

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
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLEditorKit;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import bluej.Config;
import bluej.editor.EditorWatcher;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.ParamInfo;
import bluej.parser.SourceLocation;
import bluej.parser.lexer.LocatableToken;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.JavaUtils;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SwingNodeFixed;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Code completion panel for the Moe editor.
 *
 * @author Marion Zalk
 */
public class CodeCompletionDisplay implements ListSelectionListener, MouseListener
{
    private final EditorWatcher watcher;

    private final MoeEditor editor;
    private final WindowListener editorListener;
    @OnThread(Tag.FXPlatform)
    private Text glassPaneLHS;
    @OnThread(Tag.FXPlatform)
    private Text glassPaneRHS;
    @OnThread(Tag.FXPlatform)
    private TilePane glassPaneReplacement;
    private TreeSet<AssistContent> values;
    private String prefix;
    private final String suggestionType;
    private final SourceLocation prefixBegin;
    private SourceLocation prefixEnd;

    private JList methodList;
    private JEditorPane methodDescription;

    private JComponent pane;
    private TreeSet<AssistContent> jListData;
    
    private final SwingNode swingNode;
    @OnThread(Tag.FXPlatform)
    private Stage window;
    
    private ThreadPoolExecutor threadpool;
    /**
     * We only worry about losing focus after we've been explicitly marked
     * as ready.
     */
    @OnThread(Tag.FXPlatform)
    private boolean ready;

    private static final int hiDpiScalingFactor = Config.getPropInteger("screen.hidpi.scaling", 1);

    /**
     * Construct a code completion display panel, for the given editor and with the given
     * suggestions. The location specifies the partial identifier entered by the user before
     * requesting suggestions (if any - it may be null).
     */
    public CodeCompletionDisplay(MoeEditor ed, EditorWatcher watcher, String suggestionType,
                                 AssistContent[] assistContents, LocatableToken location)
    {
        this.values= new TreeSet<AssistContent>(getComparator());

        //creates the ThreadPoolExecutor for javadocHtml completion.
        //always discard the oldest task.
        threadpool = new ThreadPoolExecutor(1, 1, 500L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1), new ThreadPoolExecutor.DiscardOldestPolicy());

        Arrays.sort(assistContents, getComparator());
        this.values.addAll(Arrays.asList(assistContents));

        this.suggestionType = suggestionType;
        Dimension size = makePanel();
        editor= ed;

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
        
        swingNode = new SwingNodeFixed();
        swingNode.setContent(pane);

        Platform.runLater(() -> {
            glassPaneLHS = new Text();
            glassPaneRHS = new Text();
            glassPaneReplacement = new TilePane(glassPaneLHS, glassPaneRHS);
            glassPaneReplacement.setMouseTransparent(true);
            glassPaneReplacement.setPrefColumns(2);
            glassPaneReplacement.setPrefRows(1);
            glassPaneReplacement.setAlignment(Pos.CENTER);
            glassPaneReplacement.setTileAlignment(Pos.CENTER);
            glassPaneReplacement.setPrefTileWidth(size.getWidth()/hiDpiScalingFactor);
            for (Text t : Arrays.asList(glassPaneLHS, glassPaneRHS))
            {
                t.setStyle("-fx-fill: rgb(200,170,100); -fx-font-size: 20px;");
            }
            window = new Stage(StageStyle.UNDECORATED);
            window.setScene(new Scene(new StackPane(swingNode, glassPaneReplacement)));
            JavaFXUtil.addFocusListener(window, focused -> {
                if (focused)
                {
                    SwingUtilities.invokeLater(() -> {
                        methodList.requestFocusInWindow();
                        editor.getSourcePane().getCaret().setVisible(true);
                    });
                }
                else
                {
                    if (ready)
                        doClose();
                }
            });
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

        //TODO adapt the window listener to the new tabbed arrangement

        this.watcher = watcher;
        if (watcher != null)
            watcher.recordCodeCompletionStarted(prefixBegin.getLine(), prefixBegin.getColumn(), null, null, prefix);
    }

    /**
     * Close the code completion display window.
     */
    @OnThread(Tag.Any)
    public void doClose()
    {
        JavaFXUtil.runNowOrLater(() -> {
            window.hide();
        });
    }

    /**
     * Creates a component with a main panel (list of available methods & values)
     * and a text area where the description of the chosen value is displayed
     */
    private Dimension makePanel()
    {
        GridLayout gridL=new GridLayout(1, 2);
        pane = new JPanel();

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
        if (!Config.isRaspberryPi()) methodDescription.setOpaque(false);

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
        if (!Config.isRaspberryPi()){
            methodDescription.setBackground(new Color(0,0,0,0));
        }else{
            methodDescription.setBackground(new Color(0,0,0));//no Alpha channel.
        }
        methodDescription.setFont(methodDescription.getFont().deriveFont((float)PrefMgr.getEditorFontSize()));

        methodList = new JList(new DefaultListModel());
        methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        methodList.addListSelectionListener(this);
        methodList.addMouseListener(this);
        methodList.requestFocusInWindow();
        methodList.setCellRenderer(new CodeCompleteCellRenderer(suggestionType));
        if (!Config.isRaspberryPi()) methodList.setOpaque(false);

        // To allow continued typing of method name prefix, we map keys to equivalent actions
        // within the editor. I.e. typing a key inserts that key character.
        inputMap = new InputMap() {
            @Override
            public Object get(final KeyStroke keyStroke)
            {
                if (keyStroke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return null;
                }
                if (keyStroke.getKeyChar() == 8 && keyStroke.getKeyEventType() == KeyEvent.KEY_TYPED
                    || (Config.isMacOS() && keyStroke.getKeyCode() == 8 && keyStroke.getKeyEventType() == KeyEvent.KEY_PRESSED)) {
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
        mlFont = mlFont.deriveFont((float) PrefMgr.getEditorFontSize());
        FontMetrics metrics = methodList.getFontMetrics(mlFont);
        Dimension size = new Dimension(metrics.charWidth('m') * 30, metrics.getHeight() * 15);

        JScrollPane scrollPane;
        if (!Config.isRaspberryPi()) {
            scrollPane = new GradientFillScrollPane(methodList, new Color(250, 246, 229), new Color(233, 210, 132));
        } else {
            scrollPane = new FillScrollPane(methodList, new Color(250, 246, 229));
        }
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(size);
        methodPanel.add(scrollPane);
        methodPanel.setFont(mlFont);

        if (!Config.isRaspberryPi()) {
            scrollPane = new GradientFillScrollPane(methodDescription, new Color(250, 246, 229), new Color(240, 220, 140));
        } else {
            scrollPane = new FillScrollPane(methodDescription, new Color(250, 246, 229));
        }
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(size);

        mainPanel.add(methodPanel);
        mainPanel.add(scrollPane);

        pane.add(mainPanel);

        inputMap = new InputMap();
        inputMap.setParent(pane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        inputMap.put(keyStroke, "escapeAction");
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        inputMap.put(keyStroke, "completeAction");

        pane.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);

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
        pane.setActionMap(actionMap);

        //setGlassPane(getCodeCompleteGlassPane());
        return size;
    }

    /*
     * whenever the prefix changes (including the creation of the codeCompletionDisplay), this method is called.
     * It searches this.values for the elements that should be displayed and creates a new model with 
     * those elements. If there is no such elements, then displays a message.
     */
    private void updatePrefix()
    {
        jListData = new TreeSet<AssistContent>(getComparator());
        Iterator<AssistContent> i = values.iterator();
        AssistContent value;
        while (i.hasNext()) {
            value = i.next();
            if (value.getName().startsWith(prefix)) {
                jListData.add(value);
            }
        }
        methodList.setListData(jListData.toArray(new AssistContent[jListData.size()]));
        methodList.setSelectedIndex(0);

        Platform.runLater(() -> setWorking(false));
    }
    
    /**
     * Adds several AssistContent to the listModel (displayed) and to the 
     * list of possible completions (values) for the update method.
     */
    public void addElements(List<AssistContent> elements)
    {
        int currentSelection = methodList.getSelectedIndex();
        values.addAll(elements); //no need to sort, since elements are sorted when added.
        //incremental prefix update.
        ArrayList<AssistContent> filteredElements = new ArrayList<AssistContent>();
        for (AssistContent element : elements) {
            if (element.getName().startsWith(prefix)) {
                filteredElements.add(element);
            }
        }
        jListData.addAll(filteredElements);

        methodList.setListData(jListData.toArray(new AssistContent[jListData.size()]));
        methodList.setSelectedIndex(currentSelection); // restore the selection
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
        if (selected != null)
        {
            String start = selected.getName();
            List<ParamInfo> params = selected.getParams();
            if (params != null)
                start += "(";
            // Replace prefix with the full name:
            editor.setSelection(prefixBegin, prefixEnd);
            editor.insertText(start, false);
            String inserted = start;
            
            if (params != null)
            {
                // Record position before we add first parameter,
                // so that we can come back and select it:
                SourceLocation selLoc = editor.getCaretLocation();
                // Put all available params in, separated by ", "
                if (!params.isEmpty())
                {
                    final String joinedParams = params.stream().map(ParamInfo::getDummyName).collect(Collectors.joining(", "));
                    editor.insertText(joinedParams, false);
                    inserted += joinedParams;
                }
                        
                editor.insertText(")", false);
                inserted += ")";
                        
                // If there were any dummy parameters, go back and select first one:
                if (params.size() > 0)
                    editor.setSelection(selLoc.getLine(), selLoc.getColumn(), params.get(0).getDummyName().length());
            }

            watcher.recordCodeCompletionEnded(prefixBegin.getLine(), prefixBegin.getColumn(), null, null, prefix, inserted);
            try
            {
                editor.save();
            }
            catch (IOException e)
            {
                Debug.reportError(e);
            }
        }

        Platform.runLater(() -> window.hide());
    }

    // ---------------- MouseListener -------------------
    /*
     * A double click results in a completion.
     */
    public void mouseClicked(MouseEvent e)
    {
        int count = e.getClickCount();
        if (count == 2) {
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
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        AssistContent selected = (AssistContent) methodList.getSelectedValue();
        if (selected == null) {
            methodDescription.setText("");
            return;
        }
        
        AssistContent.JavadocCallback callback = new AssistContent.JavadocCallback() {
            @Override
            public void gotJavadoc(AssistContent content)
            {
                AssistContent selected = (AssistContent) methodList.getSelectedValue();
                if (content == selected) {
                    setHtml(content, content.getJavadoc());
                }
            }
        };
        
        if (selected.getJavadocAsync(callback, threadpool)) {
            String jdHtml = selected.getJavadoc();
            setHtml(selected, jdHtml);
        }
        else {
            Platform.runLater(() -> setWorking(true)); // display glasspanel with "working" message.
        }
    }

    private void setHtml(AssistContent selected, String jdHtml)
    {
        if (jdHtml != null) {
            jdHtml = JavaUtils.javadocToHtml(jdHtml);
        } else {
            jdHtml = "";
        }
        String sig = Utility.escapeAngleBrackets(selected.getType())
                   + " <b>" + Utility.escapeAngleBrackets(selected.getName()) + "</b>";
        
        if (selected.getParams() != null)
            sig += Utility.escapeAngleBrackets("(" + selected.getParams().stream().map(ParamInfo::getUnqualifiedType).collect(Collectors.joining(", ")) + ")");
        
        jdHtml = "<h3>" + selected.getDeclaringClass() + "</h3>" + 
            "<blockquote><tt>" + sig + "</tt></blockquote><br>" +
            jdHtml;
        
//            
//            if (Config.isRaspberryPi()) {
//                jdHtml = "<body bgcolor=\"#FAF6E5\">" + jdHtml;
//                methodDescription.setBorder(BorderFactory.createLineBorder(new Color(250,246,229), 12));
//            }
        

        methodDescription.setText(jdHtml);
        methodDescription.setCaretPosition(0); // scroll to top
    }

    @OnThread(Tag.FXPlatform)
    public void setReady(boolean ready)
    {
        this.ready = ready;
    }

    /**
     * Unwieldy but accurate name.  Shows the window at the given position, brought to the front and focused. 
     */
    public void showFrontFocusedAt(int xpos, int ypos)
    {
        Platform.runLater(() -> {
            setReady(false);
            window.setX(xpos/hiDpiScalingFactor);
            window.setY(ypos/hiDpiScalingFactor);
            window.show();
            window.toFront();
            window.requestFocus();
            setReady(true);
        });
    }

    /**
     * A JScrollPane variant that paints a single colour fill as the background.
     *
     * Used for the Raspberry Pi.
     */
    private static class FillScrollPane extends JScrollPane
    {
        private Color c;
        private Component v;

        private FillScrollPane(Component view, Color color)
        {
            super(view);
            this.c = color;
            this.v = view;
            //the background of a JScroolPane is the viewport.
            //Changing the viewport colour, changes the background of the JScroolPane
            view.setBackground(this.c);

            if (this.v instanceof JEditorPane) {
                JEditorPane jEditorPane = (JEditorPane) this.v;
                Color bgColor = new Color(250, 246, 229);
                Utility.setJEditorPaneBackground(jEditorPane, bgColor);
            }
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            //fillRect doesn't work on JEditorPane. 
            if ((g instanceof Graphics2D) && !(this.v instanceof JEditorPane)) {
                Graphics2D g2d = (Graphics2D) g;

                int w = getWidth();
                int h = getHeight();

                Paint origPaint = g2d.getPaint();
                g2d.setPaint(this.c);
                g2d.fillRect(0, 0, w, h);
                g2d.setPaint(origPaint);
            }

        }
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
                Graphics2D g2d = (Graphics2D) g;

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

    @OnThread(Tag.FXPlatform)
    public void setWorking(boolean isWorking)
    {
        if (isWorking)
        {
            SwingUtilities.invokeLater(() -> this.methodDescription.setText("")); //clear the text
            glassPaneLHS.setText("");
            glassPaneRHS.setText(Config.getString("editor.completion.working"));
            glassPaneReplacement.setVisible(true);
        }
        else
        {
            if (glassPaneReplacement != null) // May be null during initialisation
            {
                SwingUtilities.invokeLater(() -> {
                    boolean empty = jListData.isEmpty();
                    Platform.runLater(() -> {
                        glassPaneReplacement.setVisible(empty);
                        glassPaneLHS.setText(Config.getString("editor.completion.noMatch"));
                    });
                });
            }
        }
    }
    
    /**
     * A glass pane which displays a  message.
     * if not working, then there is no match.
     */
    /*
    class CodeCompleteGlassPane extends JComponent
    {
        String message = Config.getString("editor.completion.noMatch");
        boolean isWorking = false;
        
        public void setWorking(boolean isW){
            this.isWorking = isW;
            if (isWorking){
                this.message = Config.getString("editor.completion.working");
            } else {
                this.message = Config.getString("editor.completion.noMatch");
            }
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Color origColor = g.getColor();
            Font origFont = g.getFont();
            g.setColor(msgTextColor);
            g.setFont(origFont.deriveFont(20f));
            if (isWorking){
                g.drawString(this.message, 380, 60);
            }else{
                g.drawString(this.message, 30, 60);
            }
            g.setColor(origColor);
            g.setFont(origFont);
        }
    }
    */

    private static Comparator<AssistContent> getComparator()
    {
        return Comparator.comparing(AssistContent::getName)
                .thenComparing(AssistContent::getKind)
                .thenComparing(AssistContent::getParams,
                    Utility.listComparator(Comparator.comparing(ParamInfo::getQualifiedType)));
    }

}
