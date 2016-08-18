package bluej.utility;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 18/08/2016.
 */
@OnThread(Tag.Worker)
public interface BackgroundConsumer<T>
{
    public void accept(T t);
}
