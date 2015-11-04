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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import bluej.debugger.gentype.ConstructorReflective;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContent.ParamInfo;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager;
import nu.xom.Attribute;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.CodeSuggestions;
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
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.framedjava.frames.ClassFrame;
import bluej.stride.framedjava.frames.ConstructorFrame;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.Frame.ShowReason;
import bluej.utility.Debug;
import bluej.utility.Utility;

public class ClassElement extends DocumentContainerCodeElement implements TopLevelCodeElement
{
    public static final String ELEMENT = "class";
    private final NameDefSlotFragment className;   
    private final TypeSlotFragment extendsName;
    private final List<TypeSlotFragment> implementsList;
    private final List<ImportElement> imports;
    private final List<CodeElement> fields;
    private final List<CodeElement> constructors;
    private final List<CodeElement> methods;
    private final EntityResolver projectResolver;
    private boolean abstractModifier = false;
    private JavadocUnit documentation;
    private ClassFrame frame;
    private final FrameFragment openingCurly = new FrameFragment(frame, "{");
    private final FrameFragment closingCurly = new FrameFragment(frame, "}");
    private JavaFragment classKeyword;
    private DocAndPositions sourceDocument;
    // Keep track of which slot was completing when we generated sourceDocument,
    // as this affects the content of the document, and we may have to regenerate.
    private ExpressionSlot<?> sourceDocumentCompleting;
    private HashMap<String, DocAndPositions> documentCache = new HashMap<>();

    public ClassElement(ClassFrame frame, EntityResolver projectResolver, boolean abstractModifier, NameDefSlotFragment className, 
            TypeSlotFragment extendsName, List<TypeSlotFragment> implementsList, List<CodeElement> fields, List<CodeElement> constructors, List<CodeElement> methods,
            JavadocUnit documentation, List<ImportElement> imports, boolean enabled)
    {
        this.frame = frame;
        this.abstractModifier = abstractModifier;
        this.className = className;
        this.extendsName = extendsName;
        this.documentation = documentation;
        this.imports = new LinkedList<>(imports);
        
        this.implementsList = new ArrayList<>(implementsList);
        
        this.fields = new ArrayList<CodeElement>(fields);
        this.fields.forEach(field -> field.setParent(this));
        
        this.constructors = new ArrayList<CodeElement>(constructors);
        this.constructors.forEach(constructor -> constructor.setParent(this));
        
        this.methods = new ArrayList<CodeElement>(methods);
        this.methods.forEach(method -> method.setParent(this));

        this.enable = enabled;
        if (documentation == null) {
            documentation = new JavadocUnit("");
        }
        this.projectResolver = projectResolver;
    }


    public ClassElement(Element el, EntityResolver projectResolver)
    {
        Attribute abstractAttribute = el.getAttribute("abstract");
        abstractModifier = (abstractAttribute == null) ? false : new Boolean(abstractAttribute.getValue());
        
        className = new NameDefSlotFragment(el.getAttributeValue("name"));
        final String extendsAttribute = el.getAttributeValue("extends");
        extendsName = (extendsAttribute != null) ? new TypeSlotFragment(extendsAttribute) : null;
        
        Element javadocEL = el.getFirstChildElement("javadoc");
        if (javadocEL != null) {
            documentation = new JavadocUnit(javadocEL);
        }
        if (documentation == null) {
            documentation = new JavadocUnit("");
        }
        implementsList = Utility.mapList(TopLevelCodeElement.xmlToStringList(el, "implements", "implementstype", "type"), TypeSlotFragment::new);
        imports = Utility.mapList(TopLevelCodeElement.fillChildrenElements(this, el, "imports"), e -> (ImportElement)e);
        fields = TopLevelCodeElement.fillChildrenElements(this, el, "fields");
        constructors = TopLevelCodeElement.fillChildrenElements(this, el, "constructors");
        methods = TopLevelCodeElement.fillChildrenElements(this, el, "methods");
       
        enable = new Boolean(el.getAttributeValue("enable"));
        this.projectResolver = projectResolver;
    }
    
    @Override
    public JavaSource toJavaSource()
    {
        return getDAP(null).java;
    }

