package cp2025.random_tests.randomizer;
import java.util.List;

public class RAtom {
    private RPredicate predicate;
    private List<String> terms;

    public RAtom(RPredicate predicate, List<String> terms) {
        this.predicate = predicate;
        this.terms = terms;
    }

    @Override
    public String toString() {
        return predicate.toString() + "(" + String.join(", ", terms)+ ")";
    }

    public static RAtom getRandom(RPredicate predicate, RConstants constants) {
        List<String> terms = new java.util.ArrayList<>();
        for (int i = 0; i < predicate.getArity(); i++) {
            terms.add(constants.getRandomConstant());
        }
        return new RAtom(predicate, terms);
    }

    public static RAtom getRandom(List<RPredicate> predicates, RConstants constants) {
        RPredicate predicate = predicates.get(SeededRandom.getRandom().nextInt(predicates.size()));
        return getRandom(predicate, constants);
    }
}
