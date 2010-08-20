/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr;

import java.io.File;
import java.io.BufferedReader;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.role.MIDletClassRole;
import bluej.utility.FileUtility;

/**
 * Deploy MIDlet suite, the entire project contents.
 * @author Cecilia Vargas
 */
final class MIDletDeployer
{
    static final String ICONS_DIR           = "res" + File.separator + "icons" ;     
    static final String DEFAULT_MIDLET_ICON = "default.gif"; //default in project
    static final String DEFAULT_LIB_ME_ICON = "images" + File.separator +
                                              "me-default-icon.gif"; 
    private String      toolkitBinDir; //Wireless toolkit's bin subdirectory.
    private File        verified;      //Destination directory of preverify command.   
    private List<String> args;          //To pass arguments to commands.    
    private PkgMgrFrame frame;    
    private Project     proj;
    private List<String> midlets;      //Midlets to pass to the dialog for display.   
    
    public MIDletDeployer( PkgMgrFrame pmf )
    {
        frame = pmf;    
        proj = frame.getProject();
        verified = new File( proj.getProjectDir( ), "+tmpclasses" ); 
        
        //We invoke the preverify and emulator commands with their absolute path
        //names, so we build that here using the location of the Wireless Toolkit.  
        toolkitBinDir = Config.getPropString( "bluej.javame.toolkit.dir" ) + 
                        File.separator + "bin" + File.separator;
     }    
    
    /***************************************************************************
     * Deploy the midlet suite. First preverify, then display a dialog where the
     * user enters information for the .jar and .jad files, then launch emulator.
     */
    void deploy( )
    {
        if ( ! buildPreverifyCommand( ) ) 
            return;  

        FileUtility.deleteDir( verified );        
        if ( ! verified.mkdir() ) 
            Debug.reportError( "Could not create preverify output directory" );       
     
        startProcess( ); 
             
        final MIDletDeploymentDialog dialog = 
                         new MIDletDeploymentDialog( frame, verified, midlets );
        
        if ( dialog.runEmulator( ) ) {
            new Thread( ) {
                public void run( ) {
                    launchEmulator( dialog.getJadFile( ) );
                }
            }.start( );
        }
        frame.setStatus( Config.getString( "pkgmgr.midlet.deployed" ) );
    }

    /***************************************************************************
     * Start a process builder to invoke whatever command is in args.
     * The busyRunning flag detects when the process has finished starting up,
     * which in the emulator happens once it spits out its first output line.
     * This flag is mainly used for the emulator which takes quite a while to
     * get juiced up. While flag is true the frame's progress bar is on.
     */
    private void startProcess( )
    {
        frame.startProgress( );  //Turn on the frame's progress bar.
        boolean busyRunning = true;
        ProcessBuilder pb = new ProcessBuilder( args );
        pb.directory( proj.getProjectDir( ) );
        pb.redirectErrorStream( true );
        try {
            Process process = pb.start();
            //Read and print process output
            InputStream is = process.getInputStream();
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );            
            String line;            
            while ( ( line = br.readLine() )  != null ) {
                if( busyRunning ) {
                    frame.stopProgress( ); 
                    busyRunning = false;
                } 
                Debug.message( line );
            }
         } catch ( IOException ioe ) {
             Debug.reportError( "Had trouble invoking commmand" + pb.command( ) );
             ioe.printStackTrace();
         } finally {
             if( busyRunning ) {
                 frame.stopProgress( );
                 busyRunning = false;
            }
         }
    }
    
    /***************************************************************************
     * Set up in the args List the preverify command with its options.
     */         
    private boolean buildPreverifyCommand( )
    {
        args = new ArrayList<String>();        
        args.add( toolkitBinDir + "preverify" );     
        args.add( "-classpath" );
        args.add( proj.getClassLoader( ).getJavaMElibsAsPath( ) );
        args.add( "-d" );    //destination directory
        args.add( verified.getAbsolutePath() );  
        
        getUserSpecifiedOptions( "bluej.javame.preverify.options" );  
        
        //Get all the classes to preverify, but if while doing this we find a 
        //class that has not been compiled then there is no point in deploying.
        if ( getAndCheckClassTargets( ) )
            return true;
        else
            return false;
    }

     /***************************************************************************
      * This method goes through all packages in the project to accomplish 3 tasks:
      * 1. Call getClassFiles to put into args the list of all class files 
      *    in each package. This list is the input to preverify. 
      * 2. Put into midlets the list of all MIDlets in the project. This list is
      *    displayed in the deployment dialog in a table.
      * 3. Check that every class target is compiled, and upon finding the
      *    first not-compiled class the method returns false immediately. If the
      *    project is all compiled then the method return true.   
      */      
     private boolean getAndCheckClassTargets( )
     {
         midlets = new ArrayList<String>();
         List<String> packageNames = proj.getPackageNames();
         String dot = ""; //first package is the unnamed one, so no dot
         
         for ( Object pkgName : packageNames ) {             
             Package pkg = proj.getPackage( (String) pkgName ); 
             List<ClassTarget> classTargets = pkg.getClassTargets();   
             
             for ( ClassTarget target : classTargets ) {
                  if ( ! target.upToDate( ) ) {
                      DialogManager.showMessage( frame, "not-all-compiled" );
                      return false;
                  }                    
                  if ( target.getRole( ) instanceof  MIDletClassRole ) {
                      midlets.add( (String) pkgName + dot + target );
                  }
             }
             dot = ".";
             getClassFiles( pkg );  //put all .class files in package into args
         }
         return true;
     }
     
    /***************************************************************************
     * Add to args the filenames of all the .class files--without the .class
     * extension--in the pkg parameter. Method getClassTargets ignores
     * inner classes, so we have to get all the class files in a package using 
     * this method instead. The filenames added include the qualified package name,
     * something like pkg.subpkg.myclass
     */         
    private void getClassFiles( Package pkg )
    {      
        String[ ] classFiles = pkg.getPath( ).list( new ClassFilesFilter( ) );
        //The following should rarely be true; we're just being healthily paranoid.
        if ( ( classFiles == null )  ||  ( classFiles.length == 0 ) ) 
            return; 
        String dot = "";
        String pkgName = pkg.getQualifiedName( );
        if ( ! pkgName.equals("") )
            dot = ".";
      
        for ( int i = 0; i < classFiles.length; i++ ) {
            int index = classFiles[ i ].lastIndexOf( ".class" );
            String s = classFiles[ i ].substring( 0, index );
            args.add( pkgName + dot + s );
        }  
    }        
     
    /***************************************************************************
     * Launch the emulator.
     */         
    private void launchEmulator( File jadFile )
    {
        args = new ArrayList<String>( );
        args.add( toolkitBinDir + "emulator" );
        args.add( "-Xdescriptor:" + jadFile.getName() );
        getUserSpecifiedOptions( "bluej.javame.emulator.options" );
        startProcess( );
    }

    /***************************************************************************
     * Put into args whatever options are in the configuration files.
     */         
    private void getUserSpecifiedOptions( String userOptions)
    {
        String options = Config.getPropString( userOptions, null );
        if ( options != null ) {
            StringTokenizer st = new StringTokenizer( options);
            while ( st.hasMoreTokens() ) 
                args.add( st.nextToken()  );
        }               
    } 

    /***************************************************************************
     * A filter to accept only .class files. 
     */    
    private class ClassFilesFilter implements FilenameFilter
    {
	public boolean accept( File f, String name ) {
		return ( name.toLowerCase( ).endsWith( ".class" ) );                
	}	         
    }
}