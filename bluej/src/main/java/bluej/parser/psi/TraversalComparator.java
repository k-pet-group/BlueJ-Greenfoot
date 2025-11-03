/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg
 
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
package bluej.parser.psi;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for comparing callback sequences between PSI-based visitor and
 * token-based parser implementations.
 * 
 * <p>This comparator is critical for validating that the new PSI-based {@link PsiCallbackVisitor}
 * produces identical callback sequences to the existing token-based parser. During
 * Phase 2 (Validation Infrastructure), this enables automated testing to verify correctness
 * before replacing production code.
 * 
 * <h2>Purpose</h2>
 * <p>The primary goal is to ensure that both parsing approaches invoke the same callbacks
 * in the same order with the same parameters. Any divergence indicates either:
 * <ul>
 *   <li>A bug in the PSI visitor implementation</li>
 *   <li>An undocumented behavior in the token parser</li>
 *   <li>A difference in how PSI structures Kotlin code</li>
 * </ul>
 * 
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * // Parse with both approaches
 * CallbackRecorder psiRecorder = new CallbackRecorder();
 * CallbackRecorder tokenRecorder = new CallbackRecorder();
 * 
 * psiVisitor.visitKtFile(file, psiRecorder);
 * tokenParser.parse(source, tokenRecorder);
 * 
 * // Compare results
 * ComparisonResult result = TraversalComparator.compare(psiRecorder, tokenRecorder);
 * 
 * if (!result.matches()) {
 *     System.err.println(result.getSummary());
 *     // Output shows differences for debugging
 * }
 * 
 * // Detailed statistics
 * String stats = TraversalComparator.compareStatistics(psiRecorder, tokenRecorder);
 * System.out.println(stats);
 * }</pre>
 * 
 * <h2>Comparison Strategy</h2>
 * <p>The comparison process validates multiple aspects:
 * <ol>
 *   <li><b>Count Verification:</b> Both recorders must have the same number of callbacks</li>
 *   <li><b>Sequence Matching:</b> Callbacks must appear in the same order</li>
 *   <li><b>Name Matching:</b> Each callback at position i must have the same name</li>
 *   <li><b>Parameter Comparison:</b> Optional deep comparison of callback parameters</li>
 * </ol>
 * 
 * <h2>Statistical Analysis</h2>
 * <p>Beyond simple comparison, the comparator provides statistical analysis showing:
 * <ul>
 *   <li>Distribution of callback types</li>
 *   <li>Frequency of each callback</li>
 *   <li>Quick identification of missing or extra callbacks</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p><b>Thread-safe.</b> This is a stateless utility class with only static methods.
 * All methods are pure functions that depend only on their parameters and produce
 * no side effects. Multiple threads can safely use these methods concurrently.
 * 
 * <h2>Performance Considerations</h2>
 * <p>The comparison algorithm is O(n) where n is the number of callbacks. For typical
 * source files (hundreds of callbacks), performance is negligible. For very large files
 * (thousands of callbacks), the comparison may take a few milliseconds.
 * 
 * <p>Parameter comparison, when enabled, adds overhead proportional to parameter count
 * and complexity but remains acceptable for testing scenarios.
 * 
 * @see CallbackRecorder Records callback sequences for comparison
 * @see PsiCallbackVisitor PSI-based implementation being validated
 * @see ComparisonResult Detailed comparison results
 */
public final class TraversalComparator {
    
    /**
     * Private constructor prevents instantiation of utility class.
     */
    private TraversalComparator() {
        throw new AssertionError("Utility class - do not instantiate");
    }
    
    // ==================== Core Comparison Methods ====================
    
