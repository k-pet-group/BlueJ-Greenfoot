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
import java.util.List;
import java.util.stream.Collectors;

import bluej.stride.slots.EditableSlot.MenuItemOrder;
import javafx.application.Platform;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

import bluej.utility.javafx.HangingFlowPane;
import threadchecker.OnThread;
import threadchecker.Tag;

import bluej.debugger.gentype.Reflective;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContent.ParamInfo;
import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.ASTUtility;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.elements.ClassElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.ConstructorElement;
import bluej.stride.framedjava.elements.MethodProtoElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.slots.ExpressionCompletionCalculator;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.operations.CustomFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.CompletionCalculator;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.Focus;
import bluej.stride.slots.FormalParameters;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.MethodNameDefTextSlot;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.SuggestionList;
import bluej.stride.slots.SuggestionList.SuggestionDetailsWithHTMLDoc;
import bluej.stride.slots.SuggestionList.SuggestionListListener;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.stride.slots.TypeTextSlot;
import bluej.stride.slots.WrappableSlotLabel;
import bluej.utility.javafx.SharedTransition;
import bluej.utility.Utility;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;

public class NormalMethodFrame extends MethodFrameWithBody<NormalMethodElement> //implements CodeFrame<NormalMethodElement>
{
    private final SlotLabel staticLabel;
    private BooleanProperty staticModifier = new SimpleBooleanProperty(false);
    private final SlotLabel finalLabel;
    private BooleanProperty finalModifier = new SimpleBooleanProperty(false);
    private final WrappableSlotLabel overrideLabel = new WrappableSlotLabel("") {
        @Override
        public void setView(View oldView, View newView, SharedTransition animate)
        {
            if (oldView == View.NORMAL && newView == View.JAVA_PREVIEW)
            {
                fadeOut(animate, true);
            }
            else
            {
                fadeIn(animate);
            }
        }
    };
    private String curOverrideSource = null;
    private final TypeTextSlot returnType;
    private final TextSlot<NameDefSlotFragment> methodName;
    private NormalMethodElement element;
    
