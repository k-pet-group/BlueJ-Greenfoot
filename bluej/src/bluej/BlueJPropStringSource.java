package bluej;


/**
 * Interface for a class which supports retrieving BlueJ property strings.
 * 
 * @author Davin McCall
 * @version $Id: BlueJPropStringSource.java 3515 2005-08-13 13:43:31Z polle $
 */
public interface BlueJPropStringSource
{
    public String getBlueJPropertyString(String property, String def);

    public String getLabel(String key);
    
    public void setUserProperty(String property, String value);
}
