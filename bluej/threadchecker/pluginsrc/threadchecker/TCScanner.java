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
package threadchecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

/**
 * A visitor class that visits the whole AST and does checks against the
 * thread annotations the user has provided.  One scanner is used
 * for all compilation units in a compilation
 */
class TCScanner extends TreePathScanner<Void, Void>
{
    // A bunch of helpers from the compiler infrastructure:
    private final Trees trees;
    private final Types types;
    private final Elements elements;
    // We have some logic about library packages: 
    private final HashMap<String, LocatedTag> packageAnns = new HashMap<>();
    // We have some logic about known library classes (especially to override cases in packageAnns)
    private final HashMap<String, LocatedTag> classAnns = new HashMap<>();
    // We also have some logic about known library methods that will run on a given thread
    private final List<MethodRef> methodAnns = new ArrayList<>();
    // A stack of declared types we are visiting, innermost is last
    private final LinkedList<PathAnd<ClassTree>> typeScopeStack = new LinkedList<>();
    // A stack of declared methods we are visiting, innermost is last
    // A null item means an initializer block
    private final LinkedList<PathAnd<MethodTree>> methodScopeStack = new LinkedList<>();
    // A list of tags that our nested lambdas are desugaring to:
    private final LinkedList<LocatedTag> lambdaScopeStack = new LinkedList<>();
    private final Map<String, LocatedTag> fields = new HashMap<>();
    private final List<String> objectMembers;
    
    // Types and methods can nest, e.g.
    // class A {public void foo() { Platform.runLater(new Runnable() { public void run() { } }) } }
    // So we keep a stack of the nested types and methods:
    // List of package names to exclude from the analysis:
    private final List<String> ignorePackages;
    private final LinkedList<List<TypeMirror>> callingMethodStack = new LinkedList<>();
    private final WrapDescent defaultReturn = new WrapDescent(
            () -> { callingMethodStack.addLast(null); },
            () -> { callingMethodStack.removeLast(); }
        );
    // We only process one compilation unit at a time (no nesting), so no need for stack:
    private CompilationUnitTree cu;
    private boolean inSynchronizedThis;

