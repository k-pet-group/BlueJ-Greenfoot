package bluej.utility;

import javafx.util.Pair;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtils {
    public static <A, B> Stream<Pair<A, B>> zip(
            Stream<? extends A> a,
            Stream<? extends B> b
    ) {
        Iterator<? extends A> itA = a.iterator();
        Iterator<? extends B> itB = b.iterator();

        Iterable<Pair<A, B>> iterable = () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return itA.hasNext() && itB.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                return new Pair<>(itA.next(), itB.next());
            }
        };

        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <A, B> Stream<Pair<A, B>> zip(Collection<A> a, Collection<B> b) {
        return zip(a.stream(), b.stream());
    }

    public static <A, B> Stream<Pair<A, B>> zipLongest(
            Stream<? extends A> a,
            Stream<? extends B> b
    ) {
        Iterator<? extends A> itA = a.iterator();
        Iterator<? extends B> itB = b.iterator();

        Iterator<Pair<A, B>> iterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return itA.hasNext() || itB.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                A left = itA.hasNext() ? itA.next() : null;
                B right = itB.hasNext() ? itB.next() : null;
                return new Pair<>(left, right);
            }
        };

        Spliterator<Pair<A, B>> spliterator =
                Spliterators.spliteratorUnknownSize(iterator, 0);

        return StreamSupport.stream(spliterator, false);
    }

    public static <A, B> Stream<Pair<A, B>> zipLongest(Collection<A> a, Collection<B> b) {
        return zipLongest(a.stream(), b.stream());
    }
}
