package bluej.debugmgr;

import bluej.views.CallableView;
import bluej.views.Comment;
import bluej.views.MethodView;

/**
 * This class stores information about an expression. This could be from an
 * arbitrary invocation where we only have the actual invocation string. It
 * could also be from the invocation of a method, in which case we have more
 * information such as the method signature, javadoc, etc...
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ExpressionInformation.java 2548 2004-05-26 07:14:21Z polle $
 */
public class ExpressionInformation {
	private Comment comment;
	private String signature;
	private String expression;
	private String dynamicType;
	private CallableView methodView;
    private static final Comment emptyComment = new Comment();;

	/**
	 * Generates the expression information from a method view. The actual
	 * values for arguments to the method is not yet available and should be set
	 * later.
	 * 
	 * @see #setArgumentValues(String[])
	 * @param methodView
	 *            The MethodView of the invoked method
	 */
	public ExpressionInformation(MethodView methodView, String instanceName) {
		this.methodView = methodView;
		comment = methodView.getComment();
		signature = methodView.getLongDesc();

		String invokedOn = null;
		if (methodView.isStatic()) {
			invokedOn = methodView.getClassName();
		} else {
			invokedOn = instanceName;
		}

		expression = invokedOn + "." + methodView.getName();
	}

	/**
	 * Generates the expression information from an expression. This just copies
	 * the actual string.
	 * 
	 * @param expression
	 *            The expression we are watching
	 */
	public ExpressionInformation(String expression) {
		this.expression = expression;
	}
	
	/**
	 * Sets the values for the arguments to the method call.
	 * 
	 * @param args the argument values
	 */
	public void setArgumentValues(String[] args) {
		expression += "(";

		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				String string = args[i];
				expression += string;
				if (i + 1 < args.length) {
					expression += ", ";
				}
			}
		}
		expression += ")";
	}
	
	/**
	 * Returns the javadoc for this method
	 *  
	 * @return Returns the comment. Never null.
	 */
	public Comment getComment() {
	    if(comment!=null) {
	        return comment;
	    } else {
	        return emptyComment;
	    }
	}
	
	/**
	 * Returns the expression.
	 * 
	 * @return Returns the expression.
	 */
	public String getExpression() {

		return expression;
	}
	
	/**
	 * Get the signature for the method.
	 * 
	 * @return Returns the method signature or null if it is not available
	 */
	public String getSignature() {
		return signature;
	}
	
	

	
	public String toString() {
		String newline = System.getProperty("line.separator");
		StringBuffer s = new StringBuffer();

		s.append(newline);
		s.append(getComment());		
		s.append(newline);
		s.append(getSignature());
		s.append(newline);
		s.append(getExpression());	

		return s.toString();
	}

	

}