// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html
// Any queries should be directed to Michael Kolling mik@mip.sdu.dk

package bluej.editor.moe;

import javax.swing.*;
import java.awt.*;

import bluej.Config;
import bluej.BlueJTheme;
import bluej.editor.EditorManager;
import bluej.prefmgr.*;

/**
 * A PrefPanel subclass to allow the user to interactively edit
 * editor settings
 *
 * @author  Michael Kolling
 * @version $Id: EditorPrefPanel.java 2747 2004-07-06 21:57:23Z mik $
 */
public class EditorPrefPanel extends JPanel implements PrefPanelListener
{
    private JTextField editorFontField;
    private JCheckBox hilightingBox;
    private JCheckBox autoIndentBox;
    private JCheckBox lineNumbersBox;
    private JCheckBox makeBackupBox;
    private JCheckBox showTestBox;
    private JCheckBox matchBracketsBox;

    /**
     * Setup the UI for the dialog and event handlers for the buttons.
     */
    public EditorPrefPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.generalBorder);

//        add(Box.createVerticalGlue());

        JPanel editorPanel = new JPanel(new GridLayout(4,2,0,0));
        {
            String editorTitle = Config.getString("prefmgr.edit.editor.title");
            editorPanel.setBorder(BorderFactory.createCompoundBorder(
                                        BorderFactory.createTitledBorder(editorTitle),
                                        BlueJTheme.generalBorder));
            editorPanel.setAlignmentX(LEFT_ALIGNMENT);

            JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            {
                fontPanel.add(new JLabel(Config.getString("prefmgr.edit.editorfontsize")));
                editorFontField = new JTextField(4);
                fontPanel.add(editorFontField);
            }
            editorPanel.add(fontPanel);
            editorPanel.add(new JLabel(" "));
            autoIndentBox = new JCheckBox(Config.getString("prefmgr.edit.autoindent"));
            editorPanel.add(autoIndentBox);
            lineNumbersBox = new JCheckBox(Config.getString("prefmgr.edit.displaylinenumbers"));
            editorPanel.add(lineNumbersBox);
            hilightingBox = new JCheckBox(Config.getString("prefmgr.edit.usesyntaxhilighting"));
            editorPanel.add(hilightingBox);
            makeBackupBox = new JCheckBox(Config.getString("prefmgr.edit.makeBackup"));
            editorPanel.add(makeBackupBox);
            matchBracketsBox= new JCheckBox(Config.getString("prefmgr.edit.matchBrackets"));
            editorPanel.add(matchBracketsBox);
        }
        add(editorPanel);

        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

        add(Box.createVerticalGlue());
        add(Box.createVerticalGlue());
        add(Box.createVerticalGlue());
    }

    public void beginEditing()
    {
        editorFontField.setText(String.valueOf(PrefMgr.getEditorFontSize()));
        hilightingBox.setSelected(PrefMgr.getFlag(PrefMgr.HILIGHTING));
        autoIndentBox.setSelected(PrefMgr.getFlag(PrefMgr.AUTO_INDENT));
        lineNumbersBox.setSelected(PrefMgr.getFlag(PrefMgr.LINENUMBERS));
        makeBackupBox.setSelected(PrefMgr.getFlag(PrefMgr.MAKE_BACKUP));
        matchBracketsBox.setSelected(PrefMgr.getFlag(PrefMgr.MATCH_BRACKETS));
    }

    public void revertEditing()
    {
    }

    public void commitEditing()
    {
        int newFontSize = 0;

        try {
            newFontSize = Integer.parseInt(editorFontField.getText());
            PrefMgr.setEditorFontSize(newFontSize);
        }
        catch (NumberFormatException nfe) { }

        PrefMgr.setFlag(PrefMgr.HILIGHTING, hilightingBox.isSelected());
        PrefMgr.setFlag(PrefMgr.AUTO_INDENT, autoIndentBox.isSelected());
        PrefMgr.setFlag(PrefMgr.LINENUMBERS, lineNumbersBox.isSelected());
        PrefMgr.setFlag(PrefMgr.MAKE_BACKUP, makeBackupBox.isSelected());
        PrefMgr.setFlag(PrefMgr.MATCH_BRACKETS, matchBracketsBox.isSelected());

        EditorManager.getEditorManager().refreshAll();
    }
}
