/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017 Michael Kölling and John Rosenberg
 
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
package bluej.stride.generic;


import bluej.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds a list of all the blocks, and information about them (e.g. shortcut key, description)
 */
public abstract class FrameDictionary<CATEGORY>
{
    // An Entry about a particular block:
    public static class Entry<CATEGORY>
    {
        private final FrameFactory<? extends Frame> factory;
        private final List<Character> shortcutKeys;
        private final String name;
        private final String description;
        private final CATEGORY category;
        private final boolean validOnSelection;
        private final boolean showingInCatalogue;

        public Entry(char shortcutKey, FrameFactory<? extends Frame> factory, boolean validOnSelection,
                CATEGORY category, String nameLabel, String descriptionLabel)
        {
            this(Arrays.asList(shortcutKey), factory, validOnSelection, category, nameLabel, descriptionLabel, true);
        }

        public Entry(char shortcutKey, FrameFactory<? extends Frame> factory, boolean validOnSelection,
                     CATEGORY category, String nameLabel, String descriptionLabel, boolean showingInCatalogue)
        {
            this(Arrays.asList(shortcutKey), factory, validOnSelection, category, nameLabel, descriptionLabel, showingInCatalogue);
        }

        private Entry(List<Character> shortcutKeys, FrameFactory<? extends Frame> factory, boolean validOnSelection,
                      CATEGORY category, String nameLabel, String descriptionLabel, boolean showingInCatalogue)
        {
            this.shortcutKeys = new ArrayList<>(shortcutKeys);
            this.factory = factory;
            this.validOnSelection = validOnSelection;
            this.category = category;
            this.name = Config.getString(nameLabel);
            this.description = Config.getString(descriptionLabel);
            this.showingInCatalogue = showingInCatalogue;
        }

        public boolean inCategory(CATEGORY c) { return category.equals(c); }

        public boolean hasShortcut(char k) { return shortcutKeys.contains(k); }
        
        public List<Character> getReadOnlyShortcuts() { return Collections.unmodifiableList(shortcutKeys); }

        public FrameFactory<? extends Frame> getFactory() { return factory; }

        public String getName() { return name; }

        public String getDescription() { return description; }
        
        public CATEGORY getCategory() { return category; }
        
        public String getCategoryName() { return category.toString(); }

        public Class<? extends Frame> getBlockClass() { return factory.getBlockClass(); }

        public boolean isValidOnSelection() { return validOnSelection; }

        public boolean isShowingInCatalogue() { return showingInCatalogue; }
        
        public String getShortcuts()
        {
            StringBuilder builder = new StringBuilder();
            builder.append(shortcutKeys.get(0));
            for (int i = 1; i < shortcutKeys.size(); i++) {
                builder.append(", ").append(shortcutKeys.get(i));
            }
            return builder.toString();
        }
    }
    
    private final List<Entry<CATEGORY>> entries;
    
    protected FrameDictionary(List<Entry<CATEGORY>> entries)
    {
        this.entries = entries;
    }
    
    public List<Entry<CATEGORY>> getAllBlocks()
    {
        return entries;
    }
    
    public List<Entry<CATEGORY>> getBlocksInCategory(CATEGORY c)
    {
        return entries.stream().filter(e -> e.inCategory(c)).collect(Collectors.toList());
    }
    
    /**
     * Return categories
     */
    public abstract CATEGORY[] getCategories();
    public abstract String[] getCategoryNames();
    
    /**
     * Returns empty list if no block for that key
     */
    public List<Entry<CATEGORY>> getFramesForShortcutKey(char k)
    {
        return entries.stream().filter(e -> e.hasShortcut(k)).collect(Collectors.toList());
    }
    
}
