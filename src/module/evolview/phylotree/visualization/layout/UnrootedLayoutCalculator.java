package module.evolview.phylotree.visualization.layout;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/**
 * Pure, deterministic coordinates; no Swing or mutable tree state is used here.
 * Daylight and n-body follow PHYLIP's rigid subtree rotations (Felsenstein).
 * See docs/third_party/phylip-BSD-2-Clause.txt for reference and attribution.
 */
public final class UnrootedLayoutCalculator {
    public enum Algorithm { EQUAL_ANGLE, EQUAL_DAYLIGHT, N_BODY }

    /** Nodes are in preorder; the root has parent -1 and length zero. */
    public record Input(int[] parents, double[] lengths) {
        public Input {
            parents = parents.clone();
            lengths = lengths.clone();
            if (parents.length == 0 || parents.length != lengths.length || parents[0] != -1) {
                throw new IllegalArgumentException("An unrooted layout requires a nonempty tree");
            }
            for (int i = 1; i < parents.length; i++) {
                if (parents[i] < 0 || parents[i] >= i || !Double.isFinite(lengths[i]) || lengths[i] < 0) {
                    throw new IllegalArgumentException("Branch lengths must be finite and nonnegative");
                }
            }
            lengths[0] = 0;
        }

        @Override public int[] parents() { return parents.clone(); }
        @Override public double[] lengths() { return lengths.clone(); }

        public boolean sameTree(Input other) {
            return other != null && Arrays.equals(parents, other.parents) && Arrays.equals(lengths, other.lengths);
        }
    }

    public record Result(double[] x, double[] y, double[] angles, int iterations) { }

    private static final double TWO_PI = 2 * Math.PI;
    private static final double TOLERANCE = 1e-4;
    private final Input input;
    private final double[] x, y, angles;
    private final int[] end, tips;
    private final int[][] neighbors;
    private final int[] traversal, previous;
    private final double[] projected;

    private UnrootedLayoutCalculator(Input input) {
        this.input = input;
        int size = input.parents.length;
        x = new double[size];
        y = new double[size];
        angles = new double[size];
        end = new int[size];
        tips = new int[size];
        traversal = new int[size];
        previous = new int[size];
        projected = new double[size];
        int[] degree = new int[size];
        for (int i = 1; i < size; i++) {
            degree[i]++;
            degree[input.parents[i]]++;
        }
        neighbors = new int[size][];
        for (int i = 0; i < size; i++) neighbors[i] = new int[degree[i]];
        Arrays.fill(degree, 0);
        for (int i = 1; i < size; i++) {
            int parent = input.parents[i];
            neighbors[parent][degree[parent]++] = i;
            neighbors[i][degree[i]++] = parent;
        }
        for (int i = size - 1; i >= 0; i--) {
            end[i] = Math.max(end[i], i + 1);
            if (tips[i] == 0) tips[i] = 1;
            int parent = input.parents[i];
            if (parent >= 0) {
                end[parent] = Math.max(end[parent], end[i]);
                tips[parent] += tips[i];
            }
        }
    }

    public static Result calculate(Input input, Algorithm algorithm) {
        return new UnrootedLayoutCalculator(input).calculate(algorithm);
    }

    private Result calculate(Algorithm algorithm) {
        checkCancelled();
        // Scaling by the longest branch keeps force arithmetic stable for very large/small units.
        double unit = Arrays.stream(input.lengths).max().orElse(0);
        if (unit == 0) unit = 1;
        double[] low = new double[x.length], high = new double[x.length], cursor = new double[x.length];
        high[0] = TWO_PI;
        for (int i = 1; i < x.length; i++) {
            int parent = input.parents[i];
            low[i] = cursor[parent];
            high[i] = low[i] + (high[parent] - low[parent]) * tips[i] / tips[parent];
            cursor[parent] = high[i];
            cursor[i] = low[i];
            angles[i] = (low[i] + high[i]) / 2;
            x[i] = x[parent] + input.lengths[i] / unit * Math.cos(angles[i]);
            y[i] = y[parent] + input.lengths[i] / unit * Math.sin(angles[i]);
        }
        int iterations = 0;
        if (algorithm != Algorithm.EQUAL_ANGLE) {
            double median = medianRadius();
            for (; iterations < 100; iterations++) {
                double change = 0;
                for (int pivot = 0; pivot < x.length; pivot++) {
                    checkCancelled();
                    if (neighbors[pivot].length < 3) continue;
                    change = Math.max(change, improve(pivot, algorithm, median));
                }
                if (change < TOLERANCE) { iterations++; break; }
            }
        }
        for (int i = 1; i < x.length; i++) {
            int parent = input.parents[i];
            if (Math.hypot(x[i] - x[parent], y[i] - y[parent]) > 1e-15) {
                angles[i] = Math.atan2(y[i] - y[parent], x[i] - x[parent]);
            }
        }
        // Return normalized coordinates. The adapter supplies one uniform display scale.
        return new Result(x, y, angles, iterations);
    }

    private double improve(int pivot, Algorithm algorithm, double median) {
        int count = neighbors[pivot].length;
        Integer[] order = new Integer[count];
        double[] centers = new double[count], lower = new double[count], upper = new double[count];
        int first = neighbors[pivot][0];
        double reference = Math.atan2(y[first] - y[pivot], x[first] - x[pivot]);
        for (int k = 0; k < count; k++) {
            int neighbor = neighbors[pivot][k];
            double center = positive(Math.atan2(y[neighbor] - y[pivot], x[neighbor] - x[pivot]) - reference);
            centers[k] = lower[k] = upper[k] = center;
            order[k] = k;
            // Unwrap along connected edges, not independently around the first edge.
            // Otherwise a subtree spanning the +/-pi seam appears to leave false daylight.
            int pending = 0;
            traversal[pending++] = neighbor;
            previous[neighbor] = pivot;
            projected[neighbor] = center;
            while (pending > 0) {
                int node = traversal[--pending];
                lower[k] = Math.min(lower[k], projected[node]);
                upper[k] = Math.max(upper[k], projected[node]);
                for (int next : neighbors[node]) {
                    if (next == previous[node]) continue;
                    previous[next] = node;
                    double angle = Math.atan2(y[next] - y[pivot], x[next] - x[pivot]) - reference;
                    projected[next] = Math.hypot(x[next] - x[pivot], y[next] - y[pivot]) < 1e-15
                            ? projected[node] : projected[node] + signed(angle - projected[node]);
                    traversal[pending++] = next;
                }
            }
        }
        Arrays.sort(order, (a, b) -> Double.compare(centers[a], centers[b]));
        double maximum = 0;
        for (int position = 0; position < count; position++) {
            checkCancelled();
            int k = order[position], neighbor = neighbors[pivot][k];
            double left = lower[k] - upper[order[(position + count - 1) % count]] + (position == 0 ? TWO_PI : 0);
            double right = lower[order[(position + 1) % count]] - upper[k] + (position == count - 1 ? TWO_PI : 0);
            // An overlapping angular envelope has no safe daylight in which to swing.
            if (left < -1e-10 || right < -1e-10) continue;
            left = Math.max(0, left);
            right = Math.max(0, right);
            double change;
            if (algorithm == Algorithm.EQUAL_DAYLIGHT) {
                change = (right - left) / 2;
            } else {
                double dx = x[neighbor] - x[pivot], dy = y[neighbor] - y[pivot];
                double length = Math.hypot(dx, dy);
                if (length < 1e-15) continue;
                double fx = 0, fy = 0;
                for (int j = 0; j < x.length; j++) {
                    if (j == pivot || inComponent(j, pivot, neighbor)) continue;
                    double rx = x[neighbor] - x[j], ry = y[neighbor] - y[j];
                    double distance = Math.max(Math.hypot(rx, ry), median * 1e-6);
                    double force = Math.min(1e6, (median / distance) * (median / distance));
                    fx += force * rx / distance;
                    fy += force * ry / distance;
                }
                change = (dx / length * fy - dy / length * fx) / (length / median);
                change = Math.max(-0.8 * left, Math.min(0.8 * right, change)) * 0.1;
            }
            change = Math.max(-0.8 * left, Math.min(0.8 * right, change));
            rotateComponent(pivot, neighbor, change);
            lower[k] += change;
            upper[k] += change;
            maximum = Math.max(maximum, Math.abs(change));
        }
        return maximum;
    }

    private boolean inComponent(int node, int pivot, int neighbor) {
        return input.parents[neighbor] == pivot ? node >= neighbor && node < end[neighbor]
                : node < pivot || node >= end[pivot];
    }

    private void rotateComponent(int pivot, int neighbor, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        for (int j = 0; j < x.length; j++) {
            if (!inComponent(j, pivot, neighbor)) continue;
            double dx = x[j] - x[pivot], dy = y[j] - y[pivot];
            x[j] = x[pivot] + cos * dx - sin * dy;
            y[j] = y[pivot] + sin * dx + cos * dy;
        }
    }

    private double medianRadius() {
        double[] radii = new double[x.length];
        int count = 0;
        for (int i = 1; i < x.length; i++) {
            double radius = Math.hypot(x[i], y[i]);
            if (radius > 1e-15) radii[count++] = radius;
        }
        Arrays.sort(radii, 0, count);
        return count == 0 ? 1 : radii[count / 2];
    }

    private static double positive(double angle) { return (angle % TWO_PI + TWO_PI) % TWO_PI; }
    private static double signed(double angle) { return positive(angle + Math.PI) - Math.PI; }
    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Layout cancelled");
    }
}
