/*
 * Copyright (C) 2014 heday
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package bluej.utility;

import bluej.editor.moe.CodeCompletionDisplay;
import bluej.parser.AssistContent;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;

/**
 * This class fills the html description of CodeCompletion using a SwingWorker.
 *
 * @author heday
 */
public class JavadocSwingWorker extends javax.swing.SwingWorker<String, String>
{
private final AssistContent selected;
private final CodeCompletionDisplay codeCompletionDisplay;
private static final Pattern headerPattern = Pattern.compile("{1,}\\s@\\w");
private static final Pattern paramNamePattern = Pattern.compile("param\\s+\\w"); //regular expression for the parameter name
private static final Pattern paramDescPattern = Pattern.compile("\\s+\\w"); //regurlar expression for the parameter description

    public JavadocSwingWorker(CodeCompletionDisplay  ccd, AssistContent aContent)
    {
        this.codeCompletionDisplay = ccd;
        this.selected = aContent;
    }


    /**
     * Convert javadoc comment body (as extracted by javadocToString for
     * instance) to HTML suitable for display by HTMLEditorKit.
     */
    private String javadocToHtml(String javadocString) throws InterruptedException
    {
        // find the first block tag
        
        Matcher matcher = headerPattern.matcher(javadocString);
        int i = -1;
        if (matcher.find()) {
            i = matcher.start();
        }
        if (i == -1) {//not found
            return makeCommentColour(javadocString);
        }
        // Process the block tags
        String header = javadocString.substring(0, i);
        String blocksText = javadocString.substring(i);
        String[] lines = Utility.splitLines(blocksText);

        List<String> blocks = getBlockTags(lines);

        StringBuilder rest = new StringBuilder();
        StringBuilder params = new StringBuilder();
        params.append("<h3>Parameters</h3>").append("<table border=0>");
        boolean hasParamDoc = false;
        

        for (String block : blocks) {
            matcher = paramNamePattern.matcher(block); //search the current block
            String paramName = "";
            String paramDesc = "";
            if (matcher.find()) {
                int p = matcher.end() - 1; //mark start of the parameter's name
                matcher = paramDescPattern.matcher(block.substring(p)); //search for the description on the rest of 
                //the parameter
                if (matcher.find()) {
                    int k = p + matcher.end() - 1;
                    paramName = block.substring(p, k);
                    paramDesc = block.substring(k);
                }
                //build the rest of the html.
                params.append("<tr><td valign=\"top\">&nbsp;&nbsp;&nbsp;");
                params.append(makeCommentColour(paramName));
                params.append("</td><td>");
                params.append(makeCommentColour(" - " + paramDesc));
                params.append("</td></tr>");
                hasParamDoc = true;
            } else {
                rest.append(convertBlockTag(block)).append("<br>");
            }
        }

        params.append("</table><p>");

        StringBuilder result = new StringBuilder(makeCommentColour(header));
        result.append((hasParamDoc ? params.toString() : "<p>")).append(rest.toString());
        return result.toString();
    }

    private String makeCommentColour(String text)
    {
        return "<font color='#994400'>" + text + "</font>";
    }

    /**
     * For a set of text lines representing block tags in a a javadoc comment,
     * with some block tags potentially flowing over more than one line, return
     * a list of Strings corresponding to each block tag with its complete text.
     */
    private List<String> getBlockTags(String[] lines) throws InterruptedException
    {
        LinkedList<String> blocks = new LinkedList<String>();
        StringBuilder cur = new StringBuilder();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("@")) {
                if (cur.length() > 0) {
                    blocks.addLast(cur.toString());
                }
                cur = new StringBuilder(line.substring(1));
            } else {
                //If it doesn't start with an at, it's part of the previous tag
                cur.append(" ").append(line);
            }
        }
        blocks.addLast(cur.toString());
        return blocks;
    }

    private String convertBlockTag(String block)
    {
        int k = block.indexOf(' ');
        String r = "<b>" + block.substring(0, k) + "</b> - " + makeCommentColour(block.substring(k));
        return r;
    }
        private static String escapeAngleBrackets(String sig)
    {
        return sig.replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    protected String doInBackground() 
    {
        try {
            publish("");
            if (selected != null) {
                String jdHtml = selected.getJavadoc();
                if (jdHtml != null) {
                    jdHtml = javadocToHtml(jdHtml);
                } else {
                    jdHtml = "";
                }
                String sig = escapeAngleBrackets(selected.getReturnType())
                        + " <b>" + escapeAngleBrackets(selected.getDisplayMethodName()) + "</b>"
                        + escapeAngleBrackets(selected.getDisplayMethodParams());

                jdHtml = "<h3>" + selected.getDeclaringClass() + "</h3>"
                        + "<blockquote><tt>" + sig + "</tt></blockquote><br>"
                        + jdHtml;
                return jdHtml;
            } else {
                return "";
            }
        } catch (InterruptedException e) {
            return "";
        }
    }
    
            @Override
        protected void process(List<String> chunks)
        {
            codeCompletionDisplay.setWorking(); // display glasspanel with "working" message.
        }

    @Override
    protected void done()
    {
        try {
            codeCompletionDisplay.setMethodDescriptionText(get());
        } catch (Exception ex) {
            codeCompletionDisplay.setMethodDescriptionText("");
        }
    }
        

}
