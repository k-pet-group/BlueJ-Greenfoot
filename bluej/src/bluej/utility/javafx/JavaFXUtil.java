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
package bluej.utility.javafx;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import bluej.editor.stride.CodeOverlayPane.WidthLimit;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.WritableImage;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import bluej.editor.stride.FXTabbedEditor;
import bluej.editor.stride.WindowOverlayPane;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Debug;
import bluej.utility.Utility;

import javax.imageio.ImageIO;

public class JavaFXUtil
{
    public static void setPseudoclass(String name, boolean on, Node... nodes)
    {
        if (!name.startsWith("bj-"))
            throw new IllegalArgumentException("Our pseudoclasses should begin with bj- to avoid confusion with JavaFX's pseudo classes");
        
        for (Node node : nodes)
            node.pseudoClassStateChanged(PseudoClass.getPseudoClass(name), on);        
    }


    public static boolean hasPseudoclass(Node node, String s)
    {
        return node.getPseudoClassStates().stream().filter(p -> p.getPseudoClassName().equals(s)).count() > 0;
    }
    
    /**
     * If on is true, adds all the style classes to the node.  If on is false, removes them all from the node.
     */
    public static void setStyleClass(Styleable n, boolean on, String... styleClasses)
    {
        if (on)
            addStyleClass(n, styleClasses);
        else
            removeStyleClass(n, styleClasses);
    }

    /**
     * Applies the chosen style class to a node, in place of the
     * list of possibilities (which should include chosen) given as varargs
     * 
     * If chosen is null, it is not added (so all are removed)
     */
    public static void selectStyleClass(String chosen, Styleable n, String... all)
    {
        n.getStyleClass().removeAll(all);
        if (chosen != null)
            n.getStyleClass().add(chosen);
    }
    
    /**
     * Applies the given indexed style class to a node, in place of the
     * rest of the list of possibilities, which are all removed.
     * 
     * If index is -1, all are removed
     */
    public static void selectStyleClass(int index, Styleable n, String... all)
    {
        selectStyleClass(index == -1 ? null : all[index], n, all);
    }
    
    /**
     * Turns on the pseudoclass with the given index, and turns off all others in the list.
     */
    public static void selectPseudoClass(Node node, int index, String... pseudoClasses)
    {
        for (int i = 0; i < pseudoClasses.length; i++)
        {
            JavaFXUtil.setPseudoclass(pseudoClasses[i], i == index, node);
        }
    }
    /**
     * Adds the given style-class to the node.
     * 
     * JavaFX doesn't care if you add the same class many times to a node.  In contrast, this method
     * makes sure that a class is only applied once (effectively models the classes on a node as a 
     * set, not a list) 
     */
    public static void addStyleClass(Styleable n, String... styleClasses)
    {
        for (String styleClass : styleClasses)
        {
            if (!n.getStyleClass().contains(styleClass))
                n.getStyleClass().add(styleClass);
        }
    }

    /**
     * Removes the given style-class to the node.
     * 
     * JavaFX doesn't care if you add the same class many times to a node, and if you're not
     * careful when removing a style-class, you may only remove one instance and not all.  This method
     * makes sure that all copies of a class are removed (effectively models the classes on a node as a 
     * set, not a list) 
     */
    public static void removeStyleClass(Styleable n, String... styleClasses)
    {
        //removeAll gets rid of all instances of all of styleClasses:
        n.getStyleClass().removeAll(styleClasses);
    }

    public static <T> ObservableValue<T> conditional(final ObservableBooleanValue prop, final T ifTrue, final T ifFalse)
    {
        return new When(prop).then(ifTrue).otherwise(ifFalse);
    }
    
    private static final FXCache<Font, FXCache<String, Double>> measured =
        new FXCache<Font, FXCache<String, Double>>(f -> new FXCache<String, Double>(s -> measureString(f, s), 1000), 3);

