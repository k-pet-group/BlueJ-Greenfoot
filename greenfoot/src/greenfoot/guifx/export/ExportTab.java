/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2015,2018  Poul Henriksen and Michael Kolling
 
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

package greenfoot.guifx.export;

import bluej.Config;
import greenfoot.export.Exporter;
import greenfoot.export.mygame.ExportInfo;
import greenfoot.export.mygame.ScenarioInfo;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * ExportTab is a superclass for all changing tabs that can appear
 * in the Export dialogue.
 *
 * @author Michael Kolling
 * @author Amjad Altadmri
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public abstract class ExportTab extends Tab
{
    protected final CheckBox lockScenario = new CheckBox(Config.getString("export.lock.label"));
    protected final CheckBox hideControls = new CheckBox(Config.getString("export.controls.label"));

    protected final ScenarioInfo scenarioInfo;
    protected final BooleanProperty validProperty = new SimpleBooleanProperty(true);

    /**
     * Create a an export tab for export to web pages.
     *
     * @param scenarioInfo The scenario info needed for different export functions.
     * @param iconName     The name of the icon file.
     */
    public ExportTab(ScenarioInfo scenarioInfo, String iconName)
    {
        setClosable(false);
        this.scenarioInfo = scenarioInfo;
        ImageView imageView = new ImageView(new Image(getClass().getClassLoader().getResourceAsStream(iconName)));
        imageView.setPreserveRatio(true);
        setGraphic(imageView);

        lockScenario.setSelected(true);
        lockScenario.setTooltip(new Tooltip(Config.getString("export.lock.description")));

        hideControls.setSelected(false);
        hideControls.setTooltip(new Tooltip(Config.getString("export.controls.description")));

        getStyleClass().add("export-tab");
    }

    /**
     * This method will be called when the user is about to export the scenario
     * with information from this tab. Will be called from the event thread
     * and will not publish until this method returns.
     * 
     * @return Whether to continue publishing. Continues if true, cancels if false.
     */
    public boolean prePublish()
    {
        updateInfoFromFields();
        return true;
    }

    /**
     * This method will be called when the scenario has been published with the
     * information from this tab.
     * 
     * @param success Whether the publish was successful
     */
    public void postPublish(boolean success)
    {
        // Nothing special to do here
    }

    /**
     * Return true if the user wants to lock the scenario.
     */
    public boolean isLockScenario()
    {
        return lockScenario.isSelected();
    }
    
    /**
     * Return true if the user wants to hide the scenario controls.
     * @return true if the hide controls checkbox is selected.
     */
    public boolean isHideControls()
    {
        return hideControls.isSelected();
    }

    /**
     * Add a shared style class for the tab contents.
     */
    protected void applySharedStyle()
    {
        getContent().getStyleClass().add("export-tab-content");
    }

    /**
     * Return the export function for the tab.
     */
    public abstract Exporter.ExportFunction getFunction();
    
    /**
     * Update stored scenario information with the current values entered in the dialog.
     * The stored scenario information is saved with the scenario, so the user does not need to
     * re-enter certain details.
     */
    protected abstract void updateInfoFromFields();

    /**
     * Updates the given ExportInfo with the current values entered in the dialog.
     */
    protected abstract ExportInfo getExportInfo();
}
