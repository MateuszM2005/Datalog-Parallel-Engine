package cp2025.random_tests;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import cp2025.engine.*;
import cp2025.random_tests.randomizer.RandomSleepOracle;
import cp2025.random_tests.randomizer.RProgram;
import cp2025.random_tests.randomizer.SeededRandom;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import static org.junit.jupiter.api.Assertions.*;

import cp2025.engine.Datalog.Atom;
import cp2025.engine.Datalog.Program;

public class RandomTests {
    private final AbstractDeriver simpleDeriver = new SimpleDeriver();
    private AbstractDeriver parallelDeriver = new ParallelDeriver(4);

    void testProgram(Program program, AbstractOracle oracle) throws InterruptedException{
        // final AbstractDeriver simpleDeriver = new SimpleDeriver();
        //  final AbstractDeriver parallelDeriver = new ParallelDeriver(4);

        try {
            Map<Atom, Boolean> expected = simpleDeriver.derive(program, oracle);
            Map<Datalog.Atom, Boolean> actual = parallelDeriver.derive(program, oracle);

            assertEquals(expected.size(), actual.size(), "Result maps must have same size");
            for (Map.Entry<Datalog.Atom, Boolean> e : expected.entrySet()) {
                Datalog.Atom q = e.getKey();
                assertTrue(actual.containsKey(q), "ParallelDeriver result must contain query: " + q);
                assertEquals(e.getValue(), actual.get(q), "Answer for " + q + " must match SimpleDeriver");
            }
        } catch (InterruptedException e) {
            System.out.println("Uncaught interrupted exception escaped!");
            throw new InterruptedException();
        }
    }
//
//    @RepeatedTest(1000)
//    void smallRandomProgramNullOracle(RepetitionInfo repetitionInfo) throws IOException, InterruptedException {
//        SeededRandom.setSeed(repetitionInfo.getCurrentRepetition());
//
//        RProgram randomSrc = RProgram.getRandom(3, 4, 10, 50, 3);
//        Program randomProgram = Parser.parseProgram(randomSrc.toString());
//
//        for (int i = 0; i < 20; i++) testProgram(randomProgram, new NullOracle());
//    }

//    @RepeatedTest(1)
//    void smallRandomProgramRandomOracle(RepetitionInfo repetitionInfo) throws IOException, InterruptedException {
//        SeededRandom.setSeed(repetitionInfo.getCurrentRepetition());
//
//        RProgram randomSrc = RProgram.getRandom(5, 5, 50, 50, 3);
//        Program randomProgram = Parser.parseProgram(randomSrc.toString());
//
//        for (int i = 0; i < 20; i++) testProgram(randomProgram, new RandomSleepOracle(500));
//    }

//    @RepeatedTest(4)
//    void bigRandomProgramNullOracle(RepetitionInfo repetitionInfo) throws IOException, InterruptedException {
//        SeededRandom.setSeed(repetitionInfo.getCurrentRepetition());
//
//
//        RProgram randomSrc = RProgram.getRandom(10, 10, 500, 2000, 5);
//        Program randomProgram = Parser.parseProgram(randomSrc.toString());
//        System.out.println(randomProgram);
//
//        for (int i = 0; i < 20; i++) testProgram(randomProgram, new NullOracle());
//    }
//
//    @RepeatedTest(100)
//    void bigRandomProgramRandomOracle(RepetitionInfo repetitionInfo) throws IOException, InterruptedException {
//        SeededRandom.setSeed(repetitionInfo.getCurrentRepetition());
//
//        RProgram randomSrc = RProgram.getRandom(10, 10, 500, 2000, 5);
//        Program randomProgram = Parser.parseProgram(randomSrc.toString());
//
//        for (int i = 0; i < 5; i++) testProgram(randomProgram, new RandomSleepOracle(50));
//    }
//
//    @RepeatedTest(100)
//    void mainInterruptionTest(RepetitionInfo repetitionInfo) throws IOException, InterruptedException {
//        final int maxAllowedMs = 2000;
//
//        SeededRandom.setSeed(repetitionInfo.getCurrentRepetition());
//
//        RProgram randomSrc = RProgram.getRandom(10, 15, 1000, 2000, 5);
//        Program randomProgram = Parser.parseProgram(randomSrc.toString());
//
//        for (int i = 0; i < 10; i++) {
//            AtomicReference<Instant> interruptTimeRef = new AtomicReference<>();
//
//            CountDownLatch startedLatch = new CountDownLatch(1);
//
//            Thread worker = new Thread(() -> {
//                try {
//                    startedLatch.countDown();
//
//                    parallelDeriver.derive(randomProgram, new RandomSleepOracle(500));
//
//                    Instant interruptInstant = interruptTimeRef.get();
//                    if (interruptInstant == null) {
//                        System.err.println("The program ended before interrupt.");
//                    } else {
//                        long elapsedMs = Duration.between(interruptInstant, Instant.now()).toMillis();
//                        assertTrue(elapsedMs <= maxAllowedMs,
//                                () -> "Handled interrupt too slowly (elapsed=" + elapsedMs + "ms)");
//                    }
//                } catch (InterruptedException e) {
//                    Instant interruptInstant = interruptTimeRef.get();
//                    assertNotNull(interruptInstant, "The program was interrupted, but interruption time wasn't set. Curious...");
//
//                    long elapsedMs = Duration.between(interruptInstant, Instant.now()).toMillis();
//                    assertTrue(elapsedMs <= maxAllowedMs,
//                            () -> "Interrupted handler ran too slowly (elapsed=" + elapsedMs + "ms)");
//                } catch (Throwable t) {
//                    throw new RuntimeException(t);
//                }
//            }, "deriver-" + i);
//
//            worker.start();
//
//            boolean started = startedLatch.await(1_000, java.util.concurrent.TimeUnit.MILLISECONDS);
//            assertTrue(started, "Worker did not start in time");
//
//            Thread.sleep(50);
//
//            Instant interruptInstant = Instant.now();
//            interruptTimeRef.set(interruptInstant);
//            worker.interrupt();
//
//            worker.join(3_000);
//            assertFalse(worker.isAlive(), "Worker thread did not terminate within timeout");
//        }
//    }
//
    @RepeatedTest(10)
    void differentNWorkerTest(RepetitionInfo repetitionInfo) throws IOException, InterruptedException {
        SeededRandom.setSeed(repetitionInfo.getCurrentRepetition());
        RProgram randomSrc = RProgram.getRandom(7, 7, 100, 500, 5);
        Program randomProgram = Parser.parseProgram(randomSrc.toString());

        for (int i = 1; i < 5; i++) {
            parallelDeriver = new ParallelDeriver(i);
            testProgram(randomProgram, new RandomSleepOracle(50));
        }
        for (int i = 10; i <= 100; i += 10) {
            parallelDeriver = new ParallelDeriver(i);
            testProgram(randomProgram, new RandomSleepOracle(50));
        }
    }
}
