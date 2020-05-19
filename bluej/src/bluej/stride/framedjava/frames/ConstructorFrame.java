/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2020 Michael Kölling and John Rosenberg 
 
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
import java.util.Optional;

import bluej.utility.javafx.AbstractOperation;
import javafx.beans.binding.DoubleExpression;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;

import bluej.Config;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.SuperThis;
import bluej.stride.framedjava.ast.SuperThisParamsExpressionFragment;
import bluej.stride.framedjava.ast.SuperThisFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.ConstructorElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.framedjava.slots.SuperThisParamsExpressionSlot;
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
import bluej.stride.slots.ChoiceSlot;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.FormalParameters;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.utility.Utility;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

public class ConstructorFrame extends MethodFrameWithBody<ConstructorElement> {

    private final SlotLabel headerLabel;
    private ConstructorElement element;

    private FrameContentRow callRow; // May be null
    private ChoiceSlot<SuperThis> superThis; // May be null
    private SuperThisParamsExpressionSlot superThisParams; // May be null

    private ConstructorFrame(InteractionManager editor) {
        super(editor);
        setDocumentationPromptText(Config.getString("frame.class.constructor.doc.prompt"));
        headerLabel = new SlotLabel("<constructor>");
        JavaFXUtil.addStyleClass(headerLabel, "constructor-name-caption");

        paramsPane = new FormalParameters(editor, this, this, getHeaderRow(), "constructor-param-");

        headerLabel.textProperty().bind(editor.nameProperty());

        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<? extends HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(access),
                FXCollections.observableArrayList(headerLabel),
                paramsPane.getSlots(),
                throwsPane.getHeaderItems()
        ));
    }

    public ConstructorFrame(InteractionManager editor, AccessPermissionFragment access, String documentation,
            SuperThisFragment delegate, ExpressionSlotFragment delegateParams, boolean enabled) {
        this(editor);
        this.access.setValue(access.getValue());
        access.registerSlot(this.access);
        setDocumentation(documentation);
        if (delegate != null || delegateParams != null) {
            addSuperThis(delegate, delegateParams);
        }
        frameEnabledProperty.set(enabled);
    }

    public static FrameFactory<ConstructorFrame> getFactory() {
        return new FrameFactory<ConstructorFrame>() {
            @Override
            public ConstructorFrame createBlock(InteractionManager editor) {
                return new ConstructorFrame(editor);
            }

            @Override
            public Class<ConstructorFrame> getBlockClass() {
                return ConstructorFrame.class;
            }
        };
    }

    @Override
    public void regenerateCode() {
        List<ParamFragment> params = generateParams();

        element = new ConstructorElement(this, new AccessPermissionFragment(this, access), params, throwsPane.getTypes(),
                Utility.orNull(superThis, s -> new SuperThisFragment(this, s, superThisParams)),
                superThisParams == null ? null : superThisParams.getSlotElement(), getContents(),
                new JavadocUnit(getDocumentation()), frameEnabledProperty.get());
    }

    @Override
    public ConstructorElement getCode() {
        return element;
    }

    private void addSuperThis(SuperThisFragment st, ExpressionSlotFragment params) {
        if (superThis != null) {
            superThis.setValue(st.getValue());
        } else {
            callRow = new FrameContentRow(this, "constructor-call-");
            callRow.setMargin(new Insets(0, 6, 0, 0));
            superThis = new ChoiceSlot<>(getEditor(), this, callRow, SuperThis.all(), SuperThis::isValid, "constructor-", Collections.emptyMap());
            superThis.setValue(st.getValue());

            superThisParams = new SuperThisParamsExpressionSlot(getEditor(), this, this, callRow, superThis, "constructor-param-") {

                @Override
                @OnThread(Tag.FXPlatform)
                public boolean backspaceAtStart() {
                    if (isAlmostBlank()) {
                        FrameCursor fc = getCanvas().getFirstCursor();
                        getEditor().beginRecordingState(fc);
                        removeSuperThis();
                        fc.requestFocus();
                        getEditor().endRecordingState(fc);
                        return true;
                    }
                    return super.backspaceAtStart();

                }

            };

            callRow.setHeaderItems(Arrays.asList(
                    superThis,
                    ExpressionSlot.makeBracketSlot("(", true, null),
                    superThisParams,
                    ExpressionSlot.makeBracketSlot(")", false, null)
            ));

            contents.setAll(documentationPane, getHeaderRow(), callRow, getCanvas());
            if (params != null) {
                superThisParams.setText(params);
            }
            superThisParams.requestFocus();
            getEditor().modifiedFrame(this, false);
            JavaFXUtil.setPseudoclass("bj-super-this", true, getNode());
        }
        st.registerSlot(superThis);
    }

    private void removeSuperThis() {
        if (callRow != null) {
            /* TODO shrink and remove:
             Timeline t = new Timeline(new KeyFrame(Duration.millis(0), new KeyValue(n.maxHeightProperty(), n.getHeight())),
             new KeyFrame(Duration.millis(200), new KeyValue(n.maxHeightProperty(), 0.0)));
             t.setOnFinished(e -> headBox.getChildren().remove(n));
             t.play();
             */
            callRow = null;
            superThis = null;
            superThisParams = null;
            contents.setAll(documentationPane, getHeaderRow(), getCanvas());
            getCanvas().getFirstCursor().requestFocus();
            getEditor().modifiedFrame(this, false);
            JavaFXUtil.setPseudoclass("bj-super-this", false, getNode());
        }
    }

    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursorInCanvas) {
        if (callRow == null) {
            List<ExtensionDescription> extensions = new ArrayList(super.getAvailableExtensions(canvas, cursorInCanvas));

            extensions.addAll(Arrays.asList(new ExtensionDescription(StrideDictionary.SUPER_EXTENSION_CHAR,
                    Config.getString("frame.class.add.super"), () -> addSuperThis(new SuperThisFragment(SuperThis.SUPER), null),
                    true, ExtensionSource.INSIDE_FIRST, ExtensionSource.MODIFIER), new ExtensionDescription(StrideDictionary.THIS_EXTENSION_CHAR,
                    Config.getString("frame.class.add.this"), () -> addSuperThis(new SuperThisFragment(SuperThis.THIS), null),
                    true, ExtensionSource.INSIDE_FIRST, ExtensionSource.MODIFIER)));

            return extensions;
        }
        else {
            return Arrays.asList(new ExtensionDescription('\b', Config.getString("frame.class.remove.super"),
                    () -> removeSuperThis(), true, ExtensionSource.INSIDE_FIRST));
        }
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> r = new ArrayList<>(super.getContextOperations());

        r.add(new CustomFrameOperation(getEditor(), "constructor->method",
                Arrays.asList(Config.getString("frame.operation.change"), Config.getString("frame.operation.change.to.method")),
                AbstractOperation.MenuItemOrder.TRANSFORM, this, () -> {
                    // TODO AA enhance the code
                    Frame parent = getParentCanvas().getParent().getFrame();
                    if (parent instanceof ClassFrame) {
                        FrameCanvas p = ((ClassFrame) parent).getMethodsCanvas();
                        FrameCursor c = p.getLastCursor();
                        NormalMethodElement el = new NormalMethodElement(null, new AccessPermissionFragment(this, access),
                                false, false, new TypeSlotFragment("", ""), new NameDefSlotFragment(""), generateParams(), throwsPane.getTypes(),
                                getContents(), new JavadocUnit(getDocumentation()), frameEnabledProperty.get());
                        c.insertBlockAfter(el.createFrame(getEditor()));
                        getParentCanvas().removeBlock(this);
                    }
                }));

        return r;
    }

    @Override
    public EditableSlot getErrorShowRedirect() {
        return null; // We don't want to use access permission (awkward) or parameters (which may disappear)
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
                return access;
            }
        };
    }

    @Override
    public boolean tryRestoreTo(CodeElement codeElement)
    {
        if (codeElement instanceof ConstructorElement)
        {
            ConstructorElement ce = (ConstructorElement)codeElement;
            if (this.element.hasDelegate() && !ce.hasDelegate()) {
                removeSuperThis();
            }
            if (!this.element.hasDelegate() && ce.hasDelegate())
            {
                addSuperThis(new SuperThisFragment(ce.getDelegate()), new SuperThisParamsExpressionFragment(ce.getDelegateParams(), ce.getDelegateParamsJava()));
            }
            if (this.element.hasDelegate() && ce.hasDelegate())
            {
                restoreDelegate(ce);
            }
            restoreDetails(ce);
            return true;
        }
        return false;
    }

    private void restoreDelegate(ConstructorElement ce)
    {
        if (!superThis.getValue(null).equals(ce.getDelegate())) {
            superThis.setValue(ce.getDelegate());
        }
        if (!superThisParams.getText().equals(ce.getDelegateParams())) {
            superThisParams.setText(ce.getDelegateParams());
        }
    }

    @Override
    public boolean focusWhenJustAdded()
    {
        getCanvas().getFirstCursor().requestFocus();
        return true;
    }

    @Override
    protected DoubleExpression tweakOpeningCurlyY()
    {
        if (callRow == null)
        {
            return super.tweakOpeningCurlyY();
        }
        else
        {
            return callRow.flowPaneHeight().negate();
        }
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        super.setView(oldView, newView, animate);

        if (callRow != null)
        {
            if (newView == View.JAVA_PREVIEW)
            {
                double maxAmount = getCanvas().getCurlyBracketHeight();
                JavaFXUtil.addChangeListener(animate.getProgress(), t -> {
                    callRow.getNode().setTranslateY(t.doubleValue() * maxAmount);
                });

                callRow.applyCss();
                //double left = callRow.getBorder().getInsets().getLeft();
                //callRow.setMargin(new Insets(0, 7, 0, -(left - Math.floor(left))));

                FXRunnable setPad = () -> getCanvas().setTopOutsideBorderBackgroundPadding(Optional.of(-2 + callRow.getSceneBounds().getHeight()));
                setPad.run();
                animate.addOnStopped(setPad);
            }
            else if (oldView == View.JAVA_PREVIEW)
            {
                getCanvas().setTopOutsideBorderBackgroundPadding(Optional.empty());
                double orig = callRow.getNode().getTranslateY();
                JavaFXUtil.addChangeListener(animate.getOppositeProgress(), t -> {
                    callRow.getNode().setTranslateY(t.doubleValue() * orig);
                }); 
            }


            if (newView.isBirdseye() || oldView.isBirdseye())
            {
                animate.getProgress().addListener((prop, oldVal, newVal) -> {
                    // When you cross the half way point:
                    if (oldVal.doubleValue() < 0.5 && newVal.doubleValue() >= 0.5)
                    {
                        callRow.setVisible(!newView.isBirdseye());
                    }
                });
            }
        }
    }
}
