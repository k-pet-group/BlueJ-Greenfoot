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
package bluej.editor.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import bluej.debugger.gentype.MethodReflective;
import bluej.editor.fixes.SuggestionList.SuggestionDetails;
import bluej.editor.fixes.SuggestionList.SuggestionDetailsWithHTMLDoc;
import bluej.editor.fixes.SuggestionList.SuggestionShown;
import bluej.parser.AssistContent;
import bluej.parser.AssistContentThreadSafe;
import bluej.parser.kotlin.KotlinParserUtils;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate.JavaMember;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate.Parameter;
import bluej.pkgmgr.JavadocResolver;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A Kotlin completion suggestion as shown in the editor: its popup row, documentation and insertion.
 * Java members are documented (and their parameters named) from Javadoc, as in Java completion.
 */
@OnThread(Tag.FXPlatform)
public class KotlinAssistContent extends AssistContent
{
    // Parameter names of Java methods compiled without parameter metadata.
    private static final Pattern SYNTHETIC_PARAMETER_NAME = Pattern.compile("p\\d+");

    private final KotlinCompletionCandidate candidate;
    // From Javadoc, for Java members; null if unavailable.
    private final String javadoc;
    private final List<String> javadocParameterNames;

    private KotlinAssistContent(KotlinCompletionCandidate candidate, String javadoc, List<String> javadocParameterNames)
    {
        this.candidate = candidate;
        this.javadoc = javadoc;
        this.javadocParameterNames = javadocParameterNames;
    }

    /**
     * Create the editor items for completion candidates, looking up the Javadoc of Java members
     * (one lookup per declaring class).
     *
     * @param candidates      the candidates
     * @param javadocResolver the project's Javadoc resolver
     * @param classLoader     loads a project or library class by name, returning null if unavailable
     * @return one item per candidate, in the same order
     */
    public static List<KotlinAssistContent> create(List<KotlinCompletionCandidate> candidates,
                                                   JavadocResolver javadocResolver, Function<String, Class<?>> classLoader)
    {
        Map<JavaMember, MethodReflective> documented = lookUpJavadoc(candidates, javadocResolver, classLoader);
        List<KotlinAssistContent> items = new ArrayList<>();
        for (KotlinCompletionCandidate candidate : candidates) {
            MethodReflective method = candidate.javaMember() == null ? null : documented.get(candidate.javaMember());
            String doc = method == null || method.getJavaDoc() == null || method.getJavaDoc().isEmpty() ? null : method.getJavaDoc();
            items.add(new KotlinAssistContent(candidate, doc, method == null ? null : method.getParamNames()));
        }
        return items;
    }

