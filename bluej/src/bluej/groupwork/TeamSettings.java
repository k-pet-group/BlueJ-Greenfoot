package bluej.groupwork;

/**
 * A class to represent team settings
 * 
 * @author Davin McCall
 */
public class TeamSettings
{
    private TeamworkProvider provider;
    private String protocol;
    private String server;
    private String prefix;
    private String group;
    private String username;
    private String password;
    
    public TeamSettings(TeamworkProvider provider, String protocol, String server,
            String prefix, String group, String username, String password)
    {
        this.provider = provider;
        this.protocol = protocol;
        this.server = server;
        this.prefix = prefix;
        this.group = group;
        this.username = username;
        this.password = password;
    }
    
    public TeamworkProvider getProvider()
    {
        return provider;
    }
    
    public String getProtocol()
    {
        return protocol;
    }
    
    public String getServer()
    {
        return server;
    }
    
    public String getPrefix()
    {
        return prefix;
    }
    
    public String getGroup()
    {
        return group;
    }
    
    public String getUserName()
    {
        return username;
    }
    
    public String getPassword()
    {
        return password;
    }
}
