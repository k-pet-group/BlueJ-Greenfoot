package bluej.extensions.event;

/**
 * <pre>The issue here is the following. This class is desirable to put all Extensions events under the same ubmbrealla
 * So it is s logical grouper, unfortunately at the moment it cannot be anything else, the reason being
 * 1) The id of each event should be handled in the more dedicated classes (so the id disappear here) since
 *    is has a defined meaning ONLY in the specific implementation.
 * 2) The BPackage is not always present and has a meaning in certain cases (so no point to have it here)
 * 
 * It may be argued that it is nice to have a getEvent at this level, the point is that it WILL return meaningless
 * results if it is not matched with the particular class, it is therefore safere to leave it there.
 * Damiano
 * </pre>
 * @version $Id: BluejEvent.java 1689 2003-03-10 13:40:53Z damiano $
 */

public class BluejEvent 
{
    // .. Nothing to do, really, it is a logical glue
}