    // Resolve the Javadoc of all Java members among the candidates, batched by declaring class.
    private static Map<JavaMember, MethodReflective> lookUpJavadoc(List<KotlinCompletionCandidate> candidates,
                                                                   JavadocResolver javadocResolver,
                                                                   Function<String, Class<?>> classLoader)
    {
        Map<String, Set<JavaMember>> membersByClass = candidates.stream().map(KotlinCompletionCandidate::javaMember)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(JavaMember::className, LinkedHashMap::new, Collectors.toSet()));
        Map<JavaMember, MethodReflective> documented = new HashMap<>();
        membersByClass.forEach((className, members) -> {
            try {
                Class<?> cls = classLoader.apply(className);
                if (cls == null) {
                    return;
                }
                JavaReflective reflective = new JavaReflective(cls);
                Map<String, Set<MethodReflective>> methods = reflective.getDeclaredMethods();
                Map<JavaMember, MethodReflective> found = new HashMap<>();
                for (JavaMember member : members) {
                    for (MethodReflective method : methods.getOrDefault(member.methodName(), Set.of())) {
                        if (erasedParameterTypes(method).equals(member.parameterTypes())) {
                            found.put(member, method);
                        }
                    }
                }
                javadocResolver.getJavadoc(reflective, found.values());
                documented.putAll(found);
            }
            catch (LinkageError e) {
                // A class that cannot be loaded (e.g. a missing dependency) is simply not documented.
            }
        });
        return documented;
    }

    private static List<String> erasedParameterTypes(MethodReflective method)
    {
        return method.getParamTypes().stream().map(type -> type.getErasedType().toString()).toList();
    }

    @Override
    @OnThread(Tag.Any)
    public String getName()
    {
        return candidate.prefix() + candidate.name();
    }

    @Override
    public List<ParamInfo> getParams()
    {
        if (candidate.parameters() == null) {
            return null;
        }
        List<ParamInfo> params = new ArrayList<>();
        for (int i = 0; i < candidate.parameters().size(); i++) {
            Parameter parameter = candidate.parameters().get(i);
            String name = parameterName(i);
            String description = name == null ? null : getParamDescription(name);
            params.add(new ParamInfo(parameter.type(), name, ParamInfo.makeDummyName(name != null ? name : parameter.type()),
                () -> description));
        }
        return params;
    }

    // The parameter's real name, or null if only a synthetic one (p0, p1, ...) is known.
    private String parameterName(int index)
    {
        String name = candidate.parameters().get(index).name();
        if (candidate.javaMember() != null && SYNTHETIC_PARAMETER_NAME.matcher(name).matches()) {
            return javadocParameterNames != null && index < javadocParameterNames.size()
                ? javadocParameterNames.get(index) : null;
        }
        return name;
    }

    private String getParamDescription(String name)
    {
        JavaUtils.Javadoc parsed = JavaUtils.parseJavadoc(getJavadoc());
        return parsed == null ? null : parsed.getParamDescription(name);
    }

    @Override
    public String getType()
    {
        return candidate.type();
    }

    @Override
    public Access getAccessPermission()
    {
        return candidate.access();
    }

    @Override
    public String getDeclaringClass()
    {
        String declaringClass = candidate.declaringClass();
        return declaringClass == null ? null : declaringClass.substring(declaringClass.lastIndexOf('.') + 1);
    }

    @Override
    public CompletionKind getKind()
    {
        return candidate.kind();
    }

    /**
     * Get the documentation: Javadoc for Java members, otherwise the KDoc converted to Javadoc form.
     *
     * @return the documentation text, or null if there is none
     */
    @Override
    public String getJavadoc()
    {
        return javadoc != null ? javadoc : KotlinParserUtils.kdocToJavadoc(candidate.kdoc());
    }

    /**
     * Describe this item's popup row in Kotlin syntax: the name, then the parameters and type, e.g.
     * {@code roll(steps: Int, fast: Boolean): Int} or {@code world: World}. The separate type column
     * used by Java completion is left empty.
     *
     * @return the popup row, with documentation
     */
    public SuggestionDetails getSuggestionDetails()
    {
        return new SuggestionDetailsWithHTMLDoc(getName(), signatureAfterName(false, text -> text), null,
            SuggestionShown.COMMON, getDocHTML());
    }

    /**
     * Get the HTML documentation, headed by the declaration in Kotlin syntax, e.g.
     * {@code roll(steps: Int = ..., fast: Boolean): Int}.
     *
     * @return the HTML document
     */
    public String getDocHTML()
    {
        String header = "<b>" + getName() + "</b>" + signatureAfterName(true, Utility::escapeAngleBrackets);
        return AssistContentThreadSafe.makeDocHTML(header, getJavadoc());
    }

    // The declaration after its name: "(steps: Int, fast: Boolean): Int" or ": World". Parameters without a
    // known name show just their type. Default values are marked "= ..." only if showDefaults.
    private String signatureAfterName(boolean showDefaults, UnaryOperator<String> escapeType)
    {
        StringBuilder signature = new StringBuilder();
        List<ParamInfo> params = getParams();
        if (params != null) {
            List<String> rendered = new ArrayList<>();
            for (int i = 0; i < params.size(); i++) {
                Parameter parameter = candidate.parameters().get(i);
                String name = params.get(i).getFormalName();
                rendered.add((parameter.vararg() ? "vararg " : "") + (name != null ? name + ": " : "")
                    + escapeType.apply(parameter.type())
                    + (showDefaults && parameter.optional() && !parameter.vararg() ? " = ..." : ""));
            }
            signature.append("(").append(String.join(", ", rendered)).append(")");
        }
        if (getType() != null) {
            signature.append(": ").append(escapeType.apply(getType()));
        }
        return signature.toString();
    }

    /**
     * Describe what choosing this item inserts. Properties and variables insert their name. Calls get
     * placeholders for the required arguments only (parameters with defaults are left out), and a
     * required function-typed last parameter becomes a trailing lambda: {@code repeat(_times_) { }}.
     *
     * @return the insertion
     */
    public CompletionInsertion getInsertion()
    {
        List<ParamInfo> params = getParams();
        if (params == null) {
            return CompletionInsertion.text(getName());
        }
        List<String> arguments = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            if (!candidate.parameters().get(i).optional()) {
                arguments.add(params.get(i).getDummyName());
            }
        }
        List<Parameter> parameters = candidate.parameters();
        Parameter last = parameters.isEmpty() ? null : parameters.getLast();
        if (last != null && last.functionType() && !last.optional()) {
            return CompletionInsertion.trailingLambdaCall(getName(), arguments.subList(0, arguments.size() - 1));
        }
        return CompletionInsertion.call(getName(), arguments);
    }
}
