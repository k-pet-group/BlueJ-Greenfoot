package bluej.editor.stride;

import java.util.List;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import bluej.utility.javafx.JavaFXUtil;

/**
 * Created by neil on 29/06/2016.
 */
class BorderPaneWithHighlightColor extends BorderPane
{
    private final CssMetaData<BorderPaneWithHighlightColor, Color> COLOR_META_DATA =
        JavaFXUtil.cssColor("-bj-highlight-color", BorderPaneWithHighlightColor::cssHighlightColorProperty);
    private final SimpleStyleableObjectProperty<Color> cssHighlightColorProperty = new SimpleStyleableObjectProperty<Color>(COLOR_META_DATA);
    private final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList =
        JavaFXUtil.extendCss(BorderPane.getClassCssMetaData())
            .add(COLOR_META_DATA)
            .build();

    public final SimpleStyleableObjectProperty<Color> cssHighlightColorProperty()
    {
        return cssHighlightColorProperty;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData()
    {
        return cssMetaDataList;
    }

}
