/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej.parser.kotlin;

import java.util.List;

import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Plans bounded Kotlin source edits using PSI only for structure and offsets.
 * Original source is never reformatted and no PSI mutation services are needed.
 */
@OnThread(Tag.Any)
public final class KotlinSourceEditor
{
    /**
     * One enclosing source replacement, suitable for a single editor undo operation.
     *
     * @param start inclusive zero-based UTF-16 offset in the original source
     * @param end exclusive zero-based UTF-16 offset in the original source
     * @param replacement text to insert in place of that range
     * @param newMethod whether this edit creates the preparation method
     */
    public record Edit(int start, int end, String replacement, boolean newMethod)
    {
    }

    private enum InitializationCall
    {
        NONE, PRESENT, AMBIGUOUS
    }

    private KotlinSourceEditor()
    {
    }

    /**
     * Rename matching identifier tokens in a source copy without resolving their bindings.
     * Comments and literal text are not identifiers; unrelated same-named identifiers may change.
     *
     * @param source complete source to copy
     * @param originalName unquoted source class name
     * @param destinationName validated, unquoted name for the new class
     * @return source with matching identifiers renamed, without modifying the PSI tree
     */
    public static String renameIdentifiers(String source, String originalName, String destinationName)
    {
        KtFile file = KotlinEnvironmentManager.getPsiFactory().createFile(source);
        String escapedName = "`" + originalName + "`";
        StringBuilder result = new StringBuilder(file.getText());
        for (PsiElement leaf = PsiTreeUtil.getDeepestLast(file); leaf != null; leaf = PsiTreeUtil.prevLeaf(leaf)) {
            if (leaf.getNode().getElementType() == KtTokens.IDENTIFIER
                && (leaf.getText().equals(originalName) || leaf.getText().equals(escapedName))) {
                result.replace(leaf.getTextRange().getStartOffset(), leaf.getTextRange().getEndOffset(), destinationName);
            }
        }
        return result.toString();
    }

    /**
     * Append statements to a compatible preparation method, creating a private method if absent.
     * A final init block is added only when no ordinary initialization call already exists.
     * This is syntax-only: it does not resolve inherited members or analyse constructor paths.
     * Calls in stored lambdas are ignored; calls in lambda arguments are ambiguous without knowing
     * whether the receiving function executes them, so those cases are declined.
     *
     * @param source current complete source, not previously compiled source
     * @param className exact simple name of the top-level class to edit
     * @param methodName ordinary unquoted Kotlin identifier for the preparation method
     * @param statements separate statements copied verbatim without validation or conversion;
     *                   only each statement's first line is prefixed with indentation
     * @param documentation plain text for a new method's KDoc; ignored for existing methods
     * @return one edit, or null for unsupported/ambiguous structure or malformed input source;
     *         an empty or blank-only list produces a zero-width empty edit without creating anything
     */
    public static Edit appendPreparation(String source, String className, String methodName,
                                         List<String> statements, String documentation)
    {
        if (source == null || className == null || statements == null || documentation == null
            || !KotlinParserUtils.isCreationIdentifier(methodName)) {
            return null;
        }
        KtFile file = KotlinEnvironmentManager.getPsiFactory().createFile(source);
        if (!file.getText().equals(source) || PsiTreeUtil.hasErrorElements(file)) {
            return null;
        }
        KtClass target = null;
        for (KtDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof KtNamedDeclaration named && className.equals(named.getName())) {
                if (target != null || !(declaration instanceof KtClass cls)
                    || cls.isInterface() || cls.isEnum() || cls.isAnnotation() || cls.isValue()) {
                    return null;
                }
                target = cls;
            }
        }
        if (target == null) {
            return null;
        }

        KtClassBody body = target.getBody();
        if (body != null && (body.getLBrace() == null || body.getRBrace() == null)) {
            return null;
        }
        KtNamedFunction method = null;
        for (KtDeclaration declaration : target.getDeclarations()) {
            if (declaration instanceof KtNamedDeclaration named && methodName.equals(named.getName())) {
                if (method != null || !(declaration instanceof KtNamedFunction function) || !compatible(function)) {
                    return null;
                }
                method = function;
            }
        }
        // These names can also be visible to an unqualified call; do not guess which callable is meant.
        if (target.getPrimaryConstructorParameters().stream().anyMatch(p -> methodName.equals(p.getName()))
            || target.getCompanionObjects().stream().anyMatch(c -> declaresName(c, methodName))) {
            return null;
        }
        boolean called = false;
        for (KtDeclaration declaration : target.getDeclarations()) {
            PsiElement initializer = null;
            boolean shadowed = false;
            if (declaration instanceof KtAnonymousInitializer init) {
                initializer = init.getBody();
            } else if (declaration instanceof KtProperty property) {
                initializer = property.getDelegateExpressionOrInitializer();
            } else if (declaration instanceof KtSecondaryConstructor constructor) {
                initializer = constructor.getBodyExpression();
                shadowed = constructor.getValueParameters().stream().anyMatch(p -> methodName.equals(p.getName()));
            }
            InitializationCall call = initializationCall(initializer, methodName,
                shadowed || declaresName(initializer, methodName));
            if (call == InitializationCall.AMBIGUOUS) {
                return null;
            }
            called |= call == InitializationCall.PRESENT;
        }
        if (statements.stream().allMatch(String::isBlank)) {
            return new Edit(0, 0, "", false);
        }

