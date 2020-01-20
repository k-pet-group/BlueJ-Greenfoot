/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018,2019,2020 Michael Kölling and John Rosenberg
 
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
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.editor.flow.HoleDocument;
import bluej.editor.flow.JavaSyntaxView;
import bluej.editor.flow.ScopeColorsBorderPane;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContent.ParamInfo;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ParsedTypeNode;
import bluej.stride.framedjava.ast.FrameFragment;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.frames.ClassFrame;
import bluej.stride.framedjava.frames.ConstructorFrame;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.parser.AssistContentThreadSafe;
import bluej.stride.generic.Frame.ShowReason;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import nu.xom.Attribute;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A code element corresponding to a top-level class.
 */
public class ClassElement extends DocumentContainerCodeElement implements TopLevelCodeElement
{
    public static final String ELEMENT = "class";
    /** The name of this class */
    private final NameDefSlotFragment className;
    /** The type we extend (null if none) */
    private final TypeSlotFragment extendsName;
    /** The list of types we implement (empty if none) */
    private final List<TypeSlotFragment> implementsList;
    /** The package name (will not be null, but package name within may be blank */
    private final String packageName;
    /** The list of imports in this class */
    private final List<ImportElement> imports;
    /** The list of fields in this class */
    private final List<CodeElement> fields;
    /** The list of constructors for this class */
    private final List<CodeElement> constructors;
    /** The list of methods in this class */
    private final List<CodeElement> methods;
    /** The resolver used by the project this class lives in (cached for convenience) */
    private final EntityResolver projectResolver;
    /** Is this class abstract (true) or not (false)? */
    private boolean abstractModifier = false;
    /** The documentation for this class */
    private JavadocUnit documentation;
    /** The frame corresponding to this code element (may be null) */
    private ClassFrame frame;
    /** The curly brackets and class keyword in the generated code (saved for mapping positions) */
    private final FrameFragment openingCurly;
    private final FrameFragment closingCurly;
    private JavaFragment classKeyword;
    /**
     * The generated Java code for this class, used for doing code completion without
     * needing to always regenerate the document.
     */
    private DocAndPositions sourceDocument;
    // Keep track of which slot was completing when we generated sourceDocument,
    // as this affects the content of the document, and we may have to regenerate.
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
     * (each ClassElement is immutable).
     */
    private final HashMap<String, DocAndPositions> documentCache = new HashMap<>();

    /**
     * Creates a class element from the given frame (when generating code elements for
     * analysis/compilation from the open editor)
     */
    public ClassElement(ClassFrame frame, EntityResolver projectResolver, boolean abstractModifier, NameDefSlotFragment className,
                        TypeSlotFragment extendsName, List<TypeSlotFragment> implementsList, List<? extends CodeElement> fields,
                        List<? extends CodeElement> constructors, List<? extends CodeElement> methods, JavadocUnit documentation,
                        String packageName, List<ImportElement> imports, boolean enabled)
    {
        this.frame = frame;
        this.openingCurly = new FrameFragment(this.frame, this, "{");
        this.closingCurly = new FrameFragment(this.frame, this, "}");
        this.abstractModifier = abstractModifier;
        this.className = className;
        this.extendsName = extendsName;
        this.documentation = documentation;
        this.packageName = (packageName == null) ? "" : packageName;
        this.imports = new LinkedList<>(imports);
        
        this.implementsList = new ArrayList<>(implementsList);
        
        this.fields = new ArrayList<>(fields);
        this.fields.forEach(field -> field.setParent(this));
        
        this.constructors = new ArrayList<>(constructors);
        this.constructors.forEach(constructor -> constructor.setParent(this));
        
        this.methods = new ArrayList<>(methods);
        this.methods.forEach(method -> method.setParent(this));

        this.enable = enabled;
        this.documentation = documentation;
        if (this.documentation == null) {
            this.documentation = new JavadocUnit("");
        }
        this.projectResolver = projectResolver;
    }

