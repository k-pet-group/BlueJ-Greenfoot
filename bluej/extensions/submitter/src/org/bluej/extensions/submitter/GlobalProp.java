package org.bluej.extensions.submitter;

import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This is needed to hold al properties that are global.
 * It is not just a Properties class since there is one property that is global but
 * it is dynamic. (The date)
 * Another reason is that the properties are exposed to the end user by string and I need a repository for
 * the names...
 * Third reason is to manage in a reasonable way the fact that some transport needs a set of names
 * that are indipendent from the fact that there is a URL (es ftp and mail)
 */

public class GlobalProp extends Properties
  {
  public static final String SMTPHOST_VAR="smtphost";
  public static final String USERADDR_VAR="useraddr";
  public static final String USERNAME_VAR="username";
  public static final String DATE_VAR="date";
  public static final String TITLE_VAR="title";
  public static final String SIMPLETITLE_VAR="simpletitle";
  

  /**
   * As from the Properties manual, with the addition of the special key DATE_VAR
   */
  public String getProperty ( String propKey )
    {
    if ( propKey == null ) return null;

    // If I am looking for a date return the current one.
    if ( propKey.equals(DATE_VAR) ) return new SimpleDateFormat ("dd MMMM yyyy HH:mm:ss").format (new Date());

    return super.getProperty(propKey);
    }

  /**
   * As from the Properties manual + special key DATE_VAR
   */
  public String getProperty (String propKey, String propDefault )
    {
    String propRisul = getProperty ( propKey );

    if ( propRisul == null ) return propDefault;

    return propRisul;
    }
  
  }