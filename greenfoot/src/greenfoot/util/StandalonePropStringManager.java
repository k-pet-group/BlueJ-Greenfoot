/*
 * StandalonePropStringManager.java
 *
 * Created on 3/10/2007, 15:09:39
 *
 */

package greenfoot.util;

import bluej.BlueJPropStringSource;
import java.util.Properties;


/**
 * A standalone reader of property strings for use in exported scenarios
 * as standalone jar files
 * 
 * @author Bruce Quig
 */
public class StandalonePropStringManager implements BlueJPropStringSource
{
    private Properties values;
    
    public StandalonePropStringManager(Properties props)
    {
        values = props;
    }

    public String getBlueJPropertyString(String property, String def)
    {
       return values.getProperty(property, def);
    }

    public String getLabel(String key)
    {
        return values.getProperty(key, key);
    }

    public void setUserProperty(String property, String value)
    {
        values.setProperty(property, value);
    }
    
}