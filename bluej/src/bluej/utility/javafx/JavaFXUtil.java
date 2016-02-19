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
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.image.Image;
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

/**
 * JavaFXUtil is a collection of static utility methods for JavaFX-related code
 * (effectively, the JavaFX sibling of Utility).
 */
public class JavaFXUtil
{
    /**
     * Sets the given CSS pseudo-class (bit after the colon in CSS selector) to given
     * state for given nodes
     * @param pseudoClassName The name of the CSS pseudo-class, e.g. "bj-pinned".  Must begin with "bj-" to avoid confusion with built in FX classes
     * @param enabled Should the class state be set to on (true) or off (false)
     * @param nodes A list of nodes so add/remove the pseudoclass from (based on the "enabled" parameter)
     */
    public static void setPseudoclass(String pseudoClassName, boolean enabled, Node... nodes)
    {
        if (!pseudoClassName.startsWith("bj-"))
            throw new IllegalArgumentException("Our pseudoclasses should begin with bj- to avoid confusion with JavaFX's pseudo classes");
        
        for (Node node : nodes)
            node.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), enabled);        
    }

    /**
     * Checks if the given pseudo-class is currently enabled on the given node
     * @param node The JavaFX node
     * @param pseudoClassName The name of the pseudo-class.
     * @return Whether the class is currently enabled on that node or not.
     */
    public static boolean hasPseudoclass(Styleable node, String pseudoClassName)
    {
        return node.getPseudoClassStates().stream().filter(p -> p.getPseudoClassName().equals(pseudoClassName)).count() > 0;
    }
    
    /**
     * Turns on the pseudoclass with the given index, and turns off all others in the list.
     * 
     * @param node Node to apply to
     * @param index Which index (zero-based) of pseudoClasses to turn on (-1 if none)
     * @param pseudoClasses The list of pseudo-classes to turn off (except for index, which is turned on)
     */
    public static void selectPseudoClass(Node node, int index, String... pseudoClasses)
    {
        for (int i = 0; i < pseudoClasses.length; i++)
        {
            JavaFXUtil.setPseudoclass(pseudoClasses[i], i == index, node);
        }
    }
    /**
     * Adds the given CSS style-class[es] to the node.
     * 
     * JavaFX doesn't usually care if you add the same class many times to a node.  In contrast, this method
     * makes sure that a class is only applied once (effectively models the classes on a node as a 
     * set, not a list).  Generally, you should try to avoid dynamically turning classes on
     * or off, anyway, and use pseudo-classes instead.
     * 
     * @param node The node to apply to
     * @param styleClasses The list of CSS style-classes to add.
     */
    public static void addStyleClass(Styleable node, String... styleClasses)
    {
        for (String styleClass : styleClasses)
        {
            if (!node.getStyleClass().contains(styleClass))
                node.getStyleClass().add(styleClass);
        }
    }

    /**
     * Removes the given CSS style-class to the node.
     * 
     * JavaFX doesn't care if you add the same class many times to a node, and if you're not
     * careful when removing a style-class, you may only remove one instance and not all.  This method
     * makes sure that all copies of a class are removed (effectively models the classes on a node as a 
     * set, not a list). 
     * 
     * Generally, you should try to avoid dynamically turning classes on
     * or off, anyway, and use pseudo-classes instead.  Therefore, eventually we
     * should remove this method, hence the deprecation.
     * 
     * @param node The node to apply to.
     * @param styleClasses The style-classes to remove.
     */
    @Deprecated
    public static void removeStyleClass(Styleable node, String... styleClasses)
    {
        //removeAll gets rid of all instances of all of styleClasses:
        node.getStyleClass().removeAll(styleClasses);
    }

    /**
     * Since JavaFX has no font metrics, we make our own.  The only way to
     * measure a JavaFX string is to render an off-screen text.  To avoid the repeated
     * expense, we keep a cache of font-size & text-content to width.
     * We do this for up to 3 font sizes, and up to 10k strings per font size
     * (This may seem a lot, but we usually measure substrings too, so we end up with
     * (num strings * avg string length) strings in cache.  See FXCache for more
     * details on cache behaviour.
     */
    private static final FXCache<Font, FXCache<String, Double>> measured =
        new FXCache<Font, FXCache<String, Double>>(f -> new FXCache<String, Double>(s -> {
            if (s == null || s.length() == 0)
                return 0.0;

            //Not a very elegant way to get the size of the text, but only way to really do it
            //See e.g. http://stackoverflow.com/questions/13015698/
            Text text = new Text(s);
            text.setFont(f);
            return text.getLayoutBounds().getWidth();
        }, 10000), 3);

    /**
     * Measure the given string content using the given node's font.
     * 
     * @return Width of the string, when rendered in that font, in [logical] pixels.
     *         Does not have to be integer.
     */
    public static double measureString(TextInputControl node, String str)
    {
        return measureString(node, str, true, true);
    }
    /**
     * Measure the given string content using the given node's font.
     *
     * @param includeLeftInset Whether to include node.getInsets().getLeft() in the width
     * @param includeRightInset Whether to include node.getInsets().getRight() in the width
     * @return Width of the string, when rendered in that font, in [logical] pixels.
     *         Does not have to be integer.
     */
    public static double measureString(TextInputControl node, String str, boolean includeLeftInset, boolean includeRightInset)
    {
        return measureString(node, str, node.getFont(), includeLeftInset, includeRightInset);
    }

    /**
     * Measure the given string content using the given font.
     *
     * @param node The node from which to take the insets, but NOT the font
     * @param str The string to measure the size of
     * @param overrideFont The font to use for measuring
     * @param includeLeftInset Whether to include node.getInsets().getLeft() in the width
     * @param includeRightInset Whether to include node.getInsets().getRight() in the width
     * @return Width of the string, when rendered in that font, in [logical] pixels.
     *         Does not have to be integer.
     */
    public static double measureString(TextInputControl node, String str, Font overrideFont, boolean includeLeftInset, boolean includeRightInset)
    {
        return measured.get(overrideFont).get(str) + (includeLeftInset ? node.getInsets().getLeft() : 0.0) + (includeRightInset ? node.getInsets().getRight() : 0.0);
    }

    /**
     * Measure the given string content using the font of the given node.
     * @return Width of the string, when rendered in that font, in [logical] pixels.
     *         Does not have to be integer.
     */
    public static double measureString(Labeled node, String str)
    {
        return measured.get(node.getFont()).get(str) + node.getLabelPadding().getLeft() + node.getLabelPadding().getRight() + node.getPadding().getLeft() + node.getPadding().getRight();
    }

    /** Gets a new CssMetaData instance for the given property of the class.
     *  Assumes the property is always settable.  Thus do not pass a property that might be
     *  set or bound by other code.
     *  
     *  @param propertyName The name, in CSS, of the property (e.g. "-bj-indent-width")
     *  @param propGetter An accessor on type T for the correspoding JavaFX property
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
     *
     *  @param propertyName The name, in CSS, of the property (e.g. "-bj-highlight-color")
     *  @param propGetter An accessor on type T for the correspoding JavaFX property
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

    /**
     * Writes the given image to the given filename as a PNG
     */
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

    /**
     * Works around a problem where key-presses of function keys in text fields
     * do not call accelerators.  See comments in method.
     * 
     * @param field The field on which to work around the issue.
     */
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
                    if (r != null)
                    {
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
     * The copying uses binding, so it will keep up to date with the original afterwards.
     * 
     * @param l The label to copy
     * @param fontSize The font size property (e.g. "12pt") to use for font size 
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

    /**
     * Helper method to copy the source image to the destination X, Y position in the dest image
     */
    public static void blitImage(WritableImage dest, int xOffset, int yOffset, Image src)
    {
        dest.getPixelWriter().setPixels(xOffset, yOffset, (int)(Math.ceil(src.getWidth())), (int)(Math.ceil(src.getHeight())), src.getPixelReader(), 0, 0);
    }

    /**
     * Given an ObservableValue with a source object, and a function to get an ObservableValue property of the source object,
     * forms a binding that watches for updates of either the source object or the property of the current
     * source object, to form a virtual observable value for object.property
     *
     * If at any time object becomes null, the returned observable will use the default value.  Null will never be passed
     * to property.
     *
     * @param object The source object on which to apply the property
     * @param property The property to apply to the current source object
     * @param def The default value to use when source is null
     * @param <S> The type of the source object
     * @param <T> The type inside the property
     * @return An observable corresponding to object.property
     */
    public static <S, T> ObservableValue<T> apply(ObservableValue<S> object, FXFunction<S, ObservableValue<T>> property, T def)
    {
        // Easier to create new property and use change listeners to update the binding than try
        // to make an all-in-one binding:
        ObjectProperty<T> r = new SimpleObjectProperty<>(object.getValue() == null ? def : property.apply(object.getValue()).getValue());

        addChangeListener(object, value -> {
            r.unbind();

            if (value == null)
            {
                r.setValue(def);
                // r is left unbound;
            }
            else
            {
                // Bind r to the inner property, until object changes:
                r.bind(property.apply(value));
            }
        });

        return r;
    }

    /**
     * Creates a Boolean property which tracks the value of the given source property, but adds
     * delays to copying the true and/or false values.
     *
     * It is possible, if the value keeps flipping backwards and forwards during the delay windows,
     * that you may not see the change copied for a long time.
     *
     * @param source The boolean value to copy into the returned property
     * @param delayToTrue The delay to wait before copying a true value.  Pass Duration.ZERO for no delay.
     * @param delayToFalse The delay to wait before copying a false value.  Pass Duration.ZERO for no delay.
     * @return A property as described.
     */
    public static ReadOnlyBooleanProperty delay(ObservableBooleanValue source, Duration delayToTrue, Duration delayToFalse)
    {
        SimpleBooleanProperty delayed = new SimpleBooleanProperty(source.get());

        source.addListener(new ChangeListener<Boolean>()
        {
            // The task to cancel the previous copy:
            private FXRunnable cancel = null;
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                if (cancel != null)
                    cancel.run();
                if (newValue.booleanValue())
                    cancel = runAfter(delayToTrue, () -> delayed.set(true));
                else
                    cancel = runAfter(delayToFalse, () -> delayed.set(false));
            }
        });

        return delayed;
    }

    /**
     * A builder pattern for lists.
     */
    public static class ListBuilder<T>
    {
        private ArrayList<T> list;

        private ListBuilder(List<T> list)
        {
            this.list = new ArrayList<>(list);
        }

        /**
         * Add another item to the list, return this builder again.
         */
        public ListBuilder<T> add(T t)
        {
            list.add(t);
            return this;
        }

        /**
         * Turn the builder into an actual (unmodifiable) list
         */
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

    /**
     * Makes a MenuItem with the given parameters.
     * 
     * @param text The content (label) of the menu item
     * @param run The action to run for the menu item.  May be null for none.
     * @param shortcut The shortcut to use.  May be null for none.
     */
    public static MenuItem makeMenuItem(String text, FXRunnable run, KeyCombination shortcut)
    {
        MenuItem item = new MenuItem(text);
        if (run != null)
            item.setOnAction(e -> run.run());
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    /**
     * Makes a MenuItem with the given parameters.
     *
     * @param text The content (label) of the menu item, which will be bound to
     * @param run The action to run for the menu item.  May be null for none.
     * @param shortcut The shortcut to use.  May be null for none.
     */
    public static MenuItem makeMenuItem(StringExpression text, FXRunnable run, KeyCombination shortcut)
    {
        MenuItem item = new MenuItem();
        item.textProperty().bind(text);
        if (run != null)
            item.setOnAction(e -> run.run());
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    /**
     * Makes a MenuItem which is disabled
     * @param text The content (label) of the menu item
     * @param shortcut The shortcut to display.  May be null for none.
     */
    public static MenuItem makeDisabledMenuItem(String text, KeyCombination shortcut)
    {
        MenuItem item = new MenuItem(text);
        item.setDisable(true);
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    /**
     * Makes a CheckMenuItem that binds bidirectionally to the given state.
     * 
     * @param text The content (label) for the CheckMenuItem
     * @param state The boolean state to bind the check state to (bidirectionally)
     * @param shortcut The shortcut to use.  May be null for none.
     */
    
    public static CheckMenuItem makeCheckMenuItem(String text, Property<Boolean> state, KeyCombination shortcut)
    {
        CheckMenuItem item = new CheckMenuItem(text);
        item.selectedProperty().bindBidirectional(state);
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    /**
     * Helper method to construct a menu
     * @param text The content (label/header) for the menu
     * @param items The items to put in the menu (in order).
     */
    public static Menu makeMenu(String text, MenuItem... items)
    {
        Menu menu = new Menu(text);
        menu.getItems().setAll(items);
        return menu;
    }

    /**
     * Sets up code to show a tooltip on the frame catalogue when the given node is mouse-overed.
     * 
     * @param window The editor window
     * @param target The target node near which to display the tooltip
     * @param tooltipText The text of the tooltip to show
     * @param delay The delay after the mouse enters the node, before showing the toolrip.
     */
    public static void initializeCustomTooltipCatalogue(FXTabbedEditor window, Node target, String tooltipText, Duration delay)
    {
        final Label l = new Label(tooltipText);
        l.visibleProperty().bind(l.textProperty().isNotEmpty());
        l.setMouseTransparent(true);
        l.setWrapText(true);
        JavaFXUtil.addStyleClass(l, "frame-tooltip");
        JavaFXUtil.setPseudoclass("bj-tight-border", true, l);
        l.setMaxWidth(300);

        // We use the window overlay pane to show the tooltip.  The code overlay pane
        // doesn't include the catalogue, so we can't use that.
        FXRunnable show = () -> {
            final WindowOverlayPane overlayPane = window.getOverlayPane();
            double x = overlayPane.sceneXToWindowOverlayX(target.localToScene(target.getBoundsInLocal()).getMinX());
            double y = overlayPane.sceneYToWindowOverlayY(target.localToScene(target.getBoundsInLocal()).getMaxY() + 10);
            overlayPane.addOverlay(l, new ReadOnlyDoubleWrapper(x), new ReadOnlyDoubleWrapper(y), true);
            // Use short fade-in:
            FadeTransition ft = new FadeTransition(Duration.millis(100), l);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        };

        target.addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>()
            {
                // The action to cancel showing the toolip:
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
                        // Cancel any pending show,
                        if (cancel != null)
                        {
                            cancel.run();
                            cancel = null;
                        }
                        // and remove tooltip from window (no-op if not present):
                        window.getOverlayPane().removeOverlay(l);
                    }
                }
            }
        );
    }

    /**
     * A helper class for showing a tooltip/help on mouse-over or pressing F1.
     */
    @OnThread(Tag.FX)
    private static class TooltipListener implements EventHandler<InputEvent>, ChangeListener<Boolean>
    {
        /** The label displaying the tooltip. Null when not showing. */
        private Label l;
        private final InteractionManager editor;
        /** The "parent" (not actual scene graph parent, but the node we are showing a tooltip for) */
        private final Node parent;
        /**
         * This is a tricky inversion of control.  Caller passes us a requestTooltip callback.
         * We call it back with another callback for the tooltip.  Caller then spends
         * time (potentially thread-hopping), and when it has the tooltip ready, calls
         * our callback.  We can't easily simplify this because of the gap and thread-hopping
         * inbetween.
         */
        private final FXConsumer<FXConsumer<String>> requestTooltip;
        /** An action to cancel a pending show-on-hover */
        private FXRunnable cancelHoverShow = null;
        /** Whether the tooltip is currently showing */
        private boolean showing = false;
        /** Whether the tooltip is showing because of mouse hover (only valid when showing == true) */
        private boolean mouseShown = false;

        /**
         * @param l The label to use as a tooltip 
         * @param editor Reference to the editor
         * @param parent The node for which to show the tooltip
         * @param requestTooltip The callback (see comments above).
         */
        private TooltipListener(InteractionManager editor, Node parent, FXConsumer<FXConsumer<String>> requestTooltip)
        {
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
            if (showing)
                hide();

            l = new Label();
            l.visibleProperty().bind(l.textProperty().isNotEmpty());
            l.setMouseTransparent(true);
            l.setWrapText(true);
            JavaFXUtil.addStyleClass(l, "frame-tooltip");

            requestTooltip.accept(l::setText);
            editor.getCodeOverlayPane().addOverlay(l, parent, null, l.heightProperty().negate().subtract(5.0), WidthLimit.LIMIT_WIDTH_AND_SLIDE_LEFT);
            //FadeTransition ft = new FadeTransition(Duration.millis(100), l);
            //ft.setFromValue(0.0);
            //ft.setToValue(1.0);
            //ft.play();

            showing = true;
        }

        /** Called when focused value of parent changes */
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
                l = null;
                showing = false;
            }
        }

    }


    /**
     * Initialises a custom help popup for the given node.  It will show when the user presses
     * F1, and optionally when they mouse hover.
     * 
     * @param editor Reference to the editor, so we can get access to code overlay
     * @param parent The node for which to show the tooltip
     * @param requestTooltip Calculation of tooltip, which we will pass a result-consuming tooltip
     * @param onHoverToo Whether to show on mouse hover (true) and F1, or just on F1 (false)
     */
    public static void initializeCustomHelp(InteractionManager editor, Node parent, FXConsumer<FXConsumer<String>> requestTooltip, boolean onHoverToo)
    {
        TooltipListener listener = new TooltipListener(editor, parent, requestTooltip);
        parent.addEventHandler(KeyEvent.KEY_PRESSED, listener);
        parent.focusedProperty().addListener(listener);
        if (onHoverToo)
        {
            parent.addEventHandler(MouseEvent.MOUSE_ENTERED, listener);
            parent.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
        }
    }
    
    /** 
     * Waits for the observable value to become non-null, then calls the given function on the value once.
     *
     * @param observable The value to wait to become non-null.
     * @param callback The callback to call with the non-null value.  If
     *                 observable's value is already non-null, called immediately before returning
     */
    public static <T6> void onceNotNull(ObservableValue<T6> observable, FXConsumer<T6> callback)
    {
        T6 t = observable.getValue();
        
        if (t != null)
        {
            callback.accept(t);
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
                    callback.accept(newVal);
                    observable.removeListener(this);
                }
            }
        };
        observable.addListener(listener);
        
    }

    /**
     * "Binds" the destination list to the observable source list with a transformation function applied.
     * Whenever the source list changes, the destination list is altered to match by applying
     * the given function to each element in the source list.
     * 
     * @param dest The destination list to copy to
     * @param src The source list to copy from
     * @param func The function to apply to each item from src, when copying into dest.
     * @param changeWrapper A callback which will be passed the change function.  You can use this,
     *                      for example, to acquire and release a lock before/after list modification of dest
     */
    public static <SRC2, DEST2> void bindMap(List<DEST2> dest, ObservableList<SRC2> src, Function<SRC2, DEST2> func, FXConsumer<FXRunnable> changeWrapper)
    {
        changeWrapper.accept(() -> {
            dest.clear();
            dest.addAll(Utility.mapList(src, func));
        });
        
        src.addListener(new ListChangeListener<SRC2>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends SRC2> changes)
            {
                changeWrapper.accept(() -> {
                    while (changes.next())
                    {
                        if (changes.wasPermutated() || changes.wasUpdated())
                        {
                            // Same code for updated, replaced and permutation, just recalc the range:
                            for (int i = changes.getFrom(); i < changes.getTo(); i++)
                                dest.set(i, func.apply(src.get(i)));
                        } else
                        {
                            for (int i = 0; i < changes.getRemovedSize(); i++)
                                dest.remove(changes.getFrom());
                            for (int i = 0; i < changes.getAddedSubList().size(); i++)
                                dest.add(i + changes.getFrom(), func.apply(changes.getAddedSubList().get(i)));
                        }
                    }
                });
            }
        });
    }

    /**
     * When the given future completes, calls the given callback on the FX thread
     * with the value.
     * 
     * This function is asynchronous; it does not block, but rather uses a background thread.
     * 
     * @param future The future to wait for completion on
     * @param andThen The callback to pass the completed value to, on the FX thread.
     * @param <T> The type inside the future.
     */
    public static <T> void bindFuture(Future<T> future, FXConsumer<T> andThen)
    {
        Utility.runBackground(() -> {
            try
            {
                T x = future.get();
                Platform.runLater(() -> andThen.accept(x));
            }
            catch (ExecutionException | InterruptedException e)
            {
                Debug.reportError(e);
            }
            
        });
    }    
    
    /**
     * Takes a BooleanProperty and a list of item.  Gives back an observable list that contains
     * the list of items when (and only when) the BooleanProperty is true, but is empty in the case
     * that the BooleanProperty is false.  Uses JavaFX bindings to update the list's contents.
     *
     * Note that if no reference is maintained to the BooleanExpression, it can get GC-ed,
     * in which case this methods will no longer update the list.  This may be what you want (once
     * the property is no longer in use, this listener will get GC-ed too), but if you don't then
     * make sure you store a reference to the putInList expression.
     * 
     * @param putInList Whether the list should contain the items (true expression) or be empty (false expression)
     * @param items The items to put in the list when putInList is true
     * @return The ObservableList which will (or will not) contain the items.
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
     * 
     * @param prop The observable value
     * @param callback A listener, to be called at most once, on the first change of "prop"
     * @return An action which, if run, removes this listener.  If the listener has already
     *         been run once, has no effect.
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
     * 
     * This is effectively identical to binding on non-lists, but FX doesn't contain built-in
     * support for binding lists, so we do it ourselves.
     * 
     * @param dest The list to copy to
     * @param src The list to copy from
     * @return An action which, if run, cancels this binding effect.
     */
    public static <T> FXRunnable bindList(ObservableList<T> dest, ObservableList<T> src)
    {
        ListChangeListener<T> obs = c -> dest.setAll(src);
        src.addListener(obs);
        dest.setAll(src);
        return () -> src.removeListener(obs);
    }

    /**
     * Helper method for ObservableValue.addChangeListener.  It's very rare
     * that you care about all three parameters to addChangeListener (the change,
     * the old value and the new value).  Usually, you just want the new value.
     * This method lets you specify a lambda which only takes the new value,
     * and thus saves you having two unused parameters every time.
     * 
     * @param property The observable item to add the change listener to.
     *                 Must only ever be altered on the FX thread.
     * @param listener The listener to add; whenever there is a change in the property,
     *                 the listener will be called back with the new value.
     * @param <T>      The type in the observable.
     * @return An action which, if run, removes the change listener from the property.
     */
    public static <T> FXRunnable addChangeListener(ObservableValue<T> property, FXConsumer<T> listener)
    {
        ChangeListener<T> wrapped = (a, b, newVal) -> listener.accept(newVal);
        property.addListener(wrapped);
        return () -> property.removeListener(wrapped);
    }

    /**
     * Creates a single FXRunnable which runs the given list of actions in order,
     * one after the other, on the FX thread.
     */
    public static FXRunnable sequence(FXRunnable... actions)
    {
        return () -> Arrays.stream(actions).forEach(FXRunnable::run);
    }

    /**
     * Runs the given task on the FX Platform thread after the given delay.
     * Returns a task which, if executed, cancels the task.  This can be done synchronously,
     * since the task runs on the FX thread and so does this call; you cannot make this call
     * during an execution of the task.
     * 
     * @param delay The delay before running the task (if zero or less, task is run before returning)
     * @param task The task to run (on the FX thread)
     * @return An action which, if run, cancels the execution of the task.  If the task
     *         has already run, running this returned item has no effect.
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
     * Returns an action which, if executed, cancels the task.  This can be done synchronously,
     * since the task runs on the FX thread and so does this call; you cannot make this call
     * during an execution of the task.
     * 
     * @param interval The interval between tasks, and from now until the first run.
     *                 Must be greater than zero
     * @param task     The task to run (on the FX thread)
     * @return An action which, if executed, will cancel all future executions of the task.
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

    /**
     * Checks if the drag-copy shortcut was held down during a particular mouse event.
     * 
     * On Mac this is the option key.  On Windows and Linux, this is the Ctrl key.
     */
    public static boolean isDragCopyKeyPressed(MouseEvent event)
    {
        return Config.isMacOS() ? event.isAltDown() : event.isShortcutDown();
    }

    /**
     * Binds the pseudo-classes on the given node to the given set.
     * 
     * This translates into adding/removing classes as they are added/removed
     * from the given set, as well as adding all classes from the set initially.
     * 
     * If there is an orthogonal class, i.e. one never used at all in the from set,
     * it is valid to set that class on/off on the "to" node before or after this binding call.
     * 
     * @param to The node on which to set the pseudo-classes
     * @param from The set of pseudo-classes to copy from
     */
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
