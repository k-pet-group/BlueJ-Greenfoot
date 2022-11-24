/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018  Poul Henriksen and Michael Kolling
 
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
package greenfoot.export.mygame;

import javafx.scene.image.Image;

/**
 * Details about a scenario needed for export. The precise details available will depend on the
 * export function (eg screenshot is only available for web export).
 * 
 * @author Davin McCall
 */
public class ExportInfo extends ScenarioInfo
{
    // Fields which will not be saved in the scenario local properties,
    // but will be passed to the exporter. Some of them may be saved in
    // the exported version, depending on the exported version type.
    private boolean update = false;
    private String updateDescription;
    private Image image;
    private String exportFileName;
    private String userName;
    private String password;
    private boolean keepSavedScreenshot;

    /**
     * Construct an ExportInfo with information based on the ScenarioInfo provided.
     */
    public ExportInfo(ScenarioInfo src)
    {
        super(src);
    }
    
    /**
     * If we're updating an existing scenario, return a description of the update.
     * @see #isUpdate()
     */
    public String getUpdateDescription()
    {
        return updateDescription;
    }

    /**
     * Set the update description (if this is an update).
     * @param updateDescription   The update description provided by the user.
     * @see #setUpdate(boolean)
     */
    public void setUpdateDescription(String updateDescription)
    {
        this.updateDescription = updateDescription;
    }

    /**
     * Check whether this is (as far as we're aware) an update of an existing scenario.
     * If it is {@link #getUpdateDescription()} will return a description of the update.
     */
    public boolean isUpdate()
    {
        return update;
    }

    /**
     * Specify whether this is an update of an existing scenario. Also use
     * {@link #setUpdateDescription(String)} to set the update description
     * as provided by the user.
     */
    public void setUpdate(boolean update)
    {
        this.update = update;
    }

    /**
     * Returns the image that is to be used as icon for this scenario.
     */
    public Image getImage()
    {
        return image;
    }

    /**
     * Sets the image that is to be used as icon for this scenario.
     */
    public void setImage(Image image)
    {
        this.image = image;
    }

    /**
     * Returns the file name that will be exported to. Used for local export.
     */
    public String getExportFileName()
    {
        return exportFileName;
    }

    /**
     * Sets the file name that will be exported to. Used in local export.
     */
    public void setExportFileName(String exportFileName)
    {
        this.exportFileName = exportFileName;
    }

    /**
     * Returns the user name. Used for web publish.
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * Sets the user name. Used in web publish.
     */
    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    /**
     * Returns the user's password. Used for web publish.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the user's password. Used in web publish.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Returns True if the scenario's screenshot to be kept, false otherwise.
     */
    public boolean isKeepSavedScreenshot()
    {
        return keepSavedScreenshot;
    }

    /**
     * Sets True if the scenario's screenshot should be kept, false otherwise.
     */
    public void setKeepSavedScreenshot(boolean keepSavedScreenshot)
    {
        this.keepSavedScreenshot = keepSavedScreenshot;
    }
}
