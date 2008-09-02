package bluej.pkgmgr; 

import java.util.StringTokenizer;

/**
 * Holds an Applet param entry for use in generating HTML page for Applet.
 * Can be created from user input via RunAppletDialog or from reading in from
 * saved project properties.
 *
 * @author  Bruce Quig
 * @version $Id: AppletParam.java 5851 2008-09-02 12:36:28Z polle $
 */
public final class AppletParam 
{
    public static final String PARAM = "PARAM";
    public static final String NAME = "NAME";
    public static final String PARAM_NAME = PARAM + " " + NAME;
    public static final String VALUE = "VALUE";
    
    private String paramName;
    private String paramValue;
    
    
    /**
     * Constructor for creating from input values of key and value
     * @param key name of parameter
     * @param value  value of parameter
     */    
    public AppletParam(String key, String value) 
    {
        paramName = key;
        paramValue = value;
    }        
    
    /**
     * Constructor for creating from a stored parameter String
     * eg. < PARAM NAME=foo VALUE=bar >
     * @param parameterString  
     */    
    public AppletParam(String parameterString) 
    {
        //need to parse parameter line and recreate AppletParam object
        // (?i) ignore case
        String nameRegEx = "(?i)\\s*<\\s*PARAM NAME\\s*=\\s*\"?|\"?\\s*VALUE\\s*=.*";        
        String[] names = parameterString.split(nameRegEx);
        paramName = names[1];
        
        String valueRegEx = "(?i).*VALUE\\s*=\\s*\"?|\"?\\s*>\\s*";
        String[] values = parameterString.split(valueRegEx);
        paramValue = values[1];
    }    
    
    /** 
     * Getter for property paramValue.
     * @return Value of property paramValue.
     */
    public String getParamValue() 
    {
        return paramValue;
    }    
      
  
    /** 
     * Getter for property paramName.
     * @return Value of property paramName.
     */
    public String getParamName() 
    {
        return paramName;
    }    
    
    
    /**
     * Equality relates to the name of the Applet param name so that AppletParam
     * can easily be found in a list or DefaultListModel for location and 
     * replacement.  
     * @param obj object being evaluated against
     * @return true if considered equivalent 
     */    
    public boolean equals(Object obj) {
        boolean retValue = false;
        if(obj instanceof AppletParam) {
            retValue = paramName.equals(((AppletParam)obj).getParamName());
        }
        return retValue;
    }
    
    /**
     * redefined toString method, used to provide the param string for
     * the html page
     */
    public String toString() {
        return "<" + PARAM_NAME + " = \"" + paramName
        + "\"   " + VALUE + " = \"" + paramValue + "\">";        
    }
    
}
