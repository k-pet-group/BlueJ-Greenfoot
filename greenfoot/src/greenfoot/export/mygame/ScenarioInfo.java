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

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Holds various information about a scenario.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class ScenarioInfo
{
    private String title;
    private String shortDescription;
    private String longDescription;
    private String updateDescription;
    private List<String> tags = Collections.emptyList();
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
        setPropertyIfNotNull(properties, PUBLISH_TITLE, getTitle());
        setPropertyIfNotNull(properties, PUBLISH_SHORT_DESC, getShortDescription());
        setPropertyIfNotNull(properties, PUBLISH_LONG_DESC, getLongDescription());
        setPropertyIfNotNull(properties, PUBLISH_URL, getUrl());
        setPropertyIfNotNull(properties, PUBLISH_TAGS, getTagsAsString());
        setPropertyIfNotNull(properties, PUBLISH_HAS_SOURCE, Boolean.toString(isIncludeSource()));
        setPropertyIfNotNull(properties, PUBLISH_LOCKED, Boolean.toString(isLocked()));
        setPropertyIfNotNull(properties, PUBLISH_UPDATE_DESC, getUpdateDescription());
    }

    /**
     * Sets a property in the properties passed, only if the value is not null.
     *
     * @param properties The properties table to be add to.
     * @param key        Property's name to be set.
     * @param value      Property's value as string. Could be null.
     */
    private void setPropertyIfNotNull(Properties properties, String key, String value)
    {
        if (value != null)
        {
            properties.setProperty(key, value);
        }
    }

    /**
     * Returns all the tags as one joined string. They are separated by spaces.
     */
    private String getTagsAsString()
    {
        List<String> tags = getTags();
        return tags == null ? null : String.join(" ", tags);
    }

    /**
     * Attempts to load previously saved ScenarioInfo for this project.
     */
    public void load(Properties properties)
    {
        setTitle(properties.getProperty(PUBLISH_TITLE, ""));
        setShortDescription(properties.getProperty(PUBLISH_SHORT_DESC, ""));
        setLongDescription(properties.getProperty(PUBLISH_LONG_DESC, ""));
        setUrl(properties.getProperty(PUBLISH_URL, ""));

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