    /**
     * Compares two CallbackRecorder instances for sequence equivalence.
     * 
     * <p>This is the primary validation method that checks whether PSI-based and
     * token-based parsing produce identical callback sequences. A match indicates
     * that the PSI visitor correctly replicates token parser behavior.
     * 
     * <p><b>Comparison Algorithm:</b>
     * <ol>
     *   <li>Verify callback counts match</li>
     *   <li>Compare callback names at each position</li>
     *   <li>Record all differences for debugging</li>
     *   <li>Generate comprehensive result with statistics</li>
     * </ol>
     * 
     * <p><b>Example usage:</b>
     * <pre>{@code
     * CallbackRecorder psiRecorder = new CallbackRecorder();
     * CallbackRecorder tokenRecorder = new CallbackRecorder();
     * 
     * // Execute both parsers
     * psiVisitor.visitKtFile(file, psiRecorder);
     * tokenParser.parse(source, tokenRecorder);
     * 
     * // Compare and validate
     * ComparisonResult result = TraversalComparator.compare(psiRecorder, tokenRecorder);
     * 
     * if (result.matches()) {
     *     System.out.println("✓ Parsers produce identical callback sequences");
     * } else {
     *     System.err.println("✗ Callback sequences differ:");
     *     System.err.println(result.getSummary());
     * }
     * }</pre>
     * 
     * @param psiRecorder Callbacks from PSI-based visitor traversal
     * @param tokenRecorder Callbacks from token-based parser traversal
     * @return Detailed comparison result with identified differences
     * @throws NullPointerException if either recorder is null
     */
    public static ComparisonResult compare(CallbackRecorder psiRecorder, 
                                          CallbackRecorder tokenRecorder) {
        Objects.requireNonNull(psiRecorder, "PSI recorder cannot be null");
        Objects.requireNonNull(tokenRecorder, "Token recorder cannot be null");
        
        List<String> differences = new ArrayList<>();
        
        List<CallbackRecord> psiRecords = psiRecorder.getRecords();
        List<CallbackRecord> tokenRecords = tokenRecorder.getRecords();
        
        // 1. Check callback counts
        if (psiRecords.size() != tokenRecords.size()) {
            differences.add(String.format(
                "Callback count mismatch: PSI=%d, Token=%d",
                psiRecords.size(), tokenRecords.size()));
        }
        
        // 2. Check callback sequence
        int minSize = Math.min(psiRecords.size(), tokenRecords.size());
        for (int i = 0; i < minSize; i++) {
            CallbackRecord psi = psiRecords.get(i);
            CallbackRecord token = tokenRecords.get(i);
            
            if (!psi.getCallbackName().equals(token.getCallbackName())) {
                differences.add(String.format(
                    "Callback mismatch at index %d: PSI='%s', Token='%s'",
                    i, psi.getCallbackName(), token.getCallbackName()));
                
                // For debugging: show surrounding context (3 before, 3 after)
                differences.add(buildContextString(psiRecords, tokenRecords, i));
            }
        }
        
        // 3. Check for extra callbacks
        if (psiRecords.size() > tokenRecords.size()) {
            for (int i = minSize; i < psiRecords.size(); i++) {
                differences.add(String.format(
                    "Extra PSI callback at index %d: '%s'",
                    i, psiRecords.get(i).getCallbackName()));
            }
        } else if (tokenRecords.size() > psiRecords.size()) {
            for (int i = minSize; i < tokenRecords.size(); i++) {
                differences.add(String.format(
                    "Missing PSI callback at index %d: '%s'",
                    i, tokenRecords.get(i).getCallbackName()));
            }
        }
        
        boolean matches = differences.isEmpty();
        return new ComparisonResult(matches, differences, 
                                   psiRecords.size(), tokenRecords.size());
    }
    
