package bluej.editor.moe;

import bluej.utility.javafx.FXPlatformRunnable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.value.ObservableIntegerValue;
import javafx.scene.paint.Color;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 25/04/2017.
 */
@OnThread(Tag.FX)
public interface ScopeColors
{
    public ObjectExpression<Color> scopeClassColorProperty();
    public ObjectExpression<Color> scopeClassInnerColorProperty();
    public ObjectExpression<Color> scopeClassOuterColorProperty();
    public ObjectExpression<Color> scopeMethodColorProperty();
    public ObjectExpression<Color> scopeMethodOuterColorProperty();
    public ObjectExpression<Color> scopeSelectionColorProperty();
    public ObjectExpression<Color> scopeSelectionOuterColorProperty();
    public ObjectExpression<Color> scopeIterationColorProperty();
    public ObjectExpression<Color> scopeIterationOuterColorProperty();
    public ObjectExpression<Color> scopeBackgroundColorProperty();

    public ObjectExpression<Color> breakpointOverlayColorProperty();
    public ObjectExpression<Color> stepMarkOverlayColorProperty();

    /**
     * Get a colour which has been faded toward the background according to the
     * given strength value. The higher the strength value, the less the colour
     * is faded.
     */
    public default ObjectExpression<Color> getReducedColor(ObjectExpression<Color> original, ObservableIntegerValue colorStrength)
    {
        return Bindings.createObjectBinding(() ->
        {
            Color bg = scopeBackgroundColorProperty().getValue();
            return bg.interpolate(original.getValue(), (double) colorStrength.get() / (double) ScopeHighlightingPrefDisplay.MAX);
        }, scopeBackgroundColorProperty(), colorStrength, original);
    }

    // Used for testing:
    public static ScopeColors dummy()
    {
        // Simplest thing to do is make an off-screen ScopeColorsBorderPane:
        return new ScopeColorsBorderPane();
    }
}
