package bluej.utility;

import java.util.*;

/**
 * A properties object which allows the default properties
 * to be fetched independantly of the normal calls
 *
 * @author  Andrew Patterson
 * @version $Id: DefaultProperties.java 1819 2003-04-10 13:47:50Z fisker $
 */
public class DefaultProperties extends Properties
{
    public DefaultProperties()
    {
    }

    public DefaultProperties(Properties defaults)
    {
        super(defaults);
    }

    public String getDefaultProperty(String key)
    {
        return defaults.getProperty(key);
    }

    public String getDefaultProperty(String key, String defaultValue)
    {
        return defaults.getProperty(key, defaultValue);
    }
}
