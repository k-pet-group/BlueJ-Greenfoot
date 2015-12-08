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
package bluej.stride.framedjava.frames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.generic.FrameTypeCheck;
import bluej.stride.slots.EditableSlot.MenuItemOrder;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javax.swing.SwingUtilities;

import bluej.editor.stride.BirdseyeManager;
import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.generic.FrameContentItem;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.slots.Focus;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.editor.stride.FrameEditorTab;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContent.ParamInfo;
import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.ClassElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.DocumentedMultiCanvasFrame;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.InteractionManager.Kind;
import bluej.stride.generic.RecallableFocus;
import bluej.stride.operations.CustomFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.ClassNameDefTextSlot;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.Implements;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.TriangleLabel;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.stride.slots.TypeTextSlot;
import bluej.utility.Utility;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.MultiListener;
import bluej.utility.javafx.binding.DeepListBinding;

public class ClassFrame extends DocumentedMultiCanvasFrame
  implements TopLevelFrame<ClassElement>
{
    private final SlotLabel abstractLabel = new SlotLabel("abstract");
    private final FrameContentRow importRow;
    private final BooleanBinding showInheritedToggle;
    private BooleanProperty abstractModifier = new SimpleBooleanProperty(false);
    
    private TextSlot<NameDefSlotFragment> paramClassName;
    private final InteractionManager editor;

    private final SimpleBooleanProperty headerHasKeyboardFocus = new SimpleBooleanProperty(false);
    
    private final SimpleBooleanProperty showingExtends;
    private final TextSlot<TypeSlotFragment> extendsSlot;

    private final ObservableList<InheritedCanvas> extendsInheritedCanvases = FXCollections.observableArrayList(); // May be empty
    private final FrameCanvas importCanvas;
    
    private final Implements implementsSlot;
    
    private ClassElement element;
    private final EntityResolver projectResolver;

    private final FrameCanvas fieldsCanvas;
    private final FrameCanvas constructorsCanvas;
    private final FrameCanvas methodsCanvas;
    private final SlotLabel importsLabel = makeLabel("Imports");
    private final SlotLabel fieldsLabel = makeLabel("Fields");
    private final SlotLabel constructorsLabel = makeLabel("Constructors");
    private final SlotLabel methodsLabel = makeLabel("Methods");
    private final FrameContentRow fieldsLabelRow;
    private final FrameContentRow constructorsLabelRow;
    private final FrameContentRow methodsLabelRow;
    private final TriangleLabel inheritedLabel;
    private final FrameContentItem endSpacer;
    private final TriangleLabel importTriangleLabel;
    private Map<String, List<AssistContentThreadSafe>> curMembersByClass = Collections.emptyMap();

    private static SlotLabel makeLabel(String content)
    {
        SlotLabel l = new SlotLabel(content);
        JavaFXUtil.addStyleClass(l, "class-section-label");
        return l;
    }
    
    private final ObservableList<String> boundImports = FXCollections.observableArrayList();
    
    
    public ClassFrame(InteractionManager editor, boolean abstractModifierParam, NameDefSlotFragment className, List<ImportElement> imports,
            TypeSlotFragment extendsName, List<TypeSlotFragment> implementsList, EntityResolver projectResolver, JavadocUnit documentation, boolean enabled)
    {
        super(editor, "class", "class-");
        this.editor = editor;
        this.projectResolver = projectResolver;
        this.abstractModifier.set(abstractModifierParam);
        JavaFXUtil.addChangeListener(this.abstractModifier, abs -> editor.modifiedFrame(this));

        // Spacer to make the class have a bit of space after last canvas;
        endSpacer = new FrameContentItem()
        {
            private Rectangle r = new Rectangle(1, 200, Color.TRANSPARENT);

            @Override
            public Stream<HeaderItem> getHeaderItemsDeep()
            {
                return Stream.empty();
            }

            @Override
            public Stream<HeaderItem> getHeaderItemsDirect()
            {
                return Stream.empty();
            }

            @Override
            public Bounds getSceneBounds()
            {
                return r.localToScene(r.getBoundsInLocal());
            }

            @Override
            public Optional<FrameCanvas> getCanvas()
            {
                return Optional.empty();
            }

            @Override
            public boolean focusLeftEndFromPrev()
            {
                return false;
            }

            @Override
            public boolean focusRightEndFromNext()
            {
                return false;
            }

            @Override
            public boolean focusTopEndFromPrev()
            {
                return false;
            }

            @Override
            public boolean focusBottomEndFromNext()
            {
                return false;
            }

            @Override
            public void setView(View oldView, View newView, SharedTransition animation)
            {

            }

            @Override
            public Node getNode()
            {
                return r;
            }
        };

        setDocumentation(documentation.toString());
      
        //Parameters
        paramClassName = new ClassNameDefTextSlot(editor, this, getHeaderRow(), "class-name-");
        paramClassName.addValueListener(SlotTraversalChars.IDENTIFIER);
        paramClassName.setPromptText("class name");
        paramClassName.setText(className);


        documentationPromptTextProperty().bind(new SimpleStringProperty("Write a description of your ").concat(paramClassName.textProperty()).concat(" class here..."));

        this.fieldsCanvas = new FrameCanvas(editor, this, "class-fields-");

        showingExtends = new SimpleBooleanProperty(extendsName != null);
        SlotLabel extendsLabel = new SlotLabel("extends");
        JavaFXUtil.addStyleClass(extendsLabel, "class-extends-caption");
        extendsSlot = new TypeTextSlot(editor, this, getHeaderRow(), new TypeCompletionCalculator(editor, Kind.CLASS_NON_FINAL), "class-extends-");
        extendsSlot.addValueListener(new SlotTraversalChars()
        {
            @Override
            public void backSpacePressedAtStart(HeaderItem slot)
            {
                extendsSlot.setText("");
            }
        });
        extendsSlot.addValueListener(SlotTraversalChars.IDENTIFIER);
        extendsSlot.setPromptText("parent class");
        if (extendsName != null) {
            extendsSlot.setText(extendsName);
        }
        // We must make the showing immediate when you get keyboard focus, as otherwise there
        // are problems with focusing the extends slot and then it disappears.
        // We no longer show on mouse hover:
        ObservableBooleanValue keyMouseHeader = JavaFXUtil.delay(headerHasKeyboardFocus, Duration.ZERO, Duration.millis(100));
        showingExtends.bind(extendsSlot.textProperty().isNotEmpty().or(keyMouseHeader));

        implementsSlot = new Implements(this, () -> {
            TypeTextSlot s = new TypeTextSlot(editor, this, getHeaderRow(), new TypeCompletionCalculator(editor, Kind.INTERFACE), "class-");
            s.setPromptText("interface type");
            return s;
        }, () -> fieldsCanvas.getFirstCursor().requestFocus(), editor);
        implementsList.forEach(t -> implementsSlot.addTypeSlotAtEnd(t.getContent(), false));

        JavaFXUtil.addChangeListener(keyMouseHeader, keyMouse -> {
            if (keyMouse)
                implementsSlot.ensureAtLeastOneSlot();
            else
                implementsSlot.clearIfSingleEmpty();
        });

        headerHasKeyboardFocus.bind(BooleanBinding.booleanExpression(paramClassName.effectivelyFocusedProperty()).or(BooleanBinding.booleanExpression(extendsSlot.effectivelyFocusedProperty())).or(implementsSlot.focusedProperty()));

        inheritedLabel = new TriangleLabel(editor, t -> extendsInheritedCanvases.forEach(c -> c.grow(t)),
            t -> extendsInheritedCanvases.forEach(c -> c.shrink(t)), new SimpleBooleanProperty(false));
        // Only enable the Label when we have inherited info available:
        inheritedLabel.setDisable(true);
        extendsInheritedCanvases.addListener((ListChangeListener<? super InheritedCanvas>) c -> {
            inheritedLabel.setDisable(extendsInheritedCanvases.isEmpty());
        });
        JavaFXUtil.addChangeListener(inheritedLabel.expandedProperty(), b -> editor.updateErrorOverviewBar());

        // We must keep hold of an explicit reference to this binding, rather than inlining it.
        // If you do not keep this stored in a field, it will get GC-ed.
        showInheritedToggle = showingExtends.and(Bindings.isNotEmpty(extendsInheritedCanvases));
        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<HeaderItem>>observableArrayList(
                JavaFXUtil.listBool(abstractModifier, abstractLabel),
                FXCollections.observableArrayList(headerCaptionLabel),
                FXCollections.observableArrayList(paramClassName),
                JavaFXUtil.listBool(showingExtends, extendsLabel, extendsSlot),
                JavaFXUtil.listBool(showInheritedToggle, inheritedLabel),
                implementsSlot.getHeaderItems()
            ));

        importCanvas = createImportsCanvas(imports);// TODO delete this and uncomment it in saved() if it cause NPE in future
        //importCanvas.addToLeftMargin(10.0);
        importCanvas.getShowingProperty().set(false);
        importTriangleLabel = new TriangleLabel(editor, t -> importCanvas.growUsing(t.getProgress()), t -> importCanvas.shrinkUsing(t.getOppositeProgress()), importCanvas.getShowingProperty());
        JavaFXUtil.addChangeListener(importTriangleLabel.expandedProperty(), b -> editor.updateErrorOverviewBar());
        importRow = new FrameContentRow(this, importsLabel, importTriangleLabel);
        //alterImports(editor.getImports());
        
        


        fieldsLabelRow = new FrameContentRow(this, fieldsLabel);
        addCanvas(fieldsLabelRow, fieldsCanvas);
        
        
        this.constructorsCanvas = new FrameCanvas(editor, this, "class-");

        constructorsLabelRow = new FrameContentRow(this, constructorsLabel);
        addCanvas(constructorsLabelRow, constructorsCanvas);


        this.methodsCanvas = new FrameCanvas(editor, this, "class-");

        methodsLabelRow = new FrameContentRow(this, methodsLabel);
        addCanvas(methodsLabelRow, methodsCanvas);
        
        
        frameEnabledProperty.set(enabled);
    }
    
    @Override
    public void focusOnBody(BodyFocus on)
    {
        FrameCursor c;
        if (on == BodyFocus.TOP)
        {
            c = fieldsCanvas.getFirstCursor();
        }
        else if (on == BodyFocus.BOTTOM)
        {
            c = methodsCanvas.getLastCursor();
        }
        else
        {
            // If we have any errors, focus on them
            Optional<CodeError> error = getCurrentErrors().findFirst();
            if (error.isPresent())
            {
                error.get().jumpTo(editor);
                return;
            }

            // Look for an act method:
            NormalMethodFrame act = getMethods().stream().filter(f -> f.getName().equals("act") && f.getParamsPane().isEmpty()).findFirst().orElse(null);
            if (act != null)
            {
                c = act.getFirstInternalCursor();
            }
            else
            {
                // Go to top of methods:
                c = methodsCanvas.getFirstCursor();
            }
        }
        c.requestFocus();
        editor.scrollTo(c.getNode(), -100);
    }
    
    // Can't drag class blocks:
    @Override
    public boolean canDrag()
    {    
        return false;
    }

    @Override
    public void bindMinHeight(DoubleBinding prop)
    {
        getRegion().minHeightProperty().bind(prop);        
    }

    @Override
    public synchronized void regenerateCode()
    {
        List<CodeElement> fields = getMembers(fieldsCanvas);
        List<CodeElement> constructors = getMembers(constructorsCanvas);
        List<CodeElement> methods = getMembers(methodsCanvas);
        List<ImportElement> imports = Utility.mapList(getMembers(importCanvas), e -> (ImportElement)e);
        element = new ClassElement(this, projectResolver, abstractModifier.get(), paramClassName.getSlotElement(),
                    showingExtends.get() && !extendsSlot.getText().equals("") ? extendsSlot.getSlotElement() : null,
                    implementsSlot.getTypes(), fields, constructors, methods,
                    new JavadocUnit(getDocumentation()), imports, frameEnabledProperty.get());
    }

    private List<CodeElement> getMembers(FrameCanvas frameCanvas)
    {
        List<CodeElement> members = new ArrayList<CodeElement>();
        for (CodeFrame<?> c : frameCanvas.getBlocksSubtype(CodeFrame.class)) {
            c.regenerateCode();
            members.add(c.getCode());
        }
        return members;
    }

    @Override
    @OnThread(value = Tag.Any, ignoreParent = true)
    public synchronized ClassElement getCode()
    {
        return element;
    }

    @Override
    public List<FrameOperation> getContextOperations()
    {
        ArrayList<FrameOperation> ops = new ArrayList<>();
        ops.add(new CustomFrameOperation(getEditor(), "addRemoveAbstract", Arrays.asList("Toggle abstract"), MenuItemOrder.TOGGLE_ABSTRACT, this, () ->  abstractModifier.set(!abstractModifier.get())));

        if (extendsSlot.isEmpty())
        {
            ops.add(new CustomFrameOperation(getEditor(), "addExtends", Arrays.asList("Add 'extends'"), MenuItemOrder.TOGGLE_EXTENDS, this, () -> {

                showAndFocusExtends();
            }));
        }
        else
        {
            CustomFrameOperation op = new CustomFrameOperation(getEditor(), "removeExtends", Arrays.asList("Remove 'extends " + extendsSlot.getText() + "'"), MenuItemOrder.TOGGLE_EXTENDS, this, () -> {
                extendsSlot.setText("");
            });
            op.setWideCustomItem(true);
            ops.add(op);
        }

        ops.add(new CustomFrameOperation(getEditor(), "addImplements", Arrays.asList("Add 'implements'"), MenuItemOrder.TOGGLE_IMPLEMENTS, this, () -> {
            implementsSlot.addTypeSlotAtEnd("", true);
        }));

        final List<TypeSlotFragment> types = implementsSlot.getTypes();
        for (int i = 0; i < types.size(); i++)
        {
            final int index = i;
            TypeSlotFragment type = types.get(i);
            CustomFrameOperation removeOp = new CustomFrameOperation(getEditor(), "removeImplements", Arrays.asList("Remove 'implements " + type.getContent() + "'"), MenuItemOrder.TOGGLE_IMPLEMENTS, this, () -> {
                implementsSlot.removeIndex(index);
            });
            removeOp.setWideCustomItem(true);
            ops.add(removeOp);
        }

        return ops;
    }

    /**
     * Show the extends slot, and focus it.
     */
    private void showAndFocusExtends()
    {
        // This is a bit of a hack.  We can't request focus on extendsSlot until it's part of the
        // scene, but it only gets added to the scene when it's non-empty, or when the header has
        // keyboard focus.  We could set content, focus it, then empty it, or we can take this
        // route of first focusing the class name, thus showing the extends slot, then moving
        // the focus to the extends slot:
        paramClassName.requestFocus();
        extendsSlot.requestFocus();
    }

    @Override
    public List<FrameOperation> getCutCopyPasteOperations(InteractionManager editor)
    {
        return new ArrayList<>();
    }

    @Override
    public List<ExtensionDescription> getAvailableInnerExtensions(FrameCanvas canvas, FrameCursor cursor)
    {
        ExtensionDescription abstractExtension = null;
        if (canvas.equals(fieldsCanvas)) {
            abstractExtension = new ExtensionDescription(GreenfootFrameDictionary.ABSTRACT_EXTENSION_CHAR, "Toggle abstract",
                    () -> abstractModifier.set(!abstractModifier.get()));
        }
        
        ExtensionDescription extendsExtension = null;
        if (!showingExtends.get()) {
            extendsExtension = new ExtensionDescription(GreenfootFrameDictionary.EXTENDS_EXTENSION_CHAR, "Add extends declaration", () -> {
                showAndFocusExtends();
            });
        }
        ExtensionDescription implementsExtension = new ExtensionDescription(GreenfootFrameDictionary.IMPLEMENTS_EXTENSION_CHAR, "Add implements declaration", () -> {
            implementsSlot.addTypeSlotAtEnd("", true);
        });
        
        return Utility.nonNulls(Arrays.asList(abstractExtension, extendsExtension, implementsExtension));
    }
