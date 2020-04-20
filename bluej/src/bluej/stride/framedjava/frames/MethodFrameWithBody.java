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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bluej.stride.framedjava.frames;


import bluej.Config;
import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.ThrowsTypeFragment;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.MethodWithBodyElement;
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.DocumentedSingleCanvasFrame;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.AccessPermissionSlot;
import bluej.stride.slots.ChoiceSlot;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.FormalParameters;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.Throws;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Container-block representing a method.
 * @author Fraser McKay
 */
public abstract class MethodFrameWithBody<T extends MethodWithBodyElement>
  extends DocumentedSingleCanvasFrame
  implements DebuggableFrame, CodeFrame<T>
{
    protected final ChoiceSlot<AccessPermission> access;
    protected final Throws throwsPane;
    private final Rectangle dropShadowDummy;
    protected FormalParameters paramsPane;
    private boolean showingBirdseye;
    private FXRunnable headerCleanup;

    /**
     * Default constructor.
     */
    public MethodFrameWithBody(final InteractionManager editor)
    {
        super(editor, "", "method-");

        //Parameters
        access = new AccessPermissionSlot(editor, this, getHeaderRow(), "method-");
        access.setValue(AccessPermission.PUBLIC);
        
        throwsPane = new Throws(this, () -> {
            TypeSlot s = new TypeSlot(editor, this, this, getHeaderRow(), TypeSlot.Role.THROWS_CATCH, "method-");
            s.setSimplePromptText("thrown type");
            return s;
        }, () -> getCanvas().getFirstCursor().requestFocus(), editor);
        
        dropShadowDummy = new Rectangle(0, 0, 0, 0);
        //dropShadowDummy.setManaged(false);
        //addUnmanagedToBlockContainer(dropShadowDummy);
        
        final Region headerRow = getHeaderRow().getNode();
        headerRow.getStyleClass().add("method-header");
        //dropShadowDummy.xProperty().bind(headerRow.layoutXProperty());
        //dropShadowDummy.yProperty().bind(headerRow.layoutYProperty().add(getHeadVBox().translateYProperty()));
        dropShadowDummy.widthProperty().bind(headerRow.widthProperty());
        dropShadowDummy.heightProperty().bind(headerRow.heightProperty());
       
        Rectangle small = new Rectangle();
        // Old style; bind size of shadow rectangle to width of *method frame*:
        //small.xProperty().bind(dropShadowDummy.xProperty());
        //small.widthProperty().bind(dropShadowDummy.widthProperty().add(1.0) /* fudge factor */);
        // New style; bind size of shadow rectangle to width of body canvas:

        small.widthProperty().bind(canvas.widthProperty().subtract(canvas.leftMargin()).subtract(canvas.rightMargin()));
        
        small.yProperty().bind(dropShadowDummy.heightProperty().add(dropShadowDummy.yProperty()));
        small.heightProperty().set(15.0);
        
        dropShadowDummy.clipProperty().set(small);

        if (editor != null && editor.getWindowOverlayPane() != null) {
            // We need the offset to convey two pieces of information:
            //   - whether to display the image overlay
            //   - at which offset to show it
            //
            // We use the convention that a positive offset (> 0) means don't display,
            // and 0 through negative numbers is the offset at which to display            

            final DoubleBinding offset = new DoubleBinding() {

                @Override
                protected double computeValue()
                {
                    if (!getHeaderItems().findFirst().isPresent())
                        return 0; // Still initialising

                    final double headerTopSceneY = getHeaderRow().getNode().localToScene(0, 0).getY();

                    final double headerHeight = getHeaderRow().getNode().getHeight();
                    
                    // This is the coordinates, relative to the overlay (i.e. effectively, the window) of the top
                    // of the header row
                    final double overlayY = editor.getWindowOverlayPane().sceneYToWindowOverlayY(headerTopSceneY);

                    // Smallest gap between bottom of header and bottom of frame.
                    final double marginBeneathHeader = 20.0;

                    // This is the furthest amount by which we would want to offset, before it should scroll out of view:
                    final double maxOffset = getRegion().localToScene(getRegion().getBoundsInLocal()).getMaxY() - headerTopSceneY - marginBeneathHeader - headerHeight;

                    if (overlayY > 0)
                    {
                        // Not yet at top of screen, return a positive value:
                        return overlayY;
                    }
                    else
                    {
                        if (overlayY < -maxOffset)
                        {
                            // Don't scroll any more.  Are we at all possible to be on screen?
                            if (overlayY < - (maxOffset + getHeaderRow().getNode().getHeight()))
                            {
                                // Can't be seen at all, don't show
                                return 1;
                            }
                            else
                            {
                                // Might be seen, but don't scroll more than limit
                                return overlayY + maxOffset;
                            }
                        }
                        else
                        {
                            // Scrolling, but not at limit yet, display at top of screen:
                            return 0;
                        }
                    }
                    
                    
                }

                                {
                    super.bind(editor.getObservableScroll());
                    super.bind(getRegion().layoutBoundsProperty());
                    super.bind(getRegion().heightProperty());
                    super.bind(getHeaderRow().getNode().heightProperty());
                    super.bind(getRegion().localToSceneTransformProperty());
                    super.bind(editor.getObservableViewportHeight());
                    //super.bind(getHeaderRow().getNode().localToSceneTransformProperty());
                                    super.bind(getHeaderRow().getNode().layoutBoundsProperty());
                }};
                
                dropShadowDummy.effectProperty().bind(new ObjectBinding<Effect>()
                {

                    DropShadow dropShadow = new DropShadow();

                    @Override
                    protected Effect computeValue()
                    {
                        return offset.get() <= 0 ? dropShadow : null;
                    }

                    {
                        super.bind(offset);
                    }

                    {
                        dropShadow.setRadius(8.0);
                        dropShadow.setOffsetX(4.0);
                        dropShadow.setOffsetY(4.0);
                        dropShadow.setColor(Color.color(0.6, 0.6, 0.6));
                    }
                });
            
            
            offset.addListener(new ChangeListener<Number>()
            {
                private Pane imageView;
                private boolean addingImageView = false;
                private SimpleDoubleProperty imageViewY = new SimpleDoubleProperty(0.0);

                @Override
                public void changed(ObservableValue<? extends Number> arg0, Number oldVal, Number newVal)
                {
                    if (editor.getWindowOverlayPane() == null)
                    {
                        return; // Nothing to do if there's no overlay
                    }

                    if (!isFrameEnabled() || getParentCanvas() == null)
                    {
                        return; // Don't pin header for disabled frames or frames not in a canvas (e.g. the catalogue)
                    }

                    if (editor.viewProperty().get() != View.NORMAL)
                    {
                        return; // Don't pin header if we are showing Java preview or bird's eye view
                    }

                    // If offset is positive, the method header is below the top of the screen
                    // and thus no header should be displayed.  If imageView is not null,
                    // we have an old header to get rid of
                    if (newVal.doubleValue() > 0 && imageView != null)
                    {
                        editor.getWindowOverlayPane().removeOverlay(imageView);
                        editor.getWindowOverlayPane().removeOverlay(dropShadowDummy);
                        imageView = null;
                    }
                    // If offset is zero or negative, the method header is above the top of the screen
                    // and thus we need to display a method header.
                    else if (newVal.doubleValue() <= 0)
                    {
                        imageViewY.setValue(newVal);

                        // If imageView is null, we weren't previously displaying an overlay.
                        // Also, we have a boolean mutex, as taking a snapshot can cause a relayout,
                        // which can cause this method to be re-entered:
                        if (imageView == null && !addingImageView)
                        {
                            addingImageView = true;

                            
                            imageView = getHeaderRow().makeDisplayClone(editor);
                            imageView.getStyleClass().addAll("method-header", "method-header-row-pinned-clone");
                            double sceneX = getHeaderRow().getSceneBounds().getMinX();
                            double windowOverlayX = editor.getWindowOverlayPane().sceneXToWindowOverlayX(sceneX);
                            editor.getWindowOverlayPane().addOverlay(imageView, new SimpleDoubleProperty(windowOverlayX), imageViewY);
                            imageView.applyCss();
                            small.xProperty().set(canvas.getNode().localToScene(canvas.getNode().getBoundsInLocal()).getMinX() - sceneX + canvas.leftMargin().get());
                            editor.getWindowOverlayPane().addOverlay(dropShadowDummy, new SimpleDoubleProperty(windowOverlayX), imageViewY);

                            imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>()
                            {
                                @Override
                                public void handle(MouseEvent e)
                                {
                                    // If we try to scroll exactly to the header row, we may actually
                                    // end up just beyond it, and thus still with a pinned method header.
                                    // So we use -2 as a fudge factor to scroll to just before the header row,
                                    // to make sure the header is unpinned:
                                    editor.scrollTo(headerRow, -2);
                                }
                            });

                            addingImageView = false;
                        }

                    }
                    
                    /*
                    // Play a fade if it was zero and now isn't, or vice versa:
                    if ((oldVal.doubleValue() == 0 && newVal.doubleValue() != 0) || (oldVal.doubleValue() != 0 && newVal.doubleValue() == 0)) {
                        FadeTransition ft = new FadeTransition(Duration.millis(200), getHeadVBox());
                        ft.setToValue(newVal.doubleValue() == 0 ? 1.0 : 0.8);
                        ft.play();
                    }*/
                }

                {
                    JavaFXUtil.addChangeListener(editor.viewProperty(), v -> {
                        if (v != View.NORMAL && imageView != null)
                        {
                            editor.getWindowOverlayPane().removeOverlay(imageView);
                            editor.getWindowOverlayPane().removeOverlay(dropShadowDummy);
                            imageView = null;
                        }
                        else if (v == View.NORMAL && imageView == null)
                        {
                            changed(offset, offset.get(), offset.get());
                        }
                    });

                    headerCleanup = () -> {
                        editor.getWindowOverlayPane().removeOverlay(imageView);
                        editor.getWindowOverlayPane().removeOverlay(dropShadowDummy);
                        imageView = null;
                    };
                }
            });
        }
        // Bit of a hacky way of removing gap between header and block:
        AnchorPane.setTopAnchor(canvas.getNode(), 0.0);
                
        // Method blocks don't show anything in the sidebar
        //setSidebar(param2.textProperty());
    }
    
    protected List<ParamFragment> generateParams()
    {
        return paramsPane.getSlotElement();
    }

    @Override
    public FrameCanvas createCanvas(InteractionManager editor, String stylePrefix)
    {
        return new JavaCanvas(editor, this, stylePrefix, true);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
    {
        return ((JavaCanvas)getCanvas()).showDebugBefore(null, debug);        
    }
    
    @SuppressWarnings("unchecked")
    protected List<CodeElement> getContents()
    {
        List<CodeElement> contents = new ArrayList<CodeElement>();
        getMembersFrames().forEach(f -> {
            f.regenerateCode();
            contents.add(f.getCode());
        });
        return contents;
    }

    public List<CodeFrame> getMembersFrames()
    {
        return canvas.getBlocksSubtype(CodeFrame.class);
    }
        
    @Override
    public void checkForEmptySlot()
    {
        paramsPane.checkForEmptySlot();
    }

    public void setAccess(AccessPermission value)
    {
        access.setValue(value);        
    }

    public FormalParameters getParamsPane()
    {
        return paramsPane;
    }
    
    @Override
    public List<String> getDeclaredVariablesWithin(FrameCanvas c)
    {
        if (c != getCanvas())
            throw new IllegalArgumentException("Canvas does not exist in this frame");
        
        return paramsPane.getVars().filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList());
    }

    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursorInCanvas)
    {
        ArrayList<ExtensionDescription> extensions = new ArrayList<>(super.getAvailableExtensions(canvas, cursorInCanvas));
        extensions.add(new ExtensionDescription(StrideDictionary.THROWS_EXTENSION_CHAR,
                Config.getString("frame.class.add.throw"), () -> throwsPane.addTypeSlotAtEnd("", true), true,
                ExtensionSource.INSIDE_FIRST, ExtensionSource.MODIFIER));
        return extensions;
    }

    public void addThrows(String type)
    {
        throwsPane.addTypeSlotAtEnd(type, true);
    }

    @OnThread(Tag.FXPlatform)
    public boolean hasThrowsForType(String type)
    {
        Class<?> typeCls = getEditor().loadClass(type);
        for (ThrowsTypeFragment ttf : throwsPane.getTypes())
        {
            Class<?> inThrowsCls = getEditor().loadClass(ttf.getType());
            if (inThrowsCls.isAssignableFrom(typeCls))
                return true;
        }
        return false;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        super.setView(oldView, newView, animate);
        paramsPane.setView(newView, animate);
        if (newView.isBirdseye() != oldView.isBirdseye())
        {
            //TODO
            //getDocumentationTextArea().bindScale(animate);
            if (newView.isBirdseye()) {
                canvas.shrinkUsing(animate.getProgress().negate().add(1.0));
            }
            else {
                canvas.growUsing(animate.getProgress());
            }
            
            showingBirdseye = newView.isBirdseye();
        }

        // We change the CSS styles halfway through animation so that the step-change in height is less noticeable:
        animate.getProgress().addListener((a, oldVal, newVal) -> {
            // When we pass 0.5:
            if (Math.round(oldVal.doubleValue()) != Math.round(newVal.doubleValue()))
            {
                JavaFXUtil.setPseudoclass("bj-birdseye", newView.isBirdseye(), getNode(), canvas.getNode());
                JavaFXUtil.setPseudoclass("bj-birdseye-nodoc", newView == View.BIRDSEYE_NODOC, getNode(), canvas.getNode());
                JavaFXUtil.setPseudoclass("bj-birdseye-doc", newView == View.BIRDSEYE_DOC, getNode(), canvas.getNode());
            }
        });

        if (isFrameEnabled()  && (oldView == View.JAVA_PREVIEW || newView == View.JAVA_PREVIEW))
            canvas.previewCurly(newView == View.JAVA_PREVIEW, header.getLeftFirstItem() + tweakCurlyX(), tweakOpeningCurlyY(), animate);
    }

    protected void restoreDetails(MethodWithBodyElement nme)
    {
        setDocumentation(nme.getDocumentation());
        access.setValue(nme.getAccessPermission());
        throwsPane.setTypes(nme.getThrowsTypes());
        paramsPane.setParams(nme.getParams(), f -> f.getParamType().getContent(), f -> f.getParamName().getContent());
        canvas.restore(nme.getContents(), getEditor());
    }

    @Override
    protected void cleanupFrame()
    {
        headerCleanup.run();

        super.cleanupFrame();
    }

    @Override
    protected double tweakCurlyX()
    {
        return 2;
    }
    
    protected abstract class MethodHeaderRow extends FrameContentRow
    {
        public MethodHeaderRow(Frame parentFrame, String stylePrefix)
        {
            super(parentFrame, stylePrefix);
        }
        
        protected abstract EditableSlot getSlotBeforeParams();
        
        // Returns null if and only if the params are the last focusable slot
        protected abstract EditableSlot getSlotAfterParams();

        @Override
        public void focusRight(HeaderItem src)
        {
            if (src == getSlotBeforeParams())
            {
                paramsPane.ensureAtLeastOneParameter();
            }
            super.focusRight(src);
        }

        @Override
        public void focusLeft(HeaderItem src)
        {
            if (src == getSlotAfterParams())
            {
                if (paramsPane.ensureAtLeastOneParameter())
                {
                    // If we did add a new parameter, focus the left part:
                    paramsPane.focusBeginning();
                    return;
                }
            }
            super.focusLeft(src);
        }

        @Override
        public boolean focusRightEndFromNext()
        {
            // Only make a parameter ready, if params are actually the last item:
            if (getSlotAfterParams() == null)
            {
                if (paramsPane.ensureAtLeastOneParameter())
                {
                    // If we did add a new parameter, focus the left part:
                    paramsPane.focusBeginning();
                    return true;
                }
            }
            return super.focusRightEndFromNext();
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public void escape(HeaderItem src)
        {
            if (paramsPane.findFormal(src) != null){
                paramsPane.escape(src);
            }
            else {
                super.escape(src);
            }
        }
    }
}
