package org.bluej.extensions.submitter;

/**
 * This is just a container for global vars.
 * There is a thought behind it and I am writing a paper on the issue.
 */

import org.bluej.utility.*;
import bluej.extensions.*;

public class Stat 
  {
  public Flexdbg aDbg=null;
  public BlueJ   bluej=null;

  // It is OK to have it here since there are really ONE properties running per thread...
  // You MUST be carefull with threading, however....
  public SubmissionProperties submiProp=null;
  public GlobalProp globalProp=null;


  public static final int SVC_PROP=0x00000001;
  public static final int SVC_BUTTON=0x00000002;
  public static final int SVC_PARSER=0x00000004;
  }