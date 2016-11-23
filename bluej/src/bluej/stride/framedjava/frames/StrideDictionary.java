/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameDictionary;
import bluej.stride.generic.FrameTypeCheck;

public class StrideDictionary extends FrameDictionary<StrideCategory>
{
    private static StrideDictionary instance = new StrideDictionary();
    
    public static StrideDictionary getDictionary()
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

    private StrideDictionary()
    {
        super(Arrays.asList(
            new Entry<>(' ', CallFrame.getFactory(), false, StrideCategory.CALL, "Call method", "Calls a method"),
            new Entry<>('\n', BlankFrame.getFactory(), false, StrideCategory.BLANK, "Blank", "A blank line"),
            new Entry<>('/', CommentFrame.getFactory(), false, StrideCategory.COMMENT, "Comment", "A code comment"),
            new Entry<>('=', AssignFrame.getFactory(), false, StrideCategory.ASSIGNMENT, "Assignment", "Assignment"),
            //case '*': return new MultiCommentBlock(editor);
            // Last parameter is false in the next entry as No need to show it in the Catalogue, just make it valid.
            new Entry<>('a', MethodProtoFrame.getFactory(), false, StrideCategory.INTERFACE_METHOD, "Abstract method", "A method of an interface", false),
            new Entry<>('a', MethodProtoFrame.getFactory(), false, StrideCategory.CLASS_METHOD, "Abstract method", "An abstract method of a class"),
            new Entry<>('b', BreakFrame.getFactory(), false, StrideCategory.BREAK, "Break", "Breaks out of loop"),
            new Entry<>('c', ConstructorFrame.getFactory(), false, StrideCategory.CONSTRUCTOR, "Constructor", "A constructor of a class"),
            new Entry<>('c', CaseFrame.getFactory(), false, StrideCategory.CASE, "Case (Switch)", "Handles specific value"),
            new Entry<>('c', VarFrame.getLocalConstantFactory(), false, StrideCategory.VAR_LOCAL, "Constant declaration", "Declares constant"),
            new Entry<>('c', VarFrame.getClassConstantFactory(), false, StrideCategory.CONSTANT_CLASS_FIELD, "Constant declaration", "Declares constant"),
            new Entry<>('c', VarFrame.getInterfaceConstantFactory(), false, StrideCategory.CONSTANT_INTERFACE_FIELD, "Constant declaration", "Declares constant"),
            //case 'f': return new ForBlock(editor);
            new Entry<>('f', ForeachFrame.getFactory(), true, StrideCategory.LOOP, "For-each loop", "Loop over a collection"),
            new Entry<>('i', IfFrame.getFactory(), true, StrideCategory.CONDITIONAL, "If", "Conditional execution"),
//            new Entry<>('e', IfFrame.getFactory(), false, StrideCategory.CONDITIONAL, "Else", "Else execution"),
            new Entry<>('i', ImportFrame.getFactory(), false, StrideCategory.IMPORT, "Import", "Import a class or package"),
            new Entry<>('m', NormalMethodFrame.getFactory(), false, StrideCategory.CLASS_METHOD, "Method", "A method of a class"),
            new Entry<>('m', MethodProtoFrame.getFactory(), false, StrideCategory.INTERFACE_METHOD, "Abstarct Method", "A method of an interface"),
            //case 'o': return new ObjectBlock(editor);
            new Entry<>('r', ReturnFrame.getFactory(), false, StrideCategory.RETURN, "Return", "Returns from method"),
            new Entry<>('s', SwitchFrame.getFactory(), false, StrideCategory.SWITCH, "Switch", "Chooses from several cases"),
            new Entry<>('v', VarFrame.getFactory(), false, StrideCategory.VAR, "Variable declaration", "Declares variable"),
            new Entry<>('w', WhileFrame.getFactory(), true, StrideCategory.LOOP, "While loop", "Pre-condition loop"),
            new Entry<>('x', ThrowFrame.getFactory(), false, StrideCategory.THROW, "Throw", "Throws an exception"),
            new Entry<>('y', TryFrame.getFactory(), true, StrideCategory.TRY, "Try/catch", "Try block")
            //TODO When implementing a stride debugger.
            //new Entry<>('z', BreakpointFrame.getFactory(), false, StrideCategory.COMMENT, "Breakpoint", "Pauses execution, for debugging")
        ));

        extensions.put("catch", 'c');
        //TODO Commenting it out should not do any bug, test it
        //extensions.put("default", 'd');
        extensions.put("else", 'e');
        extensions.put("elseif", 'l');
        extensions.put("finally", 'n');
    }

    @Override
    public StrideCategory[] getCategories()
    {
        return StrideCategory.values();
    }

    @Override
    public String[] getCategoryNames()
    {
        String[] names = new String[StrideCategory.values().length];
        for (int i = 0; i < names.length; i++) {
            String str = StrideCategory.values()[i].toString();
            names[i] = str.substring(0, 1) + str.substring(1).toLowerCase();
        }
        return names;
    }
    
    private static FrameTypeCheck checkCategories(StrideCategory... categories)
    {
        List<StrideCategory> categoryList = Arrays.asList(categories);
        return new FrameTypeCheck()
        {
            @Override
            public boolean canInsert(StrideCategory category)
            {
                return categoryList.contains(category);
            }

            @Override
            public boolean canPlace(Class<? extends Frame> type)
            {
                return getDictionary().getAllBlocks().stream().filter(e -> categoryList.contains(e.getCategory())).anyMatch(p -> p.getBlockClass().equals(type));
            }
        };
    }
    
    public static FrameTypeCheck checkStatement()
    {
        return checkCategories(StrideCategory.CONDITIONAL,
                StrideCategory.VAR,
                StrideCategory.VAR_LOCAL,
                StrideCategory.BLANK,
                StrideCategory.COMMENT,
                StrideCategory.LOOP,
                StrideCategory.ASSIGNMENT,
                StrideCategory.CALL,
                StrideCategory.BREAK,
                StrideCategory.SWITCH,
                StrideCategory.THROW,
                StrideCategory.TRY,
                StrideCategory.RETURN);
    }
    
    public static FrameTypeCheck checkClassField()
    {
        return checkCategories( 
                StrideCategory.VAR,
                StrideCategory.CONSTANT_CLASS_FIELD,
                StrideCategory.BLANK,
                StrideCategory.COMMENT);
    }

    public static FrameTypeCheck checkInterfaceField()
    {
        return checkCategories(
                StrideCategory.CONSTANT_INTERFACE_FIELD,
                StrideCategory.BLANK,
                StrideCategory.COMMENT);
    }
    
    public static FrameTypeCheck checkConstructor()
    {
        return checkCategories( 
                StrideCategory.CONSTRUCTOR,
                StrideCategory.COMMENT);
    }
    
    public static FrameTypeCheck checkClassMethod()
    {
        return checkCategories(
                StrideCategory.CLASS_METHOD,
                StrideCategory.COMMENT);
    }

    public static FrameTypeCheck checkInterfaceMethod()
    {
        return checkCategories(
                StrideCategory.INTERFACE_METHOD,
                StrideCategory.COMMENT);
    }
    
    public static FrameTypeCheck checkImport()
    {
        return checkCategories(StrideCategory.IMPORT);
    }

    public char getExtensionChar(String tailCanvasCaption)
    {
        return extensions.get(tailCanvasCaption);
    }
}
