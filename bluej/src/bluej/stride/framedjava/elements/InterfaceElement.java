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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import bluej.debugger.gentype.ConstructorReflective;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager;
import nu.xom.Attribute;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.CodeSuggestions;
import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.frames.InterfaceFrame;
import bluej.stride.framedjava.frames.TopLevelFrame;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.Frame.ShowReason;
import bluej.utility.Debug;
import bluej.utility.Utility;

public class InterfaceElement extends DocumentContainerCodeElement implements TopLevelCodeElement
{
    public static final String ELEMENT = "interface";
    private final NameDefSlotFragment interfaceName;   
    private final List<TypeSlotFragment> extendsTypes;
    private JavadocUnit documentation;
    
    private final List<ImportElement> imports;
    private final List<CodeElement> members;
    private InterfaceFrame frame;
    private JavaFragment openingCurly;
    
    private final EntityResolver projectResolver;
    private MoeSyntaxDocument sourceDocument;
    // Keep track of which slot was active when we generated the document,
    // as if affects results:
    private ExpressionSlot<?> sourceDocumentCompleting;
    
    public InterfaceElement(InterfaceFrame frame, EntityResolver projectResolver, NameDefSlotFragment interfaceName, List<TypeSlotFragment> extendsTypes, List<CodeElement> members, 
            JavadocUnit documentation, List<ImportElement> imports, boolean enabled)
    {
        this.frame = frame;
        this.interfaceName = interfaceName;
        this.extendsTypes = new ArrayList<>(extendsTypes);
        this.documentation = documentation;
        this.imports = new ArrayList<>(imports);
        this.members = new ArrayList<CodeElement>(members);
        for (CodeElement member : members) {
            member.setParent(this);
        }
        this.enable = enabled;
        
        if (documentation == null) {
            documentation = new JavadocUnit("");
        }
        
        this.projectResolver = projectResolver;        
    }

    public InterfaceElement(Element el, EntityResolver projectResolver)
    {
        this.projectResolver = projectResolver;
        interfaceName = new NameDefSlotFragment(el.getAttributeValue("name"));
        imports = Utility.mapList(TopLevelCodeElement.fillChildrenElements(this, el, "imports"), e -> (ImportElement)e);
        members = TopLevelCodeElement.fillChildrenElements(this, el, "methods");
        Element javadocEL = el.getFirstChildElement("javadoc");
        if (javadocEL != null) {
            documentation = new JavadocUnit(javadocEL);
        }
        if (documentation == null) {
            documentation = new JavadocUnit("");
        }
        extendsTypes = Utility.mapList(TopLevelCodeElement.xmlToStringList(el, "extends", "extendstype", "type"), (Function<String, TypeSlotFragment>)TypeSlotFragment::new); 
                
        enable = new Boolean(el.getAttributeValue("enable"));
        if (documentation == null) {
            documentation = new JavadocUnit("");
        }
    }

    @Override
    public CodeSuggestions getCodeSuggestions(PosInSourceDoc pos, ExpressionSlot<?> completing)
    {
        // Must get document before asking for position:
        MoeSyntaxDocument doc = getSourceDocument(completing);
        return doc.getParser().getExpressionType(0 /* TODO */, getSourceDocument(completing));
    }

    @Override
    public Element toXML()
    {
        Element interfaceEl = new Element(ELEMENT);
        interfaceEl.addAttribute(new Attribute("name", interfaceName.getContent()));
        if (!extendsTypes.isEmpty())
        {
            interfaceEl.appendChild(
                TopLevelCodeElement.stringListToXML(
                    Utility.mapList(extendsTypes, TypeSlotFragment::getContent),
                    "extends", "extendstype", "type"));
        }
        addEnableAttribute(interfaceEl);
        
        if (documentation != null) {
            interfaceEl.appendChild(documentation.toXML());
        }
        
        Element importsEl = new Element("imports");
        imports.forEach(imp -> importsEl.appendChild(imp.toXML()));
        interfaceEl.appendChild(importsEl);
        
        members.forEach(e -> interfaceEl.appendChild(e.toXML()));
        return interfaceEl;
    }

