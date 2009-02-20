/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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

import greenfoot.core.ProjectProperties;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
    private List<String> tags;
    private String url;
    private boolean hasSource;
    private boolean isLocked;
    
    public void setTitle(String title)
    {
        this.title = title.trim();
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
    
    public void setHasSource(boolean hasSource)
    {
        this.hasSource = hasSource;
    }
    
    public boolean isLocked()
    {
        return isLocked;
    }
    
    public void setLocked(boolean locked)
    {
        this.isLocked = locked;
    }
    
    public boolean getHasSource()
    {
        return hasSource;
    }
    /**
     * Stores the scenario info locally.
     */
    public void store(ProjectProperties properties)
    {
        properties.setString("publish.title", getTitle());
        properties.setString("publish.shortDesc", getShortDescription());
        properties.setString("publish.longDesc", getLongDescription());
        properties.setString("publish.url", getUrl());
        properties.setString("publish.tags", getTagsAsString());
        properties.setBoolean("publish.hasSource", getHasSource());
        properties.setBoolean("publish.locked", isLocked());
    }

    private String getTagsAsString()
    {
        StringBuilder tags = new StringBuilder();
        List<String> tagList = getTags();
        for (Iterator<String> iterator = tagList.iterator(); iterator.hasNext();) {
            String tag = iterator.next();
            tags.append(tag);
            tags.append(" ");
        }
        return tags.toString();
    }

    /**
     * Attempts to load previously saved ScenarioInfo for this project.
     * 
     * @return true if it found and loaded the stored values.
     */
    public boolean load(ProjectProperties properties)
    {
        try {
            properties.getBoolean("publish.hasSource");
        }
        catch (NullPointerException e) {
            return false;
        }
        
        setTitle(properties.getString("publish.title"));
        setShortDescription(properties.getString("publish.shortDesc"));
        setLongDescription(properties.getString("publish.longDesc"));
        setUrl(properties.getString("publish.url"));
        String tags = properties.getString("publish.tags");
        String[] tagArray = tags.split(" ");

        List<String> tagList = new LinkedList<String>();
        for (int i = 0; i < tagArray.length; i++) {
            String string = tagArray[i];
            tagList.add(string);
        }
        setTags(tagList);
        setHasSource(properties.getBoolean("publish.hasSource"));
        setLocked(properties.getBoolean("publish.locked"));
        return true;
    }
}