    private NormalMethodFrame(InteractionManager editor)
    {
        super(editor);
        setDocumentationPromptText("Describe your method here...");

        methodName = new MethodNameDefTextSlot(editor, this, getHeaderRow(), new MethodOverrideCompletionCalculator(), "method-name-");
        methodName.addValueListener(new SlotTraversalChars(
                () -> getHeaderRow().focusRight(methodName),
                SlotTraversalChars.METHOD_NAME.getChars()) {
            @Override
            public void deletePressedAtEnd(HeaderItem slot) {
                paramsPane.deleteFirstParam();
            }
        });
        
        returnType = new TypeTextSlot(editor, this, getHeaderRow(), new TypeCompletionCalculator(editor), "method-return-type-");
        returnType.addValueListener(SlotTraversalChars.IDENTIFIER);
        returnType.markReturnType();
        
        paramsPane = new FormalParameters(editor, this, this, getHeaderRow(), "method-param-");
        
        returnType.setPromptText("type");
        methodName.setPromptText("name");

        staticLabel = new SlotLabel("static ");
        finalLabel = new SlotLabel("final ");

        overrideLabel.addStyleClass("method-override-label");
        overrideLabel.setAlignment(HangingFlowPane.FlowAlignment.RIGHT);
        
        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(access),
                JavaFXUtil.listBool(staticModifier, staticLabel),
                JavaFXUtil.listBool(finalModifier, finalLabel),
                FXCollections.observableArrayList(returnType),
                FXCollections.observableArrayList(methodName),
                paramsPane.getSlots(),
                throwsPane.getHeaderItems(),
                FXCollections.observableArrayList(overrideLabel)
        ));

        JavaFXUtil.addChangeListener(staticModifier, b -> editor.modifiedFrame(this));
        JavaFXUtil.addChangeListener(finalModifier, b -> editor.modifiedFrame(this));
    }
    
    public NormalMethodFrame(InteractionManager editor, AccessPermissionFragment access, boolean staticModifier,
            boolean finalModifier, String returnType, String name, String documentation, boolean enabled)
    {
        this(editor);
        this.access.setValue(access.getValue());
        access.registerSlot(this.access);
        this.staticModifier.set(staticModifier);
        this.finalModifier.set(finalModifier);
        setDocumentation(documentation);
        this.returnType.setText(returnType);
        methodName.setText(name);
        frameEnabledProperty.set(enabled);
    }

    @Override
    public boolean focusWhenJustAdded()
    {
        returnType.requestFocus();
        return true;
    }
    
    public static FrameFactory<NormalMethodFrame> getFactory()
    {
        return new FrameFactory<NormalMethodFrame>() {
            @Override
            public NormalMethodFrame createBlock(InteractionManager editor)
            {
                return new NormalMethodFrame(editor);
            }
                        
            @Override 
            public Class<NormalMethodFrame> getBlockClass()
            { 
                return NormalMethodFrame.class;
            }
        };
    }

    @Override
    public void regenerateCode()
    {
        List<ParamFragment> params = generateParams();
        
        element = new NormalMethodElement(this, new AccessPermissionFragment(this, access), staticModifier.get(),
                finalModifier.get(), returnType.getSlotElement(), methodName.getSlotElement(), 
                params, throwsPane.getTypes(), getContents(), new JavadocUnit(getDocumentation()), frameEnabledProperty.get());
    }

    @Override
    public NormalMethodElement getCode()
    {
        return element;
    }

    public String getName()
    {
        return methodName.getText();
    }
    
    private class MethodOverrideCompletionCalculator implements CompletionCalculator
    {
        private List<AssistContentThreadSafe> inheritedMethods;
        private SuggestionList suggestionDisplay;

        @Override
        public void withCalculatedSuggestionList(PosInSourceDoc pos, CodeElement codeEl,
                SuggestionListListener listener, String targetType, FXConsumer<SuggestionList> handler) {
            
            ClassFrame classFrame = (ClassFrame)ASTUtility.getTopLevelElement(codeEl).getFrame();
            
            classFrame.withInheritedItems(Collections.singleton(CompletionKind.METHOD), inheritedMethodsByDeclarer ->
            {
                
                inheritedMethods = inheritedMethodsByDeclarer.values().stream().flatMap(List::stream).collect(Collectors.toList());
                
                // TODO rule out final methods
                // TODO rule out any methods already implemented in this class
                
                suggestionDisplay = new SuggestionList(getEditor(),
                        Utility.mapList(inheritedMethods, ac -> new SuggestionDetailsWithHTMLDoc(
                                ac.getName(), 
                                ExpressionCompletionCalculator.getParamsCompletionDisplay(ac),
                                ac.getType(),
                                SuggestionList.SuggestionShown.COMMON,
                                ac.getDocHTML())),
                        null, SuggestionList.SuggestionShown.RARE, null, listener);
            
                handler.accept(suggestionDisplay);
            });
        }

        @Override
        public boolean execute(TextField field, int selected, int startOfCurWord)
        {
            if (selected == -1) {
                return false;
            }
            
            AssistContentThreadSafe a = inheritedMethods.get(selected);
            methodName.setText(a.getName());
            returnType.setText(a.getType());
            
            // TODO check that we store access in AssistContent
            // access.setValue(AccessPermission.PUBLIC);
            access.setValue(AccessPermission.fromAccess(a.getAccessPermission()));
            
            paramsPane.setParams(a.getParams(), ParamInfo::getUnqualifiedType, ParamInfo::getFormalName);
            
            return true;
        }
    }
    
    @Override
    public List<FrameOperation> getContextOperations(InteractionManager editor)
    {
        List<FrameOperation> r = new ArrayList<>(super.getContextOperations(editor));
        
        r.add(new CustomFrameOperation(editor, "method->constructor",
                Arrays.asList("Change", "to constructor"), MenuItemOrder.TRANSFORM, this, () -> {
                    
                    // TODO AA enhance the code
                    Frame parent = getParentCanvas().getParent().getFrame();
                    if (parent instanceof ClassFrame) {
                        FrameCanvas p = ((ClassFrame)parent).getConstructorsCanvas();
                        FrameCursor c = p.getLastCursor();
                        ConstructorElement el = new ConstructorElement(null, new AccessPermissionFragment(this, access), generateParams(),
                                throwsPane.getTypes(), null, null, getContents(), new JavadocUnit(getDocumentation()), frameEnabledProperty.get());
                        c.insertBlockAfter(el.createFrame(getEditor()));
                        getParentCanvas().removeBlock(this);
                    }
                }
        ));
        
        r.add(new CustomFrameOperation(editor, "concrete->abstract",
                Arrays.asList("Change", "to abstract"), MenuItemOrder.TRANSFORM, this, () -> {
            FrameCursor c = getCursorBefore();
            
            MethodProtoElement el = new MethodProtoElement(null, returnType.getSlotElement(), methodName.getSlotElement(),
                    generateParams(), throwsPane.getTypes(), new JavadocUnit(getDocumentation()), frameEnabledProperty.get());
            c.insertBlockAfter(el.createFrame(getEditor()));
            c.getParentCanvas().removeBlock(this);
        }));
        
        r.add(new CustomFrameOperation(editor, "addRemoveStatic", Arrays.asList("Toggle static"), MenuItemOrder.TOGGLE_STATIC, this, () ->  staticModifier.set(!staticModifier.get())));
        r.add(new CustomFrameOperation(editor, "addRemoveFinal", Arrays.asList("Toggle final"), MenuItemOrder.TOGGLE_FINAL, this, () ->  finalModifier.set(!finalModifier.get())));
        
        return r;
    }

    protected List<ExtensionDescription> getAvailablePrefixes()
    {
        return Utility.concat(super.getAvailablePrefixes(), Arrays.asList(
                new ExtensionDescription('n', "Add/Remove final", () ->
                    new ToggleFinalMethod(getEditor()).activate(this), false, false),
                new ExtensionDescription('s', "Add/Remove static", () ->
                    new ToggleStaticMethod(getEditor()).activate(this), false, false)));
    }
    
    // Used by ReturnFrame
    public StringExpression returnTypeProperty()
    {
        return returnType.textProperty();
    }

    @OnThread(Tag.Swing)
    public void updateOverrideDisplay(ClassElement topLevel)
    {
        if (element == null)
            return;

        List<String> qualParamTypes = element.getQualifiedParamTypes(topLevel);
        // Now need to look through super-types for method with our name and right signature:
        Reflective overriddenFrom = topLevel.findSuperMethod(element.getName(), qualParamTypes);
        if (overriddenFrom != null)
        {
            String name = overriddenFrom.getSimpleName();
            if (name.indexOf('.') != -1)
                name = name.substring(name.lastIndexOf('.') + 1);
            final String nameFinal = name;
            if (curOverrideSource == null || !curOverrideSource.equals(nameFinal))
            {
                curOverrideSource = nameFinal;
                Platform.runLater(() -> overrideLabel.setText("overrides method in " + nameFinal));
            }
        }
        else
        {
            if (curOverrideSource != null)
            {
                curOverrideSource = null;
                Platform.runLater(() -> overrideLabel.setText(""));
            }
        }
    }

    @Override
    public EditableSlot getErrorShowRedirect()
    {
        return methodName;
    }

    @Override
    public void focusName()
    {
        methodName.requestFocus(Focus.LEFT);
    }

    @Override
    protected FrameContentRow makeHeader(String stylePrefix)
    {
        return new FrameContentRow(this, stylePrefix)
        {
            @Override
            public void focusRight(HeaderItem src)
            {
                if (src == methodName)
                {
                    paramsPane.ensureAtLeastOneParameter();
                }
                super.focusRight(src);
            }

            @Override
            public boolean focusRightEndFromNext()
            {
                paramsPane.ensureAtLeastOneParameter();
                return super.focusRightEndFromNext();
            }

            @Override
            public void escape(HeaderItem src)
            {
                if (paramsPane.findFormal(src) != null){
                    paramsPane.escape(src);
                }
                else {
                    super.escape(src);
                }
            }
        };
    }

    @Override
    public boolean tryRestoreTo(CodeElement codeElement)
    {
        // instanceof bit hacky, but easiest way to do it:
        if (codeElement instanceof NormalMethodElement)
        {
            NormalMethodElement nme = (NormalMethodElement)codeElement;
            staticModifier.set(nme.isStatic());
            finalModifier.set(nme.isFinal());
            returnType.setText(nme.getType());
            methodName.setText(nme.getName());
            restoreDetails(nme);
            return true;
        }
        return false;
    }

    public static class ToggleFinalMethod extends FrameOperation
    {
        private SimpleStringProperty name = new SimpleStringProperty("Toggle final");

        public ToggleFinalMethod(InteractionManager editor)
        {
            super(editor, "toggleFinalMethod", Combine.ALL);
        }

        @Override
        protected void execute(List<Frame> frames)
        {
            frames.forEach(f -> ((NormalMethodFrame)f).finalModifier.set(!targetedAllFinal(frames)));
        }

        @Override
        public List<ItemLabel> getLabels()
        {
            return Arrays.asList(new ItemLabel(name, MenuItemOrder.TOGGLE_FINAL));
        }

        @Override
        public void onMenuShowing(CustomMenuItem item)
        {
            super.onMenuShowing(item);
            updateName();
        }

        private void updateName()
        {
            name.set(targetedAllFinal(editor.getSelection().getSelected()) ? "Remove final" : "Make final");
        }

        private boolean targetedAllFinal(List<Frame> frames)
        {
            return frames.stream().allMatch(f -> ((NormalMethodFrame)f).finalModifier.get());
        }

        @Override
        public boolean onlyOnContextMenu()
        {
            return true;
        }
    }

    public static class ToggleStaticMethod extends FrameOperation
    {
        private SimpleStringProperty name = new SimpleStringProperty("Toggle static");

        public ToggleStaticMethod(InteractionManager editor)
        {
            super(editor, "toggleStaticMethod", Combine.ALL, new KeyCodeCombination(KeyCode.S));
        }

        @Override
        protected void execute(List<Frame> frames)
        {
            frames.forEach(f -> ((NormalMethodFrame) f).staticModifier.set(!targetedAllStatic(frames)));
        }

        @Override
        public List<ItemLabel> getLabels()
        {
            return Arrays.asList(new ItemLabel(name, MenuItemOrder.TOGGLE_STATIC));
        }

        @Override
        public void onMenuShowing(CustomMenuItem item)
        {
            super.onMenuShowing(item);
            updateName();
        }

        private void updateName()
        {
            name.set(targetedAllStatic(editor.getSelection().getSelected()) ? "Remove static" : "Make static");
        }

        private boolean targetedAllStatic(List<Frame> frames)
        {
            return frames.stream().allMatch(f -> ((NormalMethodFrame) f).staticModifier.get());
        }

        @Override
        public boolean onlyOnContextMenu()
        {
            return true;
        }
    }
}
