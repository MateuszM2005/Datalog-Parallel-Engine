package cp2025.random_tests.randomizer;

import cp2025.engine.AbstractOracle;
import cp2025.engine.Datalog;

import java.util.concurrent.ConcurrentHashMap;

public class ROracle implements AbstractOracle {
    private ConcurrentHashMap<Datalog.Predicate, Boolean> isCalculatable;
    private ConcurrentHashMap<Datalog.Atom, Boolean> calculatedAnswer;
    private int maxWaitTime;
    
    public ROracle(int maxWaitTime) {
        isCalculatable = new ConcurrentHashMap<>();
        calculatedAnswer = new ConcurrentHashMap<>();
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public boolean isCalculatable(Datalog.Predicate predicate) {
        isCalculatable.putIfAbsent(predicate, SeededRandom.getRandom().nextBoolean());
        return isCalculatable.get(predicate);
    }

    @Override
    public boolean calculate(Datalog.Atom statement) throws InterruptedException {
        calculatedAnswer.putIfAbsent(statement, SeededRandom.getRandom().nextBoolean());
        Thread.sleep(SeededRandom.getRandom().nextInt(maxWaitTime));
        return calculatedAnswer.get(statement);
    }
}