    /**
     * Measure a string using the node's font
     */
    public static double measureString(TextInputControl node, String str)
    {
        return measureString(node, str, true, true);
    }
    public static double measureString(TextInputControl node, String str, boolean includeLeftInset, boolean includeRightInset)
    {
        return measureString(node, str, node.getFont(), includeLeftInset, includeRightInset);
    }

    public static double measureString(TextInputControl node, String str, Font overrideFont, boolean includeLeftInset, boolean includeRightInset)
    {
        return measured.get(overrideFont).get(str) + (includeLeftInset ? node.getInsets().getLeft() : 0.0) + (includeRightInset ? node.getInsets().getRight() : 0.0);
    }
    
    private static double measureString(Font f, String str)
    {
        if (str == null || str.length() == 0)
            return 0;
        
        //Not a very elegant way to get the size of the text, but only way to really do it
        //See e.g. http://stackoverflow.com/questions/13015698/
        Text text = new Text(str);
        text.setFont(f);
        return text.getLayoutBounds().getWidth();
    }

    public static double measureString(Labeled node, String str)
    {
        return measureString(node, str, node.getFont());
    }

    public static double measureString(Labeled node, String str, Font overrideFont)
    {
        return measured.get(overrideFont).get(str) + node.getLabelPadding().getLeft() + node.getLabelPadding().getRight() + node.getPadding().getLeft() + node.getPadding().getRight();
    }

    public static double measureStringHeight(Labeled node, String str)
    {
        Text text = new Text(str);
        text.setFont(node.getFont());
        return text.getLayoutBounds().getHeight();
    }

    public static void toggleStyleClass(Boolean on, Styleable n, String styleClass)
    {
        if (on)
            addStyleClass(n, styleClass);
        else
            removeStyleClass(n, styleClass);
    }

    /** Gets a new CssMetaData instance for the given property of the class.
     *  Assumes the property is always settable.  Thus do not pass a property that might be
     *  set or bound by other code.
     */
    public static <T extends Styleable> CssMetaData<T, Number> cssSize(String propertyName, Function<T, SimpleStyleableDoubleProperty> propGetter)
    {
        return new CssMetaData <T, Number >(propertyName, StyleConverter.getSizeConverter()) {
            @Override
            public boolean isSettable(T node)
            {
                return true;
            }

            @Override
            public StyleableProperty <Number > getStyleableProperty(T node) {
                return propGetter.apply(node);
            }
        };
    }

    /** Gets a new CssMetaData instance for the given property of the class.
     *  Assumes the property is always settable.  Thus do not pass a property that might be
     *  set or bound by other code.
     */
    public static <T extends Styleable> CssMetaData<T, Color> cssColor(String propertyName, Function<T, SimpleStyleableObjectProperty<Color>> propGetter)
    {
        return new CssMetaData <T, Color >(propertyName, StyleConverter.getColorConverter()) {
            @Override
            public boolean isSettable(T node)
            {
                return true;
            }

            @Override
            public StyleableProperty <Color> getStyleableProperty(T node) {
                return propGetter.apply(node);
            }
        };
    }

    
    /** Gets a new CssMetaData instance for the given property of the class.
     *  Assumes the property is always settable.  Thus do not pass a property that might be
     *  set or bound by other code.
     *  
     *  WARNING: Using this at the moment will encounter bug RT-38723:
     *  https://javafx-jira.kenai.com/browse/RT-38723
     */
    public static <T extends Styleable> CssMetaData<T, Insets> cssInsets(String propertyName, Function<T, StyleableObjectProperty<Insets>> propGetter)
    {
        return new CssMetaData <T, Insets >(propertyName, StyleConverter.getInsetsConverter(), Insets.EMPTY) {
            @Override
            public boolean isSettable(T node)
            {
                return true;
            }

            @Override
            public StyleableProperty <Insets> getStyleableProperty(T node) {
                return propGetter.apply(node);
            }
        };
    }

