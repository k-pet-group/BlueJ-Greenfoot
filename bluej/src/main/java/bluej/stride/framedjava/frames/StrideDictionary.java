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
            new Entry<>(' ', CallFrame.getFactory(), false, StrideCategory.CALL, "stride.dictionary.call.method.name", "stride.dictionary.call.method.description"),
            new Entry<>('\n', BlankFrame.getFactory(), false, StrideCategory.BLANK, "stride.dictionary.blank.name", "stride.dictionary.blank.description"),
            new Entry<>('/', CommentFrame.getFactory(), false, StrideCategory.COMMENT, "stride.dictionary.comment.name", "stride.dictionary.comment.description"),
            new Entry<>('=', AssignFrame.getFactory(), false, StrideCategory.ASSIGNMENT, "stride.dictionary.assignment.name", "stride.dictionary.assignment.description"),
            //case '*': return new MultiCommentBlock(editor);
            // Last parameter is false in the next entry as No need to show it in the Catalogue, just make it valid.
            new Entry<>('a', MethodProtoFrame.getFactory(), false, StrideCategory.INTERFACE_METHOD, "stride.dictionary.abstract.method.name", "stride.dictionary.abstract.method.interface.description", false),
            new Entry<>('a', MethodProtoFrame.getFactory(), false, StrideCategory.CLASS_METHOD, "stride.dictionary.abstract.method.name", "stride.dictionary.abstract.method.class.description"),
            new Entry<>('b', BreakFrame.getFactory(), false, StrideCategory.BREAK, "stride.dictionary.break.name", "stride.dictionary.break.description"),
            new Entry<>('c', ConstructorFrame.getFactory(), false, StrideCategory.CONSTRUCTOR, "stride.dictionary.constructor.name", "stride.dictionary.constructor.description"),
            new Entry<>('c', CaseFrame.getFactory(), false, StrideCategory.CASE, "stride.dictionary.case.name", "stride.dictionary.case.description"),
            new Entry<>('c', VarFrame.getLocalConstantFactory(), false, StrideCategory.VAR_LOCAL, "stride.dictionary.constant.name", "stride.dictionary.constant.description"),
            new Entry<>('c', VarFrame.getClassConstantFactory(), false, StrideCategory.CONSTANT_CLASS_FIELD, "stride.dictionary.constant.name", "stride.dictionary.constant.description"),
            new Entry<>('c', VarFrame.getInterfaceConstantFactory(), false, StrideCategory.CONSTANT_INTERFACE_FIELD, "stride.dictionary.constant.name", "stride.dictionary.constant.description"),
            //case 'f': return new ForBlock(editor);
            new Entry<>('f', ForeachFrame.getFactory(), true, StrideCategory.LOOP, "stride.dictionary.for.each.name", "stride.dictionary.for.each.description"),
            new Entry<>('i', IfFrame.getFactory(), true, StrideCategory.CONDITIONAL, "stride.dictionary.if.name", "stride.dictionary.if.description"),
//            new Entry<>('e', IfFrame.getFactory(), false, StrideCategory.CONDITIONAL, "stride.dictionary.else.name", "stride.dictionary.else.description"),
            new Entry<>('i', ImportFrame.getFactory(), false, StrideCategory.IMPORT, "stride.dictionary.import.name", "stride.dictionary.import.description"),
            new Entry<>('m', NormalMethodFrame.getFactory(), false, StrideCategory.CLASS_METHOD, "stride.dictionary.method.name", "stride.dictionary.method.description"),
            new Entry<>('m', MethodProtoFrame.getFactory(), false, StrideCategory.INTERFACE_METHOD, "stride.dictionary.abstarct.name", "stride.dictionary.abstract.method.interface.description"),
            //case 'o': return new ObjectBlock(editor);
            new Entry<>('r', ReturnFrame.getFactory(), false, StrideCategory.RETURN, "stride.dictionary.return.name", "stride.dictionary.return.description"),
            new Entry<>('s', SwitchFrame.getFactory(), false, StrideCategory.SWITCH, "stride.dictionary.switch.name", "stride.dictionary.switch.description"),
            new Entry<>('v', VarFrame.getFactory(), false, StrideCategory.VAR, "stride.dictionary.variable.name", "stride.dictionary.variable.description"),
            new Entry<>('w', WhileFrame.getFactory(), true, StrideCategory.LOOP, "stride.dictionary.while.name", "stride.dictionary.while.description"),
            new Entry<>('x', ThrowFrame.getFactory(), false, StrideCategory.THROW, "stride.dictionary.throw.name", "stride.dictionary.throw.description"),
            new Entry<>('y', TryFrame.getFactory(), true, StrideCategory.TRY, "stride.dictionary.try.name", "stride.dictionary.try.description")
            //TODO When implementing a stride debugger.
            //new Entry<>('z', BreakpointFrame.getFactory(), false, StrideCategory.COMMENT, "stride.dictionary.breakpoint.name", "stride.dictionary.breakpoint.description")
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
