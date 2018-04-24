/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2018  Poul Henriksen and Michael Kolling
 
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javafx.scene.image.Image;

/**
 * Holds various information about a scenario.
 * 
 * @author Davin McCall
 */
public class ScenarioInfo
{
    private String title;
    private String shortDescription;
    private String longDescription;
    private String updateDescription;
    private List<String> tags;
    private String url;
    private boolean includeSource;
    private boolean locked;
    private boolean update = false;
    
    private static final String PUBLISH_TITLE = "publish.title";
    private static final String PUBLISH_SHORT_DESC = "publish.shortDesc";
    private static final String PUBLISH_LONG_DESC = "publish.longDesc";
    private static final String PUBLISH_URL = "publish.url";
    private static final String PUBLISH_TAGS = "publish.tags";
    private static final String PUBLISH_HAS_SOURCE = "publish.hasSource";
    private static final String PUBLISH_LOCKED = "publish.locked";
    private static final String PUBLISH_UPDATE_DESC = "publish.updateDesc";

    // Fields which will not be saved in the scenario local properties,
    // but will be passed to the exporter. Some of them may be saved in
    // the exported version, depending on the exported version type.
    private Image image;
    private String exportName;
    private String userName;
    private String password;
    private boolean hideControls;
    private boolean keepSavedScreenshot;

    /**
     * Construct a scenario info object without loading any properties.
     */
    public ScenarioInfo() { }

    /**
     * Construct a scenario info object and load related properties.
     *
     * @param properties The project's properties.
     */
    public ScenarioInfo(Properties properties)
    {
        load(properties);
    }

    public void setTitle(String title)
    {
        if (title != null)
        {
            this.title = title.trim();
        }
    }
    
    public String getTitle()
    {
        return title;
    }

    public void setShortDescription(String shortDesc)
    {
        shortDescription = shortDesc;
    }
    
    public String getShortDescription()
    {
        return shortDescription;
    }
    
    public void setLongDescription(String longDesc)
    {
        longDescription = longDesc;
    }
    
    public String getLongDescription()
    {
        return longDescription;
    }
    
    public void setTags(List<String> tags)
    {
        this.tags = tags;
    }
 
    public List<String> getTags()
    {
        return tags;
    }
    
    public void setUrl(String url)
    {
        this.url = url;
    }
    
    public String getUrl()
    {
        return url;
    }
    
    public void setIncludeSource(boolean includeSource)
    {
        this.includeSource = includeSource;
    }
    
    public boolean isLocked()
    {
        return locked;
    }
    
    public void setLocked(boolean locked)
    {
        this.locked = locked;
    }
    
    public boolean isIncludeSource()
    {
        return includeSource;
    }
    
    /**
     * Stores the scenario information into the specified project properties.
     */
    public void store(Properties properties)
    {
        properties.setProperty(PUBLISH_TITLE, getTitle());
        if (getShortDescription() != null)
        {
            properties.setProperty(PUBLISH_SHORT_DESC, getShortDescription());
        }
        if (getLongDescription()!= null)
        {
            properties.setProperty(PUBLISH_LONG_DESC, getLongDescription());
        }
        properties.setProperty(PUBLISH_URL, getUrl());
        properties.setProperty(PUBLISH_TAGS, getTagsAsString());
        properties.setProperty(PUBLISH_HAS_SOURCE, Boolean.toString(isIncludeSource()));
        properties.setProperty(PUBLISH_LOCKED, Boolean.toString(isLocked()));
        if (getUpdateDescription() != null)
        {
            properties.setProperty(PUBLISH_UPDATE_DESC, getUpdateDescription());
        }
    }

    private String getTagsAsString()
    {
        return String.join(" ", getTags());
    }

    /**
     * Attempts to load previously saved ScenarioInfo for this project.
     * 
     * @return true if it found and loaded the stored values.
     */
    public boolean load(Properties properties)
    {
        //if it is a saved scenario it should have at least a title set
        if (properties.getProperty(PUBLISH_TITLE) == null)
        {
            return false;
        }

        setTitle(properties.getProperty(PUBLISH_TITLE));
        setShortDescription(properties.getProperty(PUBLISH_SHORT_DESC));
        setLongDescription(properties.getProperty(PUBLISH_LONG_DESC));
        setUrl(properties.getProperty(PUBLISH_URL));

        List<String> tagList = new LinkedList<>();
        String tags = properties.getProperty(PUBLISH_TAGS);
        if (tags != null)
        {
            Collections.addAll(tagList, tags.split(" "));
        }
        setTags(tagList);

        setIncludeSource(Boolean.parseBoolean(properties.getProperty(PUBLISH_HAS_SOURCE, "false")));
        setLocked(Boolean.parseBoolean(properties.getProperty(PUBLISH_LOCKED, "true")));
        setUpdateDescription(properties.getProperty(PUBLISH_UPDATE_DESC));
        return true;
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

    public boolean isHideControls()
    {
        return hideControls;
    }

    public void setHideControls(boolean hideControls)
    {
        this.hideControls = hideControls;
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
