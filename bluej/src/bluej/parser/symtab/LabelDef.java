
package bluej.parser.symtab;

/*******************************************************************************
 * A label that appears in the source file. 
 ******************************************************************************/
class LabelDef extends Definition {
    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to create a new label symbol */
    LabelDef(String name,               // name of the label
             Occurrence occ,            // where it was defined
             ScopedDef parentScope) {   // scope containing the def
        super(name, occ, parentScope);
    }

}