    public TCScanner(JavacTask task, List<String> ignorePackages) throws NoSuchMethodException
    {
        // Code for redirecting stderr if you need it:
        /*
        try
        {
            System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream("J:\\tc.txt"))));
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        */
        trees = Trees.instance(task);
        types = task.getTypes();
        elements = task.getElements();
        this.ignorePackages = new ArrayList<>(ignorePackages);
        
        objectMembers = elements.getAllMembers(elements.getTypeElement("java.lang.Object"))
                           .stream().map(e -> e.getSimpleName().toString()).collect(Collectors.toList());
        
        // Temporary:
        //this.ignorePackages.addAll(Arrays.asList("bluej.editor.stride"));
        //this.ignorePackages.addAll(Arrays.asList("bluej.stride"));
        
        // Now we have all the business logic for the Java standard libraries:
        
        //Setup default package annotations.  For convenience, we mark that all subclasses
        // of classes in this package (except when overriden by classAnns) will have
        // all their methods tagged accordingly.  So anything inheriting from a JComponent,
        // for example, will have all its methods tagged as Swing.  (You can always
        // override this with a tag on the particular method)
        Arrays.asList(
                "javafx.application",
                "javafx.beans.binding",
                "javafx.beans.value",
                "javafx.collections",
                "javafx.css",
                "javafx.event",
                "javafx.scene",
                "javafx.scene.control",
                "javafx.scene.image",
                "javafx.scene.layout"
                ).forEach(pkg -> packageAnns.put(pkg, new LocatedTag(Tag.FX, false, true, "<JavaFX: " + pkg + ">")));

        // Web must be done solely on actual FX thread:
        packageAnns.put("javafx.scene.web", new LocatedTag(Tag.FXPlatform, false, true, "<JavaFX javafx.scene.web>"));

        classAnns.put("javafx.event.EventHandler", new LocatedTag(Tag.FXPlatform, false, true, "<JavaFX EventHandler>"));
        
        Arrays.asList(
                "java.awt.event",
                "java.beans",
                "javax.accessibility",
                "javax.swing",
                "javax.swing.border",
                "javax.swing.event",
                "javax.swing.table",
                "javax.swing.text",
                "javax.swing.filechooser",
                "javax.swing.plaf.metal"
                ).forEach(pkg -> packageAnns.put(pkg, new LocatedTag(Tag.Swing, false, true, "<Swing: " + pkg + ">")));
    
        //An override we need:
        methodAnns.add(new MethodRef("java.awt.Window", "dispose", new LocatedTag(Tag.Any, false, false, "<AWT>")));
        methodAnns.add(new MethodRef("javafx.beans.property.ObjectPropertyBase", "get", new LocatedTag(Tag.Any, false, false, "<JavaFX Beans>")));
        methodAnns.add(new MethodRef("javafx.beans.property.SimpleObjectProperty", "<init>", new LocatedTag(Tag.Any, false, false, "<JavaFX Beans>")));
        methodAnns.add(new MethodRef("javafx.beans.property.SimpleBooleanProperty", "<init>", new LocatedTag(Tag.Any, false, false, "<JavaFX Beans>")));

        methodAnns.add(new MethodRef("javafx.embed.swing.SwingNode", "<init>", new LocatedTag(Tag.Any, false, false, "<SwingNode>")));
        methodAnns.add(new MethodRef("javafx.embed.swing.SwingNode", "setContent", new LocatedTag(Tag.Any, false, false, "<SwingNode>")));
        methodAnns.add(new MethodRef("javafx.embed.swing.SwingNode", "getContent", new LocatedTag(Tag.Any, false, false, "<SwingNode>")));

        methodAnns.add(new MethodRef("javafx.application.Application", "launch", new LocatedTag(Tag.Any, false, false, "<FX launch>")));
        methodAnns.add(new MethodRef("javafx.application.Application", "start", new LocatedTag(Tag.FXPlatform, false, false, "<FX application>")));
        methodAnns.add(new MethodRef("javafx.animation.AnimationTimer", "handle", new LocatedTag(Tag.FXPlatform, false, false, "<AnimationTimer>")));
        
        // This one isn't actually true!  But it's used during printing so let's live:
        methodAnns.add(new MethodRef("javax.swing.JComponent", "paint", new LocatedTag(Tag.Any, false, false, "<Swing paint hack>")));
        // This is actually thread-safe:
        methodAnns.add(new MethodRef("javax.swing.JComponent", "getClientProperty", new LocatedTag(Tag.Any, false, false, "<Swing client properties>")));
        methodAnns.add(new MethodRef("javax.swing.JComponent", "putClientProperty", new LocatedTag(Tag.Any, false, false, "<Swing client properties>")));
        
        classAnns.put("java.awt.event.InputEvent", new LocatedTag(Tag.Any, false, true, "<AWT>"));
        classAnns.put("java.awt.event.ComponentEvent", new LocatedTag(Tag.Any, false, true, "<AWT>"));
        classAnns.put("java.awt.event.KeyEvent", new LocatedTag(Tag.Any, false, true, "<AWT>"));
        classAnns.put("java.awt.event.MouseEvent", new LocatedTag(Tag.Any, false, true, "<AWT>"));
        classAnns.put("java.awt.event.MouseWheelEvent", new LocatedTag(Tag.Any, false, true, "<AWT>"));
        classAnns.put("javax.swing.SizeRequirements", new LocatedTag(Tag.Any, false, true, "<Swing>"));
        classAnns.put("javax.swing.event.EventListenerList", new LocatedTag(Tag.Any, false, true, "<Swing>"));
        
        // Swing Documents can be played with from any thread:
        classAnns.put("javax.swing.event.DocumentEvent", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.AttributeSet", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.MutableAttributes", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.MutableAttributeSet", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.Document", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.Element", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.AbstractDocument", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.AbstractDocument.AbstractElement", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.AbstractDocument.AttributeContext", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.AbstractDocument.BranchElement", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.AbstractDocument.DefaultDocumentEvent", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.AbstractDocument.ElementEdit", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.PlainDocument", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.Segment", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.Position", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.StyledDocument", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.DefaultStyledDocument", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        classAnns.put("javax.swing.text.StyleContext", new LocatedTag(Tag.Any, false, true, "<Swing Documents>"));
        
        // Timers can be stopped and started from any thread, looking at the source:
        classAnns.put("javax.swing.Timer", new LocatedTag(Tag.Any, false, true, "<Swing Timer>"));
        
        
        // As far as I can tell, this one is thread safe, and we certainly use it as if it is:
        classAnns.put("javax.swing.KeyStroke", new LocatedTag(Tag.Any, false, false, "<Swing KeyStroke>"));
        
        // You can call the runLater, etc, methods from any thread:
        classAnns.put("javafx.application.Platform", new LocatedTag(Tag.Any, false, false, "<JavaFX Platform>"));
        classAnns.put("java.awt.EventQueue", new LocatedTag(Tag.Any, false, false, "<Swing EventQueue>"));
        classAnns.put("javax.swing.SwingUtilities", new LocatedTag(Tag.Any, false, false, "<Swing EventQueue>"));
        
        // These are dubious, but we've been calling these off the Swing thread for a while:
        classAnns.put("javax.swing.UIManager", new LocatedTag(Tag.Any, false, true, "<Swing UIManager>"));
        classAnns.put("javax.swing.UIManager.LookAndFeelInfo", new LocatedTag(Tag.Any, false, true, "<Swing UIManager>"));
        methodAnns.add(new MethodRef("javax.swing.plaf.metal.MetalLookAndFeel", "setCurrentTheme", new LocatedTag(Tag.Any,  false, false, "<Swing UIManager>")));

        // Desktop should be AWT-only, it seems:
        classAnns.put("java.awt.Desktop", new LocatedTag(Tag.Swing, false, true, "<AWT Desktop>"));
        
        // Threads always run in their own unique thread, of course:
        // Could do: methodAnns.add(new MethodRef("java.lang.Thread", "run", new LocatedTag(Tag.Worker, false, true, "<Thread.run>")));
        // If we use class annotation with applyToSubclasses, it solves a lot of annoyances where inner class Threads get their package's tags:
        classAnns.put("java.lang.Thread", new LocatedTag(Tag.Worker, true, true, "<Thread>"));
        methodAnns.add(new MethodRef("java.lang.Thread", "run", new LocatedTag(Tag.Worker, true, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "setPriority", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "getPriority", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "start", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "interrupt", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "join", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "currentThread", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "sleep", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "getContextClassLoader", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "setContextClassLoader", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "getStackTrace", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "interrupted", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "yield", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "setDaemon", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "setName", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "isAlive", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        methodAnns.add(new MethodRef("java.lang.Thread", "<init>", new LocatedTag(Tag.Any, false, true, "<Thread>")));
        
        
        methodAnns.add(new MethodRef("java.util.concurrent.ScheduledExecutorService", "submit", new LocatedTag(Tag.Worker, true, true, "<ScheduledExecutor>")));
        
        // This makes Runnables ignore package tags:
        //classAnns.put("java.lang.Runnable", new LocatedTag(Tag.Any, true, false, "<Runnable>"));
        
        // AWT events are dispatched from the Swing thread:
        methodAnns.add(new MethodRef("java.awt.DefaultKeyboardFocusManager", "processKeyEvent", new LocatedTag(Tag.Swing, false, false, "<AWT events>")));
    }
    
    private static String typeToName(PathAnd<ClassTree> t)
    {
        return "." + t.item.getSimpleName().toString();
    }
    
    private static <E> String enumToQual(E e)
    {
        return e.getClass().getSimpleName() + "." + e.toString();
    }
    
    private <T> void addCur(LinkedList<PathAnd<T>> list, T cur)
    {
        list.add(new PathAnd<T>(cur, getCurrentPath()));
    }
    
    @Override
    public Void visitCompilationUnit(CompilationUnitTree cu, Void arg1) {
        this.cu = cu;
        // If this is a package-info.java file, it will have package annotations
        // which we check for validity:
        checkSingle(cu.getPackageAnnotations().stream().map(t -> getSourceTag(t, () -> cu.getPackageName().toString() + " package")), cu);
        
        
        // If it (or an ancestor package) is in our ignore list, don't process the type:
        if (ignorePackages.stream().anyMatch(pkg -> cu.getPackageName().toString().startsWith(pkg)))
            return null;
        else
            return super.visitCompilationUnit(cu, arg1);
    }
    
    

    @Override
    public Void visitClass(ClassTree tree, Void arg1)
    {
        addCur(typeScopeStack, tree);
        HashMap<String, LocatedTag> fieldCopy = new HashMap<>(fields);
        fields.clear();
        Void r = super.visitClass(tree, arg1);
        typeScopeStack.removeLast();
        fields.clear();
        fields.putAll(fieldCopy);
        return r;
    }

    @Override
    public Void visitBlock(BlockTree node, Void p)
    {
        // Look for initializer blocks, to alter methodScopeStack accordingly:
        if (getCurrentPath().getParentPath().getLeaf() instanceof ClassTree)
        {
            methodScopeStack.add(new PathAnd<>(null, null));
            Void r = super.visitBlock(node, p);
            methodScopeStack.removeLast();
            return r;
        }
        else
        {
            return super.visitBlock(node, p);
        }
    }

    // Visit the declaration of a method.  We must check the tags against those of any
    // overriden methods from parent classes.
    @Override
    public Void visitMethod(MethodTree method, Void arg1)
    {
        TypeMirror methodType = trees.getTypeMirror(getCurrentPath());
        Collection<? extends TypeMirror> superTypes = allSuperTypes(trees.getTypeMirror(typeScopeStack.getLast().path), method);
        
        // Skip methods with @SuppressWarnings("threadchecker")
        if (!suppressesChecker(method))
        {
            // Check if this method declares an annotation absent from
            // its parent, and warn that it can be circumvented:
            checkAgainstOverridden(method.getName(), getSourceTag(method, () -> cu.getPackageName().toString() + typeScopeStack.stream().limit(typeScopeStack.size()).map(TCScanner::typeToName).collect(Collectors.joining("."))), methodType, superTypes, false, method);

            addCur(methodScopeStack, method);
            Void r = super.visitMethod(method, arg1);
            this.methodScopeStack.removeLast();
            return r;
        }
        else
        {
            return null;
        }
    }

    /**
     * Examines a method, to see if the effective tags it has match against any overridden
     * method from a parent class.
     * 
     * @param methodName The name of the method to check
     * @param methodTag The tag on the declaration of that method, in the class
     *                  in which it is being declared/invoked-on
     * @param methodType The type of the method.  This includes the return type and parameter types.
     * @param superTypes The super types of the class in which the method is being
     *                   declared/invoked-on.  This is all supertypes, whether by extends
     *                   or implements, all the way back up the type hierarchy to Object or interfaces.
     * @param invocation True if we are checking the method when it is being invoked,
     *                   False if we are checking the method when it is being declared
     * @param errorLocation A suitable location (within the compilation unit this.cu)
     *                      at which to issue any compiler errors
     * 
     * @return Returns the effective tag of the method
     */
    private LocatedTag checkAgainstOverridden(Name methodName, LocatedTag methodTag,
            TypeMirror methodType, Collection<? extends TypeMirror> superTypes, boolean invocation, Tree errorLocation)
    {
        boolean subTagWasPackage = false;
        LocatedTag subTag = methodTag;
        // If the method tag is missing, and this is a declaration,
        // we look for tags in the surrounding class, and failing that, package:
        if (subTag == null && !invocation)
        {
            // Look for class, then special, then package:
            subTag = getSourceTag(typeScopeStack.getLast().item, () -> cu.getPackageName().toString() + typeScopeStack.stream().limit(typeScopeStack.size() - 1).map(TCScanner::typeToName).collect(Collectors.joining(".")));
            if (subTag == null)
            {
                //if ("run".equals("" + methodName) && superTypes.stream().map(types::capture).anyMatch(st -> st != null && "java.lang.Runnable".equals(st.toString())))
                //    subTag = fromSpecial();
                if (subTag == null)
                {
                    PackageElement pkg = elements.getPackageOf(trees.getElement(typeScopeStack.getLast().path));
                    subTag = getRemoteTag(pkg, () -> pkg.getQualifiedName().toString(), errorLocation);
                    if (subTag == null)
                    {
                        subTag = packageAnns.get(pkg.getQualifiedName().toString());
                    }
                    subTagWasPackage = true;
                }
            }
        }
        // subTag may still be null afterwards if there are no relevant tags
        
        // A list of all tags we've found that apply to this method.  The String key
        // is not semantically important, but it's unique for each tag, and is used when communicating
        // the error back to the user to show the origin of each tag.
        Map<String, LocatedTag> appliedTags = new HashMap<>();
        String subTagKey = "." + methodName;
        if (subTag != null)
            appliedTags.put(subTagKey, subTag);
        
        // We look for matching declarations of the method in all super types
        for (TypeMirror st : superTypes.stream().map(types::capture).collect(Collectors.toList()))
        {
            // We must see if there any tags on the super-types themselves, as if they
            // have the applyToSubClasses flag as true (only set on Java library classes) then
            // we apply them to all methods in sub classes
            
            // Look for class, then package:
            LocatedTag superClassTag = getRemoteTag(types.asElement(st), () -> st.toString(), errorLocation);
            if (superClassTag == null)
            {
                superClassTag = classAnns.get(types.erasure(st).toString());
                if (superClassTag == null)
                {
                    PackageElement pkg = elements.getPackageOf(types.asElement(st));
                    superClassTag = getRemoteTag(pkg, () -> pkg.getQualifiedName().toString(), errorLocation);
                    if (superClassTag == null)
                    {
                        superClassTag = packageAnns.get(pkg.getQualifiedName().toString());
                    }
                }
            }
            if (superClassTag != null && (superClassTag.applyToAllSubclassMethods() /*&& types.asElement(st).getKind() != ElementKind.INTERFACE*/)
                    && (subTag == null || subTagWasPackage)
                    // Special case for Thread subclass constructors:
                    && !(st.toString().equals("java.lang.Thread") && methodName.equals(elements.getName("<init>")))
                    )
            {
                appliedTags.put(st.toString(), superClassTag);
            }
            
            boolean methodNameIsInit = methodName == null ? false : methodName.toString().equals("<init>");
            
            for (Element superMember : types.asElement(st).getEnclosedElements())
            {
                if (superMember instanceof ExecutableElement)
                {
                    // Look to see if the method in the superclass matches the method
                    // we are concerned with.  They should have the same name, and the
                    // same method signature.
                    if (methodName != null && !methodNameIsInit // Ignore constructors, they don't actually *override* the parent's
                            && superMember.getSimpleName().equals(methodName)
                            && types.isSubsignature((ExecutableType)methodType, (ExecutableType)types.asMemberOf((DeclaredType)st, superMember)))
                    {
                        // First look at a tag on the method
                        LocatedTag superTag = getRemoteTag(superMember, () -> st.toString() + "." + superMember.getSimpleName().toString(), errorLocation);
                        if (superTag == null)
                        {
                            // Failing that, look for a tag in our methodAnns override map:
                            superTag = methodAnns.stream()
                                    .filter(m -> m.matches(st, methodName, types.asMemberOf((DeclaredType)st, superMember)))
                                    .map(m -> m.tag)
                                    .findFirst()
                                    .orElse(null);
                            if (superTag == null)
                            {
                                // Failing that, look for a tag on the class, then on the package:
                                superTag = classAnns.get(types.erasure(st).toString());
                                if (superTag == null)
                                {
                                    superTag = getRemoteTag(types.asElement(st), () -> st.toString(), errorLocation);
                                    if (superTag == null)
                                    {
                                        PackageElement pkg = elements.getPackageOf(types.asElement(st));
                                        superTag = getRemoteTag(pkg, () -> pkg.getQualifiedName().toString(), errorLocation);
                                        if (superTag == null)
                                        {
                                            superTag = packageAnns.get(pkg.getQualifiedName().toString());                                            
                                        }
                                    }
                                }
                            }
                        }
                        
                        // If we have found any tag on the overridden method, store it:
                        if (superTag != null)
                        {
                            appliedTags.put(st.toString() + "." + superMember.getSimpleName().toString(), superTag);
                            if (superTag.ignoreParent() && subTagWasPackage)
                            {
                                appliedTags.remove(subTagKey);
                                subTag = null;
                            }
                                
                        }
                        
                        if (superTag != null && subTag == null)
                        {
                            // If the parent has a tag, and child doesn't, we simply inherit the tag from the parent method,
                            // so there is no check to be made
                        }
                        else if (typeScopeStack.getLast().item.getSimpleName().length() == 0)
                        {
                               // Ignore anonymous inner classes.  They have no callable methods except their parent's
                               // and cannot be called except via the parent interface, so we can safely assume
                               // that they must take all the tags of their parent, no more and no less)
                        }
                        // If we are an invocation, we don't mind if parent has no tag and we do, we just mind if it exists and doesn't match
                        // If we are not an invocation (i.e. declaration) then we care if parent has no tag and we do
                        else if ((!invocation || superTag != null) && subTag != null && !subTag.tag.canOverride(superTag == null ? null : superTag.tag) && !subTag.ignoreParent())
                        {
                            if (st.toString().equals("java.lang.Object") || st.toString().startsWith("java.lang.Comparable") || st.toString().startsWith("java.util.Comparator")) 
                            {
                                // Don't complain about toString, equals, hashCode, compare
                            }
                            else if (st.toString().startsWith("java.awt"))
                            {
                                // Don't complain that AWT classes can be called from other threads
                            }
                            //else if (methodName.toString().equals("run") && st.toString().equals("java.lang.Runnable") && matchesSpecial(subTag))
                            //{
                                // Don't complain about Runnable.run if it's being given directly to a runLater-style method.
                            //}
                            else
                                issueError("\nOverridden method " + methodName + " can be called via parent " + st.toString() + ", tagged " + superTag + "\n  without thread tag: " + subTag, errorLocation);
                        }
                    }
                }
                
            }
        }
        
        // Now that we have collected up relevant tags, see if there are conflicts:   
        final LocatedTag subTagFinal = subTag;
        if ((subTag == null || !subTag.ignoreParent()) && !"<init>".equals(methodName == null ? null : methodName.toString()) && appliedTags.values().stream().distinct().collect(Collectors.toList()).size() > 1
                && (subTag == null || !appliedTags.values().stream().allMatch(t -> subTagFinal.tag.canOverride(t.tag))))
        {
            issueError("\nMethod " + methodName + " overrides parent methods with conflicting thread tags:\n" + appliedTags.entrySet().stream().map(t -> "  " + t.getKey() + ": " + t.getValue()).collect(Collectors.joining("\n")), errorLocation);
        }
        
        // As long as there was no error, appliedTags has zero or one entries:
        return appliedTags.isEmpty() ? null : appliedTags.values().iterator().next();
    }
    

    @Override
    public Void visitMethodInvocation(MethodInvocationTree invocation, Void arg1)
    {
        AtomicReference<Name> name = new AtomicReference<>();
        
        // The method invocation, like manager.open("x.txt"), is split into the arguments
        // and the "method select", which is the "manager.open" part.  We use a visitor
        // to dig into the method select and pick out just the method name ("open"):
        
        ExpressionTree lhs = invocation.getMethodSelect().accept(new SimpleTreeVisitor<ExpressionTree, Void>() {
            @Override
            public ExpressionTree visitMemberReference(
                    MemberReferenceTree arg0, Void arg1) {
                name.set(arg0.getName());
                return arg0.getQualifierExpression();
            }

            @Override
            public ExpressionTree visitMemberSelect(MemberSelectTree arg0,
                    Void arg1) {
                name.set(arg0.getIdentifier());
                return arg0.getExpression();
            }
            
            @Override
            public ExpressionTree visitIdentifier(IdentifierTree id, Void arg1)
            {
                name.set(id.getName());
                return null;
            }

        }, null);
        
        // Stores the type of the invocation target class containing the method
        TypeMirror invokeTargetType = null;
        try
        {
            if (lhs != null)
                invokeTargetType = trees.getTypeMirror(trees.getPath(cu, lhs));
            else
                invokeTargetType = trees.getTypeMirror(typeScopeStack.getLast().path);
            
        }
        // Had some trouble here with unexpected NPE.  Now fixed, I think, so TODO remove
        catch (NullPointerException e)
        {
            System.err.println("Last: " + typeScopeStack.getLast());
            e.printStackTrace();
            trees.printMessage(Kind.ERROR, "NPE", invocation, cu);
        }
        
        List<TypeMirror> argTypes = invocation.getArguments().stream()
                .map(arg -> trees.getTypeMirror(new TreePath(getCurrentPath(), arg)))
                .collect(Collectors.toList());
        
        WrapDescent wrap = checkInvocation(name.get(), lhs, invokeTargetType, invocation.getMethodSelect().toString(), argTypes, arg1, invocation);
        Void r = scan(invocation.getTypeArguments(), arg1);
        r = scan(invocation.getMethodSelect(), arg1);
        if (wrap.before != null) wrap.before.run();
        r = scan(invocation.getArguments(), arg1);
        if (wrap.after != null) wrap.after.run();
        return r;
    }
    
    private WrapDescent checkInvocation(Name name, Tree lhs, TypeMirror invokeTargetType, String methodSelect, List<TypeMirror> invokeArgTypes, Void arg1, Tree errorLocation)
    {
        if (inDebugClass())
        {
            System.err.println("Checking invocation: " + lhs + " # " + name + " # " + invokeTargetType + " # " + methodSelect);
        }
        
        if (invokeTargetType == null)
        {
            // Odd javac-added method (e.g. access method), skip:
            return defaultReturn;
        }
        
        invokeTargetType = types.capture(invokeTargetType);
        
        Element invokedOn = types.asElement(invokeTargetType);
        
        if (invokedOn == null)
        {
            if (invokeTargetType instanceof ArrayType)
            {
                // No thread related messages on arrays anyway, so just ignore it
            }
            else
            {
                //trees.printMessage(Kind.ERROR, "Could not determine type of invocation target", errorLocation, cu);
            }
            return defaultReturn;
        }
        
        Collection<? extends TypeMirror> superTypes = allSuperTypes(trees.getTypeMirror(typeScopeStack.getLast().path), lhs);
        
        LocatedTag invokedOnTag = null;
        
        List<TypeMirror> candidateArgs = null;
        
        final String nameString = name.toString();
        
        if (nameString.equals("this"))
        {
         // TODO handle this method (follow to appropriate constructor in same class)
        }
        else if (nameString.equals("super"))
        {
         // TODO handle super method (follow to appropriate constructor in parent class)
        }
        else
        {
            // Look for matching method and check its annotations
            List<ExecutableElement> candidates = getMembers(invokedOn, lhs == null /*Including enclosing if no prefix */, false).stream()
              .filter(el -> el instanceof ExecutableElement)
              .map(el -> (ExecutableElement)el)
              .filter(el -> el.getSimpleName().toString().equals(nameString))
              .filter(el -> el.isVarArgs() || invokeArgTypes == null || el.getParameters().size() == invokeArgTypes.size())
              .collect(Collectors.toList());
            
            // Only look at types if there is still ambiguity:
            if (candidates.size() > 1)
            {
                candidates = candidates.stream()
                        .filter(el -> {
                            if (el.isVarArgs() || invokeArgTypes == null)
                                return true;
                            
                            for (int i = 0; i < invokeArgTypes.size(); i++)
                            {
                                if (!types.isAssignable(types.capture(invokeArgTypes.get(i)), el.getParameters().get(i).asType()))
                                    return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
            }
            
            if (candidates.size() == 1)
            {
                candidateArgs = candidates.get(0).getParameters().stream().
                        map(e -> types.capture(e.asType())).collect(Collectors.toList());
            }
            
            final TypeMirror tmFinal = invokeTargetType;
            List<LocatedTag> candidateDirectTags = candidates.stream().map(e -> 
                getRemoteTag(e, () -> tmFinal.toString() + "." + e.getSimpleName().toString(), errorLocation)
            ).distinct().collect(Collectors.toList());
            
            // Sometimes we may not be clever enough yet to distinguish methods,
            // but if they are have the same tag applied (or no tags) then it doesn't
            // matter which is which.  We only have a problem if there are multiple distinct tags:
            
            // TODO Although it also matters if the parent tags for each candidate differ!
            if (candidateDirectTags.size() > 1)
            {
                trees.printMessage(Kind.ERROR, "\nCould not find unambigious declaration of method " + name + " in " + invokedOn.asType().toString() + " (and tags differ between resolutions)", errorLocation, cu);
                
            }
            else if (candidates.size() == 0)
            {
                // Method doesn't exist in that class, only in a parent, so look in first parent:
                if (inDebugClass())
                    System.err.println("Looking for method " + methodSelect + " in super type of " + invokeTargetType);
                List<? extends TypeMirror> directSupers = types.directSupertypes(invokeTargetType);
                if (directSupers.size() > 0)
                {
                    if (inDebugClass())
                        System.err.println("Can't find method " + name + "{" + (invokeArgTypes == null ? "?" : invokeArgTypes.size()) + "} in " + invokeTargetType + "; trying super");
                    for (int i = 0; i < directSupers.size(); i++)
                    {
                        if (inDebugClass())
                            System.err.println("  Super " + i + ": " + directSupers.get(i));
                        WrapDescent r = checkInvocation(name, lhs, directSupers.get(i), methodSelect, invokeArgTypes, arg1, errorLocation);
                        if (r != defaultReturn)
                            return r;
                    }
                    // Fall through to check outer classes...
                }
                if (inDebugClass())
                    System.err.println("Can't find method " + name + " in " + invokeTargetType + " or supers; looking for outer classes: " + typeScopeStack.size());
                if (lhs == null && typeScopeStack.size() > 1)
                {
                    Iterator<PathAnd<ClassTree>> it = typeScopeStack.descendingIterator();
                    if (trees.getTypeMirror(it.next().path).equals(invokeTargetType)) // Check we were on first item, and move past
                    {
                        while (it.hasNext())
                        {
                            WrapDescent r = checkInvocation(name, lhs, trees.getTypeMirror(it.next().path), methodSelect, invokeArgTypes, arg1, errorLocation);
                            if (r != defaultReturn)
                                return r;
                        }
                    }
                    else
                    {
                        if (inDebugClass())
                            System.err.println("Weren't on the outermost class so no need to check");
                    }
                }
                //trees.printMessage(Kind.ERROR, "\nCould not find declaration of method " + name + " in " + invokedOn.asType().toString() + " or any super-classes", errorLocation, cu);
                return defaultReturn;
            }
            else
            {
                // Grab a matching method (remember: we've already checked they have the same direct tags,
                // so we can grab the first) though see earlier TODO about parent tags:
                Element e = candidates.get(0);
                // The element e may not be in the original invokeTargetType type; it may
                // have been in an outer class, so adjust accordingly:
                invokeTargetType = e.getEnclosingElement().asType(); //TODO deal with wildcards?
                final TypeMirror invokeTargetTypeFinal = invokeTargetType;
                LocatedTag directTag = getRemoteTag(e, () -> invokeTargetTypeFinal.toString() + "." + e.getSimpleName().toString(), errorLocation);
            
                Collection<? extends TypeMirror> invokeTargetSuperTypes = allSuperTypes(invokeTargetType, lhs);
            
                LocatedTag overridden = checkAgainstOverridden(name, directTag, e.asType(), invokeTargetSuperTypes, true, errorLocation);
             
                // Look for class, then package:
                LocatedTag classTag = getRemoteTag(e.getEnclosingElement(), () -> e.getEnclosingElement().getSimpleName().toString(), errorLocation);
                PackageElement pkg = elements.getPackageOf(e.getEnclosingElement());
                LocatedTag packageDirectTag = getRemoteTag(pkg, () -> pkg.getQualifiedName().toString() + " package", errorLocation);
                LocatedTag packagePriorTag = packageAnns.get(pkg.getQualifiedName().toString());
                
                LocatedTag superInherit = checkSingle(invokeTargetSuperTypes.stream()
                        //.filter(ty -> types.asElement(ty).getKind() != ElementKind.INTERFACE)
                        .map(ty -> {
                            LocatedTag t = getRemoteTag(types.asElement(ty), () -> ty.toString(), errorLocation);
                            if (t == null)
                            {
                                PackageElement superPkg = elements.getPackageOf(types.asElement(ty));
                                t = getRemoteTag(superPkg, () -> superPkg.getQualifiedName().toString() + " package", errorLocation);
                                if (t == null)
                                {
                                    t = packageAnns.get(superPkg.getQualifiedName().toString());
                                }
                            }
                            return t;
                        })
                        .filter(t -> t != null && t.applyToAllSubclassMethods()), errorLocation);
                
                //System.err.println("Candidates: " + candidates.stream().map(Object::toString).collect(Collectors.joining("\n\n")));
                LocatedTag methodAnnTag = methodAnns.stream()
                        .filter(m -> m.matches(invokeTargetTypeFinal, name, types.asMemberOf((DeclaredType)invokeTargetTypeFinal, e)))
                        .map(m -> m.tag)
                        .findFirst()
                        .orElse(null);
                
                List<LocatedTag> tagList = Arrays.asList(directTag, methodAnnTag,
                        classAnns.get(types.erasure(e.getEnclosingElement().asType()).toString()),
                        classTag, packageDirectTag, packagePriorTag, overridden, superInherit);
                invokedOnTag = tagList.stream().filter(t -> t != null).findFirst().orElse(null);
                if (inDebugClass())
                {
                    for (MethodRef m : methodAnns)
                        System.err.println("Method " + m.classType.toString() + " . \"" + m.methodName + "\""
                            + " class match: " + isSameType(types.erasure(m.classType), types.erasure(invokeTargetTypeFinal))
                            + " name match: " + m.methodName.equals(name.toString())
                            + " .matches: " + m.matches(invokeTargetTypeFinal, name, null));
                    System.err.println("Is thread: " + types.isSameType(types.erasure(elements.getTypeElement("java.lang.Thread").asType()), types.erasure(invokeTargetTypeFinal)));

                    System.err.println("TCScanner for " + invokeTargetTypeFinal.toString() + " . \"" + name + "\"," + e.toString() + "; All super tags: " + invokeTargetSuperTypes.stream()
                        .map(ty -> ty.toString() + ": " + getRemoteTag(types.asElement(ty), () -> ty.toString(), lhs)).collect(Collectors.joining(", ")));
                    System.err.println("Tag list: " + tagList.stream().map(t -> "" + t).collect(Collectors.joining(", ")));
                }
            }
        }
        
        if (invokedOnTag != null)
        {
            // The method has a tag, so we need to check if we match it
            if (inDebugClass())
                System.err.println("Finding tag for: " + lhs + "." + methodSelect + "(...)");
            Optional<LocatedTag> ann = getCurrentTag(superTypes, errorLocation);
            
            boolean sameInstance = lhs == null;
            
            if ((ann.isPresent() == false && invokedOnTag.tag != Tag.Any) || (ann.isPresent() && !ann.get().tag.canCall(invokedOnTag.tag, sameInstance)))
            {
                if (!inSynthetic())
                {
                    issueError("\n    Method " + invokeTargetType.toString() + "." + name + " being called requires " + invokedOnTag + "\nbut this code may be running on another thread: " + ann.map(Object::toString).orElse("unspecified") + " lambda: " + lambdaScopeStack.size() + " " + lambdaScopeStack.stream().map(x -> "" + x).collect(Collectors.joining(", ")), errorLocation);
                }
            }
            
            if (ann.isPresent() && (ann.get().tag == Tag.FX || ann.get().tag == Tag.FX) && Arrays.asList("SwingUtilities.invokeAndWait", "EventQueue.invokeAndWait").contains(methodSelect))
            {
                issueError("\n    Swing invokeAndWait method is being called, but from tag " + ann.get() + " (you cannot make FX wait for Swing)", errorLocation);
            }
        }
        /*
        if (Arrays.asList("Platform.runLater", "SwingUtilities.invokeLater", "SwingUtilities.invokeAndWait", "EventQueue.invokeLater", "EventQueue.invokeAndWait").contains(methodSelect))
        {
            return new WrapDescent(
                () -> { insideSpecial = methodSelect; },
                () -> { insideSpecial = null; }
            );
        }
        else
            */
        {
            final List<TypeMirror> candidateArgsFinal = invokeArgTypes;
            return new WrapDescent(
                () -> { callingMethodStack.addLast(candidateArgsFinal); },
                () -> { callingMethodStack.removeLast(); }
            );
        }
    }

    private void issueError(String errorMsg, Tree errorLocation)
    {
        long startPosition = trees.getSourcePositions().getStartPosition(cu, errorLocation);
        String link = cu.getLineMap() == null ? "" : cu.getSourceFile().getName() + ":" + cu.getLineMap().getLineNumber(startPosition) + ": error:"; // [line added as IntelliJ location link]";
        trees.printMessage(Kind.ERROR, "\n" + link + errorMsg, errorLocation, cu);
    }

    private boolean inSynthetic()
    {
        return methodScopeStack.stream().anyMatch(p -> {
            MethodTree item = p.item;
            if (item == null)
                return false; // Initialiser block
            Name name = item.getName();
            String s = name.toString();
            return s.contains("$");
        });
    }
    
    private Optional<LocatedTag> getCurrentTag(Collection<? extends TypeMirror> superTypes,
            Tree errorLocation)
    {
        // If if we are an initializer block, also look at the outer method/class:
        Name methodName;
        TypeMirror methodType;
        LocatedTag methodAnn;
        LocatedTag classAnn;
        LocatedTag knownMethodAnn = null;
        
        if (methodScopeStack.isEmpty() || (methodScopeStack.getLast().item == null && methodScopeStack.size() == 1))
        {
            // We are in an initializer block for a top-level class or a named inner class.  No method annotations but there might be
            // a class annotation:
            methodName = null;
            methodType = null;
            methodAnn = null;
            classAnn = getSourceTag(typeScopeStack.getLast().item, () -> cu.getPackageName().toString() + typeScopeStack.stream().limit(typeScopeStack.size() - 1).map(TCScanner::typeToName).collect(Collectors.joining(".")));
        }
        else if (methodScopeStack.getLast().item == null)
        {
            // We are in an initializer block for an anonymous inner class, declared inside a method.
            // (if methodScopeStack was size 1, we would have taken previous branch.  We are
            // only here if we are inside a method, and you can only have an initializer block
            // inside a method by using an anonymous inner class.)
            
            // The initializer will be run on object creation, which occurs immediately inside the method
            // So we just grab the tag info from the surrounding method (and its class):
            MethodTree surroundingMethod = methodScopeStack.get(methodScopeStack.size() - 2).item;
            methodName = surroundingMethod.getName();
            methodType = trees.getTypeMirror(trees.getPath(cu, surroundingMethod));
            methodAnn = getSourceTag(surroundingMethod, () -> cu.getPackageName().toString() + typeScopeStack.stream().limit(typeScopeStack.size() - 1).map(TCScanner::typeToName).collect(Collectors.joining(".")));
            classAnn = getSourceTag(typeScopeStack.get(typeScopeStack.size() - 2).item, () -> cu.getPackageName().toString() + typeScopeStack.stream().limit(typeScopeStack.size() - 2).map(TCScanner::typeToName).collect(Collectors.joining(".")));
            knownMethodAnn = methodAnns.stream()
                    .filter(m -> allSuperTypes(
                            trees.getTypeMirror(typeScopeStack.get(typeScopeStack.size() - 2).path),
                            errorLocation)
                                .stream()
                                .anyMatch(ty -> m.matches(ty, methodName, methodType)))
                    .map(m -> m.tag)
                    .findFirst().orElse(null);
        }
        else
        {
            // Otherwise, we are in a standard method; gather tag info from method and surrounding class:
            methodName = methodScopeStack.getLast().item.getName();
            methodType = trees.getTypeMirror(methodScopeStack.getLast().path);
            methodAnn = getSourceTag(methodScopeStack.getLast().item, () -> cu.getPackageName().toString() + typeScopeStack.stream().limit(typeScopeStack.size()).map(TCScanner::typeToName).collect(Collectors.joining(".")));
            classAnn = getSourceTag(typeScopeStack.getLast().item, () -> cu.getPackageName().toString() + typeScopeStack.stream().limit(typeScopeStack.size() - 1).map(TCScanner::typeToName).collect(Collectors.joining(".")));
            knownMethodAnn = methodAnns.stream()
                    .filter(m -> allSuperTypes(trees.getTypeMirror(typeScopeStack.getLast().path),
                            errorLocation).stream().anyMatch(ty -> m.matches(ty, methodName, methodType)))
                    .map(m -> m.tag)
                    .findFirst().orElse(null);
        }
        
        LocatedTag inheritedTag = methodScopeStack.isEmpty() ? null : checkAgainstOverridden(methodName, methodAnn, methodType, superTypes, false, errorLocation);
        PackageElement pkg = (PackageElement)trees.getElement(typeScopeStack.getFirst().path).getEnclosingElement();
        LocatedTag packageAnn = getRemoteTag(pkg, () -> pkg.getQualifiedName().toString() + " package", errorLocation);  
                //cu.getPackageAnnotations().stream().map(t -> getSourceTag(t, cu.getPackageName().toString() + " package")).filter(t -> t != null).findFirst().orElse(null);
        
        LocatedTag lambdaAnn = null;
        if (inDebugClass())
            System.err.println("  >>Lambda stack size: " + lambdaScopeStack.size());
        for (int i = lambdaScopeStack.size() - 1; i >= 0; i--)
        {
            lambdaAnn = lambdaScopeStack.get(i);
            if (lambdaAnn != null)
            {
                if (inDebugClass())
                    System.err.println("  >>Found lambda ann " + lambdaAnn);
                break;
            }
        }
        
        Optional<LocatedTag> ann = Optional.empty();
            
        // If we are inside a lambda, it overrides everything else:
        if (lambdaAnn != null)
            ann = Optional.of(lambdaAnn);
        // If there is method, it overrides class:
        else if (methodAnn != null)
            ann = Optional.of(methodAnn);
        else if (knownMethodAnn != null)
            ann = Optional.of(knownMethodAnn);
        // Otherwise if there is class, overrides package:
        else if (classAnn != null)
            ann = Optional.of(classAnn);
        // Otherwise if there is package, use that:
        else if (packageAnn != null)
            ann = Optional.of(packageAnn);
        else if (packageAnns.containsKey(cu.getPackageName().toString()))
            ann = Optional.of(packageAnns.get(cu.getPackageName().toString()));
        // Or an inherited method/class:
        else if (inheritedTag != null)
            ann = Optional.of(inheritedTag);
        return ann;
    }

    /**
     * Looks to see if the tag matches a surrounding Platform.runLater, etc, that we
     * are currently inside during our visitor:
     */
    /*
    private boolean matchesSpecial(LocatedTag tag)
    {
        if (tag.tag == Tag.FX_UsesSwing || tag.tag == Tag.FX && Arrays.asList("Platform.runLater").contains(insideSpecial))
        {
            return true;
        }
        else if (tag.tag == Tag.Swing && Arrays.asList("SwingUtilities.invokeAndWait", "SwingUtilities.invokeLater", "EventQueue.invokeLater", "EventQueue.invokeAndWait").contains(insideSpecial))
        {
            return true;
        }
        return false;
    }
    */
    
    private LocatedTag fromSpecial(String typeName, String call, Optional<LocatedTag> calledFrom_, Tree errorLocation)
    {
        Tag calledFrom = calledFrom_.map(lt -> lt.tag).orElse(Tag.Any); 
        if (Arrays.asList("Platform.runLater").contains(call))
        {
            if (calledFrom == Tag.FXPlatform || calledFrom == Tag.FX)
                issueError("\nCalling runLater from thread " + calledFrom, errorLocation);
            return new LocatedTag(Tag.FXPlatform, true, true, "<runLater>");
        }
        else if (Arrays.asList("SwingUtilities.invokeAndWait", "SwingUtilities.invokeLater", "EventQueue.invokeLater", "EventQueue.invokeAndWait").contains(call))
        {
            if (calledFrom == Tag.Swing)
                issueError("Calling " + call + " from Swing thread", errorLocation);
            return new LocatedTag(Tag.Swing, true, true, "<invokeLater>");
        }
        else if (Arrays.asList("background.execute").contains(call))
            return new LocatedTag(Tag.Worker, true, true, "<Executor.execute>");
        //else if (typeName.startsWith("java.") && call.endsWith("forEach")) // Bit hacky
        //{
        //    System.err.println("Found a forEach: " + call + " so returning: " + calledFrom_);
        //    return calledFrom_.orElse(null);
        //}
        else
            return null;
    }

    /**
     * Gets members of this type 
     * @param invokedOn The type to consider
     * @param includeEnclosing Whether to also include members from enclosing elements (e.g. outer classes)
     * @param includeSuperTypes Whether to also include members from superclasses
     * @return
     */
    private List<Element> getMembers(Element invokedOn, boolean includeEnclosing, boolean includeSuperTypes)
    {
        List<Element> membersInclSuperAndEnclosing = new ArrayList<>();
        for (Element e = invokedOn; e != null; e = includeEnclosing ? e.getEnclosingElement() : null)
        {
            if (e instanceof TypeElement) // TODO should use visitor, really
            {
                membersInclSuperAndEnclosing.addAll(includeSuperTypes ? elements.getAllMembers((TypeElement)e) : ((TypeElement)e).getEnclosedElements());
            }
            else if (e instanceof TypeParameterElement)
            {
                ((TypeParameterElement)e).getBounds().forEach(t ->
                    membersInclSuperAndEnclosing.addAll(getMembers(types.asElement(t), includeEnclosing, includeSuperTypes)));
            }
        }
        return membersInclSuperAndEnclosing;
    }

    // Gets all supertypes (including interfaces) of the given type:
    private Collection<? extends TypeMirror> allSuperTypes(TypeMirror orig, Tree errorLocation)
    {
        if (orig == null)
        {
            return Collections.emptyList();
        }
        
        LocatedTag ttag = getRemoteTag(types.asElement(orig), () -> orig.toString(), errorLocation);
        if (ttag != null && ttag.ignoreParent())
        {
            return Collections.emptyList();
        }
        
        List<? extends TypeMirror> supers = types.directSupertypes(orig);
        return Stream.concat(supers.stream(), supers.stream()
                .flatMap(t -> allSuperTypes(t, errorLocation).stream())).collect(Collectors.toList());
    }

    /**
     * Fetches a tag by looking at annotations on a given piece of program source
     */
    private LocatedTag getSourceTag(AnnotationTree a, Supplier<String> info)
    {
        if (a.getAnnotationType().toString().equals(OnThread.class.getSimpleName()))
        {
            Tag tag = null;
            boolean ignoreParent = false;
            boolean requireSynchronized = false;
            for (String s : a.getArguments().stream().map(Object::toString).collect(Collectors.toList()))
            {
                for (Tag t : Arrays.asList(Tag.values()))
                {
                    if (s.equals("value = " + enumToQual(t)))
                    {
                        tag = t;
                    }
                }
                if (s.equals("ignoreParent = true"))
                    ignoreParent = true;
                if (s.equals("requireSynchronized = true"))
                    requireSynchronized = true;
            }
            if (tag != null)
                return new LocatedTag(tag, ignoreParent, requireSynchronized, false, info);
            
            throw new IllegalStateException("Unknown tag: " + a.getArguments().get(0) + " in " + info);
        }
        return null;
    }

    private LocatedTag getRemoteTag(AnnotationMirror m, Supplier<String> info)
    {
        if (m.getAnnotationType().asElement().getSimpleName().toString().equals(OnThread.class.getSimpleName()))
        {
            return stringToTag(m.getElementValues().entrySet().stream().map(x -> "" + x.getKey() + " = " + x.getValue()), () -> "<" + info.get());
        }
        return null;
    }

    private LocatedTag stringToTag(Stream<String> keyVals, Supplier<String> info)
    {
        List<String> stringValues = keyVals.collect(Collectors.toList());
        Tag tag = null;
        boolean ignoreParent = false;
        boolean requireSynchronized = false;
        for (String s : stringValues)
        {
            for (Tag t : Arrays.asList(Tag.values()))
            {
                if (s.equals("value() = " + Tag.class.getCanonicalName() + "." + t.toString()))
                {
                    tag = t;
                }
            }
            if (s.equals("ignoreParent() = true"))
                ignoreParent = true;
            if (s.equals("requireSynchronized() = true"))
                requireSynchronized = true;
        }
        //Debug:
        //if ("bluej.pkgmgr.PkgMgrFrame.setStatus".equals(info))
        //    throw new IllegalStateException("" + stringValues.stream().collect(Collectors.joining("++")) + " " + ignoreParent);
        
        if (tag != null)
            return new LocatedTag(tag, ignoreParent, requireSynchronized, false, info);
        throw new IllegalArgumentException("Unknown tag: " + stringValues.stream().collect(Collectors.joining(", ")) + " in " + cu.getSourceFile().toString());
    }
    
    /**
     * Fetches a tag by looking at an Element, which may well come from a separate compilation unit
     * The name is meant to contrast with getSourceTag, which does come from the current compilation unit
     */
    private LocatedTag getRemoteTag(Element e, Supplier<String> info, Tree errorLocation)
    {
        return checkSingle(e.getAnnotationMirrors().stream().map(m -> getRemoteTag(m, info)), errorLocation);
    }
    
    /**
     * Fetches a tag by looking at annotations on a given piece of program source
     */
    private LocatedTag getSourceTag(ClassTree t, Supplier<String> enclosingStem)
    {
        return checkSingle(t.getModifiers().getAnnotations().stream().map(a -> getSourceTag(a, () -> enclosingStem.get() + "." + t.getSimpleName().toString() + " class")), t);
    }

    /**
     * Fetches a tag by looking at annotations on a given piece of program source
     */
    private LocatedTag getSourceTag(VariableTree t, Supplier<String> enclosingStem)
    {
        return checkSingle(t.getModifiers().getAnnotations().stream().map(a -> getSourceTag(a, () -> enclosingStem.get() + "." + t.getName().toString() + " field")), t);
    }
    
    /**
     * Fetches a tag by looking at annotations on a given piece of program source
     */
    private LocatedTag getSourceTag(MethodTree t, Supplier<String> enclosingStem)
    {
        return checkSingle(t.getModifiers().getAnnotations().stream().map(a -> getSourceTag(a, () -> enclosingStem.get() + "." + t.getName().toString() + " method")), t);
    }

    /**
     * Is the method tagged as @SuppressWarnings, where one of the strings is "threadchecker"?
     */
    private boolean suppressesChecker(MethodTree t)
    {
        return t.getModifiers().getAnnotations().stream().anyMatch(a -> {
            if (a.getAnnotationType().toString().equals("SuppressWarnings"))
            {
                return a.getArguments().stream().map(Object::toString).anyMatch(c -> c.contains("threadchecker"));
            }
            return false;
        });
    }
    
    /**
     * Checks that the given stream of tags only contains a single distinct kind of tag (or none).
     * If it has multiple distinct tags, a compiler error is issued at the given location.
     */
    private LocatedTag checkSingle(Stream<LocatedTag> tagStream, Tree tree)
    {
        List<LocatedTag> tags = tagStream.filter(t -> t != null).distinct().collect(Collectors.toList());
        if (tags.size() > 1)
        {
            issueError("\n    Multiple conflicting thread tags: " + tags.stream().map(Object::toString).collect(Collectors.joining(", ")), tree);
        }
        return tags.isEmpty() ? null : tags.get(0);
    }
    
    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, Void p)
    {
        Tree parent = getCurrentPath().getParentPath().getLeaf();
        TypeMirror lambdaClassType = calculatedExpectedLambdaType(parent, node);
        if (inDebugClass())
        {
            System.err.println("Lambda type " + getCurrentPath().toString() + " (i.e. " + node.toString() + ") is " + lambdaClassType + " parent was " + parent.getClass() + " " + parent + " last stack 0: " + (callingMethodStack.isEmpty() ? "empty" : (callingMethodStack.getLast() == null ? "missing" : callingMethodStack.getLast().get(0))));
        }
        if (lambdaClassType == null)
        {
            return super.visitLambdaExpression(node, p);
        }
        Optional<LocatedTag> lambdaAnn = lambdaClassToAnn(parent, lambdaClassType, node, true);
        if (lambdaAnn == null)
        {
            if (inDebugClass())
                System.err.println("   Lambda annotation error");
            return super.visitLambdaExpression(node, p);
        }
        lambdaScopeStack.add(lambdaAnn.orElse(null));
        Void r = super.visitLambdaExpression(node, p);
        lambdaScopeStack.removeLast();
        return r;
    }

    private boolean inDebugClass()
    {
        return false; //typeScopeStack.stream().anyMatch(pa -> pa.item.getSimpleName().toString().contains("CreateTestAction"));
    }

    // Null if error,
    // non-null empty if just not found
    private Optional<LocatedTag> lambdaClassToAnn(Tree parent, TypeMirror lambdaClassType, Tree errorLocation, boolean issueError)
    {
        lambdaClassType = types.capture(lambdaClassType);
        // Look for the method we must be overriding:
        Element lambdaClassElement = types.asElement(lambdaClassType);
        List<Element> lambdaClassMembers = getMembers(lambdaClassElement, false, true)
                .stream()
                .filter(e -> e instanceof ExecutableElement)
                .filter(e -> !e.getModifiers().contains(Modifier.STATIC) && !e.getModifiers().contains(Modifier.DEFAULT))
                .filter(e -> !objectMembers.contains(e.getSimpleName().toString()))
                .collect(Collectors.toList());
        if (lambdaClassMembers.size() != 1)
        {
            if (issueError && !(lambdaClassType.getKind() == TypeKind.TYPEVAR))
                trees.printMessage(Kind.ERROR, "\n    Lambda type " + (lambdaClassElement == null ? "Unknown" : lambdaClassElement.getSimpleName()) + " seems to have multiple members: " + lambdaClassMembers.stream().map(Element::getSimpleName).map(Object::toString).collect(Collectors.joining(", ")), errorLocation, cu);
            return null;
        }
        LocatedTag lambdaAnn = null;
        // First check if the method the lambda is being passed to is special,
        // e.g. SwingUtilities.invokeLater, as that overrides any annotations on
        // the lambda's type.
        if (parent instanceof MethodInvocationTree)
        {
            String call = ((MethodInvocationTree)parent).getMethodSelect().toString();
            lambdaAnn = fromSpecial(lambdaClassType.toString(), call, getCurrentTag(Collections.emptyList(), errorLocation), errorLocation);
        }
        if (lambdaAnn != null)
            return Optional.of(lambdaAnn);
        
        if (inDebugClass())
            System.err.println("  >>Getting remote tag for " + lambdaClassMembers.get(0));
        
        // Check for tags put on the method in the lambda's class:
        lambdaAnn = getRemoteTag(lambdaClassMembers.get(0), () -> "", errorLocation);
        if (inDebugClass())
            System.err.println("  >> Tag on method: " + lambdaAnn);
        if (lambdaAnn != null)
            return Optional.of(lambdaAnn);
        // Next try its class for an annotation, first directly:
        lambdaAnn = getRemoteTag(lambdaClassElement, () -> "", errorLocation);
        if (inDebugClass())
            System.err.println("  >> Tag directly on class: " + lambdaAnn);
        if (lambdaAnn != null)
            return Optional.of(lambdaAnn);
        // And then check in our special list:
        String qualClassName = elements.getPackageOf(lambdaClassElement).getQualifiedName().toString() + "." + lambdaClassElement.getSimpleName();
        lambdaAnn = classAnns.get(qualClassName);
        if (inDebugClass())
            System.err.println("  >> Tag in classAnns: " + lambdaAnn + " based on " + qualClassName);
        if (lambdaAnn != null)
            return Optional.of(lambdaAnn);
        // Otherwise try the package, first directly:
        lambdaAnn = getRemoteTag(elements.getPackageOf(lambdaClassElement), () -> "", errorLocation);
        if (inDebugClass())
            System.err.println("  >> Tag directly on package: " + lambdaAnn);
        if (lambdaAnn != null)
            return Optional.of(lambdaAnn);
        // And finally our special list:
        lambdaAnn = packageAnns.get(elements.getPackageOf(lambdaClassElement).getQualifiedName().toString());
        if (inDebugClass())
            System.err.println("  >> Tag in packageAnns: " + lambdaAnn);

        return Optional.ofNullable(lambdaAnn);
    }

    private TypeMirror calculatedExpectedLambdaType(Tree parent, Tree lambdaArg)
    {
        return parent.accept(new SimpleTreeVisitor<TypeMirror, Void>() {

            @Override
            public TypeMirror visitReturn(ReturnTree node, Void p)
            {
                if (!lambdaScopeStack.isEmpty())
                    return null; // Lambda returning a lambda; too complex for us to work out
                // Return value; look at the return type of the inner most item:
                return trees.getTypeMirror(methodScopeStack.getLast().path).accept(new TypeVisitor<TypeMirror, Void>()
                {
                    @Override
                    public TypeMirror visitExecutable(ExecutableType t, Void p2)
                    {
                        return t.getReturnType();
                    }

                    @Override
                    public TypeMirror visit(TypeMirror t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visit(TypeMirror t)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitArray(ArrayType t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitDeclared(DeclaredType t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitError(ErrorType t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitIntersection(IntersectionType t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitNoType(NoType t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitNull(NullType t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitPrimitive(PrimitiveType t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitTypeVariable(TypeVariable t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitUnion(UnionType t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitUnknown(TypeMirror t, Void aVoid)
                    {
                        return null;
                    }

                    @Override
                    public TypeMirror visitWildcard(WildcardType t, Void aVoid)
                    {
                        return null;
                    }
                }, p);
            }

            @Override
            public TypeMirror visitVariable(VariableTree node, Void p)
            {
                // We must be on RHS; we just read the type of the variable:
                return trees.getTypeMirror(trees.getPath(cu, node.getType()));
            }

            @Override
            public TypeMirror visitMethodInvocation(MethodInvocationTree node,
                    Void p)
            {

                if (!callingMethodStack.isEmpty() && callingMethodStack.getLast() != null)
                {
                    for (int i = 0; i < node.getArguments().size(); i++)
                    {
                        if (node.getArguments().get(i) == lambdaArg && i < callingMethodStack.getLast().size())
                        {
                            return types.capture(callingMethodStack.getLast().get(i));
                        }
                    }
                }
                return null;
            }

            @Override
            public TypeMirror visitNewClass(NewClassTree node, Void arg1)
            {
                if (!callingMethodStack.isEmpty() && callingMethodStack.getLast() != null)
                {
                    for (int i = 0; i < node.getArguments().size(); i++)
                    {
                        if (node.getArguments().get(i) == lambdaArg && i < callingMethodStack.getLast().size())
                        {
                            return callingMethodStack.getLast().get(i);
                        }
                    }
                }
                return null;
            }
            
        }, null);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void arg1)
    {
        List<TypeMirror> argTypes = node.getArguments().stream()
                .map(arg -> trees.getTypeMirror(new TreePath(getCurrentPath(), arg)))
                .collect(Collectors.toList());
        
        WrapDescent wrap = checkInvocation(elements.getName("<init>"), node.getIdentifier(), trees.getTypeMirror(trees.getPath(cu, node.getIdentifier())), null, argTypes, arg1, node);
        if (wrap.before != null) wrap.before.run();
        Void r = super.visitNewClass(node, arg1);
        if (wrap.after != null) wrap.after.run();
        return r;
    }

    private static class PathAnd<T>
    {
        public final T item;
        public final TreePath path;
        private PathAnd(T item, TreePath path)
        {
            this.item = item;
            this.path = path;
        }
    }

    private class MethodRef
    {
        private final TypeMirror classType;
        private final String methodName;
        private final List<TypeMirror> methodType;
        private final LocatedTag tag;

        // NoSuchMethodException is a bit of an abuse of the exception, but it fits just right...
        public MethodRef(String className, String methodName, LocatedTag tag) throws NoSuchMethodException
        {
            TypeElement el = elements.getTypeElement(className);
            if (el == null)
                throw new NoSuchMethodException();
            this.classType = el.asType();
            this.methodName = methodName;
            this.methodType = elements.getAllMembers(el).stream().filter(e -> methodName.equals(e.getSimpleName().toString())).map(Element::asType).collect(Collectors.toList());
            this.tag = tag;
        }

        public boolean matches(TypeMirror classType, Name methodName, TypeMirror methodType)
        {
            // Check methodName first as it's quickest:
            return this.methodName.equals(methodName.toString()) &&
                     isSameType(types.erasure(this.classType), types.erasure(classType)) &&
                     // TODO should we re-enable parameter comparison?
                     (true || this.methodType.contains(methodType));
        }        
        
    }

    private class WrapDescent
    {
        Runnable before;
        Runnable after;
        
        public WrapDescent(Runnable before, Runnable after)
        {
            this.before = before;
            this.after = after;
        }
        //public WrapDescent() { this(null, null); }
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void aVoid)
    {
        // Can't have unsafe field accesses in constructors or initialisers:
        if (methodScopeStack.size() > 0 && (methodScopeStack.getLast().item == null || methodScopeStack.getLast().item.getReturnType() == null))
            return super.visitIdentifier(node, aVoid);
        
        LocatedTag tag = fields.get(node.getName().toString());
        if (tag != null) 
        {
            // Our present tag:
            Collection<? extends TypeMirror> superTypes = allSuperTypes(
                    trees.getTypeMirror(typeScopeStack.getLast().path), node);

            Optional<LocatedTag> ann = getCurrentTag(superTypes, node);
            
            if (ann.isPresent() && !ann.get().tag.canCall(tag.tag, true))
            {
                issueError("\n    Field " + node.getName() + " being used requires " + tag + "\nbut this code may be running on another thread: " + ann.map(Object::toString).orElse("unspecified"), node);
            }
            
            if (tag.requireSynchronized() && methodScopeStack.size() > 0 && methodScopeStack.getLast().item != null && !methodScopeStack.getLast().item.getModifiers().getFlags().contains(Modifier.SYNCHRONIZED) && !inSynchronizedThis)
            {
                issueError("\n    Field " + node.getName() + " being used requires synchronized but method is not synchronized", node);
            }
        }
        
        return super.visitIdentifier(node, aVoid);
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void aVoid)
    {
        boolean isThis;
        if (node.getExpression() == null)
            isThis = true;
        else
        {
            // Bit hacky, but close enough:
            isThis = node.getExpression().toString().contains("this") || node.getExpression().toString().contains(".class") /* for static */;
        }
        if (isThis)
        {
            inSynchronizedThis = true;
            Void p = super.visitSynchronized(node, aVoid);
            inSynchronizedThis = false;
            return p;
        }
        else
            return super.visitSynchronized(node, aVoid);
    }

    @Override
    public Void visitVariable(VariableTree node, Void aVoid)
    {
        // Deal with fields for top-level classes (TODO handle inner classes):
        
        /*
        The rule for accessing fields is:
        
         - If the field is final:
           - If the field is primitive or String or File or AtomicInteger, access is allowed from Any thread
           - If the type of the field (possibly by way of the field type's package) has a tag, access is allowed from Any thread.  E.g. you may have a "final Package pkg;" field.  Any thread should be able to access the field, because all of Package's methods are protected by the Swing tag anyway.  Contrast with "final List foo;" -- this should not allow access from any thread because you can get race hazards modifying the list.
         - Otherwise, the field receives the tag from its enclosing type (or enclosing type's package).  So for example, Project is tagged Swing; all of Project's fields are also tagged Swing.
        
        Thus you should only get field problems flagged when some methods have different tags to the class as a whole, or you use fields from inside invokeLater/runLater.  Otherwise, all your methods are assumed to be running on the same thread, and thus there are no race hazards.
        */
        
        if (typeScopeStack.size() == 1 && methodScopeStack.size() == 0)
        {
            // Field of top-level class.  Look for tags on the field itself:
            LocatedTag explicit = getSourceTag(node, () -> cu.getPackageName().toString() + typeScopeStack.stream().map(TCScanner::typeToName).collect(Collectors.joining(".")));
            if (explicit != null)
            {
                fields.put(node.getName().toString(), explicit);
                return super.visitVariable(node, aVoid);
            }
            else if (node.getModifiers().getFlags().contains(Modifier.VOLATILE))
            {
                // Already tagged volatile, no need to protect it further:
                fields.put(node.getName().toString(), new LocatedTag(Tag.Any, false, false, "volatile"));
                return super.visitVariable(node, aVoid);
            }
            else if (node.getModifiers().getFlags().contains(Modifier.FINAL))
            {
                // If they are final-primitive or final-immutable or final-atomic, no safety issues:
                if (Arrays.asList("String", "int", "double", "boolean", "char", "float", "short", "long", "File", "AtomicInteger").contains(node.getType().toString()))
                {
                    fields.put(node.getName().toString(), new LocatedTag(Tag.Any, false, false, "final String/primitive"));
                    return super.visitVariable(node, aVoid);
                }
                else
                {
                    TypeMirror typeMirror = trees.getTypeMirror(trees.getPath(cu, node));
                    if (typeMirror == null)
                        trees.printMessage(Kind.ERROR, "Null TypeMirror", node, cu);
                    Element element = types.asElement(typeMirror);
                    if (element == null)
                    {
                        // Probably means it's an an array type, so no special tag
                    }
                    else
                    {
                        LocatedTag varTypeTag = getRemoteTag(element, () -> node.getType().toString(), node);
                        if (varTypeTag != null)
                        {
                            // The class's methods are controlled by a tag, so it's ok so access the final variable from any thread:
                            fields.put(node.getName().toString(), new LocatedTag(Tag.Any, false, false, "final BlueJ class"));
                            return super.visitVariable(node, aVoid);
                        }
                        else
                        {
                            // No tag on class, but might be one on package:
                            PackageElement pkg = elements.getPackageOf(element);
                            LocatedTag pkgTag = getRemoteTag(pkg, () -> node.getType().toString(), node);
                            if (pkgTag != null)
                            {
                                // Same logic as above
                                fields.put(node.getName().toString(), new LocatedTag(Tag.Any, false, false, "final BlueJ class"));
                                return super.visitVariable(node, aVoid);
                            }
                        }
                    }
                }
                
            }
            // Give it default tag from class if none explicitly:
            Collection<? extends TypeMirror> superTypes = allSuperTypes(
                    trees.getTypeMirror(typeScopeStack.getLast().path), node);

            Optional<LocatedTag> ann = getCurrentTag(superTypes, cu);
            fields.put(node.getName().toString(), ann.orElse(null));
        }
        return super.visitVariable(node, aVoid);
    }

    private boolean isSameType(TypeMirror a, TypeMirror b)
    {
        // So if you use the checker framework null checker, and ask for java.lang.Thread,
        // you may get a different one than is in the JDK.  So as a backup we use qualified name
        // comparison if the types aren't found to be the same:
        return types.isSameType(a, b)
             || a.toString().equals(b.toString());

    }
}
