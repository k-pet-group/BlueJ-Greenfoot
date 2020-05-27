/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2018,2019,2020 Michael Kölling and John Rosenberg
 
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

import bluej.Config;
import bluej.parser.AssistContentThreadSafe;
import bluej.editor.stride.BirdseyeManager;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContent.ParamInfo;
import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.ClassElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.*;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.operations.CopyFrameAsImageOperation;
import bluej.stride.operations.CopyFrameAsJavaOperation;
import bluej.stride.operations.CopyFrameAsStrideOperation;
import bluej.stride.operations.CustomFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.Focus;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.Implements;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.TriangleLabel;
import bluej.utility.Utility;
import bluej.utility.javafx.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassFrame extends TopLevelDocumentMultiCanvasFrame<ClassElement>
{
    private final SlotLabel abstractLabel = new SlotLabel("abstract");
    private final SimpleBooleanProperty focusHasBeenInNameOrExtends;
    private BooleanProperty abstractModifier = new SimpleBooleanProperty(false);

    private final TypeSlot extendsSlot;
    private final SimpleBooleanProperty showingExtends;
    private final TriangleLabel inheritedLabel;
    private final BooleanBinding showInheritedToggle;
    private final ObservableList<InheritedCanvas> extendsInheritedCanvases = FXCollections.observableArrayList(); // May be empty

    private final Implements implementsSlot;

    private final ReadOnlyBooleanProperty focusInName;
    private final ReadOnlyBooleanProperty focusInExtends;
    private final BooleanBinding focusInNameOrExtends;
    private Map<String, List<AssistContentThreadSafe>> curMembersByClass = Collections.emptyMap();

    private final FrameCanvas constructorsCanvas;
    private final SlotLabel constructorsLabel;
    private final FrameContentRow constructorsLabelRow;

    public ClassFrame(InteractionManager editor, EntityResolver projectResolver, String packageName, List<ImportElement> imports,
                      JavadocUnit documentation, boolean abstractModifierParam, NameDefSlotFragment className, TypeSlotFragment extendsName,
                      List<TypeSlotFragment> implementsList, boolean enabled)
    {
        super(editor, projectResolver, "class", "class-", packageName, imports, documentation, className, enabled);

        this.abstractModifier.set(abstractModifierParam);
        JavaFXUtil.addChangeListener(this.abstractModifier, abs -> editor.modifiedFrame(this, false));

        showingExtends = new SimpleBooleanProperty(extendsName != null);
        SlotLabel extendsLabel = new SlotLabel("extends");
        JavaFXUtil.addStyleClass(extendsLabel, "class-extends-caption");
        extendsSlot = new TypeSlot(editor, this, this, getHeaderRow(), TypeSlot.Role.EXTENDS, "class-extends-");
        extendsSlot.addClosingChar(' ');
        extendsSlot.setSimplePromptText("parent class");
        if (extendsName != null) {
            extendsSlot.setText(extendsName);
        }
        
        implementsSlot = new Implements(this, () -> {
            TypeSlot s = new TypeSlot(editor, this, this, getHeaderRow(), TypeSlot.Role.INTERFACE, "class-");
            s.setSimplePromptText("interface type");
            return s;
        }, () -> getCanvases().findFirst().ifPresent(c -> c.getFirstCursor().requestFocus()), editor);
        implementsList.forEach(t -> implementsSlot.addTypeSlotAtEnd(t.getContent(), false));

        // We must make the showing immediate when you get keyboard focus, as otherwise there
        // are problems with focusing the extends slot and then it disappears.
        // We no longer show on mouse hover:
        focusInName = JavaFXUtil.delay(paramName.effectivelyFocusedProperty(), Duration.ZERO, Duration.millis(100));
        focusInExtends = JavaFXUtil.delay(extendsSlot.effectivelyFocusedProperty(), Duration.ZERO, Duration.millis(100));
        focusInNameOrExtends = BooleanBinding.booleanExpression(focusInName).or(focusInExtends);
        focusHasBeenInNameOrExtends = new SimpleBooleanProperty(false);
        FXRunnable updateFocus = () ->
        {
            if (focusInNameOrExtends.get())
            {
                implementsSlot.ensureAtLeastOneSlot();
                focusHasBeenInNameOrExtends.set(true);
            }
            else
            {
                if (!implementsSlot.focusedProperty().get())
                    implementsSlot.clearIfSingleEmpty();
                focusHasBeenInNameOrExtends.set(focusHasBeenInNameOrExtends.get() && implementsSlot.focusedProperty().get());
            }
        };
        JavaFXUtil.addChangeListener(focusInNameOrExtends, f -> updateFocus.run());
        JavaFXUtil.addChangeListener(implementsSlot.focusedProperty(), f -> updateFocus.run());
        JavaFXUtil.addChangeListener(focusHasBeenInNameOrExtends, h -> showingExtends.set(!extendsSlot.isEmpty() || focusHasBeenInNameOrExtends.get()));
        extendsSlot.onTextPropertyChange(s -> showingExtends.set(!extendsSlot.isEmpty() || focusHasBeenInNameOrExtends.get()));

        inheritedLabel = new TriangleLabel(editor, t -> extendsInheritedCanvases.forEach(c -> c.grow(t)),
            t -> extendsInheritedCanvases.forEach(c -> c.shrink(t)), new SimpleBooleanProperty(false));
        // Only enable the Label when we have inherited info available:
        inheritedLabel.setDisable(true);
        extendsInheritedCanvases.addListener((ListChangeListener<? super InheritedCanvas>) c ->
            inheritedLabel.setDisable(extendsInheritedCanvases.isEmpty())
        );
        JavaFXUtil.addChangeListenerPlatform(inheritedLabel.expandedProperty(),
                b -> editor.updateErrorOverviewBar());

        // We must keep hold of an explicit reference to this binding, rather than inlining it.
        // If you do not keep this stored in a field, it will get GC-ed.
        showInheritedToggle = showingExtends.and(Bindings.isNotEmpty(extendsInheritedCanvases));
        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<? extends HeaderItem>>observableArrayList(
                JavaFXUtil.listBool(abstractModifier, abstractLabel),
                FXCollections.observableArrayList(headerCaptionLabel),
                FXCollections.observableArrayList(paramName),
                JavaFXUtil.listBool(showingExtends, List.of(extendsLabel, extendsSlot)),
                JavaFXUtil.listBool(showInheritedToggle, inheritedLabel),
                implementsSlot.getHeaderItems()
            ));

        constructorsLabel = makeLabel(Config.getString("frame.editor.label.constructors"));
        this.constructorsCanvas = new FrameCanvas(editor, this, "class-");
        constructorsLabelRow = new FrameContentRow(this, constructorsLabel);
        addCanvas(constructorsLabelRow, constructorsCanvas, 1);
    }

    @Override
    public Stream<EditableSlot> getPossiblyHiddenSlotsDirect()
    {
        return Stream.of(extendsSlot);
    }

    protected Frame findASpecialMethod()
    {
        // Look for an act method:
        return getMethods().stream().filter(f -> f.getName().equals("act") && f.getParamsPane().isEmpty()).findFirst().orElse(null);
    }

    @Override
    public synchronized void regenerateCode()
    {
        List<CodeElement> fields = getMembers(fieldsCanvas);
        List<CodeElement> constructors = getMembers(constructorsCanvas);
        List<CodeElement> methods = getMembers(methodsCanvas);
        List<ImportElement> imports = Utility.mapList(getMembers(importCanvas), e -> (ImportElement)e);
        element = new ClassElement(this, projectResolver, abstractModifier.get(), paramName.getSlotElement(),
                    showingExtends.get() && !extendsSlot.getText().equals("") ? extendsSlot.getSlotElement() : null,
                    implementsSlot.getTypes(), fields, constructors, methods, new JavadocUnit(getDocumentation()),
                    packageNameLabel == null ? null : packageNameLabel.getText(), imports, frameEnabledProperty.get());
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
        ops.add(new CopyFrameAsStrideOperation(editor));
        ops.add(new CopyFrameAsImageOperation(editor));
        ops.add(new CopyFrameAsJavaOperation(editor));
        ops.add(new CustomFrameOperation(getEditor(), "addRemoveAbstract", Arrays.asList(Config.getString("frame.class.toggle.abstract")),
                AbstractOperation.MenuItemOrder.TOGGLE_ABSTRACT, this, () ->  abstractModifier.set(!abstractModifier.get())));

        if (extendsSlot.isEmpty())
        {
            ops.add(new CustomFrameOperation(getEditor(), "addExtends", Arrays.asList(Config.getString("frame.class.add.extends")),
                    AbstractOperation.MenuItemOrder.TOGGLE_EXTENDS, this, () -> showAndFocusExtends()));
        }
        else
        {
            CustomFrameOperation op = new CustomFrameOperation(getEditor(), "removeExtends",
                    Arrays.asList(Config.getString("frame.class.remove.extends.from").replace("$", extendsSlot.getText())),
                    AbstractOperation.MenuItemOrder.TOGGLE_EXTENDS, this, () -> extendsSlot.setText(""));
            op.setWideCustomItem(true);
            ops.add(op);
        }

        ops.add(new CustomFrameOperation(getEditor(), "addImplements", Arrays.asList(Config.getString("frame.class.add.implements")),
                AbstractOperation.MenuItemOrder.TOGGLE_IMPLEMENTS, this, () -> implementsSlot.addTypeSlotAtEnd("", true)));

        final List<TypeSlotFragment> types = implementsSlot.getTypes();
        for (int i = 0; i < types.size(); i++)
        {
            final int index = i;
            TypeSlotFragment type = types.get(i);
            CustomFrameOperation removeOp = new CustomFrameOperation(getEditor(), "removeImplements",
                    Arrays.asList(Config.getString("frame.class.remove.implements").replace("$", type.getContent())),
                    AbstractOperation.MenuItemOrder.TOGGLE_IMPLEMENTS, this, () -> implementsSlot.removeIndex(index));
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
        paramName.requestFocus();
        extendsSlot.requestFocus();
    }

    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursorInCanvas)
    {
        // We deliberately don't include super.getAvailableExtensions; we can't be disabled
        ExtensionDescription abstractExtension = null;
        ExtensionDescription implementsExtension = null;
        ExtensionDescription extendsExtension = null;
        if (fieldsCanvas.equals(canvas) || canvas == null) {
            abstractExtension = new ExtensionDescription(StrideDictionary.ABSTRACT_EXTENSION_CHAR, Config.getString("frame.class.toggle.abstract"),
                    () -> abstractModifier.set(!abstractModifier.get()), true, ExtensionSource.INSIDE_FIRST, ExtensionSource.MODIFIER);
            implementsExtension = new ExtensionDescription(StrideDictionary.IMPLEMENTS_EXTENSION_CHAR, "Add implements declaration",
                    () -> implementsSlot.addTypeSlotAtEnd("", true), true, ExtensionSource.INSIDE_FIRST, ExtensionSource.MODIFIER);
            if (!showingExtends.get()) {
                extendsExtension = new ExtensionDescription(StrideDictionary.EXTENDS_EXTENSION_CHAR, "Add extends declaration",
                        () -> showAndFocusExtends(), true, ExtensionSource.INSIDE_FIRST, ExtensionSource.MODIFIER);
            }
        }

        return Utility.nonNulls(Arrays.asList(abstractExtension, extendsExtension, implementsExtension));
    }
