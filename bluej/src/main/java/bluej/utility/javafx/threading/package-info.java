/*
 This file is part of the BlueJ program.
 Copyright (C) 2026 Michael Kölling and John Rosenberg

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

/**
 * Functional interfaces and utilities for cross-thread execution on the
 * JavaFX platform thread.
 *
 * <h2>Interface hierarchy</h2>
 *
 * <p>Each functional shape (Runnable, Supplier, Consumer, Function,
 * BiConsumer, BiFunction) has four variants arranged in two dimensions:</p>
 *
 * <table>
 *   <caption>Interface variants by thread scope and exception handling</caption>
 *   <tr><th></th><th>non-throwing</th><th>throwing</th></tr>
 *   <tr><td>{@code FXPlatform}</td>
 *       <td>{@link bluej.utility.javafx.threading.FXPlatformConsumer FXPlatformConsumer}</td>
 *       <td>{@link bluej.utility.javafx.threading.FXPlatformConsumerThrowing FXPlatformConsumerThrowing}</td></tr>
 *   <tr><td>{@code FX}</td>
 *       <td>{@link bluej.utility.javafx.threading.FXConsumer FXConsumer}</td>
 *       <td>{@link bluej.utility.javafx.threading.FXConsumerThrowing FXConsumerThrowing}</td></tr>
 * </table>
 *
 * <p>The thread-scope inheritance goes {@code FX* extends FXPlatform*}.
 * This may look inverted at first glance — the <em>broader</em> type
 * extends the <em>narrower</em> one — but it is a direct consequence of
 * <strong>contravariance of the threading requirement on callbacks</strong>.
 * A functional interface's thread tag constrains where the callback
 * <em>may be invoked</em>; that is an input-side (contravariant) constraint,
 * so subtyping runs opposite to the set-inclusion of threads:</p>
 *
 * <ul>
 *   <li>{@code Tag.FXPlatform} ⊂ {@code Tag.FX} (the platform thread is
 *       always an FX thread, but not vice-versa).</li>
 *   <li>An {@code FXConsumer} (requires any FX thread) imposes a
 *       <em>weaker</em> precondition on its caller than
 *       {@code FXPlatformConsumer} (requires specifically the platform
 *       thread).</li>
 *   <li>By Liskov, the type with the weaker precondition is the
 *       <em>subtype</em>: {@code FXConsumer} IS-A
 *       {@code FXPlatformConsumer}.</li>
 * </ul>
 *
 * <p>Concretely: any call site that guarantees FXPlatform context can
 * safely invoke an {@code FXConsumer}, because FXPlatform ⊂ FX.</p>
 *
 * <h2>Throwing / non-throwing gap</h2>
 *
 * <p>Currently, the non-throwing variants do <em>not</em> extend their
 * throwing counterparts (e.g.&nbsp;{@code FXPlatformConsumer} does not extend
 * {@code FXPlatformConsumerThrowing}).  Such inheritance is technically
 * feasible in Java — the non-throwing {@code accept(T)} would override
 * {@code accept(T) throws Exception} by narrowing the throws clause, and
 * the interface would remain a valid {@code @FunctionalInterface}.</p>
 *
 * <p>In practice the gap rarely matters: a non-throwing lambda can already
 * be passed where a throwing interface is expected because the lambda simply
 * does not declare any checked exception.  The inheritance would only help
 * when passing a <em>typed variable</em> (e.g.&nbsp;passing an
 * {@code FXPlatformConsumer<T>} where {@code FXPlatformConsumerThrowing<T>}
 * is required).  If that pattern becomes common, adding the extends
 * relationship is a binary-compatible change.</p>
 *
 * <h2>Threading utilities</h2>
 *
 * <p>{@link bluej.utility.javafx.threading.JavaFXThreadingUtil} provides
 * the three-tier execution API:</p>
 * <ol>
 *   <li>{@code runPlatformLater} — always defers via {@code Platform.runLater},
 *       returns a {@code Future}.</li>
 *   <li>{@code runPlatform} — executes synchronously when already on the FX
 *       thread, otherwise delegates to {@code runPlatformLater}.</li>
 *   <li>{@code runPlatformAndWait} — blocks on {@code runPlatform().get()}.</li>
 * </ol>
 *
 * @see bluej.utility.javafx.threading.JavaFXThreadingUtil
 * @see bluej.utility.javafx.JavaFXUtil
 */
package bluej.utility.javafx.threading;
