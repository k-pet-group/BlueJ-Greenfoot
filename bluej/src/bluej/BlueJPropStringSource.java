package bluej;


/**
 * Interface for a class which supports retrieving BlueJ property strings.
 * 
 * @author Davin McCall
 * @version $Id: BlueJPropStringSource.java 4708 2006-11-27 00:47:57Z bquig $
 */
public interface BlueJPropStringSource
{
    public String getBlueJPropertyString(String property, String def);

    public String getLabel(String key);
    
    public void setUserProperty(String property, String value);
}
