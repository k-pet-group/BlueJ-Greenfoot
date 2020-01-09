/*
 This file is part of the BlueJ program.
 Copyright (C) 2014,2015,2016,2019,2020 Michael Kölling and John Rosenberg

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

import bluej.debugger.gentype.ConstructorReflective;
import bluej.editor.flow.HoleDocument;
import bluej.editor.flow.JavaSyntaxView;
import bluej.editor.flow.ScopeColorsBorderPane;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.entity.EntityResolver;
import bluej.parser.nodes.ReparseableDocument;
import bluej.stride.framedjava.ast.FrameFragment;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.frames.InterfaceFrame;
import bluej.stride.framedjava.frames.TopLevelFrame;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.parser.AssistContentThreadSafe;
import bluej.stride.generic.Frame.ShowReason;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Stream;

public class InterfaceElement extends DocumentContainerCodeElement implements TopLevelCodeElement
{
    public static final String ELEMENT = "interface";
    private final NameDefSlotFragment interfaceName;   
    private final List<TypeSlotFragment> extendsTypes;
    private JavadocUnit documentation;

    /** The package name (will not be null, but package name within may be blank */
    private final String packageName;
    private final List<ImportElement> imports;
    /** The list of fields in this class */
    private final List<CodeElement> fields;
    /** The list of methods in this class */
    private final List<CodeElement> methods;
    private final EntityResolver projectResolver;
    private InterfaceFrame frame;
    /** The curly brackets and interface keyword in the generated code (saved for mapping positions) */
    private final FrameFragment openingCurly = new FrameFragment(this.frame, this, "{");
    private final FrameFragment closingCurly = new FrameFragment(this.frame, this, "}");
    private JavaFragment interfaceKeyword;
    /**
     * The generated Java code for this interface, used for doing code completion without
     * needing to always regenerate the document.
     */
    private DocAndPositions sourceDocument;
    // Keep track of which slot was active when we generated the document,
    // as if affects results:
    private ExpressionSlot<?> sourceDocumentCompleting;
    /**
     * A map of documents for given contents.  This guards against race hazards, so
     * that we use the correct document for the given content, even when we are hopping
     * across threads and potentially generating several documents in a short space
     * of time, concurrent with looking up information in them.
     *
     * This cache does not have a size limit, but that shouldn't matter as it is per-instance
     * so the only potential differences in source code are down to which slot is being completed,
     * giving a limit on the number of documents we could generate for a given source version
     * (each InterfaceElement is immutable).
     */
    private final HashMap<String, DocAndPositions> documentCache = new HashMap<>();
    public InterfaceElement(InterfaceFrame frame, EntityResolver projectResolver, NameDefSlotFragment interfaceName,
                List<TypeSlotFragment> extendsTypes, List<CodeElement> fields, List<CodeElement> methods,
                JavadocUnit documentation, String packageName, List<ImportElement> imports, boolean enabled)
    {
        this.frame = frame;
        this.interfaceName = interfaceName;
        this.extendsTypes = extendsTypes == null ? new ArrayList<>() : new ArrayList<>(extendsTypes);
        this.documentation = documentation != null ? documentation : new JavadocUnit("");

        this.packageName = (packageName == null) ? "" : packageName;

        this.imports = new ArrayList<>(imports);

        this.fields = new ArrayList<>(fields);
        this.fields.forEach(field -> field.setParent(this));

        this.methods = new ArrayList<>(methods);
        this.methods.forEach(method -> method.setParent(this));

        this.enable = enabled;
        this.projectResolver = projectResolver;
    }

    public InterfaceElement(Element el, EntityResolver projectResolver, String packageName)
    {
        this.projectResolver = projectResolver;
        interfaceName = new NameDefSlotFragment(el.getAttributeValue("name"));
        Element javadocEL = el.getFirstChildElement("javadoc");
        if (javadocEL != null)
        {
            documentation = new JavadocUnit(javadocEL);
        }
        else
        {
            documentation = new JavadocUnit("");
        }
        extendsTypes = TopLevelCodeElement.xmlToTypeList(el, "extends", "extendstype", "type");

        this.packageName = packageName;

        imports = Utility.mapList(TopLevelCodeElement.fillChildrenElements(this, el, "imports"), e -> (ImportElement)e);
        fields = TopLevelCodeElement.fillChildrenElements(this, el, "fields");
        methods = TopLevelCodeElement.fillChildrenElements(this, el, "methods");

        enable = Boolean.valueOf(el.getAttributeValue("enable"));
    }

    /**
     * Creates an interface element with minimum information (when creating new interface from template name)
     */
    public InterfaceElement(EntityResolver entityResolver, String interfaceName, String packageName)
    {
        this(null, entityResolver, new NameDefSlotFragment(interfaceName), null, Collections.emptyList(),
                Collections.emptyList(), null, packageName, Collections.emptyList(), true);
    }

    @Override
    public ExpressionTypeInfo getCodeSuggestions(PosInSourceDoc pos, ExpressionSlot<?> completing)
    {
        // Must get document before asking for completions:
        ReparseableDocument doc = getSourceDocument(completing);
        return doc.getParser().getExpressionType(pos.offset, getSourceDocument(completing));
    }

    @Override
    public LocatableElement toXML()
    {
        LocatableElement interfaceEl = new LocatableElement(this, ELEMENT);
        interfaceEl.addAttributeCode("name", interfaceName);
        if (!extendsTypes.isEmpty())
        {
            interfaceEl.appendChild(
                TopLevelCodeElement.typeListToXML(
                    extendsTypes,
                    "extends", "extendstype", "type"));
        }
        addEnableAttribute(interfaceEl);
        
        if (documentation != null) {
            interfaceEl.appendChild(documentation.toXML());
        }

        appendCollection(interfaceEl, imports, "imports");
        appendCollection(interfaceEl, fields, "fields");
        appendCollection(interfaceEl, methods, "methods");

        interfaceEl.addAttribute(TopLevelCodeElement.getStrideVersionAttribute());
        return interfaceEl;
    }

    private void appendCollection(Element topEl, List<? extends CodeElement> collection, String name)
    {
        Element collectionEl = new Element(name);
        collection.forEach(element -> collectionEl.appendChild(element.toXML()));
        topEl.appendChild(collectionEl);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public JavaSource toJavaSource()
    {
        return getDAP(null).java;
    }

    @OnThread(Tag.FXPlatform)
    private JavaSource generateJavaSource()
    {
        List<JavaFragment> header = new ArrayList<>();
        header.add(new FrameFragment(frame, this, "public "));
        interfaceKeyword = new FrameFragment(frame, this, "interface ");
        Collections.addAll(header, interfaceKeyword, interfaceName);

        if (!extendsTypes.isEmpty()) {
            Collections.addAll(header, space(), f(frame, "extends"), space());
            header.addAll(extendsTypes.stream().collect(Utility.intersperse(() -> f(frame, ", "))));
        }

        JavaSource java = new JavaSource(null, header);
        java.prependJavadoc(documentation.getJavaCode());

        java.prependLine(Arrays.asList((JavaFragment) f(frame, "")), null);
        Utility.backwards(CodeElement.toJavaCodes(imports)).forEach(imp -> java.prepend(imp));
        java.prependLine(Collections.singletonList(f(frame, "import lang.stride.*;")), null);

        if (!packageName.equals(""))
            java.prependLine(Arrays.asList(f(frame, "package " + packageName + ";")), null);
        
        openingCurly.setFrame(frame);
        java.appendLine(Arrays.asList(openingCurly), null);
        fields.stream().filter(f -> f.isEnable()).forEach(f -> java.addIndented(f.toJavaSource()));
        methods.stream().filter(m -> m.isEnable()).forEach(m -> {
            java.appendLine(Arrays.asList((JavaFragment) f(frame, "")), null);
            java.addIndented(m.toJavaSource());
        });

        closingCurly.setFrame(frame);
        java.appendLine(Arrays.asList(closingCurly), null);
        return java;
    }

    @Override
    public InterfaceFrame createFrame(InteractionManager editor)
    {
        frame = new InterfaceFrame(editor, projectResolver, packageName, imports, documentation, interfaceName, extendsTypes, enable);
        fields.forEach(member -> frame.getfieldsCanvas().insertBlockAfter(member.createFrame(editor), null));
        methods.forEach(member -> frame.getMethodsCanvas().insertBlockAfter(member.createFrame(editor), null));
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

    public List<String> getExtends()
    {
        return Utility.mapList(extendsTypes, TypeSlotFragment::getContent);
    }

    public List<? extends CodeElement> getMethods()
    {
        return methods;
    }

    public List<? extends CodeElement> getFields()
    {
        return fields;
    }

    @Override
    public List<CodeElement> childrenUpTo(CodeElement c)
    {
        List<CodeElement> joined = new ArrayList<>();
        joined.addAll(fields);
        joined.addAll(methods);
        return joined.subList(0, joined.indexOf(c));
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
    
    @OnThread(Tag.FXPlatform)
    private ReparseableDocument getSourceDocument(ExpressionSlot completing)
    {
        JavaSyntaxView doc = getDAP(completing).getDocument(projectResolver);
        // There is no scheduled parsing for off-screen documents so we must manually finish any reparsing:
        doc.flushReparseQueue();
        return doc;
    }

    @OnThread(Tag.FXPlatform)
    private synchronized DocAndPositions getDAP(ExpressionSlot completing)
    {
        if (sourceDocument == null || sourceDocumentCompleting != completing)
        {
            IdentityHashMap<JavaFragment, Integer> positions = new IdentityHashMap<>();
            sourceDocumentCompleting = completing;
            JavaSource java = generateJavaSource();
            String src = java.toMemoryJavaCodeString(positions, completing);
            if (documentCache.containsKey(src))
            {
                // No need to generate and parse it again, just use existing one, but
                // add in our positions in case they used different fragments:
                sourceDocument = documentCache.get(src);
                sourceDocument.fragmentPositions.putAll(positions);
            }
            else
            {
                sourceDocument = new DocAndPositions(src, java, positions);
                documentCache.put(src, sourceDocument);
            }
        }
        return sourceDocument;
    }
    
    @Override
    public Stream<CodeElement> streamContained()
    {
        Stream<CodeElement> result = streamContained(fields);
        return Stream.concat(result, streamContained(methods));
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.<SlotFragment>concat(Stream.<SlotFragment>of(interfaceName), extendsTypes.stream()).filter(s -> s != null);
    }

    @Override
    public void updateSourcePositions()
    {
        Platform.runLater(() -> getSourceDocument(null));
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

    private static class DocAndPositions
    {
        public final JavaSource java;
        public final IdentityHashMap<JavaFragment, Integer> fragmentPositions;
        private String src;
        private JavaSyntaxView document;

        public DocAndPositions(String src, JavaSource java, IdentityHashMap<JavaFragment, Integer> fragmentPositions)
        {
            this.src = src;
            this.java = java;
            this.fragmentPositions = fragmentPositions;
        }

        @OnThread(Tag.FXPlatform)
        public JavaSyntaxView getDocument(EntityResolver projectResolver)
        {
            if (document == null)
            {
                HoleDocument doc = new HoleDocument();
                this.document = new JavaSyntaxView(doc, null, new ScopeColorsBorderPane(), projectResolver, new ReadOnlyBooleanWrapper(false));
                doc.replaceText(0, 0, src);
                this.document.enableParser(true);
            }
            return document;
        }
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        return findEarlyErrors(toXML().buildLocationMap());
    }
}
