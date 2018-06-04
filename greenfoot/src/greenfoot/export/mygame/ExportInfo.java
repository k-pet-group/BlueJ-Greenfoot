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
    private String exportName;
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

    public Image getImage()
    {
        return image;
    }

    public void setImage(Image image)
    {
        this.image = image;
    }

    public String getExportName()
    {
        return exportName;
    }

    public void setExportName(String exportName)
    {
        this.exportName = exportName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public boolean isKeepSavedScreenshot()
    {
        return keepSavedScreenshot;
    }

    public void setKeepSavedScreenshot(boolean keepSavedScreenshot)
    {
        this.keepSavedScreenshot = keepSavedScreenshot;
    }
}
