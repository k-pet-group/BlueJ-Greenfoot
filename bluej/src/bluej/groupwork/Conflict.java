package bluej.groupwork;

import java.io.*;
import java.io.File;
import java.util.*;
import bluej.utility.SortedProperties;

import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.utility.Debug;


/**
** This class represents a CVS generated conflict.
** It stores the two conflicting code sections as two Lists 
**
** @author Markus Ostman
** 
**/
public class Conflict     
{
  // private variables
  private List localVersion; //stores the local version of the conflict
  private List reposVersion; //stores the repository version of the conflict
  private boolean isTarget;  //conflict contains a whole target descr.
  private boolean isDepend;  //conflict contains a whole dependency descr.
  public Conflict()
  {
    localVersion = new ArrayList(); //implement using ArrayList
    reposVersion = new ArrayList();
  }

  public void addLocal(Object obj)
  {
    localVersion.add(obj);
  }

  public void addRepos(Object obj)
  {
    reposVersion.add(obj);
  }

  public List getLocalVersion()
  {
    return localVersion;
  }

  public List getReposVersion()
  {
    return reposVersion;
  }
  
  /*
   * Returns the number of conflicting lines.
   * for now we hope that the number of lines will be equal 
   * in local and repos, there is no guarantee for that though
   */
  public int numOfLines()
  {
    //Always return the smallest one
    if(localVersion.size() <= reposVersion.size())
      return localVersion.size();
    else{
      Debug.message("Conflict.java, line62: number of conf lines not equal");
      return reposVersion.size();
    }
  }

  /*
   * Checks if there are lines left in either of the versions
   * 
   * 
   */
  public boolean linesLeft()
  {
    return true;//Don't really know what to do in here yet
  }
}


