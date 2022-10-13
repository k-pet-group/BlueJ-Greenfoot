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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import nu.xom.Attribute;
import nu.xom.Element;
import bluej.parser.AssistContent.ParamInfo;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.ThrowsTypeFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.frames.NormalMethodFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;
import threadchecker.OnThread;
import threadchecker.Tag;

public class NormalMethodElement extends MethodWithBodyElement
{
    public static final String ELEMENT = "method";
    private boolean staticModifier = false;
    private boolean finalModifier = false;
    private final TypeSlotFragment returnType;
    private final NameDefSlotFragment name;
    
    public NormalMethodElement(NormalMethodFrame frame, AccessPermissionFragment access, boolean staticModifier, 
            boolean finalModifier, TypeSlotFragment returnType, NameDefSlotFragment name, List<ParamFragment> params,
            List<ThrowsTypeFragment> throwsTypes, List<CodeElement> contents, JavadocUnit documentation, boolean enabled)
    {
        super(frame, access, params, throwsTypes, contents, documentation, enabled);
        this.staticModifier = staticModifier;
        this.finalModifier = finalModifier;
        this.returnType = returnType;
        this.name = name;
    }
    
    public NormalMethodElement(Element el)
    {
        super(el);
        
        Attribute staticAttribute = el.getAttribute("static");
        staticModifier = (staticAttribute == null) ? false : Boolean.valueOf(staticAttribute.getValue());
        
        Attribute finalAttribute = el.getAttribute("final");
        finalModifier = (finalAttribute == null) ? false : Boolean.valueOf(finalAttribute.getValue());
        
        returnType = new TypeSlotFragment(el.getAttributeValue("type"), el.getAttributeValue("type-java"));
        name = new NameDefSlotFragment(el.getAttributeValue("name"));
    }
    
    public NormalMethodElement(String access, String returnType, String name, List<Entry<String,String>> params, 
            List<CodeElement> contents, String documentation)
    {
        super(access, params, contents, documentation);
        
        this.returnType = new TypeSlotFragment(returnType, returnType);
        this.name = new NameDefSlotFragment(name);
    }
    
    @Override
    public JavaSource toJavaSource()
    {
        List<JavaFragment> header = new ArrayList<>();
        
        if (staticModifier) {
            header.add(f(frame, "static "));
        }
        if (finalModifier) {
            header.add(f(frame, "final "));
        }
        
        Collections.addAll(header, access, space(), returnType, space(),  name, f(frame, "("));
        
        ParamFragment.addParamsToHeader(frame, this, params, header);
        header.add(f(frame, ")"));
        
        header.addAll(throwsToJava());
        
        return JavaSource.createMethod(frame, this, this, documentation, header, CodeElement.toJavaCodes(contents));
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement methodEl = new LocatableElement(this, ELEMENT);
        accessToXML(methodEl);
        
        if (staticModifier) {
            methodEl.addAttribute(new Attribute("static", "true"));
        }
        if (finalModifier) {
            methodEl.addAttribute(new Attribute("final", "true"));
        }
        
        methodEl.addAttributeStructured("type", returnType);
        methodEl.addAttributeCode("name", name);
        
        addEnableAttribute(methodEl);
        
        methodEl.appendChild(documentation.toXML());
        paramsToXML(methodEl);
        throwsToXML(methodEl);
        bodyToXML(methodEl);
        return methodEl;
    }
    
    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new NormalMethodFrame(editor, access, staticModifier, finalModifier,
                returnType.getContent(), name.getContent(), documentation.toString(), isEnable());
        setupFrame(editor);
        return frame;
    }
    
    @Override
    public String getType()
    {
        return returnType.getContent();
    }

    public String getName()
    {
        return name.getContent();
    }
    
    @Override
    public void show(ShowReason reason)
    {
        frame.show(reason);        
    }

    @OnThread(Tag.FXPlatform)
    public boolean equalDeclaration(String name, List<ParamInfo> params, ClassElement el)
    {
        if ( !this.name.getContent().equals(name) ) {
            return false;
        }
        if ( this.params.size() != params.size() ) {
            return false;
        }
        List<String> ourQualParams = getQualifiedParamTypes(el);
        for (int i = 0; i < params.size(); i++)
        {
            if ( !ourQualParams.get(i).equals(params.get(i).getQualifiedType()) )
            {
                return false;
            }
        }
        return true;
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        Stream<SlotFragment> s = params.stream().flatMap(p -> Stream.of(p.getParamType(), p.getParamName()));
        return Stream.<SlotFragment>concat(Stream.<SlotFragment>concat(Stream.of(returnType, name), s), throwsTypes.stream().map(ThrowsTypeFragment::getJavaSource));
    }

    public NormalMethodFrame getFrame()
    {
        return (NormalMethodFrame)frame;
    }

    public boolean isStatic()
    {
        return staticModifier;
    }

    public boolean isFinal()
    {
        return finalModifier;
    }
}
