
/**
 * Class to represent a language within a BlueJ localisation.
 * It provides an abstraction and contains the elements 
 * necessary for BlueJ to support a language.
 * 
 * @author Bruce Quig
 * @version 0.1
 */

import java.util.Properties;
import java.io.*;

public class LanguagePack
{
    // instance variables - replace the example below with your own
    private Properties labelsFile;
    public static final String LIB_DIR = "lib";
    public static final String LABEL_FILE = "labels";

    /**
     * Constructor for objects of class LanguagePack
     */
    public LanguagePack(String bluejDir, String language)
    {
        // initialise instance variables
              // load the master language as a Properties file
        labelsFile = new Properties();
        try {
            
            String labelFile = bluejDir + File.separator
                                    + LIB_DIR + File.separator
                                    + language + File.separator + LABEL_FILE;
            //System.out.println("language: " + labelFile);
            labelsFile.load(new FileInputStream(labelFile));
            System.out.println("loading language: " + language);
        }
        catch(Exception e) {
            System.err.println("Unable to open master language file");
        }        
        
    }

    /**
     * 
     * @return    the general labels for this language
     */
    public Properties getLabels()
    {
        // put your code here
        return labelsFile;
    }
}
