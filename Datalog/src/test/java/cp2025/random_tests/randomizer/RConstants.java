package cp2025.random_tests.randomizer;
import java.util.ArrayList;
import java.util.List;;

public class RConstants {
    private List<String> constants;

    public RConstants(int count) {
        constants =  new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String constName = "c" + i;
            constants.add(constName);
        }
    }

    public int size() {
        return constants.size();
    }

    public String getRandomConstant() {
        if (constants.isEmpty()) {
            return null;
        }
        int index = SeededRandom.getRandom().nextInt(constants.size());
        return constants.get(index);
    }

    public static RConstants getRandom(int range) {
        int count = SeededRandom.getRandom().nextInt(range + 1);
        return new  RConstants(count);
    }

    @Override
    public String toString() {
        return String.join(", ", constants);
    }
}