
import java.io.File;
import java.io.FileInputStream;
import java.util.*;


/**
 * Utility to synchronise language label files for bluej
 * It reads in a 'master' file and compares the keys with 
 * those of 'slave' files.  
 * Usage:
 *
 * @author Bruce Quig
 */
public class LabelChecker
{
 
    private LanguagePack master;
    private final String masterLanguageName = "english";
    private String bluejDirectory;
     
    public LabelChecker(String bluejDir)
    {
        bluejDirectory = bluejDir;
        initMaster(masterLanguageName);
    }
    
    
 
    /**
    * load master language labels
    */
    private void initMaster(String masterLanguage)
    {
        master = new LanguagePack(bluejDirectory, masterLanguage);
    }

    /**
    * Check a language, comparing it to the master.
    * Currently this language directory needs to be in the same bluej lib directory.
    *
    * @param the name of language to check.  
    */
    public void checkLanguage(String language)
    {
        LanguagePack newLanguage = new LanguagePack(bluejDirectory, language);
        Comparer.findMissingKeys(master.getLabels(), newLanguage.getLabels());
    }
    
       
    
    /**
     *
     */
    public static void main(String[] args)
    {
   //     new LabelSync().synchronize();
    }
    
    
    /**
     *
     */
    public void usage()
    {
        
    }
    
}
