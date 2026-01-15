package cp2025.random_tests.randomizer;

import java.util.Random;

public class SeededRandom {
    private static Random random;

    public static Random getRandom() {
        return random;
    }

    public static void setSeed(int seed) {
        random = new Random(seed);
    }
}
