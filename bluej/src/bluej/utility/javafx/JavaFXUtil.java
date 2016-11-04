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

import java.awt.AWTKeyStroke;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import bluej.editor.stride.CodeOverlayPane.WidthLimit;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
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
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import bluej.editor.stride.FXTabbedEditor;
import bluej.editor.stride.WindowOverlayPane;
import bluej.pkgmgr.target.ClassTarget;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Debug;
import bluej.utility.Utility;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

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
     * Like addStyleClass but also returns the node.  Useful as part of a builder
     * pattern, when you want to add a style-class for a new node, but you don't
     * otherwise need to store it in a variable before passing it to a method, e.g
     * pane.setTop(JavaFXUtil.withStyleClass(new Label("Information:"), "label-info"));
     */
    public static <T extends Styleable> T withStyleClass(T node, String... styleClasses)
    {
        addStyleClass(node, styleClasses);
        return node;
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
            private FXPlatformRunnable cancel = null;
            @Override
            // Assume we are on FX thread (could be dangerous)
            // Tag lets us circumvent thread checker:
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
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

    @OnThread(Tag.FX)
    public static void ifOnPlatform(FXPlatformRunnable action)
    {
        if (Platform.isFxApplicationThread())
        {
            // Circumvent thread checker:
            ((Runnable)action::run).run();
        }
    }

    public static void addFocusListener(Stage focusTarget, FXPlatformConsumer<Boolean> listener)
    {
        // We know focused property can only be altered on FX thread:
        focusTarget.focusedProperty().addListener((obs, oldVal, newVal) ->
                // Circumvent thread checker:
                ((FXConsumer<Boolean>)listener::accept).accept(newVal));
    }

    public static void addFocusListener(Node focusTarget, FXPlatformConsumer<Boolean> listener)
    {
        // We know focused property can only be altered on FX thread:
        focusTarget.focusedProperty().addListener((obs, oldVal, newVal) ->
                // Circumvent thread checker:
                ((FXConsumer<Boolean>)listener::accept).accept(newVal));
    }

    @OnThread(Tag.Any)
    public static void runNowOrLater(FXPlatformRunnable action)
    {
        if (Platform.isFxApplicationThread())
            // Circumvent thread checker (nasty!)
            ((Runnable)action::run).run();
        else
            Platform.runLater(action::run);
    }

    /**
     * Shows a dialog to the user (with OK/Cancel buttons) asking them to confirm an action
     * 
     * @param titleLabel The string to look up in the labels file for the title of the dialog
     * @param messageLabel The string to look up in the labels file for the message of the dialog
     * @return True if the user clicked OK, false if the user clicked Cancel or otherwise closed the dialog.
     */
    @OnThread(Tag.FXPlatform)
    public static boolean confirmDialog(String titleLabel, String messageLabel, Stage parent)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, Config.getString(messageLabel), ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(Config.getString(titleLabel));
        alert.setHeaderText(alert.getTitle());
        alert.initOwner(parent);
        alert.initModality(Modality.WINDOW_MODAL);
        Optional<ButtonType> pressed = alert.showAndWait();
        return ButtonType.OK == pressed.orElse(ButtonType.CANCEL);
    }

    /**
     * Shows an error dialog to the user, with an OK button
     * 
     * 
     */
    @OnThread(Tag.FXPlatform)
    public static void errorDialog(String titleLabel, String messageLabel)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR, Config.getString(messageLabel), ButtonType.OK);
        alert.setTitle(Config.getString(titleLabel));
        alert.setHeaderText(alert.getTitle());
        alert.showAndWait();
    }

    /**
     * Binds the on/off state of a pseudo-class to the given binding (true = pseudo-class present).
     * 
     * Note that if no reference is maintained to the BooleanBinding, it can get GC-ed,
     * in which case this method will no longer update the pseudo-class.  This is unlikely
     * to be what you want, so you should retain a reference to the exact binding passed
     * to this method for as long as you want the update to occur.
     * 
     * @param node
     * @param pseudoClass
     * @param on
     */
    public static void bindPseudoclass(Node node, String pseudoClass, BooleanExpression on)
    {
        addChangeListener(on, b -> setPseudoclass(pseudoClass, b, node));
    }

    public static void scrollTo(ScrollPane scrollPane, Node target)
    {
        double scrollWidth = scrollPane.getContent().getBoundsInLocal().getWidth();
        double scrollHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
        
        Bounds b = scrollPane.getContent().sceneToLocal(target.localToScene(target.getBoundsInLocal()));
        
        scrollPane.setHvalue(b.getMinX() / scrollWidth);
        scrollPane.setVvalue(b.getMinY() / scrollHeight);
    }

    /**
     * Runs some FX initialisation on any thread.
     *
     * We probably need to revisit the threading model.  Nowadays, FXPlatform means actually
     * on the FX Platform thread, and FX means messing with FX, but this can happen in a background
     * thread to support faster loading, as long as you don't add it to the scene.  But really,
     * this means Tag.FX code should be executable on the Swing event thread, because
     * there's no problem if you're just loading.  But if we let Tag.Swing call Tag.FX
     * then we're going to mask all sorts of problems in existing code at the moment.
     * So I've made this method for now, which lets you initialise FX items on
     * the Swing thread, but at least makes it explicit that you're meaning to do it.
     *
     * @param initCode
     * @param <T>
     * @return
     */
    @OnThread(Tag.Any)
    public static <T> T initFX(FXSupplier<T> initCode)
    {
        // Defeat thread checker:
        return ((Supplier<T>)initCode::get).get();
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
    public static void listenForContextMenu(Node node, FXPlatformBiFunction<Double, Double, Boolean> showContextMenu, KeyCode... otherKeys)
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
            if (e.getCode() == KeyCode.CONTEXT_MENU || Arrays.asList(otherKeys).contains(e.getCode()))
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
    public static MenuItem makeMenuItem(String text, FXPlatformRunnable run, KeyCombination shortcut)
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
    public static MenuItem makeMenuItem(StringExpression text, FXPlatformRunnable run, KeyCombination shortcut)
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
                FXPlatformRunnable cancel;

                @Override
                @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
    private static class TooltipListener implements EventHandler<InputEvent>, FXPlatformConsumer<Boolean>
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
        private FXPlatformRunnable cancelHoverShow = null;
        /** Whether the tooltip is currently showing */
        private boolean showing = false;
        /** Whether the tooltip is showing because of mouse hover (only valid when showing == true) */
        private boolean mouseShown = false;

        /**
         * @param editor Reference to the editor
         * @param parent The node for which to show the tooltip
         * @param requestTooltip The callback (see comments above).
         */
        @OnThread(Tag.FX)
        private TooltipListener(InteractionManager editor, Node parent, FXConsumer<FXConsumer<String>> requestTooltip)
        {
            this.editor = editor;
            this.parent = parent;
            this.requestTooltip = requestTooltip;
        }

        @Override
        @OnThread(Tag.FXPlatform)
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

        @OnThread(Tag.FXPlatform)
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
        @OnThread(Tag.FXPlatform)
        public void accept(Boolean newVal)
        {

            if (newVal.booleanValue() == false && showing && !mouseShown)
            {
                hide();
            }            
        }

        @OnThread(Tag.FXPlatform)
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
        addFocusListener(parent, listener);
        if (onHoverToo)
        {
            parent.addEventHandler(MouseEvent.MOUSE_ENTERED, listener);
            parent.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
        }
    }

    /**
     * Runs the given action (on the FX Platform thread) once the given node
     * gets added to the scene.
     */
    public static void onceInScene(Node node, FXPlatformRunnable action)
    {
        // Fairly sure scene property can only change on FX thread, but no
        // harm using runNowOrLater:
        onceNotNull(node.sceneProperty(), s -> runNowOrLater(action));
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
     * Waits for the observable value to become true, then calls the given function on the value once.
     *
     * @param observable The value to wait to become true.
     * @param callback The callback to call with the true value.
     *                 If observable's value is already true, call immediately before returning
     */
    public static void onceTrue(ObservableValue<Boolean> observable, FXConsumer<Boolean> callback)
    {
        boolean value = observable.getValue();

        if (value)
        {
            callback.accept(value);
            return;
        }

        // Can't be a lambda because we need a reference to ourselves to self-remove:
        ChangeListener<Boolean> listener = new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
                                Boolean oldValue, Boolean newVal)
            {
                if (newVal)
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
     * This function is asynchronous; it does not block.
     * 
     * @param future The future to wait for completion on
     * @param andThen The callback to pass the completed value to, on the FX thread.
     * @param <T> The type inside the future.
     */
    public static <T> void bindFuture(CompletableFuture<T> future, FXPlatformConsumer<T> andThen)
    {
        future.thenAccept(x -> JavaFXUtil.runPlatformLater(() -> andThen.accept(x)));
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
    public static <T> FXRunnable bindList(ObservableList<? super T> dest, ObservableList<T> src)
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
    public static <T> FXRunnable addChangeListener(ObservableValue<T> property, FXConsumer<? super T> listener)
    {
        ChangeListener<T> wrapped = (a, b, newVal) -> listener.accept(newVal);
        property.addListener(wrapped);
        return () -> property.removeListener(wrapped);
    }

    /** Like addChangeListener, but for when the item will definitely only be changed on the platform thread */
    @OnThread(Tag.FXPlatform)
    public static <T> FXPlatformRunnable addChangeListenerPlatform(ObservableValue<T> property, FXPlatformConsumer<T> listener)
    {
        // Defeat thread checker:
        ChangeListener<T> wrapped = (a, b, newVal) -> ((FXConsumer<T>)listener::accept).accept(newVal);
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
    @OnThread(Tag.FXPlatform)
    public static FXPlatformRunnable runAfter(Duration delay, FXPlatformRunnable task)
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
    
    public static enum DragType
    {
        /** The copy key (Mac: option, Others: Ctrl) was held down */
        FORCE_COPYING,
        /** The move key (shift) was held down */
        FORCE_MOVING,
        /** No modifier was held down */
        DEFAULT;
    }

    /**
     * Checks if the drag-copy or drag-move modifiers was held down during a particular mouse event.
     * 
     * Drag-copy: On Mac this is the option key.  On Windows and Linux, this is the Ctrl key.
     * Drag-move: The shift key.
     */
    public static DragType getDragModifiers(MouseEvent event)
    {
        boolean forceCopy = Config.isMacOS() ? event.isAltDown() : event.isShortcutDown();
        boolean forceMove = event.isShiftDown();
        if (forceCopy)
            return DragType.FORCE_COPYING;
        else if (forceMove)
            return DragType.FORCE_MOVING;
        else
            return DragType.DEFAULT;
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

    /**
     * Converts a Swing menu bar into a JavaFX menu bar.  For use when you convert
     * a Swing window into a SwingNode inside a JavaFX menu, but the Swing window
     * had a menu bar.  All the Swing actions are executed back on the Swing thread,
     * using the given eventSource in the ActionEvent
     *
     * @param swingMenu The swing menu to convert.
     * @param eventSource The source to be used in ActionEvent items which are passed to actionPerformed
     * @return An action to run on the FX thread which will create the FX menu
     */
    @OnThread(Tag.Swing)
    public static FXPlatformSupplier<MenuBar> swingMenuBarToFX(JMenuBar swingMenu, Object eventSource)
    {
        List<FXPlatformSupplier<Menu>> menus = new ArrayList<>();

        for (int i = 0; i < swingMenu.getMenuCount(); i++)
        {
            JMenu menu = swingMenu.getMenu(i);
            menus.add(swingMenuToFX(menu, eventSource, (a, b) -> {}));
        }
        return () -> {
            MenuBar menuBar = new MenuBar();
            for (FXPlatformSupplier<Menu> menuFXSupplier : menus)
            {
                menuBar.getMenus().add(menuFXSupplier.get());
            }
            return menuBar;
        };
    }
    
    public static interface SwingOrFXMenu
    {
        @OnThread(Tag.Swing)
        FXPlatformSupplier<Menu> getFXMenu(Object eventSource);
    }
    public static class SwingMenu implements SwingOrFXMenu
    {
        private final JMenu swingMenu;

        @OnThread(Tag.Any)
        public SwingMenu(JMenu swingMenu)
        {
            this.swingMenu = swingMenu;
        }

        @Override
        @OnThread(Tag.Swing)
        public FXPlatformSupplier<Menu> getFXMenu(Object eventSource)
        {
            return swingMenuToFX(swingMenu, eventSource, (a, b) -> {});
        }
    }
    public static class FXOnlyMenu implements SwingOrFXMenu
    {
        private final Menu fxMenu;

        @OnThread(Tag.Any)
        public FXOnlyMenu(Menu fxMenu)
        {
            this.fxMenu = fxMenu;
        }

        @Override
        @OnThread(Tag.Swing)
        public FXPlatformSupplier<Menu> getFXMenu(Object eventSource)
        {
            return () -> fxMenu;
        }
    }
    public static class FXPlusSwingMenu implements SwingOrFXMenu
    {
        private final FXPlatformSupplier<Menu> createFXMenu;
        private final List<JMenuItem> swingItems = new ArrayList<>();
        private final List<FXPlatformSupplier<MenuItem>> fxItems = new ArrayList<>();
        private final List<Integer> fxItemIndexes = new ArrayList<>();
        @OnThread(Tag.Swing)
        private int nextIndex = 0;
        private Runnable atEnd;

        @OnThread(Tag.Swing)
        public FXPlusSwingMenu(FXPlatformSupplier<Menu> createMenu)
        {
            this.createFXMenu = createMenu;
        }

        @Override
        @OnThread(Tag.Swing)
        public FXPlatformSupplier<Menu> getFXMenu(Object eventSource)
        {
            // So swingMenuToFX is built to add a single list of Swing items
            // to an FX menu in one go, but we want to interleave.  Rather than
            // gut swingMenuToFX to allow interleaving, we just add all the
            // swing items in one go, then interleave the FX items afterwards:
            FXPlatformSupplier<Menu> swingAdd = swingMenuToFX(swingItems, eventSource, createFXMenu, (a, b) -> {});
            return () -> {
                Menu fxMenu = swingAdd.get();
                for (int i = 0; i < fxItems.size(); i++)
                {
                    MenuItem item = fxItems.get(i).get();
                    fxMenu.getItems().add(fxItemIndexes.get(i), item);
                }
                if (atEnd != null)
                {
                    SwingUtilities.invokeLater(atEnd);
                }
                return fxMenu;
            };
        }

        @OnThread(Tag.Swing)
        public void addFX(FXPlatformSupplier<MenuItem> fxItem)
        {
            fxItems.add(fxItem);
            fxItemIndexes.add(nextIndex++);
        }

        @OnThread(Tag.Swing)
        public void addSwing(List<JMenuItem> swingItems)
        {
            this.swingItems.addAll(swingItems);
            nextIndex += swingItems.size();
        }

        @OnThread(Tag.Swing)
        public void runAtEnd(Runnable runnable)
        {
            atEnd = runnable;
        }
    }

    @OnThread(Tag.Swing)
    public static FXPlatformSupplier<MenuBar> swingMenuBarToFX(List<SwingOrFXMenu> mixedMenus, Object eventSource)
    {
        List<FXPlatformSupplier<Menu>> menus = mixedMenus.stream().map(m -> m.getFXMenu(eventSource)).collect(Collectors.toList());
        
        return () -> {
            MenuBar menuBar = new MenuBar();
            for (FXPlatformSupplier<Menu> menuFXSupplier : menus)
            {
                menuBar.getMenus().add(menuFXSupplier.get());
            }
            return menuBar;
        };
    }
    @OnThread(Tag.Swing)
    private static FXPlatformSupplier<Menu> swingMenuToFX(JMenu swingMenu, Object source, FXBiConsumer<JMenuItem, MenuItem> extraConvert)
    {
        List<JMenuItem> items = new ArrayList<>();
        for (int i = 0; i < swingMenu.getItemCount(); i++)
        {
            items.add(swingMenu.getItem(i));
        }
        String swingMenuTitle = swingMenu.getText();
        return swingMenuToFX(items, source, () -> new Menu(swingMenuTitle), extraConvert);
    }

    /**
     * Helper method for swingMenuBarToFX, see docs for that method
     */
    @OnThread(Tag.Swing)
    public static FXPlatformSupplier<Menu> swingMenuToFX(List<JMenuItem> swingMenuItems, Object source, FXPlatformSupplier<Menu> createMenu, FXBiConsumer<JMenuItem, MenuItem> extraConvert)
    {
        List<FXPlatformSupplier<? extends MenuItem>> items = new ArrayList<>();
        List<FXPlatformRunnable> onShow = new ArrayList<>();
        for (JMenuItem item : swingMenuItems)
        {
            if (item == null)
                // Separator:
                items.add(createMenuSeparator());
            else if (item instanceof JMenu)
                items.add(swingMenuToFX((JMenu)item, source, extraConvert));
            else
            {
                FXPlatformSupplier<MenuItemAndShow> menuItemAndShowFXSupplier = swingMenuItemToFX(item, source);
                FXPlatformSupplier<MenuItem> menuItemFXSupplier = () -> {
                    MenuItemAndShow itemAndShow = menuItemAndShowFXSupplier.get();
                    if (itemAndShow.onShow != null)
                        onShow.add(itemAndShow.onShow);
                    extraConvert.accept(item, itemAndShow.menuItem);
                    return itemAndShow.menuItem;
                };
                items.add(menuItemFXSupplier);
            }
        }
        return () -> {
            Menu menu = createMenu.get();
            for (FXPlatformSupplier<? extends MenuItem> menuItemFXSupplier : items)
            {
                menu.getItems().add(menuItemFXSupplier.get());
            }
            // Note: it is the supplier
            // which adds all the updates to the onShow list:
            menu.setOnShowing(e -> {
                onShow.forEach(FXPlatformRunnable::run);
            });
            return menu;
        };
    }
    
    @OnThread(Tag.Swing)
    public static FXPlatformRunnable swingMenuItemsToContextMenu(ContextMenu destination, List<JMenuItem> swingItems, Object source, FXBiConsumer<JMenuItem, MenuItem> extraConvert)
    {
        List<FXPlatformSupplier<? extends MenuItem>> items = new ArrayList<>();
        List<FXPlatformRunnable> onShow = new ArrayList<>();
        for (JMenuItem item : swingItems)
        {
            if (item == null)
                // Separator:
                items.add(createMenuSeparator());
            else if (item instanceof JMenu)
                items.add(swingMenuToFX((JMenu)item, source, extraConvert));
            else
            {
                FXPlatformSupplier<MenuItemAndShow> menuItemAndShowFXSupplier = swingMenuItemToFX(item, source);
                FXPlatformSupplier<MenuItem> menuItemFXSupplier = () -> {
                    MenuItemAndShow itemAndShow = menuItemAndShowFXSupplier.get();
                    if (itemAndShow.onShow != null)
                        onShow.add(itemAndShow.onShow);
                    extraConvert.accept(item, itemAndShow.menuItem);
                    return itemAndShow.menuItem;
                };
                items.add(menuItemFXSupplier);
            }
        }
        return () -> {
            for (FXPlatformSupplier<? extends MenuItem> menuItemFXSupplier : items)
            {
                destination.getItems().add(menuItemFXSupplier.get());
            }
            // Note: it is the supplier
            // which adds all the updates to the onShow list:
            destination.addEventHandler(WindowEvent.WINDOW_SHOWING, e -> {
                onShow.forEach(FXPlatformRunnable::run);
            });
        };
    }

    @OnThread(Tag.Any)
    private static FXPlatformSupplier<? extends MenuItem> createMenuSeparator()
    {
        return () -> new SeparatorMenuItem();
    }

    private static class MenuItemAndShow
    {
        private final MenuItem menuItem;
        /**
         * Action to run when the menu containing this item is shown:
         */
        private final FXRunnable onShow;

        public MenuItemAndShow(MenuItem menuItem, FXRunnable onShow)
        {
            this.menuItem = menuItem;
            this.onShow = onShow;
        }

        public MenuItemAndShow(MenuItem menuItem)
        {
            this(menuItem, null);
        }
    }

    /**
     * Helper method for swingMenuBarToFX, see docs for that method
     */
    @OnThread(Tag.Swing)
    private static FXPlatformSupplier<MenuItemAndShow> swingMenuItemToFX(JMenuItem swingItem, Object source)
    {
        String menuText = swingItem.getText();
        ActionListener[] actionListeners = swingItem.getActionListeners();
        AWTKeyStroke shortcut = swingItem.getAccelerator();
        if (swingItem instanceof JCheckBoxMenuItem)
        {
            JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) swingItem;
            boolean selected = checkBoxMenuItem.isSelected();
            return () -> {
                CheckMenuItem item = new CheckMenuItem(menuText);
                item.setSelected(selected);
                // Clicking the icon crosses back to the Swing thread to attempt state change:
                item.setOnAction(e -> {
                    SwingUtilities.invokeLater(() -> checkBoxMenuItem.setSelected(!checkBoxMenuItem.isSelected()));
                });
                item.setAccelerator(swingKeyStrokeToFX(shortcut));
                // We must also check the state (on Swing thread) before showing, because it may
                // have been altered by other actions, e.g. the Show Terminal menu item is affected
                // by the user closing the terminal window, not just by triggering the menu item
                FXRunnable getStatus = () -> {
                    SwingUtilities.invokeLater(() -> {
                        boolean curSelected = checkBoxMenuItem.isSelected();
                        Platform.runLater(() -> item.setSelected(curSelected));
                    });
                };
                SwingUtilities.invokeLater(() -> {
                    swingItem.addPropertyChangeListener("enabled", e2 -> {
                        boolean enabled = swingItem.isEnabled();
                        Platform.runLater(() -> item.setDisable(!enabled));
                    });
                });
                return new MenuItemAndShow(item, getStatus);
            };
        }
        else if (swingItem instanceof JRadioButtonMenuItem)
        {
            // We don't have any radio button menu items in our code currently
            throw new UnsupportedOperationException();
        }
        else
        {
            boolean startsEnabled = swingItem.isEnabled();
            // Circumvent thread-checker; it is ok to make an FX menu item on this
            // thread as long as we don't try to add it to scene graph:
            MenuItem item = ((BiFunction<String, Boolean, MenuItem>)JavaFXUtil::makeFXMenuItem).apply(menuText, startsEnabled);
            // Must add listeners now, because if change occurs between us returning an FX action,
            // and the action actually being run, we could miss it.
            swingItem.addPropertyChangeListener("enabled", e2 -> {
                boolean enabled = swingItem.isEnabled();
                Platform.runLater(() -> item.setDisable(!enabled));
            });
            swingItem.addPropertyChangeListener("action", e2 -> {
                boolean enabled = swingItem.isEnabled();
                String label = swingItem.getText();
                Platform.runLater(() -> {
                    item.setDisable(!enabled);
                    item.setText(label);
                });
            });
            
            return () -> {
                item.setOnAction(e -> {
                    SwingUtilities.invokeLater(() -> {
                        // Important we fetch action here not cache it earlier because
                        // it may change after we make the menu item:
                        Action action = swingItem.getAction();
                        if (action != null)
                            action.actionPerformed(new java.awt.event.ActionEvent(source, 0, menuText));
                        else
                            // If no action, just call action listeners ourselves:
                            Arrays.stream(actionListeners).forEach(a -> a.actionPerformed(new java.awt.event.ActionEvent(source, 0, menuText)));
                    });
                });
                item.setAccelerator(swingKeyStrokeToFX(shortcut));
                return new MenuItemAndShow(item);
            };
        }
    }

    private static MenuItem makeFXMenuItem(String menuText, boolean startsEnabled)
    {
        MenuItem item = new MenuItem(menuText);
        item.setDisable(!startsEnabled);
        return item;
    }

    /**
     * Converts an AWT key stroke combination to a JavaFX one.
     */
    private static KeyCombination swingKeyStrokeToFX(AWTKeyStroke shortcut)
    {
        if (shortcut == null)
            return null;

        int modifiers = shortcut.getModifiers();
        List<Modifier> fxModifiers = new ArrayList<>();
        if ((modifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0)
            fxModifiers.add(KeyCombination.SHIFT_DOWN);
        if ((modifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0)
            fxModifiers.add(KeyCombination.CONTROL_DOWN);
        if ((modifiers & java.awt.event.InputEvent.META_DOWN_MASK) != 0)
            fxModifiers.add(KeyCombination.META_DOWN);
        // We don't support other modifiers on menu items: e.g. Alt as a
        // menu item shortcut is not supported because it doesn't work on some OSes
        KeyCode code = awtKeyCodeToFX(shortcut.getKeyCode());
        if (code != null)
            return new KeyCodeCombination(code, fxModifiers.toArray(new KeyCombination.Modifier[0]));
        else
            return null;
    }

    /**
     * Converts an integer AWT key code to a JavaFX enum.
     */
    private static KeyCode awtKeyCodeToFX(int code)
    {
        // There may be a better way of doing this:
        switch (code)
        {
            case java.awt.event.KeyEvent.VK_A: return KeyCode.A;
            case java.awt.event.KeyEvent.VK_B: return KeyCode.B;
            case java.awt.event.KeyEvent.VK_C: return KeyCode.C;
            case java.awt.event.KeyEvent.VK_D: return KeyCode.D;
            case java.awt.event.KeyEvent.VK_E: return KeyCode.E;
            case java.awt.event.KeyEvent.VK_F: return KeyCode.F;
            case java.awt.event.KeyEvent.VK_G: return KeyCode.G;
            case java.awt.event.KeyEvent.VK_H: return KeyCode.H;
            case java.awt.event.KeyEvent.VK_I: return KeyCode.I;
            case java.awt.event.KeyEvent.VK_J: return KeyCode.J;
            case java.awt.event.KeyEvent.VK_K: return KeyCode.K;
            case java.awt.event.KeyEvent.VK_L: return KeyCode.L;
            case java.awt.event.KeyEvent.VK_M: return KeyCode.M;
            case java.awt.event.KeyEvent.VK_N: return KeyCode.N;
            case java.awt.event.KeyEvent.VK_O: return KeyCode.O;
            case java.awt.event.KeyEvent.VK_P: return KeyCode.P;
            case java.awt.event.KeyEvent.VK_Q: return KeyCode.Q;
            case java.awt.event.KeyEvent.VK_R: return KeyCode.R;
            case java.awt.event.KeyEvent.VK_S: return KeyCode.S;
            case java.awt.event.KeyEvent.VK_T: return KeyCode.T;
            case java.awt.event.KeyEvent.VK_U: return KeyCode.U;
            case java.awt.event.KeyEvent.VK_V: return KeyCode.V;
            case java.awt.event.KeyEvent.VK_W: return KeyCode.W;
            case java.awt.event.KeyEvent.VK_X: return KeyCode.X;
            case java.awt.event.KeyEvent.VK_Y: return KeyCode.Y;
            case java.awt.event.KeyEvent.VK_Z: return KeyCode.Z;
            case java.awt.event.KeyEvent.VK_0: return KeyCode.DIGIT0;
            case java.awt.event.KeyEvent.VK_1: return KeyCode.DIGIT1;
            case java.awt.event.KeyEvent.VK_2: return KeyCode.DIGIT2;
            case java.awt.event.KeyEvent.VK_3: return KeyCode.DIGIT3;
            case java.awt.event.KeyEvent.VK_4: return KeyCode.DIGIT4;
            case java.awt.event.KeyEvent.VK_5: return KeyCode.DIGIT5;
            case java.awt.event.KeyEvent.VK_6: return KeyCode.DIGIT6;
            case java.awt.event.KeyEvent.VK_7: return KeyCode.DIGIT7;
            case java.awt.event.KeyEvent.VK_8: return KeyCode.DIGIT8;
            case java.awt.event.KeyEvent.VK_9: return KeyCode.DIGIT9;
            case java.awt.event.KeyEvent.VK_COMMA: return KeyCode.COMMA;
            case java.awt.event.KeyEvent.VK_PERIOD: return KeyCode.PERIOD;
            case java.awt.event.KeyEvent.VK_BACK_QUOTE: return KeyCode.BACK_QUOTE;
            case java.awt.event.KeyEvent.VK_BACK_SLASH: return KeyCode.BACK_SLASH;
            case java.awt.event.KeyEvent.VK_SLASH: return KeyCode.SLASH;
            case java.awt.event.KeyEvent.VK_TAB: return KeyCode.TAB;
            case java.awt.event.KeyEvent.VK_BACK_SPACE: return KeyCode.BACK_SPACE;
            case java.awt.event.KeyEvent.VK_F1: return KeyCode.F1;
            case java.awt.event.KeyEvent.VK_F2: return KeyCode.F2;
            case java.awt.event.KeyEvent.VK_F3: return KeyCode.F3;
            case java.awt.event.KeyEvent.VK_F4: return KeyCode.F4;
            case java.awt.event.KeyEvent.VK_F5: return KeyCode.F5;
            case java.awt.event.KeyEvent.VK_F6: return KeyCode.F6;
            case java.awt.event.KeyEvent.VK_F7: return KeyCode.F7;
            case java.awt.event.KeyEvent.VK_F8: return KeyCode.F8;
            case java.awt.event.KeyEvent.VK_F9: return KeyCode.F9;
            case java.awt.event.KeyEvent.VK_F10: return KeyCode.F10;
            case java.awt.event.KeyEvent.VK_F11: return KeyCode.F11;
            case java.awt.event.KeyEvent.VK_F12: return KeyCode.F12;
            case java.awt.event.KeyEvent.VK_F13: return KeyCode.F13;
            case java.awt.event.KeyEvent.VK_F14: return KeyCode.F14;
            case java.awt.event.KeyEvent.VK_F15: return KeyCode.F15;
            case java.awt.event.KeyEvent.VK_F16: return KeyCode.F16;
            case java.awt.event.KeyEvent.VK_F17: return KeyCode.F17;
            case java.awt.event.KeyEvent.VK_F18: return KeyCode.F18;
            case java.awt.event.KeyEvent.VK_F19: return KeyCode.F19;
            case java.awt.event.KeyEvent.VK_F20: return KeyCode.F20;
            case java.awt.event.KeyEvent.VK_F21: return KeyCode.F21;
            case java.awt.event.KeyEvent.VK_F22: return KeyCode.F22;
            case java.awt.event.KeyEvent.VK_F23: return KeyCode.F23;
            case java.awt.event.KeyEvent.VK_F24: return KeyCode.F24;
            case java.awt.event.KeyEvent.VK_SEMICOLON: return KeyCode.SEMICOLON;
            case java.awt.event.KeyEvent.VK_COLON: return KeyCode.COLON;
            case java.awt.event.KeyEvent.VK_NUMBER_SIGN: return KeyCode.NUMBER_SIGN;
            case java.awt.event.KeyEvent.VK_ENTER: return KeyCode.ENTER;
            case java.awt.event.KeyEvent.VK_INSERT: return KeyCode.INSERT;
            case java.awt.event.KeyEvent.VK_HOME: return KeyCode.HOME;
            case java.awt.event.KeyEvent.VK_DELETE: return KeyCode.DELETE;
            case java.awt.event.KeyEvent.VK_END: return KeyCode.END;
            case java.awt.event.KeyEvent.VK_PAGE_UP: return KeyCode.PAGE_UP;
            case java.awt.event.KeyEvent.VK_PAGE_DOWN: return KeyCode.PAGE_DOWN;

        }
        return null;
    }

    public static <T,R> ObjectBinding<R> of(ObservableValue<T> t, FXFunction<T, R> accessor)
    {
        return Bindings.createObjectBinding(() -> accessor.apply(t.getValue()), t);
    }

    public static <T> DoubleBinding ofD(ObservableValue<T> t, FXFunction<T, Double> accessor)
    {
        return Bindings.createDoubleBinding(() -> accessor.apply(t.getValue()), t);
    }

    /**
     * A method which runs the given action on the platform thread when it
     * later becomes available.  A specific way to call Platform.runLater
     * from the platform thread when you really mean to (thread checker will warn
     * you otherwise).
     */
    @OnThread(Tag.FXPlatform)
    public static void runAfterCurrent(FXPlatformRunnable r)
    {
        // Defeat thread checker:
        ((FXPlatformConsumer<Runnable>)(Platform::runLater)).accept(r::run);
    }

    /**
     * Runs the action on the FX platform thread (after the current action,
     * if called on the platform thread).
     *
     * Be very careful using this: race hazards ahoy if you use it from an FX
     * loading thread, expecting it to nicely take place after the current code.
     * Instead, it could run alongside the currently executing code.
     *
     * @param r
     */
    @OnThread(Tag.FX)
    public static void runPlatformLater(FXPlatformRunnable r)
    {
        Platform.runLater(r::run);
    }

    /**
     * Draw stripes over a rectangle - yet another thing missing from the AWT
     */
    public static void stripeRect(GraphicsContext g, int x, int y, int width, int height, int separation, int thickness, Color color)
    {
        Paint prev = g.getStroke();
        g.setStroke(color);
        g.setLineWidth(thickness);
        for (int offset = separation/2; offset < width + height; offset += separation + thickness) {
            int x1, y1, x2, y2;

            if (offset < height) {
                x1 = x;
                y1 = y + offset;
            }
            else {
                x1 = x + offset - height;
                y1 = y + height;
            }

            if (offset < width) {
                x2 = x + offset;
                y2 = y;
            }
            else {
                x2 = x + width;
                y2 = y + offset - width;
            }

            g.strokeLine(x1, y1, x2, y2);
        }
        g.setStroke(prev);
    }

    public static Image createImage(int width, int height, FXConsumer<GraphicsContext> draw)
    {
        Canvas c = new Canvas(width, height);
        Scene s = new Scene(new StackPane(c));
        draw.accept(c.getGraphicsContext2D());
        WritableImage image = new WritableImage(width, height);
        SnapshotParameters p = new SnapshotParameters();
        p.setFill(Color.TRANSPARENT);
        c.snapshot(p, image);
        return image;
    }
}