    public static void writeImageTo(WritableImage image, String filename)
    {
        File file = new File(filename);
        RenderedImage renderedImage = SwingFXUtils.fromFXImage(image, null);
        try
        {
            ImageIO.write(
                renderedImage,
                "png",
                file);
        } catch (IOException e)
        {
            Debug.reportError(e);
        }
    }

    public static void workAroundFunctionKeyBug(TextField field)
    {
        // Work around taken from http://stackoverflow.com/questions/28239019/javafx-accelerator-not-working-when-textfield-has-focus
        // to work about this bug https://bugs.openjdk.java.net/browse/JDK-8089884 due to be fixed in JDK 9
        field.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            switch (e.getCode())
            {
                case F1:
                case F2:
                case F3:
                case F4:
                case F5:
                case F6:
                case F7:
                case F8:
                case F9:
                case F10:
                case F11:
                case F12:
                    //handle function key shortcut  if defined
                    Runnable r = field.getScene().getAccelerators().get(new KeyCodeCombination(e.getCode()));
                    if(r!=null) {
                        r.run();
                        e.consume();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * Makes a clone of the given label by copying its style and content.
     * The copying is a binding, so it will keep up to date with the original.
     */
    public static Label cloneLabel(Label l, ObservableValue<String> fontSize)
    {
        Label copy = new Label();
        copy.textProperty().bind(l.textProperty());
        bindList(copy.getStyleClass(), l.getStyleClass());
        copy.styleProperty().bind(l.styleProperty().concat("-fx-font-size:").concat(fontSize).concat(";"));
        bindPseudoclasses(copy, l.getPseudoClassStates());
        return copy;
    }

    public static class ListBuilder<T>
    {
        private ArrayList<T> list;

        private ListBuilder(List<T> list)
        {
            this.list = new ArrayList<>(list);
        }
        
        public ListBuilder<T> add(T t)
        {
            list.add(t);
            return this;
        }
        
        public List<T> build()
        {
            return Collections.unmodifiableList(list);
        }
    }
    
    /**
     * Helper method for extending a super-class's CSS meta data with some of your own
     */
    public static ListBuilder<CssMetaData<? extends Styleable, ?>> extendCss(
            List<CssMetaData<? extends Styleable, ?>> superClassCssMetaData)
    {
        return new ListBuilder<CssMetaData<? extends Styleable, ?>>(superClassCssMetaData);
    }

    // Note: also rounds to next highest integer (rounds up)
    private static FXRunnable bindSetter(Node n, DoubleExpression value, BiConsumer<Node, Double> set)
    {
        set.accept(n, Math.ceil(value.get()));
        ChangeListener<? super Number> listener = (a, b, newVal) -> set.accept(n, Math.ceil(newVal.doubleValue()));
        value.addListener(listener);
        return () -> value.removeListener(listener);
    }
    
    public static FXRunnable bindLeftAnchor(Node n, DoubleExpression value)
    {
        return bindSetter(n, value, AnchorPane::setLeftAnchor);
    }

    public static FXRunnable bindRightAnchor(Node n, DoubleExpression value)
    {
        return bindSetter(n, value, AnchorPane::setRightAnchor);
    }

    public static FXRunnable bindTopAnchor(Node n, DoubleExpression value)
    {
        return bindSetter(n, value, AnchorPane::setTopAnchor);
    }
    
    public static FXRunnable bindBottomAnchor(Node n, DoubleExpression value)
    {
        return bindSetter(n, value, AnchorPane::setBottomAnchor);
    }

    /**
     * Adds handlers to the given node so that a popup menu is shown (via the given function)
     * when appropriate.
     * @param node The node to listen for the mouse events on
     * @param showContextMenu The function to call to show the menu.  The parameters are X and Y
     *                        screen (*NOT* scene) coordinates.  Returns true if the menu was shown.
     */
    public static void listenForContextMenu(Node node, BiFunction<Double, Double, Boolean> showContextMenu)
    {
        EventHandler<MouseEvent> popupHandler = e -> {
            if (e.isPopupTrigger())
            {
                if (showContextMenu.apply(e.getScreenX(), e.getScreenY()))
                    e.consume();
            }
        };
        
        // According to docs for isPopupTrigger, we need this handler on pressed and released:
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, popupHandler);
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, popupHandler);
        
        
        node.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.CONTEXT_MENU)
            {
                Point2D scenePt = node.localToScene(5, 5);
                Scene scene = node.getScene();
                Point2D screenPt = scenePt.add(scene.getWindow().getX() + scene.getX(), scene.getWindow().getY() + scene.getY()); 
                if (showContextMenu.apply(screenPt.getX(), screenPt.getY()))
                    e.consume();
            }
        });
    }
    
    public static interface OkCancel
    {
        public Node getNode();
        public void requestOkFocus();
        public void requestCancelFocus();
        public WritableBooleanValue okEnabledProperty();
    }

    public static OkCancel makeOkCancel(EventHandler<ActionEvent> onOk, EventHandler<ActionEvent> onCancel)
    {
        Button ok = new Button("Ok");
        ok.setOnAction(onOk);
        Button cancel = new Button("Cancel");
        cancel.setOnAction(onCancel);
        
        HBox hbox = new HBox(cancel, new Rectangle(50, 1, Color.TRANSPARENT), ok);
        
        return new OkCancel() {
            private final BooleanProperty okEnabled = new SimpleBooleanProperty(true);
            { ok.disableProperty().bind(okEnabled.not()); }
            public Node getNode() { return hbox; }
            public void requestOkFocus() { ok.requestFocus(); }
            public void requestCancelFocus() { cancel.requestFocus(); }
            public WritableBooleanValue okEnabledProperty() { return okEnabled; }
        };
    }

    public static MenuItem makeMenuItem(String text, FXRunnable run, KeyCombination shortcut)
    {
        MenuItem item = new MenuItem(text);
        if (run != null)
            item.setOnAction(e -> run.run());
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    public static MenuItem makeDisabledMenuItem(String text, KeyCombination shortcut)
    {
        MenuItem item = new MenuItem(text);
        item.setDisable(true);
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }
    
    // Makes a CheckMenuItem that operates its own state, but calls the given callback when it changes
    public static CheckMenuItem makeCheckMenuItem(String text, Consumer<Boolean> handler, KeyCombination shortcut)
    {
        CheckMenuItem item = new CheckMenuItem(text);
        item.selectedProperty().addListener((a, b, newVal) -> handler.accept(newVal.booleanValue()));
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }
    
    // Makes a CheckMenuItem that binds bidirectionally to the given state
    public static CheckMenuItem makeCheckMenuItem(String text, Property<Boolean> state, KeyCombination shortcut)
    {
        CheckMenuItem item = new CheckMenuItem(text);
        item.selectedProperty().bindBidirectional(state);
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    // Makes a CheckMenuItem that binds bidirectionally to the given state
    public static CheckMenuItem makeCheckMenuItem(String text, Property<Boolean> state, ObservableValue<? extends KeyCombination> shortcut)
    {
        CheckMenuItem item = new CheckMenuItem(text);
        item.selectedProperty().bindBidirectional(state);
        if (shortcut != null)
            item.acceleratorProperty().bind(shortcut);
        return item;
    }

    public static Menu makeMenu(String text, MenuItem... items)
    {
        Menu menu = new Menu(text);
        menu.getItems().setAll(items);
        return menu;
    }

    
    
    public static void initializeCustomTooltip(InteractionManager editor, Node parent, String tooltipText, long delayMillis)
    {
        initializeCustomTooltip(editor, parent, new ReadOnlyStringWrapper(tooltipText), delayMillis);
    }
    
    public static void initializeCustomTooltip(InteractionManager editor, Node parent, StringExpression tooltipText, long delayMillis)
    {
        parent.focusedProperty().addListener(new ChangeListener<Boolean>() {
            final Label l;
            {
                l = new Label();
                l.textProperty().bind(tooltipText);
                l.visibleProperty().bind(l.textProperty().isNotEmpty());
                l.setMouseTransparent(true);
                l.setWrapText(true);
                JavaFXUtil.addStyleClass(l, "frame-tooltip");
            }
            // All assignments to focusRunnable happen on the event thread
            private Runnable focusRunnable;
            
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0,
                    Boolean arg1, Boolean newVal)
            {
                final Object syncOn = this;
                if (newVal.booleanValue())
                {
                    // Gained focus:
                    final Runnable r = new Runnable() { @OnThread(value = Tag.FX, ignoreParent = true) public void run() {
                        // We only focus if we are the last assignment to focusRunnable field
                        if (focusRunnable == this)
                        {
                            editor.getCodeOverlayPane().addOverlay(l, parent, null, l.heightProperty().negate(), WidthLimit.LIMIT_WIDTH_AND_SLIDE_LEFT);
                            FadeTransition ft = new FadeTransition(Duration.millis(100), l);
                            ft.setFromValue(0.0);
                            ft.setToValue(1.0);                        
                            ft.play();
                        }
                    }};
                    focusRunnable = r;
                    Utility.getBackground().schedule(() -> Platform.runLater(r), delayMillis, TimeUnit.MILLISECONDS);
                }
                else
                {
                    // Lost focus;
                    focusRunnable = null; // Effectively cancel previous tooltip displays
                    editor.getCodeOverlayPane().removeOverlay(l);
                }
                
            }
            
        });
    }

    public static void initializeCustomTooltipCatalogue(FXTabbedEditor window, Node target, String tooltipText, Duration delay)
    {
        final Label l = new Label(tooltipText);
        l.visibleProperty().bind(l.textProperty().isNotEmpty());
        l.setMouseTransparent(true);
        l.setWrapText(true);
        JavaFXUtil.addStyleClass(l, "frame-tooltip");
        JavaFXUtil.setPseudoclass("bj-tight-border", true, l);
        l.setMaxWidth(300);

        FXRunnable show = () -> {
            final WindowOverlayPane overlayPane = window.getOverlayPane();
            double x = overlayPane.sceneXToWindowOverlayX(target.localToScene(target.getBoundsInLocal()).getMinX());
            double y = overlayPane.sceneYToWindowOverlayY(target.localToScene(target.getBoundsInLocal()).getMaxY() + 10);
            overlayPane.addOverlay(l, new ReadOnlyDoubleWrapper(x), new ReadOnlyDoubleWrapper(y), true);
            FadeTransition ft = new FadeTransition(Duration.millis(100), l);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        };

        target.addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>()
                {
                    FXRunnable cancel;

                    @Override
                    public void handle(MouseEvent event)
                    {
                        if (event.getEventType() == MouseEvent.MOUSE_ENTERED)
                        {
                            cancel = JavaFXUtil.runAfter(delay, show);
                        }
                        else if (event.getEventType() == MouseEvent.MOUSE_EXITED)
                        {
                            if (cancel != null)
                            {
                                cancel.run();
                                cancel = null;
                            }
                            window.getOverlayPane().removeOverlay(l);
                        }
                    }
                }
        );
    }

    @OnThread(Tag.FX)
    private static class TooltipListener implements EventHandler<InputEvent>, ChangeListener<Boolean>
    {
        private final Label l;
        private final InteractionManager editor;
        private final Node parent;
        private final FXConsumer<FXConsumer<String>> requestTooltip;
        private FXRunnable cancelHoverShow = null;
        private boolean showing = false;
        private boolean mouseShown = false;

        private TooltipListener(Label l, InteractionManager editor, Node parent, FXConsumer<FXConsumer<String>> requestTooltip)
        {
            this.l = l;
            this.editor = editor;
            this.parent = parent;
            this.requestTooltip = requestTooltip;
        }

        @Override
        public void handle(InputEvent original)
        {
            if (original instanceof KeyEvent)
            {
                KeyEvent event = (KeyEvent) original;
                if (event.getCode() == KeyCode.F1 && !showing)
                {
                    mouseShown = false;
                    if (cancelHoverShow != null)
                    {
                        cancelHoverShow.run();
                        cancelHoverShow = null;
                    }
                    show();
                    event.consume();
                }

                if (event.getCode() == KeyCode.ESCAPE && showing && !mouseShown)
                {
                    hide();
                    // We don't consume escape events in case they trigger more functionality
                }
            }
            else if (original instanceof MouseEvent)
            {
                MouseEvent event = (MouseEvent) original;

                if (event.getEventType() == MouseEvent.MOUSE_ENTERED)
                {
                    if (cancelHoverShow != null)
                    {
                        cancelHoverShow.run();
                        cancelHoverShow = null;
                    }
                    if (!showing)
                    {
                        cancelHoverShow = JavaFXUtil.runAfter(Duration.millis(1000), () -> {
                            mouseShown = true;
                            show();
                        });
                    }
                }
                else if (event.getEventType() == MouseEvent.MOUSE_EXITED)
                {
                    if (cancelHoverShow != null)
                    {
                        cancelHoverShow.run();
                        cancelHoverShow = null;
                    }
                    if (showing && mouseShown)
                    {
                        hide();
                    }
                }
            }
        }

        private void show()
        {
            requestTooltip.accept(l::setText);
            editor.getCodeOverlayPane().addOverlay(l, parent, null, l.heightProperty().negate().subtract(5.0), WidthLimit.LIMIT_WIDTH_AND_SLIDE_LEFT);
            //FadeTransition ft = new FadeTransition(Duration.millis(100), l);
            //ft.setFromValue(0.0);
            //ft.setToValue(1.0);
            //ft.play();

            showing = true;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable,
                Boolean oldValue, Boolean newVal)
        {

            if (newVal.booleanValue() == false && showing && !mouseShown)
            {
                hide();
            }            
        }

        private void hide()
        {
            if (showing)
            {
                editor.getCodeOverlayPane().removeOverlay(l);
                showing = false;
            }
        }

    }
    
    
    // Shows only on F1 if onHoverToo is false
    public static void initializeCustomHelp(InteractionManager editor, Node parent, FXConsumer<FXConsumer<String>> requestTooltip, boolean onHoverToo)
    {
        final Label l = new Label();
        l.visibleProperty().bind(l.textProperty().isNotEmpty());
        l.setMouseTransparent(true);
        l.setWrapText(true);
        JavaFXUtil.addStyleClass(l, "frame-tooltip");
        
        TooltipListener listener = new TooltipListener(l, editor, parent, requestTooltip);
        parent.addEventHandler(KeyEvent.KEY_PRESSED, listener);
        parent.focusedProperty().addListener(listener);
        if (onHoverToo)
        {
            parent.addEventHandler(MouseEvent.MOUSE_ENTERED, listener);
            parent.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
        }
    }
    
    /** Waits for the observable value to become non-null, then calls the given function on it. */
    public static <T6> void onceNotNull(ObservableValue<T6> value, FXConsumer<T6> f)
    {
        T6 t = value.getValue();
        
        if (t != null)
        {
            f.accept(t);
            return;
        }
        
        // Can't be a lambda because we need a reference to ourselves to self-remove:
        ChangeListener<? super T6> listener = new ChangeListener<T6>() {
    
            @Override
            public void changed(ObservableValue<? extends T6> observable,
                    T6 oldValue, T6 newVal)
            {
                if (newVal != null)
                {
                    f.accept(newVal);
                    value.removeListener(this);
                }
            }
        };
        value.addListener(listener);
        
    }

    /**
     * "Binds" the destination list to the observable source list with a transformation function applied.
     * Whenever the source list changes, the destination list is altered to match by applying
     * the given function to each element in the source list.
     */
    public static <SRC2, DEST2> void bindMap(List<DEST2> dest, ObservableList<SRC2> src, Function<SRC2, DEST2> func)
    {
        dest.clear();
        dest.addAll(Utility.mapList(src, func));
        
        src.addListener(new ListChangeListener<SRC2>() {
    
            @Override
            public void onChanged(ListChangeListener.Change<? extends SRC2> changes) {
            while (changes.next())
            {
                if (changes.wasPermutated() || changes.wasUpdated())
                {
                    // Same code for updated, replaced and permutation, just recalc the range:
                    for (int i = changes.getFrom(); i < changes.getTo(); i++)
                        dest.set(i, func.apply(src.get(i)));
                }
                else
                {
                    for (int i = 0; i < changes.getRemovedSize(); i++)
                        dest.remove(changes.getFrom());
                    for (int i = 0; i < changes.getAddedSubList().size();i++)
                        dest.add(i + changes.getFrom(), func.apply(changes.getAddedSubList().get(i)));
                }
            }
            }
        });
    }

    public static <SRC, DEST> ObservableList<DEST> mapObservableList(ObservableList<SRC> src, Function<SRC, DEST> func)
    {
        ObservableList<DEST> r = FXCollections.observableArrayList();
        bindMap(r, src, func);
        return r;
    }
    
    /**
     * Runs the given function on the FX thread, and puts the result
     * in the returned future once completed.  It's the callback's job to
     * catch any exceptions and return a suitable value.
     * 
     * If you try to get() the result in an FX thread, you may well deadlock!
     */
    @OnThread(Tag.Any)
    public static <T> CompletableFuture<T> future(FXSupplier<T> r)
    {
        CompletableFuture<T> f = new CompletableFuture<>();
        Platform.runLater(() -> f.complete(r.get()));
        return f;
    }
    
    public static <T> void bindFuture(Future<T> future, FXConsumer<T> andThen)
    {
        new Thread(() -> {
            try
            {
                T x = future.get();
                Platform.runLater(() -> andThen.accept(x));
            }
            catch (ExecutionException | InterruptedException e)
            {
                Debug.reportError(e);
            }
            
        }).start();
    }
    /*
    public static <T, U> FXSupplier<U> bindFutureFunc(Future<T> future, FXFunction<T, U> andThen)
    {
        SynchronousQueue q = new SynchronousQueue<>();
        new Thread(() -> {
            try
            {
                T x = future.get();
                Platform.runLater(() -> andThen.accept(x));
            }
            catch (ExecutionException | InterruptedException e)
            {
                Debug.reportError(e);
            }
            
        }).start();
    }
    */
    
    public static <T> void bindAtomic(ObservableValue<T> from, AtomicReference<T> to)
    {
        from.addListener((a, b, newVal) -> to.set(newVal));
    }
    
    /**
     * Takes a BooleanProperty and an item.  Gives back an observable list that contains
     * the list of items when (and only when) the BooleanProperty is true, but is empty in the case
     * that the BooleanProperty is false.  Uses JavaFX bean bindings to update the list's contents.
     * @param putInList
     * @param item
     * @return
     */
    public static <T> ObservableList<T> listBool(BooleanExpression putInList, T... items)
    {
        ObservableList<T> r = FXCollections.observableArrayList();
        if (putInList.get()) {
            r.setAll(items);
        }
        putInList.addListener((a, b, newVal) -> {
            if (newVal.booleanValue()) {
                r.setAll(items);
            }
            else {
                r.clear();
            }
        });
        return r;
    }

    /**
     * Creates a ChangeListener that will execute the given action (with the new value)
     * once, on the first change, and then remove itself as a listener.  Also returns
     * an action that can remove the listener earlier.
     */
    public static <T> FXRunnable addSelfRemovingListener(ObservableValue<T> prop, FXConsumer<T> callback)
    {
        ChangeListener<T> l = new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> observable,
                    T oldValue, T newValue)
            {
                callback.accept(newValue);
                prop.removeListener(this);
            }
            
        };
        prop.addListener(l);
        return () -> prop.removeListener(l);
    }
    
    /**
     * Makes one list (dest) always contain the contents of the other (src) until the returned
     * action is executed to cancel the binding.
     */
    public static <T> FXRunnable bindList(ObservableList<T> dest, ObservableList<T> src)
    {
        ListChangeListener<T> obs = c -> dest.setAll(src);
        src.addListener(obs);
        dest.setAll(src);
        return () -> src.removeListener(obs);
    }

    /**
     * Makes one list (dest) always contain the contents of the other (src) until the returned
     * action is executed to cancel the binding.
     */
    public static <T> FXRunnable bindSet(ObservableSet<T> dest, ObservableSet<T> src)
    {
        SetChangeListener<T> obs = c -> {
            if (c.wasAdded())
                dest.add(c.getElementAdded());
            if (c.wasRemoved())
                dest.remove(c.getElementRemoved());
        };
        dest.clear();
        dest.addAll(src);
        src.addListener(obs);
        return () -> src.removeListener(obs);
    }
    
    public static <T> FXRunnable addChangeListener(ObservableValue<T> property, FXConsumer<T> listener)
    {
        ChangeListener<T> wrapped = (a, b, newVal) -> listener.accept(newVal);
        property.addListener(wrapped);
        return () -> property.removeListener(wrapped);
    }
    
    public static FXRunnable sequence(FXRunnable... actions)
    {
        return () -> Arrays.stream(actions).forEach(FXRunnable::run);
    }

    /**
     * Runs the given task on the FX Platform thread after the given delay.
     * Returns a task which, if executed, cancels the task.
     */
    public static FXRunnable runAfter(Duration delay, FXRunnable task)
    {
        if (delay.lessThanOrEqualTo(Duration.ZERO))
        {
            // Run immediately:
            task.run();
            return () -> {};
        }
        
        // The documentation for stop says it may not stop immediately, so we
        // use this boolean as a fail safe:
        BooleanProperty okToRun = new SimpleBooleanProperty(true);
        Timeline timeline = new Timeline(new KeyFrame(delay, e -> {
            if (okToRun.get())
                task.run();
        }));
        timeline.setCycleCount(1);
        timeline.play();
        return () -> {
            okToRun.set(false);
            timeline.stop();
        };
    }
    
    /**
     * Runs the given task on the FX Platform thread after the given interval,
     * and then forever-after, with the given interval between each execution.
     * 
     * Returns an action hich, if executed, cancels the task.
     */
    public static FXRunnable runRegular(Duration interval, FXRunnable task)
    {
        if (interval.lessThanOrEqualTo(Duration.ZERO))
        {
            throw new IllegalArgumentException("Cannot run at a regular interval of zero or less");
        }
        
        // The documentation for stop says it may not stop immediately, so we
        // use this boolean as a fail safe:
        BooleanProperty okToRun = new SimpleBooleanProperty(true);
        Timeline timeline = new Timeline(new KeyFrame(interval, e -> {
            if (okToRun.get())
                task.run();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.setAutoReverse(false);
        timeline.play();
        return () -> {
            okToRun.set(false);
            timeline.stop();
        };
    }
    
    public static boolean isDragCopyKeyPressed(KeyEvent event)
    {
        return Config.isMacOS() ? event.isAltDown() : event.isShortcutDown();
    }
    
    public static boolean isDragCopyKeyPressed(MouseEvent event)
    {
        return Config.isMacOS() ? event.isAltDown() : event.isShortcutDown();
    }
    
    public static void bindPseudoclasses(Node to, ObservableSet<PseudoClass> from)
    {
        from.addListener((SetChangeListener<PseudoClass>)c -> {
            if (c.wasAdded())
                to.pseudoClassStateChanged(c.getElementAdded(), true);
            if (c.wasRemoved())
                to.pseudoClassStateChanged(c.getElementRemoved(), false);
        });
        from.forEach(c -> to.pseudoClassStateChanged(c, true));
    }
}
