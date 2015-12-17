/*
 This file is part of the BlueJ program.
 Copyright (C) 2015  Michael Kolling and John Rosenberg

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
package bluej.editor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import javax.swing.*;

import bluej.Config;
import bluej.pkgmgr.TabbedEditorWindow;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.editor.moe.MoeEditor;
import bluej.pkgmgr.Project;

/**
 * A window which contains all the (Swing) editor tabs for Java
 */
@OnThread(Tag.Swing)
public class SwingTabbedEditor implements TabbedEditorWindow
{
    /** The actual GUI window */
    private final JFrame window;
    /** The tabbed pane inside the window, containing all the tabs */
    private final JTabbedPane tabPane;
    /** The project which this window is associated with */
    private final Project project;
    // We need to maintain a bidirectional mapping from tab/tab components/menubar to editor.
    /** Maps tab content to editor */
    private final IdentityHashMap<Component, MoeEditor> panelToEditor = new IdentityHashMap<>();
    /** Maps editor to tab content */
    private final IdentityHashMap<MoeEditor, Component> editorToPanel = new IdentityHashMap<>();
    /** Maps editor to tab header panel */
    private final IdentityHashMap<MoeEditor, HeaderPanel> editorToHeader = new IdentityHashMap<>();
    /** Maps editor to menu bar */
    private final IdentityHashMap<MoeEditor, MenuInfo> menuBars = new IdentityHashMap<>();
    /** The size to load up with.  May be null */
    private Rectangle startSize;

