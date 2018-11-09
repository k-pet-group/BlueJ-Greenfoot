/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018 Michael Kölling and John Rosenberg
 
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import nu.xom.Attribute;
import nu.xom.Element;
import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaSingleLineDebugHandler;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.framedjava.frames.VarFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.ShowReason;

public class VarElement extends CodeElement implements JavaSingleLineDebugHandler
{
    public static final String ELEMENT = "variable";
    private final AccessPermissionFragment varAccess;
    private boolean staticModifier = false;
    private boolean finalModifier = false;
    private final TypeSlotFragment varType;
    private final NameDefSlotFragment varName;
    private final FilledExpressionSlotFragment varValue;
    private VarFrame frame;
    
    // varValue is optional and can be null
    public VarElement(VarFrame frame, AccessPermissionFragment varAccess, boolean staticModifier, 
            boolean finalModifier, TypeSlotFragment varType, NameDefSlotFragment varName, 
            FilledExpressionSlotFragment varValue, boolean enabled)
    {
        this.frame = frame;
        this.varAccess = varAccess;
        this.staticModifier = staticModifier;
        this.finalModifier = finalModifier;
        this.varType = varType;
        this.varName = varName;
        this.varValue = varValue;
        this.enable = enabled;
    }
    
    public VarElement(Element el)
    {
        Attribute accessAttribute = el.getAttribute("access");
        varAccess = (accessAttribute == null) ? null : 
            new AccessPermissionFragment(AccessPermission.fromString(accessAttribute.getValue()));
        
        Attribute staticAttribute = el.getAttribute("static");
        staticModifier = (staticAttribute == null) ? false : Boolean.valueOf(staticAttribute.getValue());
        
        Attribute finalAttribute = el.getAttribute("final");
        finalModifier = (finalAttribute == null) ? false : Boolean.valueOf(finalAttribute.getValue());
        
        varType = new TypeSlotFragment(el.getAttributeValue("type"), el.getAttributeValue("type-java"));
        varName = new NameDefSlotFragment(el.getAttributeValue("name"));
        
        Attribute valueAttribute = el.getAttribute("value");
        varValue = (valueAttribute == null) ? null : new FilledExpressionSlotFragment(valueAttribute.getValue(), el.getAttributeValue("value-java"));
        
        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }

    public VarElement(String access, String type, String name, String value)
    {
        varAccess = (access == null) ? null : new AccessPermissionFragment(AccessPermission.fromString(access));
        varType = new TypeSlotFragment(type, type);
        varName = new NameDefSlotFragment(name);
        varValue = (value == null) ? null : new FilledExpressionSlotFragment(value, value);
    }

    @Override
    public JavaSource toJavaSource()
    {
        List<JavaFragment> fragments = new ArrayList<JavaFragment>();
        if (varAccess != null) {
            fragments.addAll(Arrays.asList(varAccess, space()));
        }
        if (staticModifier) {
            fragments.add(f(frame, "static "));
        }
        if (finalModifier) {
            fragments.add(f(frame, "final "));
        }
        
        fragments.addAll(Arrays.asList(varType, space(), varName));
        if (varValue != null) {
            fragments.addAll(Arrays.asList(f(null, " = "), varValue));
        }
        fragments.add(f(frame, ";"));
        return new JavaSource(this, fragments);
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement varEl = new LocatableElement(this, ELEMENT);
        if (varAccess != null) {
            varEl.addAttributeAccess("access", varAccess);
        }
        if (staticModifier) {
            varEl.addAttribute(new Attribute("static", "true"));
        }
        if (finalModifier) {
            varEl.addAttribute(new Attribute("final", "true"));
        }
        varEl.addAttributeStructured("type", varType);
        varEl.addAttributeCode("name", varName);
        if (varValue != null) {
            varEl.addAttributeStructured("value", varValue);
        }
        addEnableAttribute(varEl);
        return varEl;
    }
    
    @Override
    public Frame createFrame(InteractionManager editor)
    {
        frame = new VarFrame(editor, varAccess, staticModifier, finalModifier, varType, varName, varValue, isEnable() );
        return frame;
    }

    @Override
    public List<LocalParamInfo> getDeclaredVariablesAfter()
    {
        return Collections.singletonList(new LocalParamInfo(varType.getContent(), varName.getContent(), false, this));
    }

    @Override
    public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
    {
        return frame.showDebugBefore(debug);
    }
    
    @Override
    public void show(ShowReason reason)
    {
        frame.show(reason);        
    }

    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.<SlotFragment>of(varType, varName, varValue).filter(s -> s != null);
    }

    public boolean isStatic()
    {
        return staticModifier;
    }

    public boolean isFinal()
    {
        return finalModifier;
    }

    public String getType()
    {
        return varType.getContent();
    }

    public String getName()
    {
        return varName.getContent();
    }

    public String getValue()
    {
        return varValue != null ? varValue.getContent() : null;
    }
}
