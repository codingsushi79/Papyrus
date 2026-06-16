package io.papermc.paper.anticheat;

import java.util.ArrayDeque;
import java.util.Deque;

final class RollingCounter {

    private final long windowMillis;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    RollingCounter(final long windowMillis) {
        this.windowMillis = windowMillis;
    }

    int record() {
        final long now = System.currentTimeMillis();
        this.prune(now);
        this.timestamps.addLast(now);
        return this.timestamps.size();
    }

    int count() {
        this.prune(System.currentTimeMillis());
        return this.timestamps.size();
    }

    int countWithin(final long windowMillis) {
        final long now = System.currentTimeMillis();
        final long cutoff = now - windowMillis;
        int total = 0;
        for (final Long timestamp : this.timestamps) {
            if (timestamp >= cutoff) {
                total++;
            }
        }
        return total;
    }

    void clear() {
        this.timestamps.clear();
    }

    private void prune(final long now) {
        final long cutoff = now - this.windowMillis;
        while (!this.timestamps.isEmpty() && this.timestamps.peekFirst() < cutoff) {
            this.timestamps.removeFirst();
        }
    }
}
