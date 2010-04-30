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

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.editor.EditorManager;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefPanelListener;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;

/**
 * A PrefPanel subclass to allow the user to interactively edit
 * editor settings
 *
 * @author  Michael Kolling
 */
public class EditorPrefPanel extends JPanel implements PrefPanelListener
{
    private JTextField editorFontField;
    private JCheckBox hilightingBox;
    private JCheckBox autoIndentBox;
    private JCheckBox lineNumbersBox;
    private JCheckBox makeBackupBox;
    private JCheckBox matchBracketsBox;
    private ScopeHighlightingPrefDisplay scopeHighlightingPrefDisplay;

    /**
     * Setup the UI for the dialog and event handlers for the buttons.
     */
    public EditorPrefPanel()
    {
        scopeHighlightingPrefDisplay=new ScopeHighlightingPrefDisplay();
        setBorder(BlueJTheme.generalBorder);

        JComponent editorPanel = new DBox(DBoxLayout.Y_AXIS, 0, BlueJTheme.componentSpacingSmall, 0.5f);
        {
            String editorTitle = Config.getString("prefmgr.edit.editor.title");
            editorPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(editorTitle),
                    BlueJTheme.generalBorder));
            editorPanel.setAlignmentX(LEFT_ALIGNMENT);            
            JPanel topPanel=new JPanel(new GridLayout(3,2,0,0));
            
            JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            {
                fontPanel.add(new JLabel(Config.getString("prefmgr.edit.editorfontsize")+"  "));
                editorFontField = new JTextField(4);
                fontPanel.add(editorFontField);
            }
            topPanel.add(fontPanel);
            autoIndentBox = new JCheckBox(Config.getString("prefmgr.edit.autoindent"));
            topPanel.add(autoIndentBox);
            
            hilightingBox = new JCheckBox(Config.getString("prefmgr.edit.usesyntaxhilighting"));
            topPanel.add(hilightingBox);
            
            makeBackupBox = new JCheckBox(Config.getString("prefmgr.edit.makeBackup"));
            topPanel.add(makeBackupBox);

            lineNumbersBox = new JCheckBox(Config.getString("prefmgr.edit.displaylinenumbers"));
            topPanel.add(lineNumbersBox);
            
            matchBracketsBox= new JCheckBox(Config.getString("prefmgr.edit.matchBrackets"));
            topPanel.add(matchBracketsBox);
            
            JPanel bottomPanel=new JPanel(new GridLayout(1,2,0,0));
            bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(Config.getString("prefmgr.edit.colortransparency")),
                    BlueJTheme.generalBorder));
            //colour scope highlighter slider
            JPanel sliderPanel=new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
            {               
                sliderPanel.add(scopeHighlightingPrefDisplay.getHighlightStrengthSlider());
            }
              
            bottomPanel.add(sliderPanel);            
            bottomPanel.add(scopeHighlightingPrefDisplay.getColourPalette());
                        
            editorPanel.add(topPanel);
            editorPanel.add(Box.createVerticalGlue());
            editorPanel.add(bottomPanel);
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
        MoeSyntaxView.resetSyntaxHighlighting();
        PrefMgr.setFlag(PrefMgr.AUTO_INDENT, autoIndentBox.isSelected());
        PrefMgr.setFlag(PrefMgr.LINENUMBERS, lineNumbersBox.isSelected());
        PrefMgr.setFlag(PrefMgr.MAKE_BACKUP, makeBackupBox.isSelected());
        PrefMgr.setFlag(PrefMgr.MATCH_BRACKETS, matchBracketsBox.isSelected());
        PrefMgr.setScopeHighlightStrength(scopeHighlightingPrefDisplay.getStrengthValue());
        EditorManager.getEditorManager().refreshAll();
    }

}
