/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
import java.util.Map;

import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameDictionary;

public class GreenfootFrameDictionary extends FrameDictionary<GreenfootFrameCategory>
{
    private static GreenfootFrameDictionary instance = new GreenfootFrameDictionary();
    
    public static GreenfootFrameDictionary getDictionary()
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

    public static final char VAR_CHAR = 'v';
    public static final char CONSTRUCTOR_CHAR = 'c';
    public static final char METHOD_CHAR = 'm';
    public static final char ABSTRACT_METHOD_CHAR = 'a';
    
    private GreenfootFrameDictionary()
    {
        super(Arrays.asList(
            new Entry<>(' ', CallFrame.getFactory(), false, GreenfootFrameCategory.CALL, "Call method", "Calls a method"),
            new Entry<>('\n', BlankFrame.getFactory(), false, GreenfootFrameCategory.BLANK, "Blank", "A blank line"),
            new Entry<>('/', CommentFrame.getFactory(), false, GreenfootFrameCategory.COMMENT, "Comment", "A code comment"),
            new Entry<>('=', AssignFrame.getFactory(), false, GreenfootFrameCategory.ASSIGNMENT, "Assignment", "Assignment"),
            //case '*': return new MultiCommentBlock(editor);
            new Entry<>(ABSTRACT_METHOD_CHAR, MethodProtoFrame.getFactory(), false, GreenfootFrameCategory.ABSTRACT, "Abstract method", "An abstract method of a class"),
            new Entry<>('b', BreakFrame.getFactory(), false, GreenfootFrameCategory.BREAK, "Break", "Breaks out of loop"),
            new Entry<>(CONSTRUCTOR_CHAR, ConstructorFrame.getFactory(), false, GreenfootFrameCategory.CONSTRUCTOR, "Constructor", "A constructor of a class"),
            new Entry<>('c', CaseFrame.getFactory(), false, GreenfootFrameCategory.CASE, "Case (Switch)", "Handles specific value"),
            //case 'f': return new ForBlock(editor);
            new Entry<>('f', ForeachFrame.getFactory(), true, GreenfootFrameCategory.LOOP, "For-each loop", "Loop over a collection"),
            new Entry<>('i', IfFrame.getFactory(), true, GreenfootFrameCategory.CONDITIONAL, "If", "Conditional execution"),
//            new Entry<>('e', IfFrame.getFactory(), false, GreenfootFrameCategory.CONDITIONAL, "Else", "Else execution"),
            new Entry<>('i', ImportFrame.getFactory(), false, GreenfootFrameCategory.IMPORT, "Import", "Import a class or package"),
            new Entry<>(METHOD_CHAR, NormalMethodFrame.getFactory(), false, GreenfootFrameCategory.METHOD, "Method", "A method of a class"),
            //case 'o': return new ObjectBlock(editor);
            new Entry<>('r', ReturnFrame.getFactory(), false, GreenfootFrameCategory.RETURN, "Return", "Returns from method"),
            new Entry<>('s', SwitchFrame.getFactory(), false, GreenfootFrameCategory.SWITCH, "Switch", "Chooses from several cases"),
            new Entry<>(VAR_CHAR, VarFrame.getFactory(), false, GreenfootFrameCategory.VAR, "Variable declaration", "Declares variable"),
            new Entry<>('w', WhileFrame.getFactory(), true, GreenfootFrameCategory.LOOP, "While loop", "Pre-condition loop"),
            new Entry<>('x', ThrowFrame.getFactory(), false, GreenfootFrameCategory.THROW, "Throw", "Throws an exception"),
            new Entry<>('y', TryFrame.getFactory(), true, GreenfootFrameCategory.TRY, "Try/catch", "Try block")
            //TODO When implementing a stride debugger.
            //new Entry<>('z', BreakpointFrame.getFactory(), false, GreenfootFrameCategory.COMMENT, "Breakpoint", "Pauses execution, for debugging")
        ));

        extensions.put("catch", 'c');
        //TODO Commenting it out should not do any bug, test it
        //extensions.put("default", 'd');
        extensions.put("else", 'e');
        extensions.put("elseif", 'l');
        extensions.put("finally", 'n');
    }

