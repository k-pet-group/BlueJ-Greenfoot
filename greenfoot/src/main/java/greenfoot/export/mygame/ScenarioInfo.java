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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Holds various information about a scenario that is persisted when the scenario is saved.
 * @see ExportInfo
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class ScenarioInfo
{
    private String title;
    private String shortDescription;
    private String longDescription;
    private List<String> tags = Collections.emptyList();
    private String url;
    private boolean includeSource;
    private boolean locked;
    private boolean hideControls;
    
    private static final String PUBLISH_TITLE = "publish.title";
    private static final String PUBLISH_SHORT_DESC = "publish.shortDesc";
    private static final String PUBLISH_LONG_DESC = "publish.longDesc";
    private static final String PUBLISH_URL = "publish.url";
    private static final String PUBLISH_TAGS = "publish.tags";
    private static final String PUBLISH_HAS_SOURCE = "publish.hasSource";
    private static final String PUBLISH_LOCKED = "publish.locked";

    /**
     * Construct a scenario info object without loading any properties.
     */
    public ScenarioInfo()
    {
        // Nothing to do.
    }

    /**
     * Construct a scenario info object by copying fields from another.
     */
    public ScenarioInfo(ScenarioInfo src)
    {
        title = src.title;
        shortDescription = src.shortDescription;
        longDescription = src.longDescription;
        tags = new ArrayList<String>(src.tags);
        url = src.url;
        includeSource = src.includeSource;
        locked = src.locked;
    }

    /**
     * Construct a scenario info object and load related properties.
     *
     * @param properties The project's properties.
     */
    public ScenarioInfo(Properties properties)
    {
        load(properties);
    }

    /**
     * Sets the exported scenario's title, if it is not null, after trimming.
     * @param title  The title as String. Could be null.
     */
    public void setTitle(String title)
    {
        if (title != null)
        {
            this.title = title.trim();
        }
    }

    /**
     * Returns the scenario title.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Sets the scenario's short description.
     */
    public void setShortDescription(String shortDesc)
    {
        shortDescription = shortDesc;
    }

    /**
     * Returns the scenario short description.
     */
    public String getShortDescription()
    {
        return shortDescription;
    }

    /**
     * Sets the scenario description.
     */
    public void setLongDescription(String longDesc)
    {
        longDescription = longDesc;
    }

    /**
     * Returns the scenario description.
     */
    public String getLongDescription()
    {
        return longDescription;
    }

    /**
     * Sets the list of the scenario's tags.
     * @param tags  A list of strings each represents an individual tag.
     */
    public void setTags(List<String> tags)
    {
        this.tags = tags;
    }

    /**
     * Returns a list of the scenario's tags.
     */
    public List<String> getTags()
    {
        return tags;
    }

    /**
     * Sets the scenario's URL.
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * Returns the scenario's URL.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Returns True if the source code is included, false otherwise.
     */
    public boolean isIncludeSource()
    {
        return includeSource;
    }

    /**
     * Sets if the source code should be included or not.
     * @param includeSource True if the source code should be included.
     */
    public void setIncludeSource(boolean includeSource)
    {
        this.includeSource = includeSource;
    }

    /**
     * Returns True if the scenario is locked, false otherwise.
     */
    public boolean isLocked()
    {
        return locked;
    }

    /**
     * Sets if the scenario is locked or not.
     * @param locked True if the scenario should be locked.
     */
    public void setLocked(boolean locked)
    {
        this.locked = locked;
    }

    /**
     * Returns True if the exported scenario's controls are hidden, false otherwise.
     */
    public boolean isHideControls()
    {
        return hideControls;
    }

    /**
     * Sets True if the exported scenario's controls should be hidden, false otherwise.
     */
    public void setHideControls(boolean hideControls)
    {
        this.hideControls = hideControls;
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
    }
}
