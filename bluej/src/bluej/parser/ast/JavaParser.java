package bluej.parser.ast;

import java.util.List;
import java.io.*;
import antlr.*;
import antlr.collections.*;
import antlr.debug.misc.*;
import java.awt.event.*;
import java.util.*;

public class JavaParser
{
  private ASTFactory factory;
    private AST root;
    
  private long delta;

  public JavaParser() {
    delta = 0;
    setupTree();
  }

/**
Returns the AST of the current file
@return AST the top node of the AST of all files parsed.
*/
  public AST getTree() {
    System.out.println("parsing took " + getParseSeconds() + " seconds");
    return root;
  }

  public double getParseSeconds() {
    return delta / 1000.0;
  }

/**
Creates a new AST root node.
*/
  private void setupTree() {
    factory = new ASTFactory();
//    factory.setASTNodeType(SymTabAST.class.getName());
    root = (AST)factory.create(0, "AST Root");
  }

/**
Takes and AST root and a bunch of files and parses them into a single AST
@param File the file you wish to parse
@param String an array of files
@exception FileNotFoundException thrown if root of an invalid type
*/
  public void parseFiles(File root,
                         String [] files) throws FileNotFoundException
                                                 {
    for (int i = 0; i < files.length; i++) {
      doFile(new File(root, files[i]));
    }
  }

/**
Takes a File or directory and either traverses the directory or calls parseFile on the file.
@param File to be sent to parseFile or the directory to be traversed.
@exception FileNotFoundException thrown on invalid type.
*/
  public void doFile(File file) throws FileNotFoundException
                                        {
    if (file.isDirectory()) {
      parseFiles(file, file.list());
    }
    else if (isJavaFile(file)) {
      //System.out.println("   "+file.getAbsolutePath());
//      parseXXXFile( file );
    }
  }


/**
Constructor, creates a new JavaLexer object based input stream.
@param Inputstream the input to be broken up
@param String the absoulute path of the file
@return JavaLexer object called result
*/
  private JavaLexer makeLexer(InputStream stream, String path) {
    JavaLexer result = new JavaLexer(stream);
//    result.setFilename(path);
//    result.setTokenObjectClass(SymTabToken.class.getName());

    return result;
  }

/**
Constructor, creates a new java Recognizer object based on a Lexer object
@param JavaLexer object to be worked on
@param String the absolute path of the file
@return JavaRecognizer object called result
*/
private JavaRecognizer makeRecognizer(TokenStreamHiddenTokenFilter filter,
                                        String path) {
    JavaRecognizer result = new JavaRecognizer(filter);
//    result.setFilename(path);
//    result.setASTNodeClass(SymTabAST.class.getName());

    return result;
  }

/**
Boolean opperation that checks to see if a file is of type .java
@param File the file you wish to check
@return boolean
*/
  public static boolean isJavaFile(File file) {
    return ( (file.getName().length()>5) &&
             (file.getName().substring(file.getName().length()-5).equals(".java")) );
  }

/**
Processes a Tree to be modified
@param String the name of the file
@param AST the tree to be worked on
@param String an array of strings
*/
  private void doTreeAction(File file, AST tree, String[] tokenNames) {
    if (tree != null) {
      addToTree(file, tree, tokenNames);
    }
  }


/**
Adds a file to an AST tree
@param String the file you wish to add
@param AST the tree you are adding the file to
@param String the token names
*/
  private void addToTree(File file, AST tree, String[] tokenNames) {
//    ((CommonAST)tree).setVerboseStringConversion(true, tokenNames);
//    SymTabAST node = (SymTabAST)factory.create(0,file.getAbsolutePath());

//    node.setFirstChild(tree);
//    node.finishDefinition(file, root, null);
//    root.addChild(node);
  }



    public static Class parseExpression(String expr)
                                
    {
        StringReader sr = new StringReader(expr);
            
		try {
			// Create a scanner that reads from the input stream passed to us
			JavaLexer lexer = new JavaLexer(sr);

			// Create a parser that reads from the scanner
			JavaRecognizer parser = new JavaRecognizer(lexer);
            parser.setASTNodeClass("bluej.parser.ast.TypeInfoAST");

			// start parsing at the expression rule
			parser.expression();
			
            TypeInfoAST tia = (TypeInfoAST) parser.getAST();

    		ExpressionSemanticParser semparse = new ExpressionSemanticParser();
    		ExpressionSemanticParser.inMethodCall = 0;
            semparse.setASTNodeClass("bluej.parser.ast.TypeInfoAST");
    		try {
    			semparse.expression(tia);
    		}
    		catch (RecognitionException e) {
    			System.out.println(e);
    			//e.printStackTrace();
    		}

            TypeInfoAST semtia = (TypeInfoAST) semparse.getAST();

            System.out.println(semtia.toStringTree());
//            java.io.PrintWriter pw = new java.io.PrintWriter(System.out);
//            semtia.xmlSerialize(pw);
//            pw.flush();
            
            return semtia.getTypeInfoClass();
            
//			return parser.getAST();
		}
		catch (Exception e) {
  			System.out.println(e);
//			e.printStackTrace();   // so we can get stack trace		
		}
		
		return null;                                   
    }
    
