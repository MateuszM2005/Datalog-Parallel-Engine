package cp2025.random_tests.randomizer;

import java.util.Set;
import java.util.List;

public class RAtomWithVariables extends RAtom {
    private static final int MAX_VARIABLES = 6;
    private Set<String> variables = new java.util.HashSet<>();

    public RAtomWithVariables(RPredicate predicate, java.util.List<String> terms) {
        super(predicate, terms);
        for (String term : terms) {
            if (term.startsWith("VAR")) {
                variables.add(term);
            }
        }
    }

    public Set<String> getVariables() {
        return variables;
    }

    public static RAtomWithVariables getRandomWithVariables(RPredicate predicate, RConstants constants) {
        java.util.List<String> terms = new java.util.ArrayList<>();
        for (int i = 0; i < predicate.getArity(); i++) {
            if (SeededRandom.getRandom().nextBoolean() && MAX_VARIABLES > 0) {
                terms.add("VAR" + SeededRandom.getRandom().nextInt(MAX_VARIABLES));
            } else {
                terms.add(constants.getRandomConstant());
            }
        }
        return new RAtomWithVariables(predicate, terms);
    }

    public static RAtomWithVariables getRandomWithVariables(List<RPredicate> predicates, RConstants constants) {
        RPredicate predicate = predicates.get(SeededRandom.getRandom().nextInt(predicates.size()));
        return getRandomWithVariables(predicate, constants);
    }

    public static RAtomWithVariables getRandomWithSetVariables(List<RPredicate> predicates, RConstants constants, Set<String> varSet) {
        RPredicate predicate = predicates.get(SeededRandom.getRandom().nextInt(predicates.size()));
        java.util.List<String> terms = new java.util.ArrayList<>();
        for (int i = 0; i < predicate.getArity(); i++) {
            if (!varSet.isEmpty() && SeededRandom.getRandom().nextBoolean()) {
                int index = SeededRandom.getRandom().nextInt(varSet.size());
                String var = (String) varSet.toArray()[index];
                terms.add(var);
            } else {
                terms.add(constants.getRandomConstant());
            }
        }
        return new RAtomWithVariables(predicate, terms);
    }
    
}