    /**
     * Compares two CallbackRecorder instances with detailed parameter analysis.
     * 
     * <p>This extended comparison validates not only callback sequence but also
     * the parameters passed to each callback. This catches subtle bugs where
     * the callback order is correct but the parameters differ.
     * 
     * <p><b>Warning:</b> Parameter comparison is more expensive and may produce
     * false positives if parameter representations differ between implementations.
     * Use this for detailed debugging when sequence comparison alone is insufficient.
     * 
     * @param psiRecorder Callbacks from PSI-based visitor traversal
     * @param tokenRecorder Callbacks from token-based parser traversal
     * @return Detailed comparison result including parameter differences
     * @throws NullPointerException if either recorder is null
     */
    public static ComparisonResult compareWithParameters(CallbackRecorder psiRecorder,
                                                        CallbackRecorder tokenRecorder) {
        Objects.requireNonNull(psiRecorder, "PSI recorder cannot be null");
        Objects.requireNonNull(tokenRecorder, "Token recorder cannot be null");
        
        List<String> differences = new ArrayList<>();
        
        List<CallbackRecord> psiRecords = psiRecorder.getRecords();
        List<CallbackRecord> tokenRecords = tokenRecorder.getRecords();
        
        // First perform basic sequence comparison
        if (psiRecords.size() != tokenRecords.size()) {
            differences.add(String.format(
                "Callback count mismatch: PSI=%d, Token=%d",
                psiRecords.size(), tokenRecords.size()));
        }
        
        // Then compare with parameters
        int minSize = Math.min(psiRecords.size(), tokenRecords.size());
        for (int i = 0; i < minSize; i++) {
            CallbackRecord psi = psiRecords.get(i);
            CallbackRecord token = tokenRecords.get(i);
            
            if (!psi.getCallbackName().equals(token.getCallbackName())) {
                differences.add(String.format(
                    "Callback mismatch at index %d: PSI='%s', Token='%s'",
                    i, psi.getCallbackName(), token.getCallbackName()));
            } else {
                // Names match - check parameters
                List<String> paramDiffs = compareParameters(psi, token);
                if (!paramDiffs.isEmpty()) {
                    differences.add(String.format(
                        "Parameter differences at index %d ('%s'):",
                        i, psi.getCallbackName()));
                    paramDiffs.forEach(diff -> differences.add("  " + diff));
                }
            }
        }
        
        // Check for extra callbacks
        if (psiRecords.size() > minSize) {
            for (int i = minSize; i < psiRecords.size(); i++) {
                differences.add(String.format("Extra PSI callback at index %d: '%s'",
                    i, psiRecords.get(i).getCallbackName()));
            }
        } else if (tokenRecords.size() > minSize) {
            for (int i = minSize; i < tokenRecords.size(); i++) {
                differences.add(String.format("Missing PSI callback at index %d: '%s'",
                    i, tokenRecords.get(i).getCallbackName()));
            }
        }
        
        boolean matches = differences.isEmpty();
        return new ComparisonResult(matches, differences,
                                   psiRecords.size(), tokenRecords.size());
    }
    
    /**
     * Compares callback parameters in detail.
     *
     * <p>This method performs deep comparison of parameter maps, checking for:
     * <ul>
     *   <li>Missing parameters in either implementation</li>
     *   <li>Extra parameters in either implementation</li>
     *   <li>Value differences for matching parameters</li>
     * </ul>
     *
     * <p>The comparison uses {@link #areParametersEqual(Object, Object)} to handle
     * both arrays and regular objects correctly, avoiding the reference equality
     * issue with {@code Objects.deepEquals()} for non-array objects.</p>
     *
     * @param psiRecord PSI callback record
     * @param tokenRecord Token callback record
     * @return List of parameter differences (empty if parameters match)
     */
    private static List<String> compareParameters(CallbackRecord psiRecord,
                                                  CallbackRecord tokenRecord) {
        List<String> diffs = new ArrayList<>();
        
        Map<String, Object> psiParams = psiRecord.getParameters();
        Map<String, Object> tokenParams = tokenRecord.getParameters();
        
        // Check for parameter count differences
        if (psiParams.size() != tokenParams.size()) {
            diffs.add(String.format(
                "Parameter count mismatch: PSI=%d, Token=%d",
                psiParams.size(), tokenParams.size()));
        }
        
        // Check for missing/extra parameters
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(psiParams.keySet());
        allKeys.addAll(tokenParams.keySet());
        
        for (String key : allKeys) {
            if (!psiParams.containsKey(key)) {
                diffs.add(String.format("Missing PSI parameter: '%s'", key));
            } else if (!tokenParams.containsKey(key)) {
                diffs.add(String.format("Missing Token parameter: '%s'", key));
            } else {
                // Compare values (handle arrays, nulls, etc.)
                Object psiValue = psiParams.get(key);
                Object tokenValue = tokenParams.get(key);
                
                if (!areParametersEqual(psiValue, tokenValue)) {
                    diffs.add(String.format(
                        "Parameter '%s' mismatch: PSI=%s, Token=%s",
                        key, formatValue(psiValue), formatValue(tokenValue)));
                }
            }
        }
        
        return diffs;
    }
    
