/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2013,2014,2016  Michael Kolling and John Rosenberg
 
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
package bluej.prefmgr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.filefilter.DirectoryFilter;

/**
 * "Interface" preference panel. Settings for what to show (teamwork, testing tools etc)
 * and interface language.
 * 
 * @author Davin McCall
 */
public class InterfacePanel extends JPanel
        implements PrefPanelListener
{
    private static final String toolkitDir = "bluej.javame.toolkit.dir";

    private JCheckBox showTestBox;
    private JCheckBox showTeamBox;
    
    private ArrayList<String> allLangsInternal;
    private JComboBox langDropdown;
    
    private JCheckBox accessibility;
    
    public InterfacePanel()
    {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        add(box);
        
        setBorder(BlueJTheme.generalBorder);

        box.add(Box.createVerticalGlue());

        if(!Config.isGreenfoot()) {
            box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

            JPanel testPanel = new JPanel(new GridLayout(0,1,0,0));
            {
                testPanel.setBorder(BorderFactory.createCompoundBorder(
                                              BorderFactory.createTitledBorder(
                                                     Config.getString("prefmgr.misc.tools.title")),
                                              BlueJTheme.generalBorder));
                testPanel.setAlignmentX(LEFT_ALIGNMENT);

                showTestBox = new JCheckBox(Config.getString("prefmgr.misc.showTesting"));
                testPanel.add(showTestBox);

                showTeamBox = new JCheckBox(Config.getString("prefmgr.misc.showTeam"));
                testPanel.add(showTeamBox);
            }
            box.add(testPanel);

            box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        }
        
        JPanel langPanel = new JPanel();
        {
            langPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            Config.getString("prefmgr.interface.language.title")),
                            BlueJTheme.generalBorder)
                    );
            langPanel.setAlignmentX(LEFT_ALIGNMENT);
            langPanel.setLayout(new BoxLayout(langPanel, BoxLayout.Y_AXIS));
            
            Box langSelBox = new Box(BoxLayout.X_AXIS);
            {
                langSelBox.add(new JLabel(Config.getString("prefmgr.interface.language") + ":"));
                langSelBox.add(Box.createHorizontalStrut(BlueJTheme.componentSpacingSmall));

                allLangsInternal = new ArrayList<String>();
                List<String> allLangsReadable = new ArrayList<String>();
                
                for (int i = 1; ; i++) {
                    String langString = Config.getPropString("bluej.language" + i, null);
                    if (langString == null) {
                        break;
                    }
                    
                    // The format of a language string is:
                    //    internal-name:display-name:iso3cc
                    // The iso3cc (ISO country code) is optional.
                    
                    int colonIndex = langString.indexOf(':');
                    if (colonIndex == -1) {
                        continue; // don't understand this one
                    }
                    
                    int secondColon = langString.indexOf(':', colonIndex + 1);
                    if (secondColon == -1) {
                        secondColon = langString.length();
                    }
                    
                    allLangsInternal.add(langString.substring(0, colonIndex));
                    allLangsReadable.add(langString.substring(colonIndex + 1, secondColon));
                }
                
                if (allLangsInternal.isEmpty()) {
                    // Guard against modified or corrupted bluej.defs file
                    allLangsInternal.add(Config.language);
                    allLangsReadable.add(Config.language);
                }
                
                String [] langs = new String[allLangsReadable.size()];
                allLangsReadable.toArray(langs);

                langDropdown = new JComboBox(langs);
                langSelBox.add(langDropdown);
            }
            langSelBox.setAlignmentX(0.0f);
            langPanel.add(langSelBox);
            
            langPanel.add(Box.createVerticalStrut(BlueJTheme.componentSpacingSmall));
            
            JLabel t = new JLabel(Config.getString("prefmgr.interface.language.restart"));
            t.setAlignmentX(0.0f);
            langPanel.add(t);
        }
        box.add(langPanel);        
        
        JPanel accessibilityPanel = new JPanel();
        {
            accessibilityPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            Config.getString("prefmgr.accessibility.title")),
                            BlueJTheme.generalBorder)
                    );
            accessibilityPanel.setAlignmentX(LEFT_ALIGNMENT);
            accessibilityPanel.setLayout(new BoxLayout(accessibilityPanel, BoxLayout.Y_AXIS));
            
            accessibility = new JCheckBox(Config.getString("prefmgr.accessibility.support"));
            accessibilityPanel.add(accessibility);
            
            accessibilityPanel.add(Box.createVerticalStrut(BlueJTheme.componentSpacingSmall));
        }
        box.add(accessibilityPanel);
    }
    
    @Override
    public void beginEditing()
    {
        if(!Config.isGreenfoot()) {
            showTestBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_TEST_TOOLS));
            showTeamBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_TEAM_TOOLS));
        }

        String currentLang = Config.getPropString("bluej.language", "english");
        int curLangIndex = allLangsInternal.indexOf(currentLang);
        if (curLangIndex == -1) {
            curLangIndex = 0;
        }
        langDropdown.setSelectedIndex(curLangIndex);
        
        accessibility.setSelected(PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT));
    }

    @Override
    public void commitEditing()
    {
        if(!Config.isGreenfoot()) {
            PrefMgr.setFlag(PrefMgr.SHOW_TEST_TOOLS, showTestBox.isSelected());
            PrefMgr.setFlag(PrefMgr.SHOW_TEAM_TOOLS, showTeamBox.isSelected());

            PkgMgrFrame.updateTestingStatus();
            PkgMgrFrame.updateTeamStatus();
        }
        
        Config.putPropString("bluej.language", allLangsInternal.get(langDropdown.getSelectedIndex()));
        
        PrefMgr.setFlag(PrefMgr.ACCESSIBILITY_SUPPORT, accessibility.isSelected());
    }
    
    @Override
    public void revertEditing()
    {
    }
}
