package test.module.evolview.phylotree.visualization.layout;

import module.evolview.phylotree.visualization.layout.UnrootedLayoutCalculator;
import module.evolview.phylotree.visualization.layout.UnrootedLayoutCalculator.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.concurrent.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;

public class UnrootedLayoutCalculatorTest {
    public static void main(String[] args) throws Exception {
        Input[] cases = {
            new Input(new int[] {-1}, new double[] {0}),
            new Input(new int[] {-1, 0}, new double[] {0, 2}),
            new Input(new int[] {-1, 0, 0}, new double[] {0, 0, 0}),
            new Input(new int[] {-1, 0, 1, 1, 0, 4, 4, 4, 0}, new double[] {0, .2, .1, 3, 1, 1e-9, 2, .3, 2}),
            tree(64, false), tree(64, true)
        };
        for (Input input : cases) {
            for (Algorithm algorithm : Algorithm.values()) {
                Result result = UnrootedLayoutCalculator.calculate(input, algorithm);
                verifyLengths(input, result);
                Result again = UnrootedLayoutCalculator.calculate(input, algorithm);
                require(Arrays.equals(result.x(), again.x()) && Arrays.equals(result.y(), again.y()), "Deterministic output");
                if (algorithm != Algorithm.N_BODY) require(crossings(input, result) == 0, "No branch crossings: " + algorithm);
            }
        }
        Random random = new Random(73193);
        for (int sample = 0; sample < 20; sample++) {
            Input topology = tree(12 + sample, sample % 2 == 0);
            double[] lengths = topology.lengths();
            for (int i = 1; i < lengths.length; i++) lengths[i] = Math.pow(10, random.nextDouble() * 4 - 2);
            Input varied = new Input(topology.parents(), lengths);
            Result result = UnrootedLayoutCalculator.calculate(varied, Algorithm.EQUAL_DAYLIGHT);
            verifyLengths(varied, result);
            require(crossings(varied, result) == 0, "Daylight preserves planarity with unequal branches");
        }
        Input example = tree(23, false);
        Result angle = UnrootedLayoutCalculator.calculate(example, Algorithm.EQUAL_ANGLE);
        Result daylight = UnrootedLayoutCalculator.calculate(example, Algorithm.EQUAL_DAYLIGHT);
        require(daylightImbalance(example, daylight) < daylightImbalance(example, angle), "Daylight should improve angular gaps");
        double[] scaled = example.lengths();
        for (int i = 0; i < scaled.length; i++) scaled[i] = Math.scalb(scaled[i], -300);
        Result same = UnrootedLayoutCalculator.calculate(new Input(example.parents(), scaled), Algorithm.N_BODY);
        Result original = UnrootedLayoutCalculator.calculate(example, Algorithm.N_BODY);
        double difference = 0;
        for (int i = 0; i < scaled.length; i++) difference = Math.max(difference, Math.hypot(same.x()[i] - original.x()[i], same.y()[i] - original.y()[i]));
        require(difference < 1e-8, "Unit invariance, maximum difference = " + difference);
        CountDownLatch started = new CountDownLatch(1);
        FutureTask<Result> cancelled = new FutureTask<>(() -> {
            started.countDown();
            return UnrootedLayoutCalculator.calculate(tree(1000, false), Algorithm.N_BODY);
        });
        Thread worker = new Thread(cancelled, "layout-cancellation-test");
        worker.start();
        require(started.await(1, TimeUnit.SECONDS), "Numerical worker started");
        Thread.sleep(30);
        long cancelStart = System.nanoTime();
        cancelled.cancel(true);
        worker.join(1000);
        require(!worker.isAlive(), "Cancellation should stop the numerical worker");
        require(System.nanoTime() - cancelStart < 250_000_000, "Cancellation responds within 250 ms");
        Input large = tree(1000, false);
        for (Algorithm algorithm : Algorithm.values()) {
            var heapPools = ManagementFactory.getMemoryPoolMXBeans().stream().filter(pool -> pool.getType() == MemoryType.HEAP).toList();
            heapPools.forEach(pool -> pool.resetPeakUsage());
            long start = System.nanoTime();
            Result result = UnrootedLayoutCalculator.calculate(large, algorithm);
            verifyLengths(large, result);
            long peakHeap = heapPools.stream().mapToLong(pool -> pool.getPeakUsage().getUsed()).sum();
            System.out.printf(Locale.ROOT, "1000 tips %s: %.1f ms, %d iterations, peak heap %.1f MiB%n", algorithm,
                    (System.nanoTime() - start) / 1e6, result.iterations(), peakHeap / (1024.0 * 1024));
        }
        System.out.println("UnrootedLayoutCalculatorTest passed");
    }

    public static Input tree(int leaves, boolean caterpillar) {
        List<Integer> parents = new ArrayList<>();
        List<Double> lengths = new ArrayList<>();
        append(parents, lengths, -1, leaves, caterpillar);
        return new Input(parents.stream().mapToInt(Integer::intValue).toArray(), lengths.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private static void append(List<Integer> parents, List<Double> lengths, int parent, int leaves, boolean caterpillar) {
        int index = parents.size();
        parents.add(parent);
        lengths.add(parent < 0 ? 0 : .1 + ((index * 17) % 29) / 13.0);
        if (leaves > 1) {
            int first = caterpillar ? 1 : leaves / 2;
            append(parents, lengths, index, first, caterpillar);
            append(parents, lengths, index, leaves - first, caterpillar);
        }
    }

    private static void verifyLengths(Input input, Result result) {
        int[] parents = input.parents();
        double[] lengths = input.lengths();
        double scale = Arrays.stream(lengths).max().orElse(1);
        if (scale == 0) scale = 1;
        for (int i = 0; i < parents.length; i++) {
            require(Double.isFinite(result.x()[i]) && Double.isFinite(result.y()[i]) && Double.isFinite(result.angles()[i]), "Finite coordinates");
            if (parents[i] < 0) continue;
            double actual = Math.hypot(result.x()[i] - result.x()[parents[i]], result.y()[i] - result.y()[parents[i]]);
            require(Math.abs(actual - lengths[i] / scale) < 1e-9, "Preserved branch " + i);
        }
        require(result.iterations() <= 100, "Bounded iterations");
    }

    private static int crossings(Input input, Result result) {
        int count = 0;
        int[] p = input.parents();
        for (int i = 1; i < p.length; i++) for (int j = i + 1; j < p.length; j++) {
            if (p[i] == p[j] || p[i] == j || p[j] == i) continue;
            if (input.lengths()[i] == 0 || input.lengths()[j] == 0) continue;
            if (Line2D.linesIntersect(result.x()[i], result.y()[i], result.x()[p[i]], result.y()[p[i]],
                    result.x()[j], result.y()[j], result.x()[p[j]], result.y()[p[j]])) count++;
        }
        return count;
    }

    private static double daylightImbalance(Input input, Result result) {
        int[] parents = input.parents();
        double score = 0;
        for (int pivot = 0; pivot < parents.length; pivot++) {
            List<Integer> neighbors = new ArrayList<>();
            if (parents[pivot] >= 0) neighbors.add(parents[pivot]);
            for (int i = 1; i < parents.length; i++) if (parents[i] == pivot) neighbors.add(i);
            if (neighbors.size() < 3) continue;
            List<double[]> envelopes = new ArrayList<>();
            for (int neighbor : neighbors) {
                double center = positive(Math.atan2(result.y()[neighbor] - result.y()[pivot], result.x()[neighbor] - result.x()[pivot]));
                double min = center, max = center;
                for (int node = 0; node < parents.length; node++) {
                    int walk = node;
                    while (walk >= 0 && walk != pivot && walk != neighbor) walk = parents[walk];
                    boolean inside;
                    if (parents[neighbor] == pivot) inside = walk == neighbor;
                    else {
                        walk = node;
                        while (walk >= 0 && walk != pivot) walk = parents[walk];
                        inside = walk != pivot;
                    }
                    if (!inside) continue;
                    double value = center + positive(Math.atan2(result.y()[node] - result.y()[pivot], result.x()[node] - result.x()[pivot]) - center + Math.PI) - Math.PI;
                    min = Math.min(min, value); max = Math.max(max, value);
                }
                envelopes.add(new double[] {center, min, max});
            }
            envelopes.sort(Comparator.comparingDouble(a -> a[0]));
            double[] gaps = new double[envelopes.size()];
            for (int k = 0; k < gaps.length; k++) gaps[k] = envelopes.get((k + 1) % gaps.length)[1] - envelopes.get(k)[2] + (k == gaps.length - 1 ? 2 * Math.PI : 0);
            double mean = Arrays.stream(gaps).average().orElse(0);
            for (double gap : gaps) score += (gap - mean) * (gap - mean);
        }
        return score;
    }

    private static double positive(double angle) { return (angle % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI); }
    public static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
