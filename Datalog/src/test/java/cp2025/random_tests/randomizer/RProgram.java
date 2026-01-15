package cp2025.random_tests.randomizer;

import java.util.ArrayList;
import java.util.List;

public class RProgram {
    private RConstants constants;
    private List<RPredicate> predicates;
    private List<RRule> rules;
    private List<RAtom> queries;

    public RProgram(RConstants constants, List<RPredicate> predicates, List<RRule> rules,
            List<RAtom> queries) {
        this.constants = constants;
        this.predicates = predicates;
        this.rules = rules;
        this.queries = queries;
    }

    public static RProgram getRandom(int max_constants, int max_predicates, int max_rules,
            int max_queries, int max_arity) {
        RConstants constants = RConstants.getRandom(max_constants);

        max_arity = Math.min(max_arity, constants.size());

        List<RPredicate> predicates = new ArrayList<>();
        int predicateCount = SeededRandom.getRandom().nextInt(max_predicates + 1);
        for (int i = 0; i < predicateCount; i++) {
            predicates.add(RPredicate.getRandom(i, max_arity));
        }

        if (predicates.isEmpty()) {
            max_rules = 0;
            max_queries = 0;
        }

        List<RRule> rules = new ArrayList<>();
        int ruleCount = SeededRandom.getRandom().nextInt(max_rules + 1);
        for (int i = 0; i < ruleCount; i++) {
            rules.add(RRule.getRandom(predicates, constants));
        }

        List<RAtom> queries = new ArrayList<>();
        int queryCount = SeededRandom.getRandom().nextInt(max_queries + 1);
        for (int i = 0; i < queryCount; i++) {
            queries.add(RAtom.getRandom(predicates, constants));
        }

        RProgram res = new RProgram(constants, predicates, rules, queries);
        return res;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Constants: ");
        sb.append(constants.toString());
        sb.append("\nRules:\n");
        for (RRule rule : rules) {
            sb.append("\t");
            sb.append(rule.toString());
            sb.append("\n");
        }
        sb.append("Queries: ");
        for (int i = 0; i < queries.size(); i++) {
            sb.append(queries.get(i).toString());
            if (i !=  queries.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