	// Here's where we do the real work...
	public static AST parseFile(Reader r)
								 throws Exception {
		try {
			// Create a scanner that reads from the input stream passed to us
			JavaLexer lexer = new JavaLexer(r);
            lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
            // with a tab size of one, the rows and column numbers that
            // locatable token returns are model coordinates in the editor
            // (not view coordinates)
            // ie a keyword may appear to start at column 14 because of tabs
            // but in the actual document model its really at column 4
            // so we set our tabsize to 1 so that it maps directly to the
            // document model
            lexer.setTabSize(1);

            // Create a filter to handle our comments
            TokenStreamHiddenTokenFilter filter;
            filter = new TokenStreamHiddenTokenFilter(lexer);
            filter.hide(JavaRecognizer.SL_COMMENT);
            filter.hide(JavaRecognizer.ML_COMMENT);

			// Create a parser that reads from the scanner
			JavaRecognizer parser = new JavaRecognizer(filter);
            parser.setASTNodeClass("bluej.parser.ast.LocatableAST");

			// start parsing at the compilationUnit rule
			parser.compilationUnit();
			
			ASTFactory factory = new ASTFactory();
			AST root = factory.create(0,"AST ROOT");

			// do something with the tree
			CommonAST t = (CommonAST) parser.getAST();

    		UnitTestParser tparse = new UnitTestParser();
            tparse.setASTNodeClass("bluej.parser.ast.LocatableAST");
    		try {
    			tparse.compilationUnit(t);
    			// System.out.println("successful walk of result AST for "+f);
    		}
    		catch (RecognitionException e) {
    			System.err.println(e.getMessage());
    			e.printStackTrace();
    		}
			
	//		t.setVerboseStringConversion(true, parser.getTokenNames());
			root.setFirstChild(tparse.getAST());
			
			return root;
		}
		catch (Exception e) {
			System.err.println("parser exception: "+e);
			e.printStackTrace();   // so we can get stack trace		
		}
		
		return null;
	}
  
    public static List getVariableSelections(AST objBlock)
    {
        if (!(objBlock instanceof LocatableAST))
            throw new IllegalArgumentException("wrong AST type");

        LinkedList l = new LinkedList();
        LocatableAST childAST = (LocatableAST) ((BaseAST)objBlock).getFirstChild();

        // the children on a class' object block are a list of variable definitions
        // and method definitions
        while(childAST != null) {
            // we are only interested in variable definitions
            if(childAST.getType() == UnitTestParserTokenTypes.VARIABLE_DEF) {
                LocatableAST firstSib = null, secondSib = null;
                
                firstSib = (LocatableAST) childAST.getFirstChild();
                if(firstSib != null)
                    secondSib = (LocatableAST) firstSib.getNextSibling();
                    
                if (firstSib != null && secondSib != null) {                    
                    l.addFirst(secondSib);
                    l.addFirst(firstSib);
                }
            }               
            childAST = (LocatableAST) childAST.getNextSibling();            
        }            

        return l;
    }

    public static List getSetupMethodSelections(AST objBlock)
    {
        if (!(objBlock instanceof LocatableAST))
            throw new IllegalArgumentException("wrong AST type");

        LinkedList l = new LinkedList();
        LocatableAST childAST = (LocatableAST) ((BaseAST)objBlock).getFirstChild();

        // the children on a class' object block are a list of variable definitions
        // and method definitions
        while(childAST != null) {
            // we are only interested in method definitions
            if(childAST.getType() == UnitTestParserTokenTypes.METHOD_DEF) {
                LocatableAST firstSib = null, secondSib = null, thirdSib = null;
                
                firstSib = (LocatableAST) childAST.getFirstChild();
                if(firstSib != null && firstSib.getText().equals("setUp"))
                    secondSib = (LocatableAST) firstSib.getNextSibling();
                else
                    continue;

                if (secondSib != null) {
                    thirdSib = (LocatableAST) secondSib.getNextSibling();
                }
                    
                if (secondSib != null && thirdSib != null) {                    
                    l.addFirst(thirdSib);
                    l.addFirst(secondSib);

                    return l;
                }
            }               
            childAST = (LocatableAST) childAST.getNextSibling();            
        }            

        return l;
    }

    public static LocatableAST getOpeningBracketSelection(AST classBlock)
    {
        if (!(classBlock instanceof LocatableAST))
            throw new IllegalArgumentException("wrong AST type");

        return (LocatableAST) classBlock.getNextSibling();
    }

    public static LocatableAST getMethodInsertSelection(AST classBlock)
    {
        if (!(classBlock instanceof LocatableAST))
            throw new IllegalArgumentException("wrong AST type");

        return (LocatableAST) classBlock.getNextSibling();
    }
	
	public static void doTreeAction(AST t, String[] tokenNames)
    {
		if ( t==null ) return;
		if ( false ) {
			((CommonAST)t).setVerboseStringConversion(true, tokenNames);
			ASTFactory factory = new ASTFactory();
			AST r = factory.create(0,"AST ROOT");
			r.setFirstChild(t);
			final ASTFrame frame = new ASTFrame("Java AST", r);
			frame.setVisible(true);
			frame.addWindowListener(
				new WindowAdapter() {
                   public void windowClosing (WindowEvent e) {
                       frame.setVisible(false); // hide the Frame
                       frame.dispose();
                   }
		        }
			);
			// System.out.println(t.toStringList());
		}
/*		JavaTreeParser tparse = new JavaTreeParser();
		try {
			tparse.compilationUnit(t);
			// System.out.println("successful walk of result AST for "+f);
		}
		catch (RecognitionException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} */

	}

}
