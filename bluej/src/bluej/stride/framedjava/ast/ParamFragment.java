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
package bluej.stride.framedjava.ast;

import java.util.List;

import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.LocatableElement;
import bluej.utility.Debug;
import nu.xom.Attribute;
import nu.xom.Element;
import bluej.stride.generic.Frame;

public class ParamFragment
{
    public static final String ELEMENT = "params";
    private final TypeSlotFragment paramType;
    private final NameDefSlotFragment paramName;
    
    public ParamFragment(TypeSlotFragment paramType, NameDefSlotFragment paramName)
    {
        this.paramType = paramType;
        this.paramName = paramName;
    }
    
    public ParamFragment(Element el)
    {
        paramType = new TypeSlotFragment(el.getAttributeValue("type"), el.getAttributeValue("type-java"));
        paramName = new NameDefSlotFragment(el.getAttributeValue("name"));
    }

    public Element toXML()
    {
        LocatableElement paramEl = new LocatableElement(null, ELEMENT);
        paramEl.addAttributeStructured("type", paramType);
        paramEl.addAttributeCode("name", paramName);
        return paramEl;
    }
    
    public TypeSlotFragment getParamType()
    {
        return paramType;
    }
    
    public NameDefSlotFragment getParamName()
    {
        return paramName;
    }
    
    public static void addParamsToHeader(Frame frame, CodeElement src, List<ParamFragment> params, List<JavaFragment> header)
    {
        for (int i = 0; i < params.size();i++)
        {
            header.add(params.get(i).getParamType());
            header.add(new FrameFragment(frame, src, " "));
            header.add(params.get(i).getParamName());
            if (i != params.size() - 1) {
                header.add(new FrameFragment(frame, src, ", "));
            }
        }
    }
}
/*
{
    public static final String ELEMENT = "param";
    private final TypeSlotFragment paramType;
    private final NameDefSlotFragment paramName;
    
    public ParamFragment(TypeSlotFragment paramType, NameDefSlotFragment paramName)
    {
        this.paramType = paramType;
        this.paramName = paramName;
    }

    @Override
    public String getJavaCode(boolean forceValid)
    {
        String t = !forceValid || Parser.parseableAsType(paramType.getContent()) ? paramType.getContent() : "int";
        String n = !forceValid || Parser.parseableAsNameDef(paramName.getContent()) ? paramName.getContent() : Parser.generateNewDummyName();
        return "final " + t + " " + n;
    }

    public Element toXML()
    {
        Element paramEl = new Element(ELEMENT);
        paramEl.addAttribute(new Attribute("type", paramType.getContent()));
        paramEl.addAttribute(new Attribute("name", paramName.getContent()));
        return paramEl;
    }
    
    public ParamFragment(Element el)
    {
        paramType = new TypeSlotFragment(el.getAttributeValue("type"));
        paramName = new NameDefSlotFragment(el.getAttributeValue("name"));
    }
    
    public TypeSlotFragment getParamType()
    {
        return paramType;
    }
    
    public NameDefSlotFragment getParamName()
    {
        return paramName;
    }

    @Override
    public void recordPosition(int positionInFile, int lineNo, int columnNo)
    {
        // Currently, final is always added in front of param type in the method declaration.
        int paramTypeShift = "final".length() + 1;
        paramType.recordPosition(positionInFile + paramTypeShift, lineNo, columnNo + paramTypeShift);
        int paramNameShift = paramType.getContent().length() + 1;
        paramName.recordPosition(paramType.getPositionInFile(0) + paramNameShift, lineNo, paramType.getPositionInLine() + paramNameShift);
    }
    
    @Override
    public boolean positionRecorded()
    {
        return paramType.positionRecorded() && paramName.positionRecorded();
    }

    @OnThread(Tag.FX)
    public Stream<RecallableFocus> getFocusables()
    {
        return Stream.of(paramType.getSlot(), paramName.getSlot());
    }
    
    public Stream<CodeError> getCurrentErrors()
    {
        return Stream.concat(paramType.getCurrentErrors(), paramName.getCurrentErrors());
    }
    
    public boolean validForCompilation()
    {
        return paramType.validForCompilation() && paramName.validForCompilation();
    }
    
    @OnThread(Tag.FX)
    public void showDetectedErrors()
    {
        paramType.showDetectedErrors();
        paramName.showDetectedErrors();
    }
}*/
