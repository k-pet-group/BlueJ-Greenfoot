package bluej.editor.moe.autocomplete.parser;

import java.lang.reflect.*;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import java.util.*;
import bluej.editor.moe.autocomplete.*;
import javax.swing.text.Element;

import java.io.File;

/**
 * This class has been created using a number of methods
 * from the SpeedJava class of SpeedJava (version 0.3).
 * We created this class so that the MoeAutocompleteManager
 * can easily interact with the SpeedJava parsing methods.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class Parser{

    private boolean parsing=false;

    private Document document;
    private File projectRoot;
    private ArrayList listeners;

    private int currentLineNumber;
    private JavaSourceInfo sourceInfo;

    /**
     * This constructs a Parser for a given document.
     * It's purpose is to parse a document and to
     * find the type of an expression before a
     * given dot position within the document.
     *
     * Each MoeAutocompleteManger should construct one
     * and only one Parser for the Document/MoeEditor
     * it is managing the auto-complete for.
     *
     * Once a MoeAutocompleteManager has constructed
     * it's Parser it should register itself as
     * a ParserListener for it's parser.  This way
     * the MoeAutocompleteManager will get informed
     * when the parsing has been completed.  As the
     * parsing is done in a separate thread the editor
     * does not lock up during the parsing.
     *
     * THIS CLASS IS NOT A SPEEDJAVA CLASS BUT DOES
     * CONTAIN SOME METHODS THAT HAVE BEEN TAKEN FROM SPEED JAVA
     * VERSION 0.3.  THESE METHODS HAVE BEEN INDICATED.
     *
     * @param document the Document that this parser is responsible for parsing.
     * @param projectRoot the root of the project for the specified document.
     */
    public Parser(Document document, File projectRoot){
        this.document = document;
        this.projectRoot = projectRoot;
        listeners = new ArrayList();
    }

    /**
     * This method can be used to determine
     * whether the parser is currently parsing.
     * The parseAndDetermineType method should only
     * be called if this method returns false.
     *
     * THIS METHOD IS NOT SPEEDJAVA CODE
     *
     * @return whether this Parser object is currently parsing the document.
     */
    public boolean isParsing(){
        return parsing;
    }

    /**
     * This method can be used to register a ParserListener
     * object.  The ParserListener will be informed once
     * the type has been established or not established.
     * The MoeAutocompleteManager should register itself
     * as a ParserListener.
     *
     * THIS METHOD IS NOT SPEEDJAVA CODE
     *
     * @param listener the ParserListener to be notified once parsing has completed.
     */
    public void addParserListener(ParserListener listener) {
        listeners.add(listener);
    }

    /**
     * This removes a ParserListener so that it is no longer
     * notified every time parsing is completed.
     *
     * THIS METHOD IS NOT SPEEDJAVA CODE
     *
     * @param listener the ParserListener to be removed.
     */
    public void removeParserListener(ParserListener listener) {
        listeners.remove(listener);
    }


    /**
     * This method starts the parsing in a separate thread.
     * The separate thread does the parsing and also determines
     * the type of the expression before the given dot position.
     *
     * The ParserListeners are informed once the type has been
     * established or could not be determined.
     *
     * THIS METHOD IS NOT SPEEDJAVA CODE
     *
     * @param dotPos the location of the dot in the document
     *               where the preceding type needs to be determined.
     */
    public void parseAndDetermineType(int dotPos) {

        Debug.printParserMessage("Parse.parseAndDetermineType");

        String fullCode = "";
        String beforeDot = "";

        try{
            fullCode = document.getText(0, document.getLength());
            beforeDot = document.getText(0, dotPos);
        }
        catch(BadLocationException e){
        }

        currentLineNumber =
            document.getDefaultRootElement().getElementIndex(dotPos);


        final String fullCode_f = fullCode;
        final String beforeDot_f = beforeDot;

        Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    parseAndDetermineTypeInternal(fullCode_f, beforeDot_f);
                }
            }
        );

        thread.setPriority(thread.MIN_PRIORITY);
        thread.start();
    }


    /**
     * This method does the parsing and determines the type.
     * It then informs the ParserListeners about the result.
     *
     * THIS METHOD IS NOT SPEEDJAVA CODE
     *
     * @param fullCode the entire source code to be parsed.
     * @param beforeDot the code before the dot that needs it's type determined.
     */
    private void parseAndDetermineTypeInternal(String fullCode, String beforeDot) {
        parsing = true;

        ClassUtilities.setProjectRoot(projectRoot);

        sourceInfo = new JavaSourceInfo();
        sourceInfo.parse(fullCode);
        Class cls = backtype(beforeDot);

        /*
        System.out.println("Sleeping");
        try{
            Thread.sleep(5000);
        }
        catch(java.lang.InterruptedException e){
        }
        */

        if(cls!=null){
            if(cls.isPrimitive()){
                cls=null;
            }
            else if (cls.isArray()){
                cls=null;
            }
            else if (wasTypeArray){
                cls=null;
            }
        }

        //Determine the members for the class that
        //has been determined.
        ArrayList moeDropDownItems = new ArrayList();
        if(cls!=null){
            moeDropDownItems = getMoeDropDownItems(cls,backtypeModifiers);
        }

        parsing = false;

        //Inform the listeners about the determined class
        //and the members for that class.
        Iterator it = listeners.iterator();
        while(it.hasNext()){
            ParserListener listener = (ParserListener) it.next();

            if(cls==null){
                listener.typeNotEstablished();
            }
            else{
                listener.typeEstablished(cls, moeDropDownItems);
            }
        }
    }



    /**
     * This method gets alls the MoeDropDownItems that are applicable
     * to the specified class.  The modifiers parameter is used
     * to restrict the contents of the ArrayList so that it only
     * contains Methods and Fields that have the specified modifiers
     * ie Static only.
     *
     * THIS METHOD HAS BEEN ADAPTED FROM THE getMembers METHOD OF
     * THE SpeedJava CLASS OF SPEEDJAVA (VERSION 0.3)
     *
     * @param cls The class to get the MoeDropDownItems for.
     *            (ie MoeDropDownFields and MoeDropDownMethods)
     * @param modifiers can be used to only return the static MoeDropDownItems.
     * @return an ArrayList containing MoeDropDownItems
     *         for the specified class.
     */
  private ArrayList getMoeDropDownItems(Class cls, int modifiers) {
    ArrayList list = new ArrayList();

    TreeMap map = new TreeMap();
    if (cls == OurClass.class) {
      Scope rootScope = sourceInfo.getRootScope();
      if (rootScope == null) {
        return list;
      }
      TreeMap members = rootScope.getMembers(currentLineNumber);
      Set set = members.keySet();
      int i = 0;
      int n = set.size();

      Iterator iter = set.iterator();
      String[] entry;
      while (iter.hasNext()) {
        entry = (String[])(members.get(iter.next()));

          if (isField(entry)) {
                    //Field
                    //0=type
                    //1=name
            String fieldType = entry[0];
                    String fieldName = entry[1];

                    MoeDropDownField mddField =
                        new MoeDropDownField(fieldName, fieldType, false);
                    list.add(mddField);
          }
                else{
                    //Method
                    //0=return type
                    //1=method name
                    //2=params with brackets
                    String methodName = entry[1];
                    String methodReturnType = entry[0];
                    String methodParams = entry[2];
                    MoeDropDownMethod mddMethod =
                        new MoeDropDownMethod(methodName, methodReturnType,
                                              methodParams, false);
                    list.add(mddMethod);
                }
      }
      cls = sourceInfo.getSuperclass();
    }

    Class origClass = cls;
    //while (cls != null) {
        if (cls != null) {
      Method[] methods = cls.getMethods();
      Field[] fields = cls.getFields();
      for (int i = 0; i < methods.length; i++) {
        if ((methods[i].getModifiers() & modifiers) == modifiers) {
                    MoeDropDownMethod mddMethod = new MoeDropDownMethod(methods[i]);
                    list.add(mddMethod);
        }
      }
      for (int i = 0; i < fields.length; i++) {
        if ((fields[i].getModifiers() & modifiers) == modifiers) {
          MoeDropDownField mddField = new MoeDropDownField(fields[i]);
                    list.add(mddField);
        }
      }
      if (cls.isInterface()) {
        // Needs fixing
        cls = null;
      } else {
        cls = cls.getSuperclass();
      }
    }

    return list;
  }












    /**
     * THIS METHOD HAS BEEN TAKEN FROM THE SpeedJava
     * CLASS OF SPEEDJAVA (VERSION 0.3)
     */
  private static boolean isField(String[] array) {
    return (array != null && array.length == 4);
  }

    /**
     * THIS METHOD HAS BEEN TAKEN FROM THE SpeedJava
     * CLASS OF SPEEDJAVA (VERSION 0.3)
     *
     * Following description by Jim Wissner (<A href="mailto:jim@jbrix.org">jim@jbrix.org</A>):
     *
     * Figure out what the type of the member named 'str' within prevType is.
     */
    private Class getType(String str,Class prevType, String phraseSoFar) {
        Debug.printParserMessage("getType(  ["  + str + "] , [" + prevType + "] , [" + phraseSoFar + "]");

        String s = str.trim();

        while (s.endsWith("]") && s.indexOf('[') > -1) {
            s = s.substring(0,s.lastIndexOf('['));
        }
        while (s.length() >= 2 && s.charAt(0) == '('  && s.charAt(s.length() - 1) == ')' ) {
            if (s.length() == 2) {
                return prevType;
            }
            s = s.substring(1,s.length() - 1).trim();
            if (s.length() < 1) {
                return prevType;
            }
        }
        while (s.endsWith("]") && s.indexOf('[') > -1) {
            s = s.substring(0,s.lastIndexOf('['));
        }
        if (s.trim().length() < 1) {
            return null;   //OurClass.class;
            //The above line was changed by Darren Link
            //to prevent the list from appearing every
            //time the dot character is pressed.
        }
        if (s.indexOf('(') < 0) {
            if (s.equals("this")) {
                return prevType;
            } else if (s.equals("super")) {
                // Umm. really should return prevType's superclass...
                return sourceInfo.getSuperclass();
            } else if (s.equals("class")) {
                return Class.class;
            } else {
                Class cls = getFieldType(s,prevType);
                if (str.trim().endsWith("]")) {
                    wasTypeArray = false;
                }
                if (cls != null) {
                    return cls;
                }
                String type = sourceInfo.getAbsoluteType(phraseSoFar);
                try {
//                  cls = Class.forName(type);
                    cls = ClassUtilities.findClass(type);
                    backtypeModifiers = Modifier.STATIC;
                    return cls;
                } catch (java.lang.Throwable ex) {
                }
            }
        } else if (s.startsWith("(")) {
            // casted...
        } else {
            // method call...
            String mname = s.substring(0,s.indexOf('('));
            Class cls;
            if (prevType == OurClass.class) {
                cls = sourceInfo.getMemberType(mname,currentLineNumber);
                if (cls != null) {
                    return cls;
                }
            }
            String type = null;
            cls = prevType;
            Method[] mthds = (cls != null? cls.getMethods() : null);
            while (type == null && cls != null) {
                for (int i = 0; i < mthds.length; i++) {
                    if (mthds[i].getName().equals(mname)) {
                        return mthds[i].getReturnType();
                    }
                }
                cls = cls.getSuperclass();
                if (cls != null) {
                    mthds = cls.getMethods();
                }
            }
        }
        return null;
    }


    /**
     * THIS METHOD HAS BEEN TAKEN FROM THE SpeedJava
     * CLASS OF SPEEDJAVA (VERSION 0.3)
     */
    private Class getFieldType(String varStr,Class ownerType) {
        if (ownerType == null) {
            return null;
        } else if (ownerType == OurClass.class) {
            return sourceInfo.getMemberType(varStr,currentLineNumber);
        } else {
            Field[] fields = ownerType.getFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getName().equals(varStr)) {
                    return fields[i].getType();
                }
            }
        }
        return null;
    }


    /**
     * THIS METHOD HAS BEEN TAKEN FROM THE SpeedJava
     * CLASS OF SPEEDJAVA (VERSION 0.3)
     *
     * Following description by Jim Wissner (<A href="mailto:jim@jbrix.org">jim@jbrix.org</A>):
     *
     * This is a tricky, but fun, and very important method.  It looks back
     * from the current pos to find the type of the previous "phrase". So,
     * for instance, we must know that if a period is typed after the
     * code:  someObject.getClass().getName().toLowerCase()
     * then we are calling a string.  Keep in mind any of those calls in
     * that "chain" may have params and other calls, so we have to
     * be careful counting our parens.
     *
     * It works ok, but could be improved.  Array handling is somewhat
     * sketchy.
     *
     * Should be combined somehow with backphrase() - a bit of
     * redundancy.
     *
     * Some test cases we have to work on:
     *
     * SomeClass.this.var.whatever().;
     * mypackage.SomeClass.class.getName().;
     * OuterClass.this.InnerClass.class.getName().;
     * InnerClass.class.getName().;
     * (new Whatever()).
     * casting...
     *
     * comma messes up multi-dim arrays (see below)
     *
     */

    private boolean wasTypeArray;
    private int backtypeModifiers = 0;

    private Class backtype(String s) {
        if (s==null) return null;
        if(s.length()<1) return null;

        char lastChar = s.charAt(s.length() - 1);
        if(lastChar=='.') return null;

        try {
            int start = s.length()-1;
            char c;

            int parenStack = 0;
            String token = "";
            Vector v = new Vector();
            boolean expectsDot = false;

            for(int pos=s.length()-1; pos>=0; pos--){

                c = s.charAt(pos);

                if(!(      c == ' '
                        || c == '\t'
                        || c == '\n'
                        || c == '_'
                        || (c >= 'A' && c <= 'Z')
                        || (c >= 'a' && c <= 'z')
                        || (c >= '0' && c <= '9')
                        || parenStack > 0
                        || c == '.'
                        || c == '('
                        || c == ')'
                        || c == '['
                        || c == ']')) {
                        break;
                }

                if (c == '.' && parenStack < 1) {
                    expectsDot = false;
                    if (token.length() > 0) {
                        v.addElement(token);
                        token = "";
                    }
                } else if (c == ')') {
                    parenStack++;
                    token = c + token;
                } else if (c == '(') {
                    parenStack--;
                    if (parenStack < 0) {
                        break;
                    }
                    token = c + token;
                } else if (c == ' ' || c == '\t' || c == '\n') {
                    if (parenStack < 1) {
                        expectsDot = true;
                    }
                    token = c + token;
                } else if (expectsDot) {
                    break;
                } else {
                    token = c + token;
                }

            }


            if (token.length() > 0) {
                v.addElement(token);
            }

            String segment = "";
            for (int i = v.size() - 1; i >= 0; i--) {
                segment += v.elementAt(i) + " - ";
            }

// casting considerations:
// z((String)x).getValue().
// ((String)x).getValue().
// (((String)x).getValue()).
// ((String)(x.getValue()).

            // Now we've got our "stack" or types, let's dereference them:
            s = "";
            Class prevType = OurClass.class;

            int i;
            for (i = v.size() - 1; i >= 0; i--) {
                s += (s.length() > 1? "." : "");
                s += ((String)(v.elementAt(i))).trim();
                backtypeModifiers = 0;
                prevType = getType((String)(v.elementAt(i)),prevType,s);
            }

            /**
            if (false&&prevType == null) {
                JOptionPane.showMessageDialog(
                        null,
                        s+" "+prevType,
                        "Information",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            */

            return prevType;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    private class OurClass {}

}