    /**
     * Constructs a SwingTabbedEditor for the given project
     */
    public SwingTabbedEditor(Project project, Rectangle startSize)
    {
        window = new JFrame(project.getProjectName() + " - Java");
        this.startSize = startSize;
        tabPane = new JTabbedPane();
        // Create some actions for selecting tabs, which will later be bound to Ctrl+1, etc
        for (int i = 1; i <= 9; i++)
        {
            tabPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(Character.forDigit(i, 10), Config.isMacOS() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK), "selectTab" + i);
            final int tabIndex = i - 1;
            tabPane.getActionMap().put("selectTab" + i, new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (tabPane.getTabCount() > tabIndex)
                        tabPane.setSelectedIndex(tabIndex);
                }
            });
        }
        
        window.add(tabPane);

        // When the selected tab changes, we must set the menubar, and notify the editors
        // as to whether they are visible or not:
        tabPane.addChangeListener(evt -> {
            MoeEditor editor = panelToEditor.get(tabPane.getSelectedComponent());
            if (menuBars.containsKey(editor))
                window.setJMenuBar(menuBars.get(editor).menuBar);
            else
                window.setJMenuBar(null);
            updateTitle();
            for (MoeEditor ed : editorToPanel.keySet())
            {
                ed.notifyVisibleTab(ed == editor && window.isFocused());
            }
        });
        
        WindowFocusListener windowFocusListener = new WindowFocusListener() {
            
            @Override
            public void windowLostFocus(WindowEvent e)
            {
                for (MoeEditor ed : editorToPanel.keySet())
                    ed.notifyVisibleTab(false);
            }
            
            @Override
            public void windowGainedFocus(WindowEvent e)
            {
                MoeEditor editor = panelToEditor.get(tabPane.getSelectedComponent());
                for (MoeEditor ed : editorToPanel.keySet())
                    ed.notifyVisibleTab(ed == editor);
            }
        };
        
        window.addWindowFocusListener(windowFocusListener);

        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // If the window is closed, close all remaining tabs:
        window.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent ev)
            {
                super.windowClosed(ev);
                List<MoeEditor> editors = new ArrayList<>(editorToPanel.keySet());
                editors.forEach(e -> setEditorVisible(false, e));
                project.removeSwingTabbedEditor(SwingTabbedEditor.this);
            }
        });
        
        this.project = project;
    }

    private void updateTitle()
    {
        final MoeEditor editor = panelToEditor.get(tabPane.getSelectedComponent());
        if (editor != null)
            window.setTitle(editor.getTitle() + " - " + project.getProjectName());
        else
            window.setTitle(project.getProjectName());
        project.updateSwingTabbedEditorDestinations();
    }

    /**
     * Sets the given editor visible or not.
     * @param visible If true, makes tab for MoeEditor if needed, and shows the tab and the window.
     *                If false, closes the tab.
     * @param editor The editor in question
     */
    public void setEditorVisible(boolean visible, MoeEditor editor)
    {
        if (editor == null)
            throw new IllegalArgumentException("Cannot show null editor");

        if (visible)
        {
            // Must ask for editorTab before calling pack(), otherwise pack makes window zero-size:
            
            Component editorTab = getMoeEditorTab(editor);
            
            if (!window.isShowing())
            {
                window.pack();
                // Default:
                window.setSize(700, 700);
                // Override with saved if available:
                if (startSize != null)
                {
                    window.setLocation(startSize.x, startSize.y);
                    window.setSize(startSize.width, startSize.height);
                }
                window.setVisible(true);
                // On Windows7, there was a problem with the window rendering white on re-show,
                // until it was resized.  As a work-around, we do the resize ourselves:
                EventQueue.invokeLater(() -> {
                    // We need a resize but don't really want to resize, just need one pixel change.
                    // If window size is odd, -1, if it's even then +1.  That way, repeated re-shows
                    // will not continually grow or shrink window:
                    if (window.getWidth() % 2 == 0)
                        window.setSize(window.getWidth() + 1, window.getHeight());
                    else
                        window.setSize(window.getWidth() - 1, window.getHeight());
                });
            }
            bringToFront();
            tabPane.setSelectedComponent(editorTab);
        }
        else
        {
            Component tab = editorToPanel.get(editor);
            tabPane.remove(tab);
            panelToEditor.remove(tab);
            editorToPanel.remove(editor);
            editorToHeader.remove(editor);
            editor.setParent(null);
            if (tabPane.getTabCount() == 0)
            {
                window.dispose();
                project.removeSwingTabbedEditor(this);
            }
            editorToHeader.values().forEach(HeaderPanel::updateMoveNew);
        }
    }

    /**
     * Fetches the editor tab component corresponding to the given editor.  If it doesn't
     * currently exist, creates it and adds it to the tab pane.
     */
    private Component getMoeEditorTab(final MoeEditor editor)
    {
        // If you try to put MoeEditor directly into the JTabbedPane as a tab, it will not work.
        // I am not 100% sure why, but presumably we are doing something complex with the JPanel
        // which JTabbedPane does not support (e.g. JTabbedPane says don't call setVisible on a tab;
        // we override it so this shouldn't be an issue, but there is presumably some other similar
        // problem).  Wrapping the MoeEditor in a further JPanel seems to solve the issue, even if
        // it does feel a bit hacky.
        
        if (!editorToPanel.containsKey(editor))
        {       
            JPanel tmp = new JPanel();
            tmp.setLayout(new BorderLayout());
            tmp.add(editor,BorderLayout.CENTER);
            panelToEditor.put(tmp, editor);
            editorToPanel.put(editor, tmp);
            editor.setParent(this);
            HeaderPanel header = new HeaderPanel(editor);
            editorToHeader.put(editor, header);
            tabPane.addTab(editor.getTitle(), tmp);
            tabPane.setTabComponentAt(tabPane.indexOfComponent(tmp), header);
            editorToHeader.values().forEach(HeaderPanel::updateMoveNew);
        }
        return editorToPanel.get(editor);
    }

    /**
     * Brings the window to the front.
     */
    public void bringToFront()
    {
        window.toFront();
        Utility.bringToFront(window);
        MoeEditor editor = panelToEditor.get(tabPane.getSelectedComponent());
        editor.getSourcePane().requestFocusInWindow();
        editor.getSourcePane().setVisible(true);
    }

    /**
     * Delete for Project.scheduleCompilation
     */
    public void scheduleCompilation(boolean immediate)
    {
        project.scheduleCompilation(immediate);
    }

    /**
     * Gets the menu bar for the given editor.
     */
    public JMenuBar getJMenuBar(MoeEditor e)
    {
        return menuBars.get(e).menuBar;
    }

    /**
     * Sets the menu bar for the given editor.
     */
    public void setJMenuBar(MoeEditor e, JMenuBar menuBar, JMenu moveMenu)
    {
        menuBars.put(e, new MenuInfo(menuBar, moveMenu));
    }

    /**
     * Sets the tab title for the given editor.
     */
    public void setTitle(MoeEditor moeEditor, String title)
    {
        editorToHeader.get(moeEditor).setTitle(title);
        updateTitle();
    }

    public void updateMoveDestinations()
    {
        editorToHeader.values().forEach(HeaderPanel::updateMoveMenuDestinations);
    }

    /**
     * A component for the tab header; contains a title bale, and a close button
     */
    private class HeaderPanel extends JPanel
    {
        private final JLabel label;
        private final JMenu contextMoveMenu;
        private final JMenuItem contextMoveNew;
        private final JMenuItem mainMoveNew;
        private final MoeEditor editor;

        public HeaderPanel(MoeEditor editor)
        {
            this.editor = editor;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(false);
            label = new JLabel(editor.getTitle());
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            label.setOpaque(false);
            label.setInheritsPopupMenu(true);
            add(label);
            
            CloseIcon closeIcon = new CloseIcon();
            
            JButton close = new JButton(closeIcon);
            close.setBorder(null);
            close.setBorderPainted(false);
            close.setOpaque(false);
            close.setContentAreaFilled(false);
            close.addActionListener(e -> setEditorVisible(false, editor));
            close.setInheritsPopupMenu(true);
            add(close);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (SwingUtilities.isMiddleMouseButton(e))
                    {
                        setEditorVisible(false, editor);
                    }
                    else
                    {
                        setEditorVisible(true, editor);
                    }
                }
            });
            // For close, all buttons (including middle) should close tab:
            close.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    setEditorVisible(false, editor);
                }
            });

            JPopupMenu contextMenu = new JPopupMenu();
            JMenuItem closeItem = new JMenuItem(Config.getString("editor.closeLabel"));
            closeItem.addActionListener(e -> {
                setEditorVisible(false, editor);
            });
            contextMenu.add(closeItem);

            contextMoveMenu = new JMenu(Config.getString("frame.classmenu.move"));
            menuBars.get(editor).moveMenu.setText(Config.getString("frame.classmenu.move"));
            contextMoveNew = new JMenuItem(Config.getString("frame.classmenu.move.new"));
            mainMoveNew = new JMenuItem(Config.getString("frame.classmenu.move.new"));
            final ActionListener moveToNew = e -> {
                final SwingTabbedEditor newWindow = project.createNewSwingTabbedEditor();
                moveTabTo(editor, newWindow);
            };
            contextMoveNew.addActionListener(moveToNew);
            mainMoveNew.addActionListener(moveToNew);
            updateMoveNew();
            updateMoveMenuDestinations();
            contextMenu.add(contextMoveMenu);
            this.setComponentPopupMenu(contextMenu);
        }
        
        public void setTitle(String title)
        {
            label.setText(title);
        }

        public void updateMoveNew()
        {
            contextMoveNew.setEnabled(tabPane.getTabCount() > 1);
            mainMoveNew.setEnabled(tabPane.getTabCount() > 1);
        }

        public void updateMoveMenuDestinations()
        {
            contextMoveMenu.removeAll();
            contextMoveMenu.add(contextMoveNew);
            JMenu mainMoveMenu = menuBars.get(editor).moveMenu;
            mainMoveMenu.removeAll();
            mainMoveMenu.add(mainMoveNew);
            updateMoveNew();
            List<SwingTabbedEditor> editorWindows = project.getAllSwingTabbedEditors();

            editorWindows.stream().filter(ste -> ste != SwingTabbedEditor.this).forEach(ste -> {
                if (contextMoveMenu.getItemCount() == 1)
                {
                    // Add a divider:
                    contextMoveMenu.addSeparator();
                }
                if (mainMoveMenu.getItemCount() == 1)
                {
                    mainMoveMenu.addSeparator();
                }

                JMenuItem contextMoveItem = new JMenuItem(Config.getString("frame.classmenu.move.existing") + ": " + ste.getTitle());
                JMenuItem mainMoveItem = new JMenuItem(Config.getString("frame.classmenu.move.existing") + ": " + ste.getTitle());
                final ActionListener moveNew = e -> {
                    moveTabTo(editor, ste);
                };
                contextMoveItem.addActionListener(moveNew);
                mainMoveItem.addActionListener(moveNew);
                contextMoveMenu.add(contextMoveItem);
                mainMoveMenu.add(mainMoveItem);
            });
        }
    }

    private String getTitle()
    {
        return window.getTitle();
    }

    public void moveTabTo(MoeEditor editor, SwingTabbedEditor window)
    {
        // Copy across the menu bar first, before we remove it:
        final MenuInfo menuInfo = menuBars.get(editor);
        window.setJMenuBar(editor, menuInfo.menuBar, menuInfo.moveMenu);
        setEditorVisible(false, editor);
        window.setEditorVisible(true, editor);
        window.bringToFront();
    }

    /**
     * A hand-drawn 16x16 black cross icon for the close button.
     */
    private class CloseIcon implements Icon
    {
        private int width = 16;
        private int height = 16;
        

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2d = (Graphics2D) g;
            height = g2d.getFontMetrics(g2d.getFont()).getHeight();
            width = height;
            int offset = 5;
            
            g2d.setColor(Color.BLACK);
            
            g2d.drawLine(x + offset , y + offset, width - offset , height - offset ); // draw "\"
            g2d.drawLine(width - offset, y + offset, x + offset , height - offset); // draw "/"

            g2d.dispose();
        }

        /**
         * @return the width
         */
        @Override
        public int getIconWidth()
        {
            return width;
        }

        /**
         * @return the height
         */
        @Override
        public int getIconHeight()
        {
            return height;
        }
    }

    private static class MenuInfo
    {
        public final JMenuBar menuBar;
        public final JMenu moveMenu;

        public MenuInfo(JMenuBar menuBar, JMenu moveMenu)
        {
            this.menuBar = menuBar;
            this.moveMenu = moveMenu;
        }
    }

    public int getX()
    {
        return window.getX();
    }

    public int getY()
    {
        return window.getY();
    }

    public int getWidth()
    {
        return window.getWidth();
    }

    public int getHeight()
    {
        return window.getHeight();
    }
}
