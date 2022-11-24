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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
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

/**
 * A parent class which is shared between ConstructorElement and NormalMethodElement.
 * These two items have a lot in comment, which is collected here.
 */
public abstract class MethodWithBodyElement extends DocumentContainerCodeElement implements JavaSingleLineDebugHandler
{
    // The parameters of the constructor/method
    protected final List<ParamFragment> params;
    // Any types which are thrown by the constructor/method
    protected final List<ThrowsTypeFragment> throwsTypes;
    // The items in the body of the constructor/method
    protected final List<CodeElement> contents;
    // The frame we are linked to
    protected MethodFrameWithBody<?> frame;
    // Our access permission.
    protected AccessPermissionFragment access;

    /**
     * Constructor when generated from the GUI
     */
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

    /**
     * Constructor when loaded from file/clipboard
     */
    public MethodWithBodyElement(Element el)
    {
        access = new AccessPermissionFragment(AccessPermission.fromString(el.getAttributeValue("access")));
        params = new ArrayList<>();
        this.throwsTypes = new ArrayList<>();
        contents = new ArrayList<>();
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
        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }

    /**
     * Constructor when automatically generated, e.g. by Save the World in Greenfoot
     */
    public MethodWithBodyElement(String access, List<Entry<String,String>> params, List<CodeElement> contents, String documentation)
    {
        this.access = new AccessPermissionFragment(AccessPermission.fromString(access));
        this.contents = new ArrayList<>(contents);
        this.documentation = new JavadocUnit(documentation);
        
        this.throwsTypes = new ArrayList<>();
        
        this.params = new ArrayList<>();
        if (params != null ) {
            for (Entry<String, String> entry : params) {
                this.params.add(new ParamFragment(new TypeSlotFragment(entry.getKey(), entry.getKey()), new NameDefSlotFragment(entry.getValue())));
            }
        }
    }

    /**
     * Helper method for subclasses when generating XML: add access attribute to given XML
     */
    protected void accessToXML(LocatableElement methodEl)
    {
        if (access != null) {
            methodEl.addAttributeAccess("access", access);
        }
    }

    /**
     * Helper method for subclasses when generating XML: add params child element to given XML
     */
    protected void paramsToXML(Element methodEl)
    {
        Element paramsEl = new Element("params");
        for (ParamFragment param : params) {
            paramsEl.appendChild(param.toXML());
        }
        methodEl.appendChild(paramsEl);
    }

    /**
     * Helper method for subclasses when generating XML: add throws child element to given XML
     */
    protected void throwsToXML(Element methodEl)
    {
        Element throwsEl = new Element("throws");
        for (ThrowsTypeFragment t : throwsTypes) {
            throwsEl.appendChild(t.toXML());
        }
        methodEl.appendChild(throwsEl);
    }

    /**
     * Helper method for subclasses when generating XML: add body child element to given XML
     */
    protected void bodyToXML(Element methodEl)
    {
        Element bodyEl = new Element("body");
        for (CodeElement c : contents) {
            bodyEl.appendChild(c.toXML());
        }
        methodEl.appendChild(bodyEl);
    }

    /**
     * Helper method for subclasses when generating Java: Turn throws declaration into Java
     */
    protected List<JavaFragment> throwsToJava()
    {
        if (throwsTypes.isEmpty())
            return Collections.emptyList();
        
        ArrayList<JavaFragment> typesAndCommas = throwsTypes.stream().map(ThrowsTypeFragment::getJavaSource).collect(Utility.intersperse(() -> (JavaFragment)f(null, ", ")));
        
        typesAndCommas.add(0, space());
        typesAndCommas.add(0, f(frame, "throws"));
        typesAndCommas.add(0, space());
        
        return typesAndCommas;
    }

    /**
     * Helper method for subclasses when generating a Frame (in our frame field)
     */
    @OnThread(Tag.FX)
    protected void setupFrame(InteractionManager editor)
    {
        frame.setAccess(access.getValue());
        
        frame.setDocumentation(documentation.toString());
        
        params.forEach(item -> frame.getParamsPane().addFormal(item.getParamType(), item.getParamName()));
        contents.forEach(c -> frame.getCanvas().insertBlockAfter(c.createFrame(editor), null));
        throwsTypes.forEach(t -> frame.addThrows(t.getType()));
    }

    @Override
    public List<LocalParamInfo> getDeclaredVariablesWithin(CodeElement child)
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
    
    @OnThread(Tag.FXPlatform)
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
