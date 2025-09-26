package org.example.formulaone.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomOdds {
    private static final List<Integer> OPTIONS = List.of(2, 3, 4);
    private RandomOdds() {}

    public static int pick() {
        return OPTIONS.get(ThreadLocalRandom.current().nextInt(OPTIONS.size()));
    }
}
