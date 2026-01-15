package cp2025;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import cp2025.engine.AbstractDeriver;
import cp2025.engine.AbstractOracle;
import cp2025.engine.Datalog.Atom;
import cp2025.engine.Datalog.Predicate;
import cp2025.engine.Datalog.Program;
import cp2025.engine.NullOracle;
import cp2025.engine.ParallelDeriver;
import cp2025.engine.Parser;

public class MyTests {
    private static AbstractDeriver deriver = new ParallelDeriver(5);

    private record SleepOracle(IntSupplier delayMillis) implements AbstractOracle {
        @Override
        public boolean isCalculatable(Predicate predicate) {
            return predicate.id().equals("blue");
        }

        @Override
        public boolean calculate(Atom statement) throws InterruptedException {
            Thread.sleep(delayMillis.getAsInt());
            return statement.toString().equals("blue(a)");
        }
    }

    private static AbstractOracle RandomSleepOracle() {
        return new SleepOracle(() -> ThreadLocalRandom.current().nextInt(1000));
    }

    private static AbstractOracle LongSleepOracle() {
        return new SleepOracle(() -> 5000);
    }

    private static AbstractOracle InstantOracle() {
        return new SleepOracle(() -> 0);
    }

    /**
     * Testuje przerwanie wykonywania {@link AbstractOracle#calculate(Atom) calculate()}
     * przez wiele wątków, gdy jeden z nich zakończy obliczenia jako pierwszy.
     */
    @RepeatedTest(100)
    public void testMultipleCalculations() throws IOException {
        Program program = Parser.parseProgram("""
                Constants: a, b
                Rules:
                Queries: blue(a), blue(a), blue(a), blue(b), blue(b), blue(b), blue(a), blue(b)
            """);

        checkDeriver(program, List.of(true, true, true, false, false, false, true, false), RandomSleepOracle());
    }

    /**
     * Testuje zachowanie programu w sytuacji, gdy główny wątek wykonujący
     * {@link AbstractDeriver#derive(Program, AbstractOracle) derive(program, oracle)}
     * zostanie przerwany.
     */
    @RepeatedTest(100)
    public void testMainThreadInterrupt() throws IOException, InterruptedException {
        Program program = Parser.parseProgram("""
                Constants: a, b
                Rules:
                Queries: blue(a), blue(a), blue(a), blue(b), blue(b), blue(b), blue(a), blue(b)
            """);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<Thread> thread = new AtomicReference<>();

        Future<?> future = executor.submit(() -> {
            thread.set(Thread.currentThread());
            deriver.derive(program, LongSleepOracle());
            return null;
        });

        Thread.sleep(100);
        thread.get().interrupt();

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(InterruptedException.class, ex.getCause());

        executor.shutdownNow();
    }

    @RepeatedTest(100)
    public void testBasic() throws IOException {
        Program program = Parser.parseProgram("""
                Constants: a, b
                Rules: blue(a)  :- blue(b).
                       blue(b)  :- blue(a).
                       red(X)   :- blue(a), blue(X).
                       green(a) :- green(b).
                       green(b) :- green(a).
                Queries: blue(a), blue(b), red(b), green(b), green(a), red(a)
            """);

        checkDeriver(program, List.of(true, false, false, false, false, true), RandomSleepOracle());
    }

    /**
     * Wielokrotnie próbuje wykrzaczyć się na złym przeplocie.
     */
    @RepeatedTest(100)
    public void testBadInterleaving() throws IOException {
        Program program = Parser.parseProgram("""
                Constants: a, b
                Rules: red(X) :- blue(X).
                Queries: blue(a), blue(b), red(a), red(b)
            """);

        for (int i = 0; i < 10000; i++)
            checkDeriver(program, List.of(true, false, true, false), InstantOracle());
    }

    /**
     * Sprawdza, czy klasa korzystająca ze statycznych atrybutów jest odporna
     * na ich nadpisanie przy równoczesnym wywołaniu więcej niż jednego
     * {@link AbstractDeriver#derive(Program, AbstractOracle) derive(program, oracle)}.
     */
    @RepeatedTest(100)
    public void testStaticAttributes() throws IOException, InterruptedException, ExecutionException {
        Program program1 = Parser.parseProgram("""
                Constants: a, b
                Rules: blue(a) :- blue(b).
                       blue(b) :- blue(a).
                Queries: blue(a), blue(b)
            """);

        Program program2 = Parser.parseProgram("""
                Constants: a, b
                Rules: blue(a) :- .
                       blue(b) :- .
                Queries: blue(a), blue(b)
            """);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<?> f1 = executor.submit(() -> checkDeriver(program1, List.of(false, false), new NullOracle()));
        Future<?> f2 = executor.submit(() -> checkDeriver(program2, List.of(true, true), new NullOracle()));

        f1.get();
        f2.get();

        executor.shutdown();
    }

    public void checkDeriver(Program program, List<Boolean> expectedResults, AbstractOracle oracle) {
        try {
            deriver = new ParallelDeriver(5);
            var resultsMap = deriver.derive(program, oracle);
            List<Boolean> results = program.queries().stream().map(resultsMap::get).toList();
            assertEquals(expectedResults, results);
        } catch (InterruptedException e) {
            fail("Derivation was interrupted", e);
        }
    }
}