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
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.generic.InteractionManager;
import nu.xom.Element;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.SuperThis;
import bluej.stride.framedjava.ast.SuperThisFragment;
import bluej.stride.framedjava.ast.SuperThisParamsExpressionFragment;
import bluej.stride.framedjava.ast.ThrowsTypeFragment;
import bluej.stride.framedjava.frames.ConstructorFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class ConstructorElement extends MethodWithBodyElement
{
    public static final String ELEMENT = "constructor";
    private final SuperThisFragment delegate;
    private final SuperThisParamsExpressionFragment delegateParams;
    
    public ConstructorElement(ConstructorFrame frame, AccessPermissionFragment access, List<ParamFragment> params,
                              List<ThrowsTypeFragment> throwsTypes, SuperThisFragment delegate,
                              SuperThisParamsExpressionFragment delegateParams, List<CodeElement> contents,
                              JavadocUnit documentation, boolean enabled)
    {
        super(frame, access, params, throwsTypes, contents, documentation, enabled);
        this.delegate = delegate;
        this.delegateParams = delegateParams;
    }

    public ConstructorElement(String javaDoc)
    {
        this(null, new AccessPermissionFragment(AccessPermission.PUBLIC), Collections.emptyList(), Collections.emptyList(),
                null, null, Collections.emptyList(), new JavadocUnit(javaDoc), true);
    }
    
    public ConstructorElement(Element el)
    {
        super(el);

        SuperThisFragment loadedDelegate = null;
        SuperThisParamsExpressionFragment loadedDelegateParams = null;
        for (int i = 0; i < el.getChildElements().size(); i++) {
            final Element section = el.getChildElements().get(i);
            if (section.getLocalName().equals("delegate"))
            {
                String target = section.getAttributeValue("target");
                switch (target)
                {
                    case "super":
                    case "this":
                        loadedDelegate = new SuperThisFragment(SuperThis.fromString(target));
                        loadedDelegateParams = new SuperThisParamsExpressionFragment(section.getAttributeValue("params"), section.getAttributeValue("params-java"));
                        break;
                }
            }
        }
        delegate = loadedDelegate;
        delegateParams = loadedDelegateParams;
    }

    @Override
    public JavaSource toJavaSource()
    {   
        List<JavaFragment> header = new ArrayList<>();
        
        Collections.addAll(header, access, space(), ((ClassElement)getParent()).getNameElement((ConstructorFrame)frame), f(frame, "("));
                
        ParamFragment.addParamsToHeader(frame, this, params, header);
        header.add(f(frame, ")"));

        header.addAll(throwsToJava());
        
        List<JavaSource> effectiveContents = new ArrayList<>();
        
        if (delegate != null) {
            effectiveContents.add(new JavaSource(this, delegate, f(frame, "("), delegateParams, f(frame, ");")));
        }
        
        effectiveContents.addAll(CodeElement.toJavaCodes(contents));
        
        return JavaSource.createMethod(frame, this, this, documentation, header, effectiveContents);
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement methodEl = new LocatableElement(this, ELEMENT);
        accessToXML(methodEl);
        addEnableAttribute(methodEl);

        methodEl.appendChild(documentation.toXML());
        
        paramsToXML(methodEl);
        throwsToXML(methodEl);
        
        LocatableElement delegateEl;
        if (delegate != null) {
            delegateEl = new LocatableElement(null, "delegate");
            delegateEl.addAttributeSuperThis("target", delegate);
            delegateEl.addAttributeStructured("params", delegateParams);
            methodEl.appendChild(delegateEl);
        }
        
        bodyToXML(methodEl);
        return methodEl;
    }

    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new ConstructorFrame(editor, access, documentation.toString(), delegate, delegateParams, isEnable());
        setupFrame(editor);
        return frame;
    }
    
    @Override
    public String getType()
    {
        // We have no (return) type:
        return null;
    }
    
    @Override
    public void show(ShowReason reason)
    {
        frame.show(reason);        
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        Stream<SlotFragment> s = params.stream().flatMap(p -> Stream.of(p.getParamType(), p.getParamName()));
        if (delegate != null)
            s = Stream.concat(s, Stream.of(delegateParams));
        return Stream.concat(s, throwsTypes.stream().map(ThrowsTypeFragment::getJavaSource));
    }

    public boolean hasDelegate()
    {
        return delegate != null;
    }

    public SuperThis getDelegate()
    {
        return delegate == null ? null : delegate.getValue();
    }

    public String getDelegateParams()
    {
        return delegateParams == null ? null : delegateParams.getContent();
    }

    public String getDelegateParamsJava()
    {
        return delegateParams == null ? null : delegateParams.getJavaCode();
    }
}
