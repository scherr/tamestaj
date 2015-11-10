package tamestaj;

import com.google.common.collect.ImmutableSet;

import java.util.*;

@SuppressWarnings("unused")
final class Partitioning<T> {
    private final class Entry<T> {
        private final T value;

        private int rank;
        private Entry<T> parent;
        private Entry<T> next;

        private Entry(T value) {
            this.value = value;

            this.rank = 0;
            this.parent = this;
            this.next = this;
        }
    }

    private final HashMap<T, Entry<T>> entryMap = new HashMap<>();

    private Entry<T> findRoot(Entry<T> entry) {
        if (entry.parent != entry) {
            entry.parent = findRoot(entry.parent);
        }
        return entry.parent;
    }

    void combinePartitions(T a, T b) {
        if (a == b) {
            return;
        }

        Entry<T> entryA = entryMap.get(a);
        if (entryA == null) {
            entryA = new Entry<>(a);
            entryMap.put(a, entryA);
        } else {
            entryA = findRoot(entryA);
        }

        Entry<T> entryB = entryMap.get(b);
        if (entryB == null) {
            entryB = new Entry<>(b);
            entryMap.put(b, entryB);
        } else {
            entryB = findRoot(entryB);
        }

        if (entryA == entryB) {
            return;
        }

        Entry<T> bNext = entryB.next;
        entryB.next = entryA.next;
        entryA.next = bNext;

        if (entryA.rank > entryB.rank) {
            entryB.parent = entryA;
        } else if (entryA.rank < entryB.rank) {
            entryA.parent = entryB;
        } else {
            entryA.parent = entryB;
            entryB.rank++;
        }
    }

    // TODO: Cache this?
    ImmutableSet<T> getPartition(T t) {
        Entry<T> entry = entryMap.get(t);
        if (entry == null) {
            return ImmutableSet.of(t);
        } else {
            // entry = findRoot(entry);
            ImmutableSet.Builder<T> builder = ImmutableSet.builder();
            builder.add(entry.value);
            Entry<T> start = entry;
            while (entry.next != start) {
                entry = entry.next;
                builder.add(entry.value);
            }

            return builder.build();
        }
    }
}