    /**
     * Compares two parameter values for equality, handling arrays correctly.
     *
     * <p>This method fixes the issue with {@link Objects#deepEquals(Object, Object)}
     * which performs reference equality for non-array objects. Instead, this method:
     * <ul>
     *   <li>Uses {@code equals()} for regular objects (proper value equality)</li>
     *   <li>Uses {@link Arrays#deepEquals(Object[], Object[])} for arrays</li>
     *   <li>Handles null values correctly</li>
     * </ul>
     *
     * @param a First parameter value
     * @param b Second parameter value
     * @return true if parameters are equal, false otherwise
     */
    private static boolean areParametersEqual(Object a, Object b) {
        // Handle same reference
        if (a == b) {
            return true;
        }
        
        // Handle null cases
        if (a == null || b == null) {
            return false;
        }
        
        // Handle arrays - need deep comparison
        if (a.getClass().isArray() && b.getClass().isArray()) {
            // Both are arrays - use deep comparison
            if (a.getClass().getComponentType().isPrimitive()
                    || b.getClass().getComponentType().isPrimitive()) {
                // Primitive arrays need special handling
                return Arrays.deepEquals(new Object[]{a}, new Object[]{b});
            } else {
                // Object arrays
                return Arrays.deepEquals((Object[]) a, (Object[]) b);
            }
        }
        
        // For regular objects, use equals() for proper value equality
        return a.equals(b);
    }
    
    /**
     * Formats a parameter value for display in error messages.
     * 
     * <p>Handles special cases like nulls, arrays, and long strings to produce
     * concise, readable output.
     * 
     * @param value The parameter value to format
     * @return Formatted string representation
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value.getClass().isArray()) {
            return Arrays.deepToString(new Object[]{value});
        }
        
        String str = value.toString();
        if (str.length() > 50) {
            return str.substring(0, 47) + "...";
        }
        
        return str;
    }
    
    /**
     * Builds context string showing callbacks around a mismatch point.
     * 
     * <p>Shows 3 callbacks before and after the mismatch to help understand
     * the divergence pattern.
     * 
     * @param psiRecords PSI callback records
     * @param tokenRecords Token callback records
     * @param mismatchIndex Index where mismatch occurred
     * @return Formatted context string
     */
    private static String buildContextString(List<CallbackRecord> psiRecords,
                                            List<CallbackRecord> tokenRecords,
                                            int mismatchIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("  Context (showing 3 before/after mismatch):\n");
        
        int start = Math.max(0, mismatchIndex - 3);
        int end = Math.min(Math.min(psiRecords.size(), tokenRecords.size()), mismatchIndex + 4);
        
        sb.append("  PSI:   ");
        for (int i = start; i < end; i++) {
            if (i == mismatchIndex) sb.append("[");
            sb.append(psiRecords.get(i).getCallbackName());
            if (i == mismatchIndex) sb.append("]");
            if (i < end - 1) sb.append(" → ");
        }
        sb.append("\n");
        
        sb.append("  Token: ");
        for (int i = start; i < end; i++) {
            if (i == mismatchIndex) sb.append("[");
            sb.append(tokenRecords.get(i).getCallbackName());
            if (i == mismatchIndex) sb.append("]");
            if (i < end - 1) sb.append(" → ");
        }
        
        return sb.toString();
    }
    
