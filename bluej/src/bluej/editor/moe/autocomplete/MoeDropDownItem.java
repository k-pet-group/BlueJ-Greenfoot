package bluej.editor.moe.autocomplete;

/**
 *  This class is the super class for all items displayed
 *  in a MoeDropDownList.  This class cannot be instantiated
 *  because it is declared abstract.  Sub classes of this
 *  class must define the abstract methods.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */

import javax.swing.ImageIcon;

public abstract class MoeDropDownItem implements Comparable{


    /**
     *  This abstract method must be defined in all
     *  subclasses in order to obtain the insertable code
     *  for an auto-complete operation.
     *
     *  @return the code to be inserted into the source code
     */
    public abstract String getInsertableCode();


    /**
     *  This abstract method must be defined in all
     *  subclasses in order to specify what text is
     *  displayed in the MoeDropDownList.
     *
     *  @return the text to be shown in the MoeDropDownList
     */
    public abstract String getDisplayString();


    /**
     *  This method must be defined in all
     *  subclasses in order to specify the icon
     *  that is displayed in the MoeDropDownList.
     */
    public abstract ImageIcon getIcon();


    /**
     *  This method must be defined in all
     *  subclasses in order to specify the icon
     *  that is displayed in the MoeDropDownList
     *  when the item is selected.
     */
    public abstract ImageIcon getSelectedIcon();


    /**
     *  This method returns the display string
     *  for all subclasses.
     */
    public String toString(){
        return getDisplayString();
    }


    /**
     *  This method is used by the MoeDropDownList in order
     *  to sort the items that are displayed in the list.
     *  This compareTo() method enables the MoeDropDownFields
     *  to be sorted alphabetically followed by the
     *  MoeDropDownMethods sorted alphabetically.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     *
     */
    public int compareTo(Object o){
        if(o==null){
            return -1;
        }
        else{

            if(o instanceof MoeDropDownItem){
                MoeDropDownItem mdditem = (MoeDropDownItem) o;

                if(this instanceof MoeDropDownMethod){
                    if(mdditem instanceof MoeDropDownField){
                        return 1;  //Fields should come before methods
                    }
                    else if(mdditem instanceof MoeDropDownMethod){
                    // We are comparing two instances of MoeDropDownMethod
                    return getDisplayString().compareTo
                                              (mdditem.getDisplayString());
                    }

                }
                else if(this instanceof MoeDropDownField){
                    if(mdditem instanceof MoeDropDownField){
                        // We are comparing two instances of MoeDropDownField
                        return getDisplayString().compareTo
                                                  (mdditem.getDisplayString());
                    }
                    else if(mdditem instanceof MoeDropDownMethod){
                        return -1;  //Fields should come before methods;
                    }
                }
                else if(this instanceof MoeDropDownPackage){
                    if(mdditem instanceof MoeDropDownPackage){
                        // We are comparing two instances of MoeDropDownPackage
                        return getDisplayString().compareTo
                                                  (mdditem.getDisplayString());
                    }
                    else{
                        //Packages should come before anything else
                        //The list should only contain packages so
                        //this case should never occur
                        return -1;
                    }
                }

            }
            //This case should never occur
        return -1;
        }
    }


    /**
     *  This method can be used by the subclasses in order
     *  to determine the display string for a field,
     *  parameter or return type.
     *
     * @param c the class object to be converted to a String.
     * @return the String representation of the class object.
     */
  public String formatParameter(Class c){
    String name = c.getName();
    if(c.isPrimitive()){
        return name;
      }
    else{

      if(c.isArray()){
                int openBracketLPos = name.indexOf("[L");

                if(openBracketLPos >=0){
                    //The class object represents an object array

                    //name will contain any number of open brackets,
                    //a capital L, the object type and then a semi-colon.
                    //The number of open brackets = the dimensions of the
                    //array.
                    int start = openBracketLPos + 2;
                    int semiColon = name.lastIndexOf(";");
                    if(semiColon>start){
                        String objType = name.substring(start, semiColon);
                        int dimensions = countOpenBrackets(name);
                        String brackets =
                            createDimensionalBrackets(dimensions);
                        return objType + brackets;
                    }
                    else{
                        return "";
                    }
                }
                else{
                    //The class object represents a primitive array

                    //name will contain any number of open brackets
                    //followed by a single letter indicating the
                    //primitive type.
                    //The number of open brackets = the dimensions of the
                    //array.

                    int lastOpenBracketPos = name.lastIndexOf("[");
                    String letter = name.substring(lastOpenBracketPos+1,
                                                   lastOpenBracketPos+2);
                    char ch = letter.charAt(0);
                    String primType = encodingLetterToPrimitiveType(ch);
                    int dimensions = countOpenBrackets(name);
                    String brackets = createDimensionalBrackets(dimensions);
                    return primType + brackets;
                }
      }
      else{
                //Fine
        return name;
      }

      }
    }


    /**
     * This method converts a primitive type character encoding
     * to it's String representation.
     *
     * @param encoding the char to be converted to a String.
     * @return the char converted to a String.  Empty if un recognized encoding.
     */
    private String encodingLetterToPrimitiveType(char encoding){
        if(encoding=='B'){
            return "byte";
        }
        else if(encoding=='C'){
            return "char";
        }
        else if(encoding=='D'){
            return "double";
        }
        else if(encoding=='F'){
            return "float";
        }
        else if(encoding=='I'){
            return "int";
        }
        else if(encoding=='J'){
            return "long";
        }
        else if(encoding=='S'){
            return "short";
        }
        else if(encoding=='Z'){
            return "boolean";
        }
        else if(encoding=='V'){
            return "void";
        }
        else{
            return "";
        }
    }


    /**
     * This method counts the number of square opening brackets
     * in the given String.
     *
     * @param s the String to be searched for square opening brackets.
     * @return the number of square opening brackets in the String.
     */
    private int countOpenBrackets(String s){
        int count = 0;
        for(int i=0; i<s.length(); i++){
            char ch = s.charAt(i);
            if(ch=='[') count++;
        }
        return count;
    }


    /**
     * This method converts the dimensions of an array
     * into the correct number of open/ close bracket pairs.
     * if dimensions is 2 this method will return "[][]"
     *
     * @param dimensions the dimensions of the array.
     * @return a String representation for the dimensions of an array.
     */
    private String createDimensionalBrackets(int dimensions){
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<dimensions; i++ ){
            sb.append("[]");
        }
        return sb.toString();
    }

}
