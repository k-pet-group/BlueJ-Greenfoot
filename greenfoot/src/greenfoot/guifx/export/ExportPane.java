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

import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * ExportPane is a superclass for all changing panes that can appear 
 * in the Export dialogue.
 *
 * @author Michael Kolling
 * @author Amjad Altadmri
 */
public abstract class ExportPane extends Tab
{
    /**
     * An enum for the different export functions
     */
    public enum ExportFunction
    {
        Publish, Project, App;

        /**
         * Returns the export function which corresponds to the passed name.
         * In case the name doesn't match a function, returns
         * ExportFunction.Publish as a default function.
         *
         * @param name The function name
         * @return The corresponding function to the name passed,
         *         otherwise return ExportFunction.Publish
         */
        public static ExportFunction getFunction(String name)
        {
            try
            {
                return ExportFunction.valueOf(name);
            }
            catch (IllegalArgumentException ex)
            {
                return ExportFunction.Publish;
            }
        }
    }

    protected final CheckBox lockScenario = new CheckBox(Config.getString("export.lock.label"));
    protected final CheckBox hideControls = new CheckBox(Config.getString("export.controls.label"));

    /**
     * Create a an export pane for export to web pages.
     */
    public ExportPane(String iconName)
    {
        setClosable(false);
        setGraphic(new ImageView(new Image(getClass().getClassLoader().getResourceAsStream(iconName))));

        lockScenario.setSelected(true);
        lockScenario.setTooltip(new Tooltip(Config.getString("export.lock.description")));

        hideControls.setSelected(false);
        hideControls.setTooltip(new Tooltip(Config.getString("export.controls.description")));

        getStyleClass().add("export-pane");
    }

    /**
     * This method will be called when this pane is activated (about to be
     * shown/visible)
     */
    public void activated()
    {
        // Nothing special to do here
    }

    /**
     * This method will be called when the user is about to export the scenario
     * with information from this pane. Will be called from the event thread
     * and will not publish until this method returns.
     * 
     * @return Whether to continue publishing. Continues if true, cancels if false.
     */
    public boolean prePublish()
    {
        // Nothing special to do here
        return true;
    }

    /**
     * This method will be called when the scenario has been published with the
     * information from this pane.
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
    public boolean lockScenario()
    {
        return lockScenario.isSelected();
    }
    
    /**
     * Return true if the user wants to hide the scenario controls.
     * @return true if the hide controls checkbox is selected.
     */
    public boolean hideControls()
    {
        return hideControls.isSelected();
    }

    /**
     * Add a shared style class for the tab contents.
     */
    protected void applySharedStyle()
    {
        getContent().getStyleClass().add("export-pane-content");
    }

    /**
     * Return the export function for the pane.
     */
    public abstract ExportFunction getFunction();
}