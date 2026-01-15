package cp2025.random_tests.randomizer;

public class RPredicate {
    private String name;
    private int arity;

    public RPredicate(String name, int arity) {
        this.name = name;
        this.arity = arity;
    }

    public String getName() {
        return name;
    }

    public int getArity() {
        return arity;
    }

    @Override
    public String toString() {
        return name;
    }

    public static RPredicate getRandom(int i, int max_arity) {
        String lident = "pred_" + i;
        int arity = SeededRandom.getRandom().nextInt(max_arity + 1);
        return new RPredicate(lident, arity);
    }
}