/*
    private void removeExtends()
    {
        showingExtends.set(false);
        paramClassName.requestFocus(Focus.RIGHT);
        extendsSlot.setText("");
        editor.modifiedFrame(this);
    }
*/
    @Override
    public void saved()
    {
        if (extendsInheritedCanvases.isEmpty()) {
            updateInheritedItems();
        }
    }
    
    private FrameCanvas createImportsCanvas(final List<ImportElement> imports)
    {
        FrameCanvas importCanvas = new FrameCanvas(editor, new CanvasParent() {
            
            @Override
            public FrameCursor findCursor(double sceneX, double sceneY, FrameCursor prevCursor, FrameCursor nextCursor, List<Frame> exclude, boolean isDrag, boolean canDescend)
            {
                return ClassFrame.this.importCanvas.findClosestCursor(sceneX, sceneY, exclude, isDrag, canDescend);
            }

            @Override
            public FrameTypeCheck check(FrameCanvas canvasBase)
            {
                return GreenfootFrameDictionary.checkImport();
            }
            
            @Override
            public List<ExtensionDescription> getAvailableInnerExtensions(FrameCanvas canvas, FrameCursor cursor)
            {
                return Collections.emptyList();
            }
            
            @Override
            public Frame getFrame()
            {
                return ClassFrame.this;
            }

            @Override
            public InteractionManager getEditor()
            {
                return editor;
            }

        }, "class-import-");
        
        importCanvas.setAnimateLeftMarginScale(true);
        
        // Add available import frames:
        List<ImportElement> importsRev = new ArrayList<>(imports);
        Collections.reverse(importsRev);
        importsRev.forEach(item -> importCanvas.insertBlockBefore(item.createFrame(editor), importCanvas.getFirstCursor()));
        
        importCanvas.shrinkUsing(new ReadOnlyDoubleWrapper(0.0));
        
        new DeepListBinding<String>(boundImports) {
            private final ChangeListener<String> listener = (a, b, c) -> update();
            private final MultiListener<ObservableStringValue> stringListener
                = new MultiListener<>(v -> { v.addListener(listener); return () -> v.removeListener(listener); });
            
            @Override
            protected Stream<ObservableList<?>> getListenTargets()
            {
                return Stream.of(importCanvas.getBlockContents());
            }

            @Override
            protected Stream<String> calculateValues()
            {
                return importCanvas.getBlockContents().stream().map(f -> (ImportFrame)f).map(ImportFrame::getImport);
            }
            
            @Override
            protected void update()
            {
                stringListener.listenOnlyTo(importCanvas.getBlockContents().stream().map(f -> (ImportFrame)f).map(ImportFrame::importProperty));
                super.update();
            }
            
        }.startListening();
        
        return importCanvas;
    }
    
    public ObservableList<String> getImports()
    {
        return boundImports;
    }
    
    public void addImport(String importSrc)
    {
        importCanvas.insertBlockAfter(new ImportFrame(editor, importSrc), importCanvas.getLastCursor());
    }

    public void addDefaultConstructor()
    {
        constructorsCanvas.getFirstCursor().insertBlockAfter(ConstructorFrame.getFactory().createBlock(editor));
    }

    private Comparator<String> getSuperClassComparator()
    {
        return (a, b) -> {
            if (a == null || b == null)
                throw new IllegalArgumentException("Null strings for super-class names");
            if (a.equals(b))
                return 0;

            // Object is always last:
            if ("java.lang.Object".equals(a))
                return 1;
            else if ("java.lang.Object".equals(b))
                return -1;
            // Direct super-class is always first:
            if (extendsSlot.getText().equals(a))
                return -1;
            else if (extendsSlot.getText().equals(b))
                return 1;
            else
                return a.compareTo(b);
        };
    }


    private void updateInheritedItems()
    {
        // Add available frames:
        withInheritedItems(new HashSet<CompletionKind>(Arrays.asList(CompletionKind.FIELD, CompletionKind.METHOD)), membersByClass ->
        {
            if (inheritedEquals(membersByClass, curMembersByClass))
            {
                // Same as before, so nothing to do:
                return;
            }
            extendsInheritedCanvases.forEach(c -> removeCanvas(c.canvas));
            extendsInheritedCanvases.clear();

            // We should probably sort by inheritance hierarchy, but for now we'll just go near-alphabetical
            List<String> classNames = membersByClass.keySet().stream().sorted(getSuperClassComparator()).collect(Collectors.toList());

            // Add in reverse order:
            Collections.reverse(classNames);

            classNames.forEach(cls -> {
                InheritedCanvas section = new InheritedCanvas(this, editor, cls, classNames.size() == 1);
                // If triangle already folded in, make sure everything is collapsed:
                if (inheritedLabel.expandedProperty().get() == false)
                {
                    section.canvas.shrinkUsing(new ReadOnlyDoubleWrapper(0.0));
                    if (section.optionalCollapse != null)
                        section.optionalCollapse.setVisible(false);
                    if (section.precedingDividerLabel != null)
                        section.precedingDividerLabel.shrinkInstantly();
                }
                extendsInheritedCanvases.add(section);
                addCanvas(section.precedingDivider, section.canvas, 0);

                List<AssistContentThreadSafe> items = membersByClass.get(cls);
                List<AssistContentThreadSafe> methods = items.stream().filter(a -> a.getKind() == CompletionKind.METHOD).collect(Collectors.toList());
                List<AssistContentThreadSafe> fields = items.stream().filter(a -> a.getKind() == CompletionKind.FIELD).collect(Collectors.toList());
                //InheritedFrame inheritedFrame = new InheritedFrame(editor, cls, fields, methods);
                // Reversed as insertBlockBefore will cause them to be inserted in a reversed order.
                Collections.reverse(fields);
                fields.forEach(field ->
                                section.canvas.insertBlockBefore(new InheritedFieldFrame(editor,
                                                AccessPermission.fromAccess(field.getAccessPermission()), field.getType(), field.getName()),
                                        section.canvas.getFirstCursor())
                );

                // Reversed as insertBlockBefore will cause them to be inserted in a reversed order.
                Collections.reverse(methods);
                methods.forEach(method ->
                                section.canvas.insertBlockBefore(new InheritedMethodFrame(editor, this, cls,
                                                AccessPermission.fromAccess(method.getAccessPermission()), method.getType(), method.getName(), method.getParams()),
                                        section.canvas.getFirstCursor())
                );
            });
            extendsInheritedCanvases.forEach(s -> s.canvas.getCursors().forEach(c -> c.getNode().setFocusTraversable(false)));
            curMembersByClass = membersByClass;
        });
    }

    /**
     * Checks if two sets of inherited items are the same, in terms of the information they would
     * produce in the inherited canvas.
     * @param a One of the sets, mapping super-class name to items
     * @param b The other set
     * @return True if they are equal in that they would produce the same display in the inherited canvas.
     */
    private static boolean inheritedEquals(Map<String, List<AssistContentThreadSafe>> a, Map<String, List<AssistContentThreadSafe>> b)
    {
        // Must be same size:
        if (a.size() != b.size())
            return false;

        Set<String> ak = a.keySet();
        Set<String> bk = b.keySet();

        // Must have same set of super-classes:
        if (!ak.equals(bk))
            return false;

        for (String k : ak)
        {
            List<AssistContentThreadSafe> av = a.get(k);
            List<AssistContentThreadSafe> bv = b.get(k);

            // Each super-class must have same number of items:
            if (av.size() != bv.size())
                return false;

            for (int i = 0; i < av.size(); i++)
            {
                AssistContentThreadSafe ax = av.get(i);
                AssistContentThreadSafe bx = bv.get(i);

                // The inherited canvas signature/info differs by field and method, and contains:
                //   Field: access permission, type, name
                //   Method: access permission, return type, name, params

                // Check the items present in both fields and methods:
                if (ax.getKind() != bx.getKind()
                    || ax.getAccessPermission() != bx.getAccessPermission()
                    || !ax.getName().equals(bx.getName())
                    || !ax.getType().equals(bx.getType()))
                    return false;

                // For methods, check the params:
                if (ax.getKind() == CompletionKind.METHOD)
                {
                    List<ParamInfo> ap = ax.getParams();
                    List<ParamInfo> bp = bx.getParams();

                    // Must have same number of params:
                    if (ap.size() != bp.size())
                        return false;

                    // For params, need to check that name and type matches
                    for (int j = 0; j < ap.size(); j++)
                    {
                        if (!ap.get(j).getFormalName().equals(bp.get(j).getFormalName()) ||
                            !ap.get(j).getQualifiedType().equals(bp.get(j).getQualifiedType()))
                            return false;
                    }
                }
            }
        }

        return true;
    }

    public void withInheritedItems(Set<CompletionKind> kinds, FXConsumer<Map<String, List<AssistContentThreadSafe>>> handler)
    {
        // Get all available items
        editor.withAccessibleMembers(element.getPosInsideClass(), kinds, true, allMembers ->
        {
            // Split by class:
            HashMap<String, List<AssistContentThreadSafe>> methodsByClass = new HashMap<>();
            for (AssistContentThreadSafe a : allMembers)
            {
                if (a.getDeclaringClass().equals(paramClassName.getText()))
                {
                    continue; // Don't show an inherited frame for ourselves!
                }

                if (methodsByClass.containsKey(a.getDeclaringClass()))
                {
                    methodsByClass.get(a.getDeclaringClass()).add(a);
                } else
                {
                    List<AssistContentThreadSafe> l = new ArrayList<>();
                    l.add(a);
                    methodsByClass.put(a.getDeclaringClass(), l);
                }
            }
            methodsByClass.forEach((k, v) -> v.sort(Comparator.comparing(AssistContentThreadSafe::getName).thenComparing(ac -> Utility.mapList(ac.getParams(), ParamInfo::getUnqualifiedType), Utility.listComparator())));
            handler.accept(methodsByClass);
        });
    }
    
    @Override
    public BirdseyeManager prepareBirdsEyeView(SharedTransition animate)
    {
        final List<FrameCanvas> canvases = Arrays.asList(constructorsCanvas, methodsCanvas);

        int startingCanvas = 0;
        // Guaranteed to pick a canvas if canDoBirdseye returned true:
        while (canvases.get(startingCanvas).blockCount() == 0)
            startingCanvas += 1;

        Frame startingFrame = canvases.get(startingCanvas).getBlockContents().get(0);

        // See if focus owner belongs in any of these frames:
        Node focusOwner = canvases.get(0).getNode().getScene().getFocusOwner();
        canvasLoop: for (int i = 0; i < canvases.size(); i++)
        {
            for (Frame f : canvases.get(i).getBlockContents())
            {
                if (nodeInside(focusOwner, (Parent)f.getNode()))
                {
                    startingCanvas = i;
                    startingFrame = f;
                    break canvasLoop;
                }

            }
        }

        // Keep compiler happy:
        final int finalStartingCanvas = startingCanvas;
        final Frame finalStartingFrame = startingFrame;

        return new BirdseyeManager()
        {
            private int canvasIndex = finalStartingCanvas;
            private Frame frame = finalStartingFrame;

            @Override
            public Node getNodeForRectangle()
            {
                return frame.getNode();
            }

            private Frame getFrameAt(double sceneX, double sceneY)
            {
                // Need to see if those coordinates match a rectangle:
                for (FrameCanvas canvas : canvases)
                {
                    for (Frame f : canvas.getBlockContents())
                    {
                        Node n = f.getNode();
                        Point2D scene = n.localToScene(n.getBoundsInLocal().getMinX(), n.getBoundsInLocal().getMinY());

                        if (scene.getX() <= sceneX && sceneX < scene.getX() + n.getBoundsInLocal().getWidth()
                                && scene.getY() <= sceneY && sceneY < scene.getY() + n.getBoundsInLocal().getHeight())
                        {
                            return f;
                        }
                    }
                }
                return null;
            }

            @Override
            public boolean canClick(double sceneX, double sceneY)
            {
                return getFrameAt(sceneX, sceneY) != null;
            }

            @Override
            public FrameCursor getClickedTarget(double sceneX, double sceneY)
            {
                Frame f = getFrameAt(sceneX, sceneY);
                if (f != null)
                {
                    return f.getFirstInternalCursor();
                }
                else
                {
                    return null;
                }
            }

            @Override
            public FrameCursor getCursorForCurrent()
            {
                return frame.getFirstInternalCursor();
            }

            @Override
            public void up()
            {
                Frame before = canvases.get(canvasIndex).getFrameBefore(canvases.get(canvasIndex).getCursorBefore(frame));
                if (before == null)
                {
                    int prospective = canvasIndex - 1;
                    while (prospective >= 0)
                    {
                        if (canvases.get(prospective).blockCount() > 0)
                        {
                            canvasIndex = prospective;
                            frame = canvases.get(canvasIndex).getFrameBefore(canvases.get(canvasIndex).getLastCursor());
                            return;
                        }
                        // Keep looking back
                        prospective -= 1;
                    }
                    // Nothing above us; nowhere to go
                }
                else
                {
                    frame = before;
                }
            }

            @Override
            public void down()
            {
                Frame after = canvases.get(canvasIndex).getFrameAfter(canvases.get(canvasIndex).getCursorAfter(frame));
                if (after == null)
                {
                    int prospective = canvasIndex + 1;
                    while (prospective < canvases.size())
                    {
                        if (canvases.get(prospective).blockCount() > 0)
                        {
                            canvasIndex = prospective;
                            frame = canvases.get(canvasIndex).getFrameAfter(canvases.get(canvasIndex).getFirstCursor());
                            return;
                        }
                        // Keep looking forward
                        prospective += 1;
                    }
                    // Nothing below us; nowhere to go
                }
                else
                {
                    frame = after;
                }
            }
        };
    }

    private boolean nodeInside(Node target, Parent parent)
    {
        // Borrowed from http://stackoverflow.com/questions/17731330/in-javafx-how-do-i-determine-if-the-node-gaining-focus-is-a-child-node-of-myself

        for (Node node : parent.getChildrenUnmodifiable())
        {
            if (node == target)
            {
                return true;
            }
            else if (node instanceof Parent)
            {
                if (nodeInside(target, (Parent) node))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canDoBirdseye()
    {
        // Only if we have some members:
        return !(constructorsCanvas.getBlockContents().isEmpty() && methodsCanvas.getBlockContents().isEmpty());
    }

    @Override
    public List<NormalMethodFrame> getMethods()
    {
        // abstract methods?
        return methodsCanvas.getBlocksSubtype(NormalMethodFrame.class);
    }

    @Override
    public List<ConstructorFrame> getConstructors()
    {
        return constructorsCanvas.getBlocksSubtype(ConstructorFrame.class);
    }

    @Override
    public void insertAtEnd(Frame frame)
    {
        methodsCanvas.getLastCursor().insertBlockAfter(frame);
    }

    public FrameCanvas getfieldsCanvas()
    {
        return fieldsCanvas;
    }
    
    public FrameCanvas getConstructorsCanvas()
    {
        return constructorsCanvas;
    }
    
    public FrameCanvas getMethodsCanvas()
    {
        return methodsCanvas;
    }

    @Override
    public ObservableStringValue nameProperty()
    {
        return paramClassName.textProperty();
    }

    public void findMethod(String methodName, List<ParamInfo> params, FXConsumer<NormalMethodFrame> callback)
    {
        ClassElement el = getCode();
        SwingUtilities.invokeLater(() -> {
            Optional<NormalMethodFrame> method = el.streamMethods()
                    .filter(e -> {
                        if (!(e instanceof NormalMethodElement))
                            return false;
                        NormalMethodElement m = (NormalMethodElement) e;
                        return m.equalDeclaration(methodName, params, el);
                    }).map(e -> ((NormalMethodElement) e).getFrame())
                    .findFirst();
            Platform.runLater(() -> callback.accept(method.orElse(null)));
        });
    }

    @Override
    public Stream<RecallableFocus> getFocusables()
    {
        // All slots, and all cursors:
        return getFocusablesInclContained();
    }
    
    @Override
    public void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        setViewNoOverride(oldView, newView, animateProgress);
        boolean java = newView == View.JAVA_PREVIEW;
        //Debug.message("Setting view: " + view + " " + java);
        if (oldView == View.JAVA_PREVIEW || newView == View.JAVA_PREVIEW)
        {
            fieldsCanvas.previewCurly(java, true, false, header.getLeftFirstItem(), null, animateProgress);
            methodsCanvas.previewCurly(java, false, true, header.getLeftFirstItem(), null, animateProgress);
        }

        getCanvases().forEach(canvas -> {
            canvas.setView(oldView, newView, animateProgress);
            canvas.getCursors().forEach(c -> c.setView(newView, animateProgress));
        });

        if (!extendsInheritedCanvases.isEmpty())
        {
            if (newView != View.NORMAL)
                inheritedLabel.expandedProperty().set(false);
        }
        inheritedLabel.setVisible(newView == View.NORMAL);
        final List<FrameContentRow> labelRows = Arrays.asList(importRow, fieldsLabelRow, constructorsLabelRow, methodsLabelRow);
        if (newView == View.NORMAL)
        {
            animateProgress.addOnStopped(() -> {
                importTriangleLabel.setVisible(true);
                importTriangleLabel.setManaged(true);
                labelRows.forEach(r -> r.setSnapToPixel(true));
            });
        }
        else
        {
            labelRows.forEach(r -> r.setSnapToPixel(false));
            importTriangleLabel.setVisible(false);
            importTriangleLabel.setManaged(false);
        }
        // Always show imports in Java preview:
        if (java)
            importTriangleLabel.expandedProperty().set(true);
        // And don't show in bird's eye:
        else if (newView == View.BIRDSEYE)
            importTriangleLabel.expandedProperty().set(false);

        List<SlotLabel> animateLabels = Arrays.asList(importsLabel, fieldsLabel, constructorsLabel, methodsLabel);
        if (java)
        {
            animateLabels.forEach(l -> l.shrinkVertically(animateProgress));
        }
        else if (oldView == View.JAVA_PREVIEW)
        {
            animateLabels.forEach(l -> l.growVertically(animateProgress));
        }
    }
    
    public void compiled()
    {
        updateInheritedItems();
    }

    @Override
    public FrameCanvas getImportCanvas()
    {
        return importCanvas;
    }

    @Override
    public void ensureImportCanvasShowing()
    {
        importCanvas.getShowingProperty().set(true);
    }

    @Override
    public Stream<FrameCanvas> getPersistentCanvases()
    {
        return getCanvases().filter(canvas -> !extendsInheritedCanvases.contains(canvas));
    }

    @Override
    public EditableSlot getErrorShowRedirect()
    {
        return paramClassName;
    }

    @Override
    public void focusName()
    {
        paramClassName.requestFocus(Focus.LEFT);
    }

    @Override
    public FrameTypeCheck check(FrameCanvas canvas)
    {
        if (canvas == fieldsCanvas)
            return GreenfootFrameDictionary.checkField();
        else if (canvas == methodsCanvas)
            return GreenfootFrameDictionary.checkClassMethod();
        else if (canvas == constructorsCanvas)
            return GreenfootFrameDictionary.checkConstructor();
        else
            throw new IllegalStateException("Asking about canvas unknown to ClassFrame");
    }
    
    @Override
    public CanvasKind getChildKind(FrameCanvas c)
    {
        if (c == fieldsCanvas)
            return CanvasKind.FIELDS;
        else if (c == constructorsCanvas)
            return CanvasKind.CONSTRUCTORS;
        else if (c == methodsCanvas)
            return CanvasKind.METHODS;
        else
            return CanvasKind.STATEMENTS; // Not true, but it's our default for now
    }

    @Override
    protected void modifyChildren(List<FrameContentItem> updatedChildren)
    {
        super.modifyChildren(updatedChildren);
        updatedChildren.add(0, importRow);
        updatedChildren.add(1, importCanvas);
        updatedChildren.add(endSpacer);
    }

    @Override
    public void restore(ClassElement target)
    {
        paramClassName.setText(target.getName());
        abstractModifier.set(target.isAbstract());
        restoreExtends(target);
        implementsSlot.setTypes(target.getImplements());
        importCanvas.restore(target.getImports(), editor);
        methodsCanvas.restore(target.getMethods(), editor);
        fieldsCanvas.restore(target.getFields(), editor);
        constructorsCanvas.restore(target.getConstructors(), editor);
        
    }

    private void restoreExtends(ClassElement target)
    {
        String targetExtends = target.getExtends();
        if (targetExtends != null) {
            if (!extendsSlot.getText().equals(targetExtends)) {
                extendsSlot.setText(targetExtends);
            }
        }
        else if (showingExtends.get()) {
            extendsSlot.setText("");
        }
    }

    @Override
    protected FrameContentRow makeHeader(String stylePrefix)
    {
        return new FrameContentRow(this, stylePrefix) {
            @Override
            public boolean focusRightEndFromNext()
            {
                implementsSlot.ensureAtLeastOneSlot();
                Utility.findLast(implementsSlot.getTypeSlots()).get().requestFocus(Focus.RIGHT);
                return true;
            }
        };
    }
}
