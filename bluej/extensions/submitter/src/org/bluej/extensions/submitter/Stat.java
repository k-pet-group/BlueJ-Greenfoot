package org.bluej.extensions.submitter;

/**
 * This is just a container for global vars.
 * There is a thought behind it and I am writing a paper on the issue.
 */

import org.bluej.utility.*;
import bluej.extensions.*;

import org.bluej.extensions.submitter.properties.TreeData;

public class Stat 
  {
  public Flexdbg aDbg=null;
  public BlueJ   bluej=null;

  public GlobalProp   globalProp=null;    // Properties that are global go here
  public TreeData     treeData=null;      // This holds the data
  public SubmitDialog submitDialog=null;  // This allows user interaction
  
  public static final int SVC_PROP=0x00000001;
  public static final int SVC_BUTTON=0x00000002;
  public static final int SVC_PARSER=0x00000004;
  public static final int SVC_TREE=0x00000008;
  }