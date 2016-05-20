/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016 Michael Kölling and John Rosenberg
 
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


import bluej.stride.operations.FrameOperation;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds a list of all the extensions, and information about them (e.g. shortcut key, description)
 */
public abstract class ExtensionsDictionary
{
    // An Entry about a particular extension:
    public static class Entry
    {
        private final FrameOperation operation;
        private final List<Character> shortcutKeys;
        private final String name;
        private final String description;
//        private final CATEGORY category;
        private final boolean validOnSelection;

        public Entry(List<Character> shortcutKeys, FrameOperation operation, boolean validOnSelection, //CATEGORY category,
                    String name, String description)
        {
            this.shortcutKeys = new ArrayList<>(shortcutKeys);
            this.operation = operation;
//            this.category = category;
            this.name = name;
            this.description = description;
            this.validOnSelection = validOnSelection;
        }

        public Entry(char shortcutKey, FrameOperation operation, boolean validOnSelection, // CATEGORY category,
                     String name, String description)
        {
            this(Arrays.asList(shortcutKey), operation, validOnSelection, //category,
                    name, description);
        }

//        public boolean inCategory(CATEGORY c) { return category.equals(c); }

        public boolean hasShortcut(char k) { return shortcutKeys.contains(k); }

        public List<Character> getReadOnlyShortcuts() { return Collections.unmodifiableList(shortcutKeys); }

        public FrameOperation getOperation() { return operation; }

        public String getName() { return name; }

        public String getDescription() { return description; }

//        public CATEGORY getCategory() { return category; }
//
//        public String getCategoryName() { return category.toString(); }

        @OnThread(Tag.FXPlatform)
        public void activate(Frame frame) { operation.activate(frame); }

        public boolean validOnSelection() { return validOnSelection; }

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

    private final List<Entry> entries;

    protected ExtensionsDictionary(List<Entry> entries)
    {
        this.entries = entries;
    }
    
    public List<Entry> getAllExtensions()
    {
        return entries;
    }
    
//    public List<Entry<CATEGORY>> getBlocksInCategory(CATEGORY c)
//    {
//        return entries.stream().filter(e -> e.inCategory(c)).collect(Collectors.toList());
//    }
//
//    /**
//     * Return categories
//     */
//    public abstract CATEGORY[] getCategories();
//    public abstract String[] getCategoryNames();

    /**
     * Returns empty list if no block for that key
     */
    public List<Entry> getExtyensionsForShortcutKey(char k)
    {
        return entries.stream().filter(e -> e.hasShortcut(k)).collect(Collectors.toList());
    }
    
}
