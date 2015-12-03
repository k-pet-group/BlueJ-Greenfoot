/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015 Michael Kölling and John Rosenberg 
 
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

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.generic.InteractionManager;
import nu.xom.Attribute;
import nu.xom.Element;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.TextSlotFragment;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.frames.ImportFrame;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.Frame.ShowReason;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.TextSlot;

public class ImportElement extends CodeElement
{
    public static final String ELEMENT = "import";
    private ImportFrame frame;
    private final ImportFragment importValue;
    
    public ImportElement(Element el)
    {
        importValue = new ImportFragment(el.getAttributeValue("target"), null);
        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }
    
    public ImportElement(String src, TextSlot<ImportFragment> slot, boolean enabled)
    {
        importValue = new ImportFragment(src, slot);
        this.enable = enabled;
    }

    @Override
    public JavaSource toJavaSource()
    {
        return new JavaSource(null, f(frame, "import "), importValue, f(frame, ";"));
    }

    @Override
    public Element toXML()
    {
        Element importEl = new Element(ELEMENT);
        importEl.addAttribute(new Attribute("target", importValue.getContent()));
        addEnableAttribute(importEl);
        return importEl;
    }

    @Override
    public ImportFrame createFrame(InteractionManager editor)
    {
        frame = new ImportFrame(editor, this, isEnable());
        return frame;
    }
    
    @Override
    public void show(ShowReason reason)
    {
        frame.show(reason);        
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.of(importValue);
    }
    
    public static class ImportFragment extends TextSlotFragment
    {
        private TextSlot<ImportFragment> slot;

        public ImportFragment(String content, TextSlot slot)
        {
            super(content);
            this.slot = slot;
        }

        @Override
        public void registerSlot(TextSlot slot)
        {
            this.slot = slot;
        }

        @Override
        public Future<List<CodeError>> findLateErrors(InteractionManager editor, CodeElement parent)
        {
            // No late errors at the moment.  Could provide typo corrections.
            return null;
        }

        @Override
        protected String getJavaCode(Destination dest, ExpressionSlot<?> completing)
        {
            return getContent();
        }

        @Override
        public EditableSlot getSlot()
        {
            return slot;
        }

        @Override
        public Stream<SyntaxCodeError> findEarlyErrors()
        {
            return Stream.empty();
        }
    }

    public ImportFragment getImport()
    {
        return importValue;
    }
}