    /**
     * Creates a class element from the given XML element, used when loading code
     * from disk or from the clipboard.
     */
    public ClassElement(Element el, EntityResolver projectResolver, String packageName)
    {
        Attribute abstractAttribute = el.getAttribute("abstract");
        abstractModifier = (abstractAttribute == null) ? false : Boolean.valueOf(abstractAttribute.getValue());
        
        className = new NameDefSlotFragment(el.getAttributeValue("name"));
        final String extendsAttribute = el.getAttributeValue("extends");
        extendsName = (extendsAttribute != null) ? new TypeSlotFragment(extendsAttribute, el.getAttributeValue("extends-java")) : null;

        this.packageName = packageName;

        Element javadocEL = el.getFirstChildElement("javadoc");
        if (javadocEL != null) {
            documentation = new JavadocUnit(javadocEL);
        }
        if (documentation == null) {
            documentation = new JavadocUnit("");
        }
        implementsList = TopLevelCodeElement.xmlToTypeList(el, "implements", "implementstype", "type");
        imports = Utility.mapList(TopLevelCodeElement.fillChildrenElements(this, el, "imports"), e -> (ImportElement)e);
        fields = TopLevelCodeElement.fillChildrenElements(this, el, "fields");
        constructors = TopLevelCodeElement.fillChildrenElements(this, el, "constructors");
        methods = TopLevelCodeElement.fillChildrenElements(this, el, "methods");
       
        enable = Boolean.valueOf(el.getAttributeValue("enable"));
        this.projectResolver = projectResolver;
        this.openingCurly = new FrameFragment(null, this, "{");
        this.closingCurly = new FrameFragment(null, this, "}");
    }

    /**
     * Creates a class element with minimum information (when creating new class from template name)
     */
    public ClassElement(EntityResolver entityResolver, boolean abstractModifier, String className, String packageName,
                        List<? extends CodeElement> constructors)
    {
        this(null, entityResolver, abstractModifier, new NameDefSlotFragment(className), null,
                Collections.emptyList(), Collections.emptyList(), constructors, Collections.emptyList(),
                null, packageName, Collections.emptyList(), true);
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
        if (abstractModifier) {
            header.add(f(frame, "abstract "));
        }
        classKeyword = new FrameFragment(frame, this, "class ");
        Collections.addAll(header, classKeyword, className);
        
        if (extendsName != null && !extendsName.isEmpty()) {
            Collections.addAll(header, space(), f(frame, "extends"), space(), extendsName);
        }
        if (!implementsList.isEmpty())
        {
            header.addAll(Arrays.asList(space(), f(frame, "implements"), space()));
            header.addAll(implementsList.stream().collect(Utility.intersperse(() -> f(null, ", "))));
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
        constructors.stream().filter(c -> c.isEnable()).forEach(c -> {
            java.appendLine(Arrays.asList((JavaFragment) f(frame, "")), null);
            java.addIndented(c.toJavaSource());
        });
        methods.stream().filter(m -> m.isEnable()).forEach(m -> {
            java.appendLine(Arrays.asList((JavaFragment) f(frame, "")), null);
            java.addIndented(m.toJavaSource());
        });

        closingCurly.setFrame(frame);
        java.appendLine(Arrays.asList(closingCurly), null);
        return java;
    }
    
    @Override
    public LocatableElement toXML()
    {
        LocatableElement classEl = new LocatableElement(this, ELEMENT);
        if (abstractModifier) {
            classEl.addAttribute(new Attribute("abstract", String.valueOf(abstractModifier)));
        }
        classEl.addAttributeCode("name", className);
        if (extendsName != null) {
            classEl.addAttributeStructured("extends", extendsName);
        }
        addEnableAttribute(classEl);
        
        if (documentation != null) {
            classEl.appendChild(documentation.toXML());
        }

        appendCollection(classEl, imports, "imports");
        classEl.appendChild(TopLevelCodeElement.typeListToXML(implementsList, "implements", "implementstype", "type"));
        
        appendCollection(classEl, fields, "fields");
        appendCollection(classEl, constructors, "constructors");
        appendCollection(classEl, methods, "methods");

        classEl.addAttribute(TopLevelCodeElement.getStrideVersionAttribute());

        return classEl;
    }

    private void appendCollection(Element topEl, List<? extends CodeElement> collection, String name)
    {
        Element collectionEl = new Element(name);
        collection.forEach(element -> collectionEl.appendChild(element.toXML()));
        topEl.appendChild(collectionEl);
    }

    @Override
    public ClassFrame createFrame(InteractionManager editor)
    {
        frame = new ClassFrame(editor, projectResolver, packageName, imports, documentation, abstractModifier, className, extendsName, implementsList, isEnable());
        fields.forEach(member -> frame.getfieldsCanvas().insertBlockAfter(member.createFrame(editor), null));
        constructors.forEach(member -> frame.getConstructorsCanvas().insertBlockAfter(member.createFrame(editor), null));
        methods.forEach(member -> frame.getMethodsCanvas().insertBlockAfter(member.createFrame(editor), null));
        return frame;
    }
    
    @Override
    public ClassFrame createTopLevelFrame(InteractionManager editor)
    {
        return createFrame(editor);
    }
    
    public String getName()
    {
        return className.getContent();
    }

    @Override
    public List<CodeElement> childrenUpTo(CodeElement c)
    {
        List<CodeElement> joined = new ArrayList<>();
        joined.addAll(fields);
        joined.addAll(constructors);
        joined.addAll(methods);
        return joined.subList(0, joined.indexOf(c));
    }

    public JavaFragment getNameElement(ConstructorFrame frame)
    {
        return new JavaFragment() {
            @Override
            protected String getJavaCode(Destination dest, ExpressionSlot<?> completing, Parser.DummyNameGenerator dummyNameGenerator)
            {
                return getName();
            }

            @Override
            public ErrorShower getErrorShower() {
                return frame;
            }

            @Override
            protected JavaFragment getCompileErrorRedirect()
            {
                return null;
            }

            @Override
            public Stream<SyntaxCodeError> findEarlyErrors()
            {
                return Stream.empty();
            }

            @Override
            public void addError(CodeError codeError)
            {
                frame.addError(codeError);
            }
        };
    }

    @Override
    public ClassElement getTopLevelElement()
    {
        return this;
    }

    public ClassFrame getFrame()
    {
        return frame;
    }
    
    public PosInSourceDoc getPosInsideClass()
    {
        return openingCurly.getPosInSourceDoc(+1);
    }
    
    @Override
    public List<ImportElement> getImports()
    {
        return Collections.unmodifiableList(imports);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public ExpressionTypeInfo getCodeSuggestions(PosInSourceDoc pos, ExpressionSlot<?> completing)
    {
        // Must get document before getting position:
        JavaSyntaxView doc = getSourceDocument(completing);
        Optional<Integer> resolvedPos = resolvePos(doc, pos);
        return resolvedPos.map(rpos -> doc.getParser().getExpressionType(rpos, doc))
                          .orElse(null);
    }
    
    @OnThread(Tag.FXPlatform)
    private Optional<Integer> resolvePos(JavaSyntaxView doc, PosInSourceDoc pos)
    {
        DocAndPositions docAndPositions = documentCache.get(doc.getFullText());
        Optional<Integer> resolvedPos = Optional.ofNullable(docAndPositions.fragmentPositions.get(pos.getFragment()));
        return resolvedPos.map(p -> p + pos.offset);
    }

    @Override
    public String getStylePrefix()
    {
        return "class-";
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public EntityResolver getResolver()
    {
        return getSourceDocument(null).getParser();
    }

    @Override
    public InteractionManager getEditor()
    {
        return getFrame().getEditor();
    }

    @Override
    public void show(ShowReason reason)
    {
        frame.show(reason);        
    }

    @OnThread(Tag.FXPlatform)
    private JavaSyntaxView getSourceDocument(ExpressionSlot completing)
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
        result = Stream.concat(result, streamContained(constructors));
        return Stream.concat(result, streamContained(methods));
    }
    
    @Override
    protected Stream<SlotFragment> getDirectSlotFragments()
    {
        return Stream.<SlotFragment>of(className, extendsName).filter(s -> s != null);
    }
    
    public Stream<CodeElement> streamMethods()
    {
        return methods.stream();
    }

    @OnThread(Tag.FXPlatform)
    public Reflective qualifyType(String name, PosInSourceDoc pos)
    {
        final JavaSyntaxView doc = getSourceDocument(null);
        final Optional<Integer> rpos = resolvePos(doc, pos);
        if (!rpos.isPresent())
            return null;
        ParsedNode node = doc.getParser().findNodeAtOrAfter(rpos.get(), 0).getNode();
        if (node instanceof JavaParentNode)
        {
            JavaParentNode jNode = (JavaParentNode)node;
            JavaType t = jNode.resolvePackageOrClass(name, getClassNode()).getType();
            if (t instanceof GenTypeClass)
                return ((GenTypeClass)t).getReflective();
        }
        return null;
    }

    @OnThread(Tag.FXPlatform)
    private Reflective getClassNode()
    {
        // Must get document before getting position:
        JavaSyntaxView doc = getSourceDocument(null);
        ParsedNode node = doc.getParser().findNodeAtOrAfter(resolvePos(doc, classKeyword.getPosInSourceDoc()).get(), 0).getNode();
        if (node instanceof ParsedTypeNode)
        {
            return new ParsedReflective((ParsedTypeNode)node);
        }
        return null;
    }

    // Returns name of uppermost class with this method:
    @OnThread(Tag.FXPlatform)
    public Reflective findSuperMethod(String name, List<String> qualParamTypes)
    {
        if (classKeyword == null)
            return null;

        String superClass;

        if (extendsName == null || extendsName.getContent().isEmpty())
            superClass = "Object";
        else
            superClass = extendsName.getContent();

                // Make sure source document has been created:
        getSourceDocument(null);
        
        Reflective qualSuper = qualifyType(superClass, classKeyword.getPosInSourceDoc());
        while (qualSuper != null)
        {
            Set<MethodReflective> overloads = qualSuper.getDeclaredMethods().get(name);
                    
            if (overloads != null && overloads.stream().anyMatch(m -> qualParamTypes.equals(Utility.mapList(m.getParamTypes(), t -> t.toString(false)))))
                return qualSuper;
        
            qualSuper = qualSuper.getSuperTypesR().stream().filter(r -> !r.isInterface()).findFirst().orElse(null);
        }
        return null;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void updateSourcePositions()
    {
        getSourceDocument(null);
    }

    public boolean isAbstract()
    {
        return abstractModifier;
    }

    public String getExtends()
    {
        return extendsName != null ? extendsName.getContent() : null;
    }

    public List<String> getImplements()
    {
        return Utility.mapList(implementsList, TypeSlotFragment::getContent);
    }

    public List<? extends CodeElement> getMethods()
    {
        return methods;
    }

    public List<? extends CodeElement> getFields()
    {
        return fields;
    }

    public List<? extends CodeElement> getConstructors()
    {
        return constructors;
    }

    @OnThread(Tag.FXPlatform)
    @Override
    public List<ConstructorReflective> getSuperConstructors()
    {
        if (extendsName == null || extendsName.getContent().isEmpty() || classKeyword == null)
        {
            return Collections.emptyList();
        }

        // Make sure source document has been created:
        getSourceDocument(null);

        Reflective qualSuper = qualifyType(extendsName.getContent(), classKeyword.getPosInSourceDoc());
        if (qualSuper != null)
        {
            return qualSuper.getDeclaredConstructors();
        }
        return Collections.emptyList();
    }

    @Override
    public List<AssistContentThreadSafe> getThisConstructors()
    {
        return constructors.stream()
            .filter(c -> c instanceof ConstructorElement)
            .map(c -> (ConstructorElement)c)
            .map(c -> {
                List<ParamInfo> paramInfo = Utility.mapList(c.getParams(), p -> new ParamInfo(p.getParamType().getContent(), p.getParamName().getContent(), "", () -> ""));
                return new AssistContentThreadSafe(c.getAccessPermission().asAccess(), getName(), c.getDocumentation(), CompletionKind.CONSTRUCTOR, getName(), null, paramInfo, null, null, null);
            })
            .collect(Collectors.toList());
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
    @OnThread(Tag.FXPlatform)
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        return findEarlyErrors(toXML().buildLocationMap());
    }
}
