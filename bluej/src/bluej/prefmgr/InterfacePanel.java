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

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * "Interface" preference panel. Settings for what to show (teamwork, testing tools etc)
 * and interface language.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public class InterfacePanel extends VBox
        implements PrefPanelListener
{
    private static final String toolkitDir = "bluej.javame.toolkit.dir";

    private CheckBox showTestBox;
    private CheckBox showTeamBox;
    
    private ArrayList<String> allLangsInternal;
    private ComboBox langDropdown;
    
    private CheckBox accessibility;
    
    public InterfacePanel()
    {
        JavaFXUtil.addStyleClass(this, "prefmgr-pref-panel");
        
        if(!Config.isGreenfoot()) {
            Pane testPanel = new HBox();
            JavaFXUtil.addStyleClass(testPanel, "prefmgr-interface-checkboxes");
            {
                showTestBox = new CheckBox(Config.getString("prefmgr.misc.showTesting"));
                testPanel.getChildren().add(showTestBox);

                showTeamBox = new CheckBox(Config.getString("prefmgr.misc.showTeam"));
                testPanel.getChildren().add(showTeamBox);
            }
            getChildren().add(PrefMgrDialog.headedVBox("prefmgr.misc.tools.title", Arrays.asList(testPanel)));
        }
        
        List<Node> langPanel = new ArrayList<>();
        {
            
            {
                
                
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

                langDropdown = new ComboBox(FXCollections.observableArrayList(langs));
            }
            langPanel.add(PrefMgrDialog.labelledItem("prefmgr.interface.language", langDropdown));
            
            Label t = new Label(Config.getString("prefmgr.interface.language.restart"));
            langPanel.add(t);
        }
        getChildren().add(PrefMgrDialog.headedVBox("prefmgr.interface.language.title", langPanel));        
        
        accessibility = new CheckBox(Config.getString("prefmgr.accessibility.support"));
        getChildren().add(PrefMgrDialog.headedVBox("prefmgr.accessibility.title", Arrays.asList(accessibility)));
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
        langDropdown.getSelectionModel().select(curLangIndex);
        
        accessibility.setSelected(PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT));
    }

    @Override
    public void commitEditing()
    {
        if(!Config.isGreenfoot()) {
            PrefMgr.setFlag(PrefMgr.SHOW_TEST_TOOLS, showTestBox.isSelected());
            PrefMgr.setFlag(PrefMgr.SHOW_TEAM_TOOLS, showTeamBox.isSelected());

            SwingUtilities.invokeLater(() -> {
                PkgMgrFrame.updateTestingStatus();
                PkgMgrFrame.updateTeamStatus();
            });
        }
        
        Config.putPropString("bluej.language", allLangsInternal.get(langDropdown.getSelectionModel().getSelectedIndex()));
        
        PrefMgr.setFlag(PrefMgr.ACCESSIBILITY_SUPPORT, accessibility.isSelected());
    }
    
    @Override
    public void revertEditing()
    {
    }
}
