/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2020 Michael Kölling and John Rosenberg
 
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
import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
import nu.xom.Attribute;
import nu.xom.Element;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.ThrowsTypeFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.frames.MethodProtoFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class MethodProtoElement extends DocumentContainerCodeElement
{
    public static final String ELEMENT = "methodproto";
    private final TypeSlotFragment returnType;
    private final NameDefSlotFragment name;
    private final List<ParamFragment> params;
    private final List<ThrowsTypeFragment> throwsTypes;
    private MethodProtoFrame frame;
    
    public MethodProtoElement(MethodProtoFrame frame, TypeSlotFragment returnType,
            NameDefSlotFragment name, List<ParamFragment> params, List<ThrowsTypeFragment> throwsTypes, JavadocUnit documentation, boolean enabled)
    {
        this.frame = frame;
        this.returnType = returnType;
        this.name = name;
        this.params = params;
        this.throwsTypes = throwsTypes;
        this.enable = enabled;

        this.documentation = documentation;
        if (this.documentation == null) {
            this.documentation = new JavadocUnit("");
        }
    }

    public MethodProtoElement(Element el)
    {
        returnType = new TypeSlotFragment(el.getAttributeValue("type"), el.getAttributeValue("type-java"));
        name = new NameDefSlotFragment(el.getAttributeValue("name"));
        params = new ArrayList<ParamFragment>();
        throwsTypes = new ArrayList<>();
        for (int i = 0; i < el.getChildElements().size(); i++) {
            final Element child = el.getChildElements().get(i);
            switch (child.getLocalName())
            {
                case "params":
                    params.add(new ParamFragment(child));
                    break;
                case "throws":
                    for (int j = 0; j < child.getChildElements().size(); j++) {
                        throwsTypes.add(new ThrowsTypeFragment(child.getChildElements().get(j)));
                    }
                    break;
                case "javadoc":
                    documentation = new JavadocUnit(child);
                    break;
            }
        }
        if (documentation == null) {
            documentation = new JavadocUnit("");
        }
        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }
    
    @Override
    public JavaSource toJavaSource()
    {
        List<JavaFragment> header = getHeaderFragments();
        
        if (getParent() instanceof ClassElement) {
            header.add(0, f(frame, "abstract "));
        }
        
        JavaSource javaSource = new JavaSource(null, header);
        javaSource.prependJavadoc(documentation.getJavaCode());
        return javaSource;
    }
    
    private List<JavaFragment> getHeaderFragments()
    {
        List<JavaFragment> header = new ArrayList<>();
        Collections.addAll(header, returnType, space(),  name, f(frame, "("));
        ParamFragment.addParamsToHeader(frame, this, params, header);
        header.add(f(frame, ")"));
        header.addAll(throwsToJava());
        header.add(f(frame, ";"));
        return header;
    }

    /**
     * Helper method for subclasses when generating Java: Turn throws declaration into Java
     */
    private List<JavaFragment> throwsToJava()
    {
        if (throwsTypes.isEmpty())
            return Collections.emptyList();

        ArrayList<JavaFragment> typesAndCommas = throwsTypes.stream().map(ThrowsTypeFragment::getJavaSource).collect(Utility.intersperse(() -> (JavaFragment)f(null, ", ")));

        typesAndCommas.add(0, space());
        typesAndCommas.add(0, f(frame, "throws"));
        typesAndCommas.add(0, space());

        return typesAndCommas;
    }

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new MethodProtoFrame(editor, returnType, name, params, throwsTypes,
                documentation.toString(), isEnable()); 
        return frame;
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement methodEl = new LocatableElement(this, ELEMENT);
        methodEl.addAttributeStructured("type", returnType);
        methodEl.addAttributeCode("name", name);
        addEnableAttribute(methodEl);
        methodEl.appendChild(documentation.toXML());
        params.forEach(param -> methodEl.appendChild(param.toXML()));
        Element throwsEl = new Element("throws");
        for (ThrowsTypeFragment t : throwsTypes) {
            throwsEl.appendChild(t.toXML());
        }
        methodEl.appendChild(throwsEl);
        return methodEl;
    }
    
    @Override
    public void show(ShowReason reason)
    {
        frame.show(reason);        
    }

    @Override
    public List<CodeElement> childrenUpTo(CodeElement c)
    {
        return Collections.emptyList();
    }
    
    @Override
    public Stream<CodeElement> streamContained()
    {
        return Stream.empty();
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.concat(Stream.of(returnType, name), params.stream().flatMap(p -> Stream.of(p.getParamType(), p.getParamName())));
    }
}
