package bluej.groupwork;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.io.File;

import javax.swing.*;

import bluej.pkgmgr.*;

import com.ice.cvsc.*;

/**
 ** @version $Id: GroupInfo.java 426 2000-04-14 01:11:12Z markus $
 ** @author Markus Ostman
 ** Interface between a group project and a bluej package
 **/
public interface GroupInfo
{
    /**Method 
     ** 
     **  
     ** @param pkgDir The directory name of a Package
     ** @param 
     ** @returns Group information or null if something went wrong  
     **/
    GroupInfo getGroupInfo(String pkgDir);

    /**Method 
     ** 
     **  
     ** @param 
     ** @returns CVSProject
     **/
    CVSProject getProject();

    /**Method 
     ** 
     **  
     ** @param localDir The path to the local directory
     ** @param 
     ** @returns void  
     **/
    void initializeGroupInfo(String localDirName, 
			     JFrame currentFrame,
			     String password);

    /**Method to verify login to group project 
     ** 
     **  
     ** @param 
     ** @param 
     ** @returns void  
     **/
    void verifyLogin();
    

} // end interface GroupInfo
