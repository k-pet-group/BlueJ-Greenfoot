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
package bluej.stride.framedjava.frames;

import bluej.Config;
import bluej.debugger.gentype.Reflective;
import bluej.editor.fixes.SuggestionList;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContent.ParamInfo;
import bluej.stride.framedjava.ast.ASTUtility;
import bluej.stride.framedjava.ast.AccessPermission;
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
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.parser.AssistContentThreadSafe;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.operations.CustomFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.operations.ToggleBooleanProperty;
import bluej.stride.slots.*;
import bluej.editor.fixes.SuggestionList.SuggestionDetailsWithHTMLDoc;
import bluej.editor.fixes.SuggestionList.SuggestionListListener;
import bluej.utility.Utility;
import bluej.utility.javafx.*;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextField;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NormalMethodFrame extends MethodFrameWithBody<NormalMethodElement> //implements CodeFrame<NormalMethodElement>
{
    //TODO Refactor and make them private
    public static final String STATIC_NAME = "static";
    public static final String TOGGLE_STATIC_METHOD = "toggleStaticMethod";
    public static final String FINAL_NAME = "final";
    public static final String TOGGLE_FINAL_METHOD = "toggleFinalMethod";

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
    @OnThread(Tag.FXPlatform)
    private String curOverrideSource = null;
    private final TypeSlot returnType;
    private final TextSlot<NameDefSlotFragment> methodName;
    private NormalMethodElement element;
    
    private NormalMethodFrame(InteractionManager editor)
    {
        super(editor);
        setDocumentationPromptText(Config.getString("frame.class.method.doc.prompt"));

        methodName = new MethodNameDefTextSlot(editor, this, getHeaderRow(), new MethodOverrideCompletionCalculator(), "method-name-");
        methodName.addValueListener(new SlotTraversalChars(
                () -> getHeaderRow().focusRight(methodName),
                SlotTraversalChars.METHOD_NAME.getChars()) {
            @Override
            public void deletePressedAtEnd(HeaderItem slot) {
                paramsPane.deleteFirstParam();
            }
        });
        
        returnType = new TypeSlot(editor, this, this, getHeaderRow(), TypeSlot.Role.RETURN, "method-return-type-");
        returnType.addClosingChar(' ');
        returnType.markReturnType();
        
        paramsPane = new FormalParameters(editor, this, this, getHeaderRow(), "method-param-");
        
        returnType.setSimplePromptText("type");
        methodName.setPromptText("name");

        staticLabel = new SlotLabel("static ");
        finalLabel = new SlotLabel("final ");

        modifiers.put(STATIC_NAME, staticModifier);
        modifiers.put(FINAL_NAME, finalModifier);

        overrideLabel.addStyleClass("method-override-label");
        overrideLabel.setAlignment(HangingFlowPane.FlowAlignment.RIGHT);
        
        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<? extends HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(access),
                JavaFXUtil.listBool(staticModifier, staticLabel),
                JavaFXUtil.listBool(finalModifier, finalLabel),
                FXCollections.observableArrayList(returnType),
                FXCollections.observableArrayList(methodName),
                paramsPane.getSlots(),
                throwsPane.getHeaderItems(),
                FXCollections.observableArrayList(overrideLabel)
        ));

        JavaFXUtil.addChangeListener(staticModifier, b -> editor.modifiedFrame(this, false));
        JavaFXUtil.addChangeListener(finalModifier, b -> editor.modifiedFrame(this, false));
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
        @OnThread(Tag.FXPlatform)
        public void withCalculatedSuggestionList(PosInSourceDoc pos, CodeElement codeEl,
                                                 SuggestionListListener listener, FXPlatformConsumer<SuggestionList> handler) {
            
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
    @OnThread(Tag.FXPlatform)
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> operations = new ArrayList<>(super.getContextOperations());
        
        InteractionManager editor = getEditor();
        
        operations.add(new CustomFrameOperation(editor, "method->constructor",
                Arrays.asList(Config.getString("frame.operation.change"), Config.getString("frame.operation.change.to.constructor")),
                AbstractOperation.MenuItemOrder.TRANSFORM, this,
                () -> {
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
        
        operations.add(new CustomFrameOperation(editor, "concrete->abstract",
                Arrays.asList(Config.getString("frame.operation.change"), Config.getString("frame.operation.change.to.abstract")),
                AbstractOperation.MenuItemOrder.TRANSFORM, this,
                () -> {
                    FrameCursor c = getCursorBefore();
                    MethodProtoElement el = new MethodProtoElement(null, returnType.getSlotElement(), methodName.getSlotElement(),
                        generateParams(), throwsPane.getTypes(), new JavadocUnit(getDocumentation()), frameEnabledProperty.get());
                    c.insertBlockAfter(el.createFrame(getEditor()));
                    c.getParentCanvas().removeBlock(this);
                }
        ));

        operations.addAll(getStaticFinalOperations());

        return operations;
    }

    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas innerCanvas, FrameCursor cursorInCanvas)
    {
        final List<ExtensionDescription> extensions = new ArrayList<>(super.getAvailableExtensions(innerCanvas, cursorInCanvas));
        getStaticFinalOperations().stream().forEach(op -> extensions.add(new ExtensionDescription(op, this, true,
                ExtensionSource.BEFORE, ExtensionSource.AFTER, ExtensionSource.MODIFIER, ExtensionSource.SELECTION)));
        return extensions;
    }

    private List<ToggleBooleanProperty> getStaticFinalOperations()
    {
        List<ToggleBooleanProperty> operations = new ArrayList<>();
        operations.add(new ToggleBooleanProperty(getEditor(), TOGGLE_FINAL_METHOD, FINAL_NAME, 'n'));
        operations.add(new ToggleBooleanProperty(getEditor(), TOGGLE_STATIC_METHOD, STATIC_NAME, 's'));
        return operations;
    }

    // Used by ReturnFrame
    public StringExpression returnTypeProperty()
    {
        return returnType.javaProperty();
    }

    @OnThread(Tag.FXPlatform)
    public void updateOverrideDisplay(ClassElement topLevel)
    {
        if (element == null)
            return;

        final NormalMethodElement cachedElement = element;
        
        // Now need to look through super-types for method with our name and right signature:

        List<String> qualParamTypes = cachedElement.getQualifiedParamTypes(topLevel);
        Reflective overriddenFrom = topLevel.findSuperMethod(cachedElement.getName(), qualParamTypes);
        if (overriddenFrom != null)
        {
            String name = overriddenFrom.getSimpleName();
            if (name.indexOf('.') != -1)
                name = name.substring(name.lastIndexOf('.') + 1);
            final String nameFinal = name;
            if (curOverrideSource == null || !curOverrideSource.equals(nameFinal))
            {
                curOverrideSource = nameFinal;
                overrideLabel.setText(Config.getString("frame.class.overrides.from").replace("$", nameFinal));
            }
        }
        else
        {
            if (curOverrideSource != null)
            {
                curOverrideSource = null;
                overrideLabel.setText("");
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
        return new MethodHeaderRow(this, stylePrefix)
        {
            @Override
            protected EditableSlot getSlotAfterParams()
            {
                return throwsPane.getTypeSlots().findFirst().orElse(null);
            }

            @Override
            protected EditableSlot getSlotBeforeParams()
            {
                return methodName;
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
}