/*
    private void removeExtends()
    {
        showingExtends.set(false);
        paramClassName.requestFocus(Focus.RIGHT);
        extendsSlot.setText("");
        editor.modifiedFrame(this, false);
    }
*/
    @Override
    @OnThread(Tag.FXPlatform)
    public void saved()
    {
        if (extendsInheritedCanvases.isEmpty()) {
            updateInheritedItems();
        }
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

    @OnThread(Tag.FXPlatform)
    private void updateInheritedItems()
    {
        // Add available frames:
        withInheritedItems(new HashSet<>(Arrays.asList(CompletionKind.FIELD, CompletionKind.METHOD)), membersByClass ->
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
                        String aFormalName = ap.get(j).getFormalName();
                        String bFormalName = bp.get(j).getFormalName();
                        if ((aFormalName == null ? bFormalName != null : !aFormalName.equals(bFormalName)) ||
                            !ap.get(j).getQualifiedType().equals(bp.get(j).getQualifiedType()))
                            return false;
                    }
                }
            }
        }

        return true;
    }

    @OnThread(Tag.FXPlatform)
    public void withInheritedItems(Set<CompletionKind> kinds, FXPlatformConsumer<Map<String, List<AssistContentThreadSafe>>> handler)
    {
        // Get all available items
        editor.withAccessibleMembers(getCode().getPosInsideClass(), kinds, true, allMembers ->
        {
            // Split by class:
            HashMap<String, List<AssistContentThreadSafe>> methodsByClass = new HashMap<>();
            for (AssistContentThreadSafe a : allMembers)
            {
                if (a.getDeclaringClass().equals(paramName.getText()))
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
            // We use empty list when params are absent (e.g. for fields), even though this isn't quite right as
            // then a field will compare equal to a no-param method of the same name.  But we're only sorting here,
            // not eliminating duplicates, so we don't mind particularly if those two items are ordered arbitrarily:
            Comparator<AssistContentThreadSafe> comparator =
                Comparator.comparing(AssistContentThreadSafe::getName)
                    .thenComparing(ac -> ac.getParams() == null ?
                                    Collections.emptyList() :
                                    Utility.mapList(ac.getParams(), ParamInfo::getUnqualifiedType)
                            , Utility.listComparator());
            methodsByClass.forEach((k, v) -> v.sort(comparator));
            handler.accept(methodsByClass);
        });
    }

    @Override
    public BirdseyeManager prepareBirdsEyeView(SharedTransition animate)
    {
        final List<FrameCanvas> canvases = Arrays.asList(constructorsCanvas, methodsCanvas);

        int startingCanvas = -1; // It's incremented before use
        Frame startingFrame = null;
        // Guaranteed to pick a canvas if canDoBirdseye returned true:
        while (startingFrame == null)
        {
            startingCanvas += 1;
            startingFrame = canvases.get(startingCanvas).getBlockContents().stream()
                    .filter(f -> !(f instanceof CommentFrame)).findFirst().orElse(null);
        }

        // See if focus owner belongs in any of these frames:
        Node focusOwner = canvases.get(0).getNode().getScene().getFocusOwner();
        canvasLoop: for (int i = 0; i < canvases.size(); i++)
        {
            for (Frame f : canvases.get(i).getBlockContents())
            {
                if (!(f instanceof CommentFrame)
                    && (nodeInside(focusOwner, (Parent)f.getNode()) 
                        || f.getCursorBefore().getNode() == focusOwner
                        || (f.getCursorAfter().getFrameAfter() == null 
                            && f.getCursorAfter().getNode() == focusOwner)))
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

            @Override
            public Node getNodeForVisibility()
            {
                return getHeaderNodeOf(frame);
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
                // If candidate is null, we're at the end of a canvas.
                Frame candidate = canvases.get(canvasIndex).getFrameBefore(
                                    canvases.get(canvasIndex).getCursorBefore(frame));
                int prospective = canvasIndex;
                while (candidate == null || candidate instanceof CommentFrame)
                {
                    if (candidate == null)
                    {
                        // At beginning of one canvas, so go to previous:
                        prospective -= 1;
                        if (prospective < 0)
                        {
                            // Gone beyond beginning:
                            candidate = null;
                            break;
                        }
                        candidate = canvases.get(prospective).getFrameBefore(
                                        canvases.get(prospective).getLastCursor());
                    }
                    else
                    {
                        // Still may be opportunity to look upwards within this canvas:
                        candidate = canvases.get(prospective).getFrameBefore(
                                        canvases.get(prospective).getCursorBefore(candidate));
                    }
                }

                if (candidate != null && !(candidate instanceof CommentFrame))
                {
                    frame = candidate;
                    canvasIndex = prospective;
                }
            }

            @Override
            public void down()
            {
                // If candidate is null, we're at the end of a canvas.
                Frame candidate = canvases.get(canvasIndex).getFrameAfter(
                                    canvases.get(canvasIndex).getCursorAfter(frame));
                int prospective = canvasIndex;
                while (candidate == null || candidate instanceof CommentFrame)
                {
                    if (candidate == null)
                    {
                        // At end of one canvas, so go to next:
                        prospective += 1;
                        if (prospective >= canvases.size())
                        {
                            // Gone beyond end:
                            candidate = null;
                            break;
                        }                        
                        candidate = canvases.get(prospective).getFrameAfter(
                                        canvases.get(prospective).getFirstCursor());
                    }
                    else
                    {
                        // Still may be opportunity to look downwards within this canvas:
                        candidate = canvases.get(prospective).getFrameAfter(
                                        canvases.get(prospective).getCursorAfter(candidate));
                    }
                }

                if (candidate != null && !(candidate instanceof CommentFrame))
                {
                    frame = candidate;
                    canvasIndex = prospective;
                }
            }
        };
    }

    @Override
    public void addExtendsClassOrInterface(String className)
    {
        extendsSlot.setText(className);
    }

    @Override
    public void removeExtendsClass()
    {
        extendsSlot.setText("");
    }

    @Override
    public void addImplements(String className)
    {
        implementsSlot.addTypeSlotAtEnd(className, false);
    }

    @Override
    public void removeExtendsOrImplementsInterface(String interfaceName)
    {
        List<TypeSlotFragment> implementsTypes = implementsSlot.getTypes();
        for (int i = 0; i < implementsTypes.size(); i++)
        {
            if (implementsTypes.get(i).getContent().equals(interfaceName))
            {
                implementsSlot.removeIndex(i);
                return;
            }
        }
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
        return Stream.concat(
                constructorsCanvas.getBlockContents().stream(),
                methodsCanvas.getBlockContents().stream())
            .anyMatch(f -> !(f instanceof CommentFrame));
    }

    @Override
    public void addDefaultConstructor()
    {
        constructorsCanvas.getFirstCursor().insertBlockAfter(ConstructorFrame.getFactory().createBlock(editor));
    }

    @Override
    public List<ConstructorFrame> getConstructors()
    {
        return constructorsCanvas.getBlocksSubtype(ConstructorFrame.class);
    }


    @Override
    public List<NormalMethodFrame> getMethods()
    {
        // abstract methods?
        return methodsCanvas.getBlocksSubtype(NormalMethodFrame.class);
    }

    public FrameCanvas getConstructorsCanvas()
    {
        return constructorsCanvas;
    }

    public void findMethod(String methodName, List<ParamInfo> params, FXConsumer<NormalMethodFrame> callback)
    {
        ClassElement el = getCode();
        JavaFXUtil.runNowOrLater(() ->
        {
            Optional<NormalMethodFrame> method = el.streamMethods()
                    .filter(e ->
                    {
                        if (!(e instanceof NormalMethodElement))
                            return false;
                        NormalMethodElement m = (NormalMethodElement) e;
                        return m.equalDeclaration(methodName, params, el);
                    }).map(e -> ((NormalMethodElement) e).getFrame())
                    .findFirst();
            callback.accept(method.orElse(null));
        });
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        super.setView(oldView, newView, animateProgress);
        if (!extendsInheritedCanvases.isEmpty())
        {
            if (newView != View.NORMAL)
                inheritedLabel.expandedProperty().set(false);
        }
        inheritedLabel.setVisible(newView == View.NORMAL);
    }

    @Override
    protected List<FrameContentRow> getLabelRows()
    {
        return Arrays.asList(importRow, fieldsLabelRow, constructorsLabelRow, methodsLabelRow);
    }

    @Override
    protected List<SlotLabel> getCanvasLabels()
    {
        return Arrays.asList(importsLabel, fieldsLabel, constructorsLabel, methodsLabel);
    }

    @OnThread(Tag.FXPlatform)
    public void compiled()
    {
        // The runAfterCurrent is added to solve BLUEJ-1040
        JavaFXUtil.runAfterCurrent(() -> updateInheritedItems());
    }

    @Override
    public Stream<FrameCanvas> getPersistentCanvases()
    {
        List<FrameCanvas> extendsFrameCanvases = extendsInheritedCanvases.stream()
                .map(inheritedCanvas -> inheritedCanvas.canvas).collect(Collectors.toList());
        return getCanvases().filter(canvas -> !extendsFrameCanvases.contains(canvas));
    }

    @Override
    public FrameTypeCheck check(FrameCanvas canvas)
    {
        if (canvas == fieldsCanvas)
            return StrideDictionary.checkClassField();
        else if (canvas == methodsCanvas)
            return StrideDictionary.checkClassMethod();
        else if (canvas == constructorsCanvas)
            return StrideDictionary.checkConstructor();
        else
            throw new IllegalStateException("Asking about canvas unknown to ClassFrame");
    }

    @Override
    public CanvasKind getChildKind(FrameCanvas c)
    {
        // Note: importCanvas doesn't reach here as it has its own parent
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
    public void restore(ClassElement target)
    {
        paramName.setText(target.getName());
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
                // Show extends too, if we enter the header like this:
                focusHasBeenInNameOrExtends.set(true);
                implementsSlot.ensureAtLeastOneSlot();
                Utility.findLast(implementsSlot.getTypeSlots()).get().requestFocus(Focus.RIGHT);
                return true;
            }
        };
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public boolean backspaceAtStart(FrameContentItem srcRow, HeaderItem src)
    {
        if (src == extendsSlot)
        {
            extendsSlot.setText("");
            paramName.requestFocus(Focus.RIGHT);
            return false;
        }
        return super.backspaceAtStart(srcRow, src);
    }
}
