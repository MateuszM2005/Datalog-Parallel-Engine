package cp2025.random_tests.randomizer;

import java.util.List;

public class RRule {
    private static int MAX_BODY_SIZE = 6;

    private RAtomWithVariables head;
    private List<RAtomWithVariables> body;

    public RRule(RAtomWithVariables head, List<RAtomWithVariables> body) {
        this.head = head;
        this.body = body;
    }

    public static RRule getRandom(List<RPredicate> predicates, RConstants constants) {
        RAtomWithVariables head = RAtomWithVariables.getRandomWithVariables(predicates, constants);

        List<RAtomWithVariables> body = new java.util.ArrayList<>();
        int bodySize = SeededRandom.getRandom().nextInt(MAX_BODY_SIZE);
        for (int i = 0; i < bodySize; i++) {
            body.add(RAtomWithVariables.getRandomWithSetVariables(predicates, constants, head.getVariables()));
        }

        return new RRule(head, body);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(head.toString());
        sb.append(" :- ");
        if (!body.isEmpty()) {
            List<String> bodyStrings = new java.util.ArrayList<>();
            for (RAtomWithVariables atom : body) {
                bodyStrings.add(atom.toString());
            }
            sb.append(String.join(", ", bodyStrings));
        }
        sb.append(".");
        return sb.toString();
    }
}