    // ==================== Statistical Analysis Methods ====================
    
    /**
     * Calculates callback statistics from a recorder.
     * 
     * <p>Returns a map showing how many times each callback was invoked.
     * Useful for understanding callback distribution and identifying patterns.
     * 
     * <p><b>Example output:</b>
     * <pre>{@code
     * {
     *   "beginClass": 5,
     *   "endClass": 5,
     *   "gotMethodDeclaration": 15,
     *   "gotField": 8
     * }
     * }</pre>
     * 
     * @param recorder The callback recorder to analyze
     * @return Map of callback names to invocation counts
     * @throws NullPointerException if recorder is null
     */
    public static Map<String, Integer> getCallbackStatistics(CallbackRecorder recorder) {
        Objects.requireNonNull(recorder, "Recorder cannot be null");
        
        Map<String, Integer> stats = new HashMap<>();
        
        for (CallbackRecord record : recorder.getRecords()) {
            String name = record.getCallbackName();
            stats.merge(name, 1, Integer::sum);
        }
        
        return stats;
    }
    
    /**
     * Compares callback statistics between two recorders.
     * 
     * <p>Produces a human-readable comparison showing callback frequencies
     * side-by-side. Each line shows a callback name, PSI count, and Token count,
     * with a checkmark (✓) if counts match or an X (✗) if they differ.
     * 
     * <p><b>Example output:</b>
     * <pre>
     * Callback Statistics Comparison:
     *   ✓ beginClass                PSI:   5, Token:   5
     *   ✗ endClass                  PSI:   4, Token:   5
     *   ✓ gotMethodDeclaration      PSI:  15, Token:  15
     *   ✗ gotField                  PSI:   9, Token:   8
     * </pre>
     * 
     * @param psiRecorder PSI-based callback recorder
     * @param tokenRecorder Token-based callback recorder
     * @return Formatted statistics comparison string
     * @throws NullPointerException if either recorder is null
     */
    public static String compareStatistics(CallbackRecorder psiRecorder,
                                          CallbackRecorder tokenRecorder) {
        Objects.requireNonNull(psiRecorder, "PSI recorder cannot be null");
        Objects.requireNonNull(tokenRecorder, "Token recorder cannot be null");
        
        Map<String, Integer> psiStats = getCallbackStatistics(psiRecorder);
        Map<String, Integer> tokenStats = getCallbackStatistics(tokenRecorder);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Callback Statistics Comparison:\n");
        
        Set<String> allCallbacks = new HashSet<>();
        allCallbacks.addAll(psiStats.keySet());
        allCallbacks.addAll(tokenStats.keySet());
        
        // Sort callbacks for consistent output
        List<String> sortedCallbacks = allCallbacks.stream()
            .sorted()
            .collect(Collectors.toList());
        
        for (String callback : sortedCallbacks) {
            int psiCount = psiStats.getOrDefault(callback, 0);
            int tokenCount = tokenStats.getOrDefault(callback, 0);
            
            String status = psiCount == tokenCount ? "✓" : "✗";
            sb.append(String.format("  %s %-30s PSI: %3d, Token: %3d\n",
                                   status, callback, psiCount, tokenCount));
        }
        
        return sb.toString();
    }
    
    // ==================== ComparisonResult Class ====================
    
    /**
     * Result of comparing two callback sequences.
     * 
     * <p>This immutable result object contains:
     * <ul>
     *   <li>Match status (true if sequences are identical)</li>
     *   <li>List of all differences found</li>
     *   <li>Callback counts from both recorders</li>
     *   <li>Human-readable summary for debugging</li>
     * </ul>
     * 
     * <p><b>Usage example:</b>
     * <pre>{@code
     * ComparisonResult result = TraversalComparator.compare(psi, token);
     * 
     * if (!result.matches()) {
     *     System.err.println("Validation failed!");
     *     System.err.println(result.getSummary());
     *     
     *     // Detailed analysis
     *     for (String diff : result.getDifferences()) {
     *         System.err.println("  - " + diff);
     *     }
     *     
     *     fail("Callback sequences do not match");
     * }
     * }</pre>
     * 
     * <p><b>Immutability:</b> Once created, this result cannot be modified.
     * All collections returned are unmodifiable views.
     */
    public static class ComparisonResult {
        private final boolean matches;
        private final List<String> differences;
        private final int psiCallbackCount;
        private final int tokenCallbackCount;
        
