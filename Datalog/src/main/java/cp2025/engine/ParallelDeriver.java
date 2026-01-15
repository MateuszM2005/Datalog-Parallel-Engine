package cp2025.engine;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ExecutorService;

import cp2025.engine.Datalog.*;

import static java.util.concurrent.Executors.newFixedThreadPool;


public class ParallelDeriver implements AbstractDeriver {
    private final int numWorkers;

    private ExecutorService pool;
    private volatile boolean isShutdown = false;

    private Set<Thread> threadSet = ConcurrentHashMap.newKeySet();
    private ConcurrentHashMap<Thread, Set<Atom>> inProgressReference = new ConcurrentHashMap<>();

    public ParallelDeriver(int numWorkers) {this.numWorkers = numWorkers;}

    @Override
    public Map<Atom, Boolean> derive(Program input, AbstractOracle oracle)
            throws InterruptedException {
        pool = newFixedThreadPool(numWorkers);
        try {
            // Give threads the tasks.
            ParallelDeriver.ParallelDeriverState state = new ParallelDeriver.ParallelDeriverState(input, oracle);
            Map<Atom, Future<ParallelDeriverState.DerivationResult>> futures = new HashMap<>();
            for (Atom query : input.queries()) {
                futures.put(query, pool.submit(() -> {
                    // Each thread gets its own local in-progress set
                    Set<Atom> localInProgress = new HashSet<>();
                    threadSet.add(Thread.currentThread());
                    inProgressReference.put(Thread.currentThread(), ConcurrentHashMap.newKeySet());
                    ParallelDeriverState.DerivationResult tmp = state.deriveStatement(query, localInProgress);
                    threadSet.remove(Thread.currentThread());
                    inProgressReference.remove(Thread.currentThread());
                    return tmp;
                }));
            }
            // Collect results.
            Map<Atom, Boolean> results = new HashMap<>();
            for (Map.Entry<Atom, Future<ParallelDeriverState.DerivationResult>> entry : futures.entrySet()) {
                try {
                    results.put(entry.getKey(), entry.getValue().get().derivable);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof InterruptedException) {
                        System.out.println("Interrupted exception: " + cause.getMessage());
                    }
                    if (cause instanceof CalculatedException) {
                        System.out.println("Calculated exception: "+((CalculatedException) cause).atoms.size());
                    }
                    throw new RuntimeException("Error during derivation of " + entry.getKey(), cause);
                }
            }
            return results;
        } catch (InterruptedException e) {
            isShutdown = true;
            pool.shutdownNow();
            System.out.println(e.getMessage());
            throw new InterruptedException("Main thread interrupted, all calculations are shut down.");
        } finally {
            pool.shutdown();
        }
    }

    private class ParallelDeriverState {
        private final Program input;
        private final AbstractOracle oracle;

        private final ConcurrentHashMap<Predicate, List<Rule>> predicateToRules;

        private final Map<Atom, Boolean> knownStatements = new ConcurrentHashMap<>();


        public ParallelDeriverState(Program input, AbstractOracle oracle) {
            this.input = input;
            this.oracle = oracle;

            // Build the predicateToRules map.
            Map<Predicate, List<Rule>> tmp = input.rules().stream().collect(
                    java.util.stream.Collectors.groupingBy(rule -> rule.head().predicate()));
            predicateToRules = new ConcurrentHashMap<Predicate, List<Rule>>(tmp);
        }


        private record DerivationResult(boolean derivable, Set<Atom> failedStatements) {}

        public void interrupt(){

        }

        public ParallelDeriver.ParallelDeriverState.DerivationResult deriveStatement(Atom goal, Set<Atom> inProgressStatements) throws InterruptedException, CalculatedException {
            // Check if we already know the result for this statement.
            if (knownStatements.containsKey(goal))
                return new ParallelDeriver.ParallelDeriverState.DerivationResult(knownStatements.get(goal), Set.of());

            // Interrupted: if shutdown flag is up we wrap up, else this checks if computations were already done.
            // interrupts are only caught here, as this method is called when every calculation starts.
            if (Thread.interrupted()){
                if(isShutdown) throw new InterruptedException("Shutdown");

                // Build a set of known atoms;
                Set<Atom> atoms = new HashSet<>();
                for(Atom a : inProgressStatements){
                    if(knownStatements.containsKey(a)) atoms.add(a);
                }

                // Create the exception
                if(!atoms.isEmpty()) {
                    if(atoms.contains(goal)){
                        atoms.remove(goal);
                        if(atoms.isEmpty()) return new DerivationResult(knownStatements.get(goal), Set.of());
                        else throw new CalculatedException(atoms);
                    }
                    else throw new CalculatedException(atoms);
                }

            }

            // This try catch is to guarantee that wherever this thread was interrupted, we go as far as possible
            try {
                // Try to actually derive the statement using rules.
                // Check if the statement is calculatable.
                inProgressReference.get(Thread.currentThread()).add(goal);

                if (oracle.isCalculatable(goal.predicate())) {
                    boolean result = oracle.calculate(goal);
                    if (result) {
                        knownStatements.put(goal, true);
                        for(Thread t : threadSet)
                            if(t != Thread.currentThread() && inProgressReference.contains(t) && inProgressReference.get(t).contains(goal)) t.interrupt();
                    }
                    knownStatements.put(goal, result);
                    return new ParallelDeriver.ParallelDeriverState.DerivationResult(result, Set.of());
                }

                // Check for cycles, to avoid infinite loops.
                if (inProgressStatements.contains(goal)) {
                    // Return false but do not store the result (we may find a different derivation later).
                    return new ParallelDeriver.ParallelDeriverState.DerivationResult(false, Set.of(goal));
                }

                inProgressStatements.add(goal);
                ParallelDeriver.ParallelDeriverState.DerivationResult result = deriveNewStatement(goal, inProgressStatements);
                inProgressStatements.remove(goal);
                inProgressReference.get(Thread.currentThread()).remove(goal);

                if (result.derivable) {
                    knownStatements.put(goal, true);
                    //Interrupt
                    for(Thread t : threadSet)
                        if(t != Thread.currentThread() && inProgressReference.contains(t) && inProgressReference.get(t).contains(goal)) t.interrupt();
                } else {
                    // We can only deduce non-derivability when there are no in-progress statements
                    // (at the top of the recursion).
                    if (inProgressStatements.isEmpty())
                        for (Atom s : result.failedStatements)
                            knownStatements.put(s, false);
                    //Interrupt
                    for(Thread t : threadSet)
                        if(t != Thread.currentThread() && inProgressReference.contains(t)){
                            boolean flag = false;
                            for (Atom s : result.failedStatements)
                                if(inProgressReference.get(t).contains(s)) flag = true;
                            if(flag) t.interrupt();
                        }
                }
                return result;
            } catch (CalculatedException e) {
                inProgressStatements.remove(goal);
                if(e.atoms.contains(goal)) {
                    e.atoms.remove(goal);
                    if(e.atoms.size() == 0)
                        return new DerivationResult(knownStatements.get(goal), Set.of());
                    else
                        throw e;
                } else
                    throw e;
            } catch (InterruptedException e) { // Oracle got interrupted.
                inProgressStatements.remove(goal);
                if(isShutdown) throw e;

                // Build a set of known atoms;
                Set<Atom> atoms = new HashSet<>();
                for(Atom a : inProgressStatements){
                    if(knownStatements.containsKey(a)) atoms.add(a);
                }
                // Create the exception
                if(atoms.contains(goal)){
                    atoms.remove(goal);
                    if(atoms.isEmpty()) return new DerivationResult(knownStatements.get(goal), Set.of());
                    else throw new CalculatedException(atoms);
                }
                else throw new RuntimeException("Pointless interrupt");
            } finally {
                inProgressReference.get(Thread.currentThread()).remove(goal);
            }
        }

        // This complicated code, which I don't even want to understand any more than to deduce it just calls the code above,
        // may remain unchanged, I am thankful to god for this fact.
        private ParallelDeriver.ParallelDeriverState.DerivationResult deriveNewStatement(Atom goal, Set<Atom> inProgressStatements) throws InterruptedException, CalculatedException {
            List<Rule> rules = predicateToRules.get(goal.predicate());
            if (rules == null)
                return new ParallelDeriver.ParallelDeriverState.DerivationResult(false, Set.of(goal));

            Set<Atom> failedStatements = new HashSet<>();

            for (Rule rule : rules) {
                Optional<List<Atom>> partiallyAssignedBody = Unifier.unify(rule, goal);
                if (partiallyAssignedBody.isEmpty())
                    continue;

                List<Variable> variables = Datalog.getVariables(partiallyAssignedBody.get());
                FunctionGenerator<Variable, Constant> iterator = new FunctionGenerator<>(variables,
                        input.constants());
                for (Map<Variable, Constant> assignment : iterator) {
                    List<Atom> assignedBody = Unifier.applyAssignment(partiallyAssignedBody.get(),
                            assignment);
                    ParallelDeriver.ParallelDeriverState.DerivationResult result = deriveBody(assignedBody, inProgressStatements);
                    if (result.derivable)
                        return new ParallelDeriver.ParallelDeriverState.DerivationResult(true, Set.of());
                    failedStatements.addAll(result.failedStatements);
                }
            }

            failedStatements.add(goal);
            return new ParallelDeriver.ParallelDeriverState.DerivationResult(false, failedStatements);
        }


        private ParallelDeriver.ParallelDeriverState.DerivationResult deriveBody(List<Atom> body, Set<Atom> inProgressStatements) throws InterruptedException, CalculatedException {
            for (Atom statement : body) {
                ParallelDeriver.ParallelDeriverState.DerivationResult result = deriveStatement(statement, inProgressStatements);
                if (!result.derivable)
                    return new ParallelDeriver.ParallelDeriverState.DerivationResult(false, result.failedStatements);
            }
            return new ParallelDeriver.ParallelDeriverState.DerivationResult(true, Set.of());
        }
    }
    //This is to make passing easier and to iterate through DFS depth only once per interrupt.
    private static class CalculatedException extends Exception {
        private Set<Atom> atoms;
        CalculatedException(Set<Atom> atoms){
            this.atoms = atoms;
        }
    }
}
