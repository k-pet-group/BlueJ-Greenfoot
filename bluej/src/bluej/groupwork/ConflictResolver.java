package bluej.groupwork;

import java.io.*;
import java.io.File;
import java.util.*;
import bluej.utility.SortedProperties;

import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.utility.Debug;


/**
** This class resolves conflicts generated from a CVS merge.
** It only handles conflicts in special type of files. 
** File types:  bluej.pkg, bluej.pkh,  
** @author Markus Ostman
** 
**/
public class ConflictResolver     
{
  // private variables
  private List fileContent;  //stores the content of the file line by line
  private Properties props;  //stores the content of the file in Properties
  private List conflictList; //Holds all conflicts
  
  // =========================== PUBLIC METHODS ===========================
  public ConflictResolver()
  {
    fileContent = new ArrayList();//We implement it with an Arraylist
    conflictList = new ArrayList();
    props = new SortedProperties();//Use the sorted properties
  }
  
  /**
   * resolves a CVS conflict in a given file
   * The method is restricted to very specialized file types.
   * eg. bluej.pkg, bluej.pkh
   * @param filePath   Path to the conflicted file
   * @param
   * @returns
   */
  public void resolveConflict(String filePath)
  {
    //.temp put all of this in a separate metod
    //read the file
    BufferedReader in=null;
    try{
      in = new BufferedReader(new FileReader(filePath));
    }
    catch (FileNotFoundException e){
      Debug.message("The file could not be found");
    }

    //Read the file into a Properties object
    //This will not include duplicate info
    try{
      //./temp to be safe maybe this should be read from the .#bluej.pkg
      FileInputStream input = new FileInputStream(filePath);
      props.load(input);
    } catch(IOException e) {
      Debug.reportError("Error loading in Conflict resolver" +
                        filePath + ": " + e);
    }
    
    String tmp = "";
    //Create a separate String object for every line
    //and put it in the List 
    do{
      try{
        tmp=in.readLine();
        if(tmp!=null)
          fileContent.add(tmp);
      }catch (IOException e1){
        Debug.message("problem with IO");
      }catch (Exception e2){
        Debug.message("problem");
      }
    }
    while(tmp!= null);
    
    //Check the file type
    if(filePath.endsWith(".pkg") || filePath.endsWith(".pkh"))
      resolvePkgFile(fileContent);
    else
      Debug.message("Sorry can only resolve *.pkg or *.pkh");
  }
  
  /**
   * Handle .pkg-files
   */
  public void resolvePkgFile(List fileContent)
  {
    //.temp this could be called in resolveConflict()
    //Here all conflicts are found and put in a List
    //maybe this approach is bad? Iterating twice!
    findConflict(fileContent);

    String local = "";
    String repos = "";

    //Iterate through all conflicts
    for (Iterator itr=conflictList.iterator();itr.hasNext();){
      Conflict conf = (Conflict)itr.next();

      //Resolve every line of each Conflict
      for (int i=0;i<conf.numOfLines();i++){
        local = (String)conf.getLocalVersion().get(i);
        repos = (String)conf.getReposVersion().get(i);
        
        //Check if the whole key string is equal in the conflicting lines
        if(local.substring(0, local.indexOf('=')).equals(repos.substring(0, repos.indexOf('=')))){
          //Check if it is identified as "Not important"
          if(local.substring(local.lastIndexOf('.'),
                             local.indexOf('=')).equals(".width") ||
             local.substring(local.lastIndexOf('.'),
                             local.indexOf('=')).equals(".height") ||
             local.substring(local.lastIndexOf('.'),
                             local.indexOf('=')).equals(".x") || 
             local.substring(local.lastIndexOf('.'),
                             local.indexOf('=')).equals(".y")){
            this.props.put(local.substring(0,local.indexOf('=')),
                           local.substring(local.indexOf('=')+1));
            //Here we don't have to do anything if we read props properly
            
          }
        } 
      }
    }
    
    //Save the resolved pkg file
    File file = new File("/home/markus/bluejPrototype/resolving", "resolved.pkg");
    try {
      props.store(new FileOutputStream(file),"Resolved file");
    } catch(Exception e) {
      Debug.reportError("could not save properties file: " +
                        file.getName());
    }
  }//End resolvePkgFile
  
  /**
   * Find all conflicts and store them in conflictList
   */
  private void findConflict(List fileContent)
  {
    int i = 0;
    Conflict conf = null;

    //Iterate trough to find the conflicts
    for (Iterator itr=fileContent.iterator();itr.hasNext();){
      String tmp = (String)itr.next();
           
      if(tmp.startsWith("<<<<<<<")){     //Found a conflict
        tmp = (String)itr.next();
        i++;                             //count the conflicts
        conf = new Conflict();
        while(!tmp.startsWith("=======") && itr.hasNext()){
          Debug.message("first conf"+i+": "+tmp);
          conf.addLocal(tmp);
          tmp = (String)itr.next();
        }
        tmp = (String)itr.next();
        while(!tmp.startsWith(">>>>>>>") && itr.hasNext()){
          Debug.message("second conf"+i+": "+tmp);
          conf.addRepos(tmp);
          tmp = (String)itr.next();
        }
        this.conflictList.add(conf);
      }
    }
  }//End findConflict
}