    @Override
    public GreenfootFrameCategory[] getCategories()
    {
        return GreenfootFrameCategory.values();
    }

    @Override
    public String[] getCategoryNames()
    {
        String[] names = new String[GreenfootFrameCategory.values().length];
        for (int i = 0; i < names.length; i++) {
            String str = GreenfootFrameCategory.values()[i].toString();
            names[i] = str.substring(0, 1) + str.substring(1).toLowerCase();
        }
        return names;
    }
    
    @Override
    public boolean isValidStatment(Class<? extends Frame> blockClass)
    {
        return getAllBlocks().stream().filter(t ->
                t.getCategory().equals(GreenfootFrameCategory.CONDITIONAL) ||
                t.getCategory().equals(GreenfootFrameCategory.VAR) ||
                t.getCategory().equals(GreenfootFrameCategory.BLANK) ||
                t.getCategory().equals(GreenfootFrameCategory.COMMENT) ||
                t.getCategory().equals(GreenfootFrameCategory.LOOP) ||
                t.getCategory().equals(GreenfootFrameCategory.ASSIGNMENT) ||
                t.getCategory().equals(GreenfootFrameCategory.CALL) ||
                t.getCategory().equals(GreenfootFrameCategory.BREAK) ||
                t.getCategory().equals(GreenfootFrameCategory.SWITCH) ||
                t.getCategory().equals(GreenfootFrameCategory.THROW) ||
                t.getCategory().equals(GreenfootFrameCategory.TRY) ||
                t.getCategory().equals(GreenfootFrameCategory.RETURN) )
                .anyMatch(p -> p.getBlockClass().equals(blockClass));
    }
    
    @Override
    public boolean isValidField(Class<? extends Frame> blockClass)
    {
        return getAllBlocks().stream().filter(t -> 
                t.getCategory().equals(GreenfootFrameCategory.VAR) ||
                t.getCategory().equals(GreenfootFrameCategory.BLANK) ||
                t.getCategory().equals(GreenfootFrameCategory.COMMENT) )
                .anyMatch(p -> p.getBlockClass().equals(blockClass));
    }
    
    @Override
    public boolean isValidConstructor(Class<? extends Frame> blockClass)
    {
        return getAllBlocks().stream().filter(t -> 
                t.getCategory().equals(GreenfootFrameCategory.CONSTRUCTOR) || 
                t.getCategory().equals(GreenfootFrameCategory.COMMENT) )
                .anyMatch(p -> p.getBlockClass().equals(blockClass));
    }
    
    @Override
    public boolean isValidClassMethod(Class<? extends Frame> blockClass)
    {
        return getAllBlocks().stream().filter(t -> 
                t.getCategory().equals(GreenfootFrameCategory.ABSTRACT) ||
                t.getCategory().equals(GreenfootFrameCategory.METHOD) || 
                t.getCategory().equals(GreenfootFrameCategory.COMMENT) )
                .anyMatch(p -> p.getBlockClass().equals(blockClass));
    }
    
    @Override
    public boolean isValidInterfaceMethod(Class<? extends Frame> blockClass)
    {
        return getAllBlocks().stream().filter(t -> 
                t.getCategory().equals(GreenfootFrameCategory.VAR) ||
                t.getCategory().equals(GreenfootFrameCategory.ABSTRACT) ||
                t.getCategory().equals(GreenfootFrameCategory.COMMENT) )
                .anyMatch(p -> p.getBlockClass().equals(blockClass));
    }

    public char getExtensionChar(String tailCanvasCaption) {
        return extensions.get(tailCanvasCaption);
    }
}
