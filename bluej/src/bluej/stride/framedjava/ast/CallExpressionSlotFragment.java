/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2019,2020 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import bluej.stride.framedjava.errors.EmptyError;
import bluej.editor.fixes.FixSuggestion;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.frames.IfFrame;
import bluej.stride.framedjava.frames.ReturnFrame;
import bluej.stride.framedjava.frames.SwitchFrame;
import bluej.stride.framedjava.frames.ThrowFrame;
import bluej.stride.framedjava.frames.WhileFrame;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.Frame;

/**
 * Created by neil on 04/12/2015.
 */
public class CallExpressionSlotFragment extends FilledExpressionSlotFragment
{
    private static final List<String> KEYWORDS = Arrays.asList("if", "while", "switch", "return", "throw"); 
    
    public CallExpressionSlotFragment(String content, String javaCode)
    {
        super(content, javaCode);
    }

    public CallExpressionSlotFragment(String content, String javaCode, ExpressionSlot slot)
    {
        super(content, javaCode, slot);
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        Stream<SyntaxCodeError> superErrors = super.findEarlyErrors();
        // Look for a blank frame and give an error:
        if (content.equals("()"))
        {
            return Stream.concat(Stream.of(new EmptyError(this, "Method name cannot be blank")), superErrors);
        }
        else if (content.endsWith(")") && KEYWORDS.stream().anyMatch(k -> content.startsWith(k + "(")))
        {
            String keyword = content.substring(0, content.indexOf("("));
            String innerStride = content.substring(content.indexOf("(") + 1, content.length() - 1);
            String javaCode = getJavaCode();
            String innerJava = javaCode.substring(javaCode.indexOf("(") + 1, javaCode.length() - 1);
            FixSuggestion fix = new ReplaceKeywordCallWithFrame(keyword, innerStride, innerJava);
            return Stream.concat(Stream.of(new SyntaxCodeError(this, keyword + " is not a valid method name") {
                @Override
                public List<FixSuggestion> getFixSuggestions()
                {
                    ArrayList<FixSuggestion> fixes = new ArrayList<>();
                    fixes.addAll(super.getFixSuggestions());
                    fixes.add(fix);
                    return fixes;
                }
            }), superErrors);
        }
        return superErrors;
    }

    private class ReplaceKeywordCallWithFrame extends FixSuggestion
    {
        private final String keyword;
        private final String innerStride;
        private final String innerJava;

        public ReplaceKeywordCallWithFrame(String keyword, String innerStride, String innerJava)
        {
            this.keyword = keyword;
            this.innerStride = innerStride;
            this.innerJava = innerJava;
        }

        @Override
        public String getDescription()
        {
            return "Replace with " + keyword + " frame";
        }

        @Override
        public void execute()
        {
            Frame frame = getSlot().getParentFrame();
            FilledExpressionSlotFragment inner = new FilledExpressionSlotFragment(innerStride, innerJava);
            switch (keyword)
            {
                case "if":
                    frame.getParentCanvas().replaceBlock(frame, new IfFrame(frame.getEditor(), inner, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, true));
                    break;
                case "while":
                    frame.getParentCanvas().replaceBlock(frame, new WhileFrame(frame.getEditor(), inner, true));
                    break;
                case "switch":
                    frame.getParentCanvas().replaceBlock(frame, new SwitchFrame(frame.getEditor(), inner, true));
                    break;
                case "return":
                    frame.getParentCanvas().replaceBlock(frame, new ReturnFrame(frame.getEditor(), inner, true));
                    break;
                case "throw":
                    frame.getParentCanvas().replaceBlock(frame, new ThrowFrame(frame.getEditor(), inner, true));
                    break;
            }
        }
    }
}