        String newline = source.contains("\r\n") ? "\r\n" : "\n";
        String classIndent = lineIndent(source, target.getTextRange().getStartOffset());
        if (classIndent == null) {
            classIndent = "";
        }
        String memberIndent = childIndent(source, target.getDeclarations(), classIndent, "    ");
        String indentUnit = memberIndent.substring(classIndent.length());
        StringBuilder changed = new StringBuilder(source);
        String additions = "";
        if (method == null) {
            additions = newMethod(methodName, statements, documentation, memberIndent, indentUnit, newline);
        }
        if (!called) {
            additions += (additions.isEmpty() ? "" : newline) + memberIndent + "init {" + newline
                + memberIndent + indentUnit + methodName + "()" + newline + memberIndent + "}" + newline;
        }
        if (body == null) {
            // Include a same-line comment before starting the braces on a fresh line.
            changed.insert(bodylessEnd(target), newline + classIndent + "{" + newline + additions + classIndent + "}");
        } else if (!additions.isEmpty()) {
            boolean hasContent = !source.substring(body.getLBrace().getTextRange().getEndOffset(),
                body.getRBrace().getTextRange().getStartOffset()).isBlank();
            insertBeforeBrace(changed, source, body.getRBrace(),
                (hasContent ? newline : "") + additions, classIndent, newline);
        }
        // The class-tail insertion is later in the source, so the original method offsets still apply.
        if (method != null) {
            String methodIndent = lineIndent(source, method.getTextRange().getStartOffset());
            if (methodIndent == null) {
                methodIndent = memberIndent;
            }
            KtBlockExpression block = method.getBodyBlockExpression();
            String statementIndent = childIndent(source, block.getStatements(), methodIndent, indentUnit);
            insertBeforeBrace(changed, source, block.getRBrace(),
                statementText(statements, statementIndent, newline), methodIndent, newline);
        }
        return enclosingEdit(source, changed.toString(), method == null);
    }

    private static boolean compatible(KtNamedFunction function)
    {
        KtTypeReference returnType = function.getTypeReference();
        return function.getValueParameterList() != null && function.getValueParameters().isEmpty()
            && function.getTypeParameters().isEmpty() && function.getReceiverTypeReference() == null
            && function.getContextReceivers().isEmpty() && function.hasBlockBody()
            && function.getBodyBlockExpression() != null
            && function.getBodyBlockExpression().getLBrace() != null && function.getBodyBlockExpression().getRBrace() != null
            && !function.hasModifier(KtTokens.SUSPEND_KEYWORD) && !function.hasModifier(KtTokens.EXTERNAL_KEYWORD)
            && !function.hasModifier(KtTokens.OPERATOR_KEYWORD) && !function.hasModifier(KtTokens.INFIX_KEYWORD)
            && !function.hasModifier(KtTokens.ABSTRACT_KEYWORD) && !function.hasModifier(KtTokens.EXPECT_KEYWORD)
            && (returnType == null || returnType.getText().equals("Unit") || returnType.getText().equals("kotlin.Unit"));
    }

    private static boolean declaresName(PsiElement element, String name)
    {
        if (element == null) {
            return false;
        }
        for (PsiElement child : element.getChildren()) {
            if (child instanceof KtNamedDeclaration named && name.equals(named.getName())) {
                return true;
            }
            if (!(child instanceof KtClassOrObject) && !(child instanceof KtNamedFunction)
                && !(child instanceof KtLambdaExpression) && declaresName(child, name)) {
                return true;
            }
        }
        return false;
    }

    private static InitializationCall initializationCall(PsiElement element, String name, boolean shadowed)
    {
        if (element == null || element instanceof KtClassOrObject) {
            return InitializationCall.NONE;
        }
        if (element instanceof KtNamedFunction function) {
            return function.isAnonymous() ? lambdaCall(function, function.getBodyExpression(), name) : InitializationCall.NONE;
        }
        if (element instanceof KtLambdaExpression lambda) {
            return lambdaCall(lambda, lambda.getBodyExpression(), name);
        }
        InitializationCall result = InitializationCall.NONE;
        if (element instanceof KtCallExpression call && call.getCalleeExpression() instanceof KtNameReferenceExpression ref
            && name.equals(ref.getReferencedName()) && call.getValueArgumentList() != null
            && call.getValueArguments().isEmpty() && call.getLambdaArguments().isEmpty() && call.getTypeArguments().isEmpty()) {
            if (call.getParent() instanceof KtQualifiedExpression qualified && qualified.getSelectorExpression() == call) {
                if (qualified instanceof KtDotQualifiedExpression && qualified.getReceiverExpression() instanceof KtThisExpression self) {
                    KtClass owner = PsiTreeUtil.getParentOfType(self, KtClass.class);
                    String label = self.getLabelName();
                    result = label == null || (owner != null && label.equals(owner.getName()))
                        ? InitializationCall.PRESENT : InitializationCall.AMBIGUOUS;
                }
            } else {
                result = shadowed ? InitializationCall.AMBIGUOUS : InitializationCall.PRESENT;
            }
        }
        for (PsiElement child : element.getChildren()) {
            InitializationCall nested = initializationCall(child, name, shadowed);
            if (nested == InitializationCall.AMBIGUOUS || result == InitializationCall.AMBIGUOUS) {
                return InitializationCall.AMBIGUOUS;
            }
            if (nested == InitializationCall.PRESENT) {
                result = nested;
            }
        }
        return result;
    }

    private static InitializationCall lambdaCall(PsiElement lambda, PsiElement body, String name)
    {
        PsiElement use = lambda;
        while (use.getParent() instanceof KtParenthesizedExpression || use.getParent() instanceof KtLabeledExpression) {
            use = use.getParent();
        }
        // A stored function value is deferred. A passed/invoked lambda might run now, including
        // with another receiver; neither counting it nor adding a second call is safe without resolution.
        if (use.getParent() instanceof KtValueArgument || use.getParent() instanceof KtCallExpression) {
            return initializationCall(body, name, false) == InitializationCall.NONE
                ? InitializationCall.NONE : InitializationCall.AMBIGUOUS;
        }
        return InitializationCall.NONE;
    }

    private static String newMethod(String name, List<String> statements, String documentation,
                                    String indent, String unit, String newline)
    {
        StringBuilder text = new StringBuilder();
        if (!documentation.isBlank()) {
            text.append(indent).append("/**").append(newline);
            for (String line : documentation.split("\\R", -1)) {
                // Keep supplied prose inside the generated comment even if it mentions a comment delimiter.
                text.append(indent).append(" * ").append(line.replace("/*", "/ *").replace("*/", "* /")).append(newline);
            }
            text.append(indent).append(" */").append(newline);
        }
        return text.append(indent).append("private fun ").append(name).append("() {").append(newline)
            .append(statementText(statements, indent + unit, newline))
            .append(indent).append("}").append(newline).toString();
    }

    private static String statementText(List<String> statements, String indent, String newline)
    {
        StringBuilder text = new StringBuilder();
        for (String statement : statements) {
            // Indent operations, not their embedded lines: those may belong to an unchanged raw argument.
            text.append(indent).append(statement);
            if (!statement.endsWith("\n")) {
                text.append(newline);
            }
        }
        return text.toString();
    }

    private static String childIndent(String source, List<? extends PsiElement> children, String parent, String fallback)
    {
        for (PsiElement child : children) {
            String indent = lineIndent(source, child.getTextRange().getStartOffset());
            if (indent != null && indent.startsWith(parent) && indent.length() > parent.length()) {
                return indent;
            }
        }
        return parent + fallback;
    }

    // Null means the element starts after other non-whitespace text on its line.
    private static String lineIndent(String source, int offset)
    {
        int lineStart = source.lastIndexOf('\n', offset - 1) + 1;
        String prefix = source.substring(lineStart, offset);
        return prefix.isBlank() ? prefix : null;
    }

    private static int bodylessEnd(KtClass target)
    {
        int end = target.getTextRange().getEndOffset();
        for (PsiElement sibling = target.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling instanceof PsiComment || (sibling instanceof PsiWhiteSpace && !sibling.getText().contains("\n"))) {
                end = sibling.getTextRange().getEndOffset();
            } else {
                break;
            }
        }
        return end;
    }

    private static void insertBeforeBrace(StringBuilder changed, String original, PsiElement brace,
                                          String text, String closingIndent, String newline)
    {
        int offset = brace.getTextRange().getStartOffset();
        String indent = lineIndent(original, offset);
        if (indent != null) {
            changed.insert(offset - indent.length(), text);
        } else {
            changed.insert(offset, newline + text + closingIndent);
        }
    }

    private static Edit enclosingEdit(String original, String changed, boolean newMethod)
    {
        int start = 0;
        while (start < original.length() && start < changed.length() && original.charAt(start) == changed.charAt(start)) {
            start++;
        }
        int end = original.length();
        int changedEnd = changed.length();
        while (end > start && changedEnd > start && original.charAt(end - 1) == changed.charAt(changedEnd - 1)) {
            end--;
            changedEnd--;
        }
        return new Edit(start, end, changed.substring(start, changedEnd), newMethod);
    }
}