    @Override
    public JavaSource toJavaSource()
    {
        JavaSource java;
        openingCurly = f(frame, "{");
        if (extendsTypes.isEmpty()) {
            java = new JavaSource(null, f(frame, "public interface "), interfaceName, openingCurly);
        }
        else {
            ArrayList<JavaFragment> line = new ArrayList<>();
            line.addAll(Arrays.asList(f(frame, "public interface "), interfaceName, f(frame, " extends ")));
            line.addAll(extendsTypes.stream().collect(Utility.intersperse(() -> f(frame, ", "))));
            line.add(openingCurly);
            java = new JavaSource(null, line);
        }
        
        java.prependJavadoc(documentation.getJavaCode());
        
        // TODO What if the import is a specific class which isn't found?
        // TODO pop up imports dialog in this case?
        imports.forEach(imp -> java.prependLine(Arrays.asList((JavaFragment)f(frame, "import " + imp + ";")), null));
        java.prependLine(Arrays.asList((JavaFragment)f(frame, "// WARNING: This file is auto-generated and any changes to it will be overwritten")), null);
        
        members.forEach(c -> {
            if (c.isEnable()) {
                java.addIndented(c.toJavaSource());
            }
        });
        
        java.appendLine(Arrays.asList((JavaFragment)f(frame, "}")), null);
        return java;
    }

    @Override
    public InterfaceFrame createFrame(InteractionManager editor)
    {
        frame = new InterfaceFrame(editor, interfaceName, extendsTypes, projectResolver, documentation, enable);
        members.forEach(member -> frame.getCanvas().insertBlockAfter(member.createFrame(editor), null));
        return frame;
    }
    
    @Override
    public InterfaceFrame createTopLevelFrame(InteractionManager editor)
    {
        return createFrame(editor);
    }

    @Override
    public List<ImportElement> getImports()
    {
        return Collections.unmodifiableList(imports);
    }

    @Override
    public String getName()
    {
        return interfaceName.getContent();
    }

    @Override
    public List<CodeElement> childrenUpTo(CodeElement c)
    {
        return members.subList(0, members.indexOf(c));
    }
    
    @Override
    public String getStylePrefix()
    {
        return "interface-";
    }
    
    @Override
    public EntityResolver getResolver()
    {
        return getSourceDocument(null).getParser();
    }

    @Override
    public TopLevelFrame getFrame()
    {
        return frame;
    }
    
    @Override
    public InteractionManager getEditor()
    {
        return frame.getEditor();
    }
    
    @Override
    public void show(ShowReason reason)
    {
        frame.show(reason);        
    }
    
    @OnThread(Tag.Swing)
    private MoeSyntaxDocument getSourceDocument(ExpressionSlot<?> completing)
    {
        if (sourceDocument == null || sourceDocumentCompleting != completing)
        {
            sourceDocument = new MoeSyntaxDocument(projectResolver);
            sourceDocumentCompleting = completing;
            try {
                String src = toJavaSource().toMemoryJavaCodeString(null /* TODO */, completing);
                sourceDocument.insertString(0, src, null);
                sourceDocument.enableParser(true);
            }
            catch (BadLocationException e) {
                Debug.reportError(e);
            }
        }
        return sourceDocument;
    }
    
    @Override
    public Stream<CodeElement> streamContained()
    {
        return streamContained(members);
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.<SlotFragment>concat(Stream.<SlotFragment>of(interfaceName), extendsTypes.stream()).filter(s -> s != null);
    }

    @Override
    public void updateSourcePositions()
    {
        SwingUtilities.invokeLater(() -> getSourceDocument(null));
    }

    @Override
    public List<ConstructorReflective> getSuperConstructors()
    {
        // No constructors in interfaces:
        return Collections.emptyList();
    }

    @Override
    public List<AssistContentThreadSafe> getThisConstructors()
    {
        // No constructors in an interface:
        return Collections.emptyList();
    }
}
