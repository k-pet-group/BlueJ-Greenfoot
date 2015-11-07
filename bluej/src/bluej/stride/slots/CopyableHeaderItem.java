package bluej.stride.slots;

import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import bluej.stride.generic.InteractionManager;

/**
 * Created by neil on 29/10/2015.
 */
public interface CopyableHeaderItem
{
    Stream<? extends Node> makeDisplayClone(InteractionManager editor);
}
