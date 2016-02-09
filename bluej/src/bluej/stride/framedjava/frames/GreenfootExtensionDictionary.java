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
package bluej.stride.framedjava.frames;


import bluej.stride.generic.ExtensionsDictionary;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameDictionary;
import bluej.stride.generic.FrameTypeCheck;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GreenfootExtensionDictionary extends ExtensionsDictionary
{
    private static GreenfootExtensionDictionary instance = new GreenfootExtensionDictionary();

    public static GreenfootExtensionDictionary getDictionary()
    {
        return instance;
    }

    private Map<String, Character> extensions = new HashMap<>();

    public static final char ABSTRACT_EXTENSION_CHAR = 'b';
    public static final char DEFAULT_EXTENSION_CHAR = 'd';//
    public static final char EXTENDS_EXTENSION_CHAR = 'e';
    public static final char IMPLEMENTS_EXTENSION_CHAR = 'i';
    public static final char THIS_EXTENSION_CHAR = 't';
    public static final char SUPER_EXTENSION_CHAR = 'u';
    public static final char THROWS_EXTENSION_CHAR = 'o';

    private GreenfootExtensionDictionary()
    {
        super(Arrays.asList(
//            new Entry<>('\n', BlankFrame.getFactory(), false, GreenfootFrameCategory.BLANK, "Blank", "A blank line"),
//            new Entry<>('y', TryFrame.getFactory(), true, GreenfootFrameCategory.TRY, "Try/catch", "Try block")
        ));

        extensions.put("catch", 'c');
        //TODO Commenting it out should not do any bug, test it
        //extensions.put("default", 'd');
        extensions.put("else", 'e');
        extensions.put("elseif", 'l');
        extensions.put("finally", 'n');
    }

//    @Override
//    public GreenfootFrameCategory[] getCategories()
//    {
//        return GreenfootFrameCategory.values();
//    }
//
//    @Override
//    public String[] getCategoryNames()
//    {
//        String[] names = new String[GreenfootFrameCategory.values().length];
//        for (int i = 0; i < names.length; i++) {
//            String str = GreenfootFrameCategory.values()[i].toString();
//            names[i] = str.substring(0, 1) + str.substring(1).toLowerCase();
//        }
//        return names;
//    }

//    private static FrameTypeCheck checkCategories(GreenfootFrameCategory... categories)
//    {
//        List<GreenfootFrameCategory> categoryList = Arrays.asList(categories);
//        return new FrameTypeCheck()
//        {
//            @Override
//            public boolean canInsert(GreenfootFrameCategory category)
//            {
//                return categoryList.contains(category);
//            }
//
//            @Override
//            public boolean canPlace(Class<? extends Frame> type)
//            {
//                return getDictionary().getAllBlocks().stream().filter(e -> categoryList.contains(e.getCategory())).anyMatch(p -> p.getBlockClass().equals(type));
//            }
//        };
//    }
    
//    public static FrameTypeCheck checkStatement()
//    {
//        return checkCategories(GreenfootFrameCategory.CONDITIONAL,
//                GreenfootFrameCategory.VAR,
//                GreenfootFrameCategory.VAR_LOCAL,
//                GreenfootFrameCategory.BLANK,
//                GreenfootFrameCategory.COMMENT,
//                GreenfootFrameCategory.LOOP,
//                GreenfootFrameCategory.ASSIGNMENT,
//                GreenfootFrameCategory.CALL,
//                GreenfootFrameCategory.BREAK,
//                GreenfootFrameCategory.SWITCH,
//                GreenfootFrameCategory.THROW,
//                GreenfootFrameCategory.TRY,
//                GreenfootFrameCategory.RETURN);
//    }
    
//    public static FrameTypeCheck checkField()
//    {
//        return checkCategories(
//                GreenfootFrameCategory.VAR,
//                GreenfootFrameCategory.VAR_FIELD,
//                GreenfootFrameCategory.BLANK,
//                GreenfootFrameCategory.COMMENT);
//    }
//
//    public static FrameTypeCheck checkConstructor()
//    {
//        return checkCategories(
//                GreenfootFrameCategory.CONSTRUCTOR,
//                GreenfootFrameCategory.COMMENT);
//    }
//
//    public static FrameTypeCheck checkClassMethod()
//    {
//        return checkCategories(
//                GreenfootFrameCategory.ABSTRACT,
//                GreenfootFrameCategory.METHOD,
//                GreenfootFrameCategory.COMMENT);
//    }
    /*
    @Override
    public boolean isValidInterfaceMethod(Class<? extends Frame> blockClass)
    {
        return getAllBlocks().stream().filter(t -> 
                GreenfootFrameCategory.VAR,
                GreenfootFrameCategory.ABSTRACT,
                GreenfootFrameCategory.COMMENT) )
                .anyMatch(p -> p.getBlockClass().equals(blockClass));
    }
    */
    
//    public static FrameTypeCheck checkImport()
//    {
//        return checkCategories(GreenfootFrameCategory.IMPORT);
//    }

    public char getExtensionChar(String tailCanvasCaption) {
        return extensions.get(tailCanvasCaption);
    }
}
