
package bluej.parser.symtab;

import java.util.Hashtable;

/*******************************************************************************
 * Keeps track of all strings encountered in the file that represent
 *  identifiers.  This way we only ever keep a single copy of a string
 *  and all symbols refer to it.
 ******************************************************************************/
class StringTable        
{ 
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** The hash table that holds all the strings.  Note that if we were tuning
     *  this we'd adjust the size of the hash table to find a fairly 
     *  efficient setting
     */
    private Hashtable names = new Hashtable(); 


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Get a name from the StringTable */
    String getName(String name) {
        if (name == null) return null;
        String uniqueName = (String)names.get(name);
        if (uniqueName != null)
            return uniqueName;
            
        names.put(name, name);
        return name;
    }


    /** Write out that this is a string table... */
    public String toString() {
        return "StringTable";
    }   
}
