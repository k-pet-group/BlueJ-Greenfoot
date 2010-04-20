/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

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
package bluej.editor.moe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import bluej.parser.nodes.CommentNode;
import bluej.parser.nodes.ContainerNode;
import bluej.parser.nodes.ExpressionNode;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.TypeInnerNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.utility.Debug;

public class MoeIndent
{
    public static boolean calculateIndentsAndApply(MoeSyntaxDocument doc)
    {
        return calculateIndentsAndApply(doc, 0, doc.getLength());
    }
    
    public static boolean calculateIndentsAndApply(MoeSyntaxDocument doc, int startPos, int endPos)
    {
        Element rootElement = doc.getDefaultRootElement();
        List<DocumentAction> updates = new ArrayList<DocumentAction>(rootElement.getElementCount());

        IndentCalculator ii = new RootIndentCalculator();

        boolean lastLineWasBlank = false;
        boolean perfect = true;

        for (int i = 0; i < rootElement.getElementCount(); i++) {
            Element el = rootElement.getElement(i);
            
            // If the element overlaps at all with our area of interest:
            if (el.getEndOffset() > startPos && el.getStartOffset() < endPos) {

                boolean thisLineBlank = isWhiteSpaceOnly(getElementContents(doc, el));
                DocumentAction update = null;
    
                if (thisLineBlank) {
                    if (lastLineWasBlank) {
                        // Consecutive blank lines; remove this one:
                        if (el.getEndOffset() <= doc.getLength())
                        {
                            update = new DocumentRemoveLineAction(el);
                            perfect = false;
                        }
                    }
                    else {
                        // Single blank line (thus far), remove all spaces from
                        // it (and don't interrupt perfect status):
                        update = new DocumentIndentAction(el, "");
                    }
                }
                else {
                    NodeAndPosition<ParsedNode> root = new NodeAndPosition<ParsedNode>(doc.getParser(),
                            0, doc.getParser().getSize());
                    String indent = calculateIndent(el, root, ii, doc);
                    update = new DocumentIndentAction(el, indent);
                    perfect = perfect && getElementContents(doc, el).startsWith(indent);
                }
    
                if (update != null)
                    updates.add(update);
                lastLineWasBlank = thisLineBlank;
            }
        }

        // Now apply them all:
        for (DocumentAction update : updates) {
            update.apply(doc);
        }

        return perfect;
    }

    /**
     * Finds the indent for the given element by looking at the nodes in the parse tree
     * 
     * @param el The element to calculate the indent for
     * @param start The Node that is either the one directly containing the given element,
     *              or is an ancestor of the one that directly contains the given element,
     *              or may not contain the element at all (in which case null will be returned)
     * @param startIC The IndentCalculator corresponding to start
     * @param doc The document involved
     * @return The indent that the element should have, up to the first non-whitespace character.
     *         Returns null if start does not contain the given element
     */
    private static String calculateIndent(Element el,
            NodeAndPosition<ParsedNode> start, IndentCalculator startIC, MoeSyntaxDocument doc)
    {
        int pos = el.getStartOffset() + findFirstNonIndentChar(getElementContents(doc, el), true);
        if (pos >= start.getPosition() && pos < start.getEnd()) {

            // The slightly awkward way to loop through the children of "start":
            for (Iterator<NodeAndPosition<ParsedNode>> i = start.getNode().getChildren(start.getPosition()); i.hasNext(); )
            {
                NodeAndPosition<ParsedNode> nap = i.next();
                String inner = calculateIndent(el, nap, startIC.getForChild(nap.getNode()), doc);
                if (inner != null)
                    return inner;
            }
            try {
                return startIC.getCurIndent(doc.getText(pos, 1).charAt(0));
            }
            catch (BadLocationException e) {
                return "";
            }
        }
        else {
            return null;
        }
    }
    
    // ---------------------------------------
    // Indent calculation:
    

    /**
     * An interface that calculates the indentation level that
     * the corresponding node should have.  You should use getForChild as you
     * descend the parse tree to get the indentation for child nodes.
     */
    private static interface IndentCalculator
    {
        /**
         * Gets the IndentCalculator for the given child node of the node that this
         * IndentCalculator instance corresponds to
         */
        public IndentCalculator getForChild(ParsedNode n);
        /**
         * Gets the indent for a line in the current node that begins with the
         * given character.  This allows for comments (such as this one right here)
         * to have their leading asterisks indented by an extra space. 
         */
        public String getCurIndent(char beginsWith);
    }
    
    /**
     * An implementation of IndentCalculator for the root node of the document.
     */
    private static class RootIndentCalculator implements IndentCalculator
    {
        public IndentCalculator getForChild(ParsedNode n)
        {
            return new NodeIndentCalculator("", n);
        }

        public String getCurIndent(char beginsWith)
        {
            return "";
        }
    }
    
    private static class NodeIndentCalculator implements IndentCalculator
    {
        private final String existingIndent;
        private final ParsedNode parent;