    private JavaSource generateJavaSource()
    {
        List<JavaFragment> header = new ArrayList<>();
        header.add(new FrameFragment(frame, "public "));
        if (abstractModifier) {
            header.add(f(frame, "abstract "));
        }
        classKeyword = new FrameFragment(frame, "class ");
        Collections.addAll(header, classKeyword, className);
        
        if (extendsName != null) {
            Collections.addAll(header, f(frame, " extends "), extendsName);
        }
        if (!implementsList.isEmpty())
        {
            header.add(f(frame, " implements "));
            header.addAll(implementsList.stream().collect(Utility.intersperse(() -> f(frame, ", "))));
        }

        JavaSource java = new JavaSource(null, header);
        java.prependJavadoc(documentation.getJavaCode());

        java.prependLine(Arrays.asList((JavaFragment) f(frame, "")), null);
        Utility.backwards(CodeElement.toJavaCodes(imports)).forEach(imp -> java.prepend(imp));

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
    public Element toXML()
    {    
        Element classEl = new Element(ELEMENT);
        if (abstractModifier) {
            classEl.addAttribute(new Attribute("abstract", String.valueOf(abstractModifier)));
        }
        classEl.addAttribute(new Attribute("name", className.getContent()));
        if (extendsName != null) {
            classEl.addAttribute(new Attribute("extends", extendsName.getContent()));
        }
        addEnableAttribute(classEl);
        
        if (documentation != null) {
            classEl.appendChild(documentation.toXML());
        }
        
        appendCollection(classEl, imports, "imports");
        classEl.appendChild(TopLevelCodeElement.stringListToXML(Utility.mapList(implementsList, TypeSlotFragment::getContent), "implements", "implementstype", "type"));
        
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
        frame = new ClassFrame(editor, abstractModifier, className, imports, extendsName, implementsList, projectResolver, documentation, isEnable());
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
            protected String getJavaCode(Destination dest, ExpressionSlot<?> completing)
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
    @OnThread(Tag.Swing)
    public CodeSuggestions getCodeSuggestions(PosInSourceDoc pos, ExpressionSlot<?> completing)
    {
        // Must get document before getting position:
        MoeSyntaxDocument doc = getSourceDocument(completing);
        Optional<Integer> resolvedPos = resolvePos(doc, pos);
        return resolvedPos.map(rpos -> doc.getParser().getExpressionType(rpos, getSourceDocument(completing)))
                          .orElse(null);
    }
    
    @OnThread(Tag.Swing)
    private Optional<Integer> resolvePos(MoeSyntaxDocument doc, PosInSourceDoc pos)
    {
        DocAndPositions docAndPositions = null;
        try
        {
            docAndPositions = documentCache.get(doc.getText(0, doc.getLength()));
        }
        catch (BadLocationException e)
        {
            Debug.reportError(e);
        }
        Optional<Integer> resolvedPos = Optional.ofNullable(docAndPositions.fragmentPositions.get(pos.getFragment()));
        return resolvedPos.map(p -> p + pos.offset);
    }

    @Override
    public String getStylePrefix()
    {
        return "class-";
    }

    @Override
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

    @OnThread(Tag.Swing)
    private MoeSyntaxDocument getSourceDocument(ExpressionSlot completing)
    {
        return getDAP(completing).getDocument(projectResolver);
    }
    
    @OnThread(Tag.Any)
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
                DocAndPositions dap = documentCache.get(src);
                dap.fragmentPositions.putAll(positions);
                return dap;
            }
            else
            {
                DocAndPositions dap = new DocAndPositions(src, java, positions);
                documentCache.put(src, dap);
                return dap;
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

    @OnThread(Tag.Swing)
    public Reflective qualifyType(String name, PosInSourceDoc pos)
    {
        final MoeSyntaxDocument doc = getSourceDocument(null);
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

    @OnThread(Tag.Swing)
    private Reflective getClassNode()
    {
        // Must get document before getting position:
        MoeSyntaxDocument doc = getSourceDocument(null);
        ParsedNode node = doc.getParser().findNodeAtOrAfter(resolvePos(doc, classKeyword.getPosInSourceDoc()).get(), 0).getNode();
        if (node instanceof ParsedTypeNode)
        {
            return new ParsedReflective((ParsedTypeNode)node);
        }
        return null;
    }

    // Returns name of uppermost class with this method:
    @OnThread(Tag.Swing)
    public Reflective findSuperMethod(String name, List<String> qualParamTypes)
    {
        if (extendsName == null || extendsName.getContent().isEmpty() || classKeyword == null)
            return null;

        // Make sure source document has been created:
        getSourceDocument(null);
        
        Reflective qualSuper = qualifyType(extendsName.getContent(), classKeyword.getPosInSourceDoc());
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
    public void updateSourcePositions()
    {
        SwingUtilities.invokeLater(() -> getSourceDocument(null));
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

    @OnThread(Tag.Swing)
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
                List<ParamInfo> paramInfo = Utility.mapList(c.getParams(), p -> new ParamInfo(p.getParamType().getContent(), p.getParamName().getContent(), "", ""));
                return new AssistContentThreadSafe(c.getAccessPermission().asAccess(), getName(), c.getDocumentation(), CompletionKind.CONSTRUCTOR, getName(), null, paramInfo, null, null, null);
            })
            .collect(Collectors.toList());
    }

    private static class DocAndPositions
    {
        public final JavaSource java;
        public final IdentityHashMap<JavaFragment, Integer> fragmentPositions;
        private String src;
        private MoeSyntaxDocument document;

        public DocAndPositions(String src, JavaSource java, IdentityHashMap<JavaFragment, Integer> fragmentPositions)
        {
            this.src = src;
            this.java = java;
            this.fragmentPositions = fragmentPositions;
        }
        
        @OnThread(Tag.Swing)
        public MoeSyntaxDocument getDocument(EntityResolver projectResolver)
        {
            if (document == null)
            {
                document = new MoeSyntaxDocument(projectResolver);
                try
                {
                    document.insertString(0, src, null);
                }
                catch (BadLocationException e)
                {
                    Debug.reportError(e);
                }
                document.enableParser(true);
            }
            return document;
        }
    }
}
