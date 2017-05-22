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
package bluej.stride.framedjava.elements;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.framedjava.elements.LocatableElement.LocationMap;
import bluej.stride.framedjava.errors.DirectSlotError;
import bluej.stride.generic.InteractionManager;
import nu.xom.Attribute;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.ast.FrameFragment;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public abstract class CodeElement
{
    protected boolean enable = true;
    
    public boolean isEnable()
    {
        return enable;
    }

    public void setEnable(boolean enable)
    {
        this.enable = enable;
    }
    
    protected void addEnableAttribute(Element element)
    {
        element.addAttribute(new Attribute("enable", String.valueOf(enable)));
    }
    
    /**
     * Finds any errors in this element, including syntax errors.
     * 
     * If this element returns no errors, then the code can be generated into valid Java.
     * 
     * (TODO in the future, this should strengthen to: returning no errors means code won't give syntax error)
     */
    @OnThread(Tag.FXPlatform)
    public final Stream<SyntaxCodeError> findEarlyErrors(LocationMap rootPathMap)
    {
        if (!isEnable())
            return Stream.empty();
        return toJavaSource().getAllFragments().flatMap(fragment -> fragment.findEarlyErrors().peek(e -> e.recordPath(rootPathMap.locationFor(fragment))));
    }

    @OnThread(Tag.FXPlatform)
    public final Stream<Future<List<DirectSlotError>>> findDirectLateErrors(InteractionManager editor, LocationMap rootPathMap)
    {
        if (!isEnable())
            return Stream.empty();
        return getDirectSlotFragments().map(g -> g.findLateErrors(editor, this, rootPathMap)).filter(x -> x != null);
    }
    
    protected abstract Stream<SlotFragment> getDirectSlotFragments();
    
    // The return should only be compiled if validForCompilation() returns true.
    // However, it might be used for parsing (for code completion) at any point.
    @OnThread(Tag.FXPlatform)
    public abstract JavaSource toJavaSource();
    
    public abstract LocatableElement toXML();

    @OnThread(Tag.FX)
    public abstract Frame createFrame(InteractionManager editor);
    
    private ContainerCodeElement parent;
    
    public void setParent(ContainerCodeElement e)
    {
        parent = e;
    }
    
    public ContainerCodeElement getParent()
    {
        return parent;
    }
    
    /**
     * Information about a local variable or parameter variable
     */
    public static class LocalParamInfo
    {
        private final String type;
        private final String name;
        private final boolean param;
        private final CodeElement declarer;
        
        public LocalParamInfo(String type, String name, boolean param, CodeElement declarer)
        {
            this.type = type;
            this.name = name;
            this.param = param;
            this.declarer = declarer;
        }
        
        public String getType()
        {
            return type;
        }
        public String getName()
        {
            return name;
        }

        public boolean isParam()
        {
            return param;
        }

        public CodeElement getDeclarer()
        {
            return declarer;
        }
    }
   
    
    //Gets variables declared by this block that are in scope of following blocks
    public List<LocalParamInfo> getDeclaredVariablesAfter()
    {
        return Collections.emptyList();
    }

    @OnThread(Tag.FXPlatform)
    public static List<JavaSource> toJavaCodes(List<? extends CodeElement> contents)
    {
        return contents.stream().filter(c -> c.isEnable()).map(c -> c.toJavaSource()).collect(Collectors.toList());
    }
    
    // Convenience helper for constructing boilerplate:
    /*
    protected static Boilerplate b(String s)
    {
        return new Boilerplate(s);
    }
    */
    // Convenience helper for constructing FrameFragment:
    protected FrameFragment f(Frame frame, String s)
    {
        return new FrameFragment(frame, this, s);
    }
    
    protected JavaFragment space()
    {
        return new FrameFragment(null, this, " ");
    }
    
    /**
     * Display errors (syntactic or semantic) that have been detected from the CodeElement-based
     * source tree.  Errors provided back by Java are processed elsewhere.
     */
    public static void preserveWhitespace(Element el)
    {
        el.addAttribute(new Attribute("xml:space", "http://www.w3.org/XML/1998/namespace", "preserve"));
    }
    
    // Matches method from SingleLineDebugHandler
    @OnThread(Tag.FX)
    public final void showException()
    {
        show(ShowReason.EXCEPTION);
    }
    
    @OnThread(Tag.FX)
    public abstract void show(ShowReason reason);
    
    // Streams all contained elements, excluding this element.
    public Stream<CodeElement> streamContained() { return Stream.empty(); }
}