        /**
         * Creates a new comparison result.
         * 
         * @param matches true if callback sequences match exactly
         * @param differences List of identified differences (empty if matches is true)
         * @param psiCallbackCount Number of callbacks in PSI recorder
         * @param tokenCallbackCount Number of callbacks in Token recorder
         */
        public ComparisonResult(boolean matches, List<String> differences,
                               int psiCallbackCount, int tokenCallbackCount) {
            this.matches = matches;
            this.differences = new ArrayList<>(differences); // Defensive copy
            this.psiCallbackCount = psiCallbackCount;
            this.tokenCallbackCount = tokenCallbackCount;
        }
        
        /**
         * Returns whether the callback sequences match.
         * 
         * @return true if sequences are identical, false if differences exist
         */
        public boolean matches() {
            return matches;
        }
        
        /**
         * Returns the list of identified differences.
         * 
         * <p>Each string in the list describes a specific difference, such as:
         * <ul>
         *   <li>"Callback count mismatch: PSI=50, Token=51"</li>
         *   <li>"Callback mismatch at index 15: PSI='beginClass', Token='beginInterface'"</li>
         *   <li>"Extra PSI callback at index 48: 'endClass'"</li>
         * </ul>
         * 
         * @return Unmodifiable list of differences (empty if sequences match)
         */
        public List<String> getDifferences() {
            return Collections.unmodifiableList(differences);
        }
        
        /**
         * Returns the number of callbacks in the PSI recorder.
         * 
         * @return PSI callback count
         */
        public int getPsiCallbackCount() {
            return psiCallbackCount;
        }
        
        /**
         * Returns the number of callbacks in the Token recorder.
         * 
         * @return Token callback count
         */
        public int getTokenCallbackCount() {
            return tokenCallbackCount;
        }
        
        /**
         * Returns a human-readable summary of the comparison.
         * 
         * <p>The summary includes:
         * <ul>
         *   <li>Overall match status (MATCH or MISMATCH)</li>
         *   <li>Callback counts from both recorders</li>
         *   <li>Detailed list of all differences (if any)</li>
         * </ul>
         * 
         * <p><b>Example output for matching sequences:</b>
         * <pre>
         * Comparison Result: MATCH
         * PSI callbacks: 50
         * Token callbacks: 50
         * </pre>
         * 
         * <p><b>Example output for mismatched sequences:</b>
         * <pre>
         * Comparison Result: MISMATCH
         * PSI callbacks: 49
         * Token callbacks: 50
         * 
         * Differences:
         *   - Callback count mismatch: PSI=49, Token=50
         *   - Missing PSI callback at index 48: 'endClass'
         * </pre>
         * 
         * @return Formatted summary string suitable for logging or display
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Comparison Result: ").append(matches ? "MATCH" : "MISMATCH").append("\n");
            sb.append("PSI callbacks: ").append(psiCallbackCount).append("\n");
            sb.append("Token callbacks: ").append(tokenCallbackCount).append("\n");
            
            if (!differences.isEmpty()) {
                sb.append("\nDifferences:\n");
                for (String diff : differences) {
                    sb.append("  - ").append(diff).append("\n");
                }
            }
            
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return getSummary();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComparisonResult that = (ComparisonResult) o;
            return matches == that.matches &&
                   psiCallbackCount == that.psiCallbackCount &&
                   tokenCallbackCount == that.tokenCallbackCount &&
                   Objects.equals(differences, that.differences);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(matches, differences, psiCallbackCount, tokenCallbackCount);
        }
    }
}