        private final static String STANDARD_INDENT = "    ";
        private final static String CONTINUATION_INDENT = "    ";
        // To make it line up like this:
        // /**
        //  *
        //  *
        //  */
        // This must be a single space:
        private final static String COMMENT_ASTERISK_INDENT = " ";

        public NodeIndentCalculator(String existingIndent, ParsedNode parent)
        {
            this.existingIndent = existingIndent;
            this.parent = parent;
        }

        public IndentCalculator getForChild(ParsedNode child)
        {
            String newIndent = existingIndent;

            // I realise that using instanceof is sinful, but because I need
            // to know the type of both the parent and the child node, there is no
            // easy way to fold this method into either the parent or child node type
            // (either would still use instanceof on the other), so I'm keeping
            // it here for now:         

            if (child instanceof TypeInnerNode)
                newIndent += STANDARD_INDENT;
            else if (parent instanceof MethodNode
                    && !(child instanceof CommentNode))
                // comments that are children of methods are actually the comment
                // before the method, and thus shouldn't be indented any differently
                newIndent += STANDARD_INDENT;
            else if (parent instanceof ContainerNode)
                newIndent += STANDARD_INDENT;
            else if (parent instanceof ExpressionNode
                    && child instanceof ExpressionNode)
                // Expressions that are children of expressions are function arguments,
                // and thus use the continuation indent:
                newIndent += CONTINUATION_INDENT;

            return new NodeIndentCalculator(newIndent, child);
        }

        public String getCurIndent(char beginsWith)
        {
            if (parent instanceof CommentNode && beginsWith == '*')
                return existingIndent + COMMENT_ASTERISK_INDENT;
            else
                return existingIndent;
        }
    }



    private interface DocumentAction
    {
        public void apply(MoeSyntaxDocument doc);

    }

    private static class DocumentRemoveLineAction implements DocumentAction
    {
        private Element lineToRemove;

        public DocumentRemoveLineAction(Element lineToRemove)
        {
            this.lineToRemove = lineToRemove;
        }

        public void apply(MoeSyntaxDocument doc)
        {
            try {
                doc.remove(lineToRemove.getStartOffset(), lineToRemove.getEndOffset() - lineToRemove.getStartOffset());
            }
            catch (BadLocationException e) {
                Debug.reportError("Problem while trying to remove line from document: "
                        + lineToRemove.getStartOffset() + "->" + lineToRemove.getEndOffset()
                        + " in document of size " + doc.getLength(), e);
            }
        }
    }

    /**
     * A class representing an update to the indentation on a line of the document.  This is different
     * to a LineAction because it intrinsically knows which line it needs to update
     */
    private static class DocumentIndentAction implements DocumentAction
    {
        private Element el;
        private String indent;

        public DocumentIndentAction(Element el, String indent)
        {
            this.el = el;
            this.indent = indent;
        }

        // Because we keep element references, we don't have to worry about the offsets
        // altering, because they will alter before we process the line, and thus
        // everything works nicely.
        public void apply(MoeSyntaxDocument doc)
        {
            String line = getElementContents(doc, el);
            int lengthPrevWhitespace = findFirstNonIndentChar(line, true);
            boolean anyTabs = false;
            for (char c : line.substring(0, lengthPrevWhitespace).toCharArray()) {
                if (c == '\t')
                    anyTabs = true;
            }
            // If we want to put in 4 spaces, and there are already exactly 4 tabs,
            // without the anyTabs check, we would leave the whitespace alone;
            // hence why we need the check:
            if (indent != null && (anyTabs || (indent.length() != lengthPrevWhitespace))) {
                try {
                    doc.replace(el.getStartOffset(), lengthPrevWhitespace,
                            indent, null);
                }
                catch (BadLocationException e) {
                    Debug.reportError("Error doing indent in DocumentUpdate", e);
                }
            }
        }
    }

    private static String getElementContents(MoeSyntaxDocument doc, Element el)
    {
        try {
            return doc.getText(el.getStartOffset(), el.getEndOffset() - el.getStartOffset());
        }
        catch (BadLocationException e) {
            Debug.reportError("Error getting element contents in document", e);
            return "";
        }
    }

    /**
     * Return true if s contains only whitespace (or nothing).
     */
    public static boolean isWhiteSpaceOnly(String s)
    {
        return s.trim().length() == 0;
    }

    /**
     * Find the position of the first non-indentation character in a string.
     * Indentation characters are <whitespace>, //, *, /*, /**.
     */
    public static int findFirstNonIndentChar(String s, boolean whitespaceOnly)
    {
        int cnt = 0;
        char ch = s.charAt(0);
    
        // if this line ends a comment, indent whitepace only;
        // otherwise indent across whitespace, asterisks and comment starts
    
        if (whitespaceOnly) {
            while (ch == ' ' || ch == '\t') { // SPACE or TAB
                cnt++;
                ch = s.charAt(cnt);
            }
        }
        else {
            while (ch == ' ' || ch == '\t' || ch == '*') { // SPACE, TAB or *
                cnt++;
                ch = s.charAt(cnt);
            }
            if ((s.charAt(cnt) == '/') && (s.charAt(cnt + 1) == '*'))
                cnt += 2;
        }
        return cnt;
    }
}