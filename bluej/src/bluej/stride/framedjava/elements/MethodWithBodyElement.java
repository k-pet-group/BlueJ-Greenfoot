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
package bluej.stride.framedjava.elements;


import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
import nu.xom.Attribute;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.gentype.Reflective;
import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaSingleLineDebugHandler;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.ThrowsTypeFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.framedjava.frames.MethodFrameWithBody;

public abstract class MethodWithBodyElement extends DocumentContainerCodeElement implements JavaSingleLineDebugHandler
{
    protected final List<ParamFragment> params;
    protected final List<ThrowsTypeFragment> throwsTypes;
    protected final List<CodeElement> contents;
    protected MethodFrameWithBody<?> frame;
    protected AccessPermissionFragment access;
    
    public MethodWithBodyElement(MethodFrameWithBody<?> frame, AccessPermissionFragment access, List<ParamFragment> params,
            List<ThrowsTypeFragment> throwsTypes, List<CodeElement> contents, JavadocUnit documentation, boolean enabled)
    {
        this.frame = frame;
        this.access = access;
        this.params = params;
        this.throwsTypes = throwsTypes;
        this.contents = contents;
        this.documentation = documentation;
        
        for (CodeElement c : this.contents) {
            c.setParent(this);
        }
        this.enable = enabled;

        if (this.documentation == null) {
            this.documentation = new JavadocUnit("");
        }
    }

    public MethodWithBodyElement(Element el)
    {
        access = new AccessPermissionFragment(AccessPermission.fromString(el.getAttributeValue("access")));
        params = new ArrayList<ParamFragment>();
        this.throwsTypes = new ArrayList<>();
        contents = new ArrayList<CodeElement>();
        for (int i = 0; i < el.getChildElements().size(); i++) {
            final Element child = el.getChildElements().get(i);
            switch (child.getLocalName()) {
                case "params":
                    for (int j = 0; j < child.getChildElements().size(); j++) {
                        params.add(new ParamFragment(child.getChildElements().get(j)));
                    }
                    break;
                case "throws":
                    for (int j = 0; j < child.getChildElements().size(); j++) {
                        throwsTypes.add(new ThrowsTypeFragment(child.getChildElements().get(j)));
                    }
                    break;
                case "body":
                    for (int j = 0; j < child.getChildElements().size(); j++) {
                        CodeElement member = Loader.loadElement(child.getChildElements().get(j));
                        contents.add(member);
                        member.setParent(this);
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
        enable = new Boolean(el.getAttributeValue("enable"));
    }
    
    public MethodWithBodyElement(String access, List<Entry<String,String>> params, List<CodeElement> contents, String documentation)
    {
        this.access = new AccessPermissionFragment(AccessPermission.fromString(access));
        this.contents = contents;
        this.documentation = new JavadocUnit(documentation);
        
        this.throwsTypes = new ArrayList<>();
        
        this.params = new ArrayList<>();
        if (params != null ) {
            for (int i = 0; i < params.size(); i++) {
                Entry<String, String> entry = params.get(i);
                this.params.add(new ParamFragment(new TypeSlotFragment(entry.getKey()), new NameDefSlotFragment(entry.getValue())));
            }
        }
    }
    
    protected void accessToXML(Element methodEl)
    {
        if (access != null) {
            methodEl.addAttribute(new Attribute("access", access.getContent()));
        }
    }

    protected void paramsToXML(Element methodEl)
    {
        Element paramsEl = new Element("params");
        for (ParamFragment param : params) {
            paramsEl.appendChild(param.toXML());
        }
        methodEl.appendChild(paramsEl);
    }
    
    protected void throwsToXML(Element methodEl)
    {
        Element throwsEl = new Element("throws");
        for (ThrowsTypeFragment t : throwsTypes) {
            throwsEl.appendChild(t.toXML());
        }
        methodEl.appendChild(throwsEl);
    }

    protected void bodyToXML(Element methodEl)
    {
        Element bodyEl = new Element("body");
        for (CodeElement c : contents) {
            bodyEl.appendChild(c.toXML());
        }
        methodEl.appendChild(bodyEl);
    }
    
    @OnThread(Tag.FX)
    protected void makeFrame(InteractionManager editor)
    {
        frame.setAccess(access.getValue());
        
        frame.setDocumentation(documentation.toString());
        
        params.forEach(item -> frame.getParamsPane().addFormal(item.getParamType(), item.getParamName()));
        contents.forEach(c -> frame.getCanvas().insertBlockAfter(c.createFrame(editor), null));
        throwsTypes.forEach(t -> frame.addThrows(t.getType()));
    }

    @Override
    public List<LocalParamInfo> getDeclaredVariablesWithin()
    {
        List<LocalParamInfo> vars = new ArrayList<>();
        params.forEach(param -> vars.add(new LocalParamInfo(param.getParamType().getContent(), param.getParamName().getContent(), true, this)));
        return vars;
    }
    
    @Override
    public List<CodeElement> childrenUpTo(CodeElement c)
    {
        return contents.subList(0, contents.indexOf(c));
    }

    @Override
    public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
    {
        return frame.showDebugBefore(debug);   
    }

    @Override
    public MethodWithBodyElement getMethodElement()
    {
        return this;
    }
    
    public List<CodeElement> getContents()
    {
        return contents;
    }

    public abstract String getType(); // returns null if NA, e.g. for constructors
    
    @Override
    public Stream<CodeElement> streamContained()
    {
        return streamContained(contents);
    }
    
    @OnThread(Tag.Swing)
    public List<String> getQualifiedParamTypes(ClassElement topLevel)
    {
        return params.stream().map(p -> {
            TypeSlotFragment paramType = p.getParamType();
            Reflective qualifyType = topLevel.qualifyType(paramType.getContent(), paramType.getPosInSourceDoc());
            if (qualifyType == null)
                return paramType.getContent(); // Assume needs no qualification, e.g. primitive
            return qualifyType.getName();
            }
        ).collect(Collectors.toList());
    }

    public AccessPermission getAccessPermission()
    {
        return access.getValue();
    }

    public List<String> getThrowsTypes()
    {
        return Utility.mapList(throwsTypes, ThrowsTypeFragment::getType);
    }

    public List<ParamFragment> getParams()
    {
        return params;
    }
}
