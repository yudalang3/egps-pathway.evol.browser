package module.evolview.phylotree.visualization.layout;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/** Numeric distances are retained independently of their formatted labels and pixel positions. */
public final class BranchLengthTicks {
    public record Tick(double value, String label) { }

    private BranchLengthTicks() { }

    public static double niceFloor(double value) {
        if (!(value > 0) || !Double.isFinite(value)) return 0;
        double power = Math.pow(10, Math.floor(Math.log10(value)));
        if (power == 0) return value;
        double fraction = value / power;
        return (fraction >= 5 ? 5 : fraction >= 2 ? 2 : 1) * power;
    }

    public static String format(double value) {
        if (value == 0) return "0";
        BigDecimal rounded = BigDecimal.valueOf(value).round(new MathContext(12)).stripTrailingZeros();
        return Math.abs(value) < .0001 || Math.abs(value) >= 10000
                ? rounded.toString().replace("E+", "E") : rounded.toPlainString();
    }

    public static List<Tick> calculate(double maximum, int pixels, int labelWidth) {
        if (!(maximum > 0) || !Double.isFinite(maximum) || pixels <= 0) return List.of();
        int intervals = Math.max(1, Math.min(100, pixels / Math.max(48, labelWidth + 16)));
        double step = niceFloor(maximum / intervals);
        double desired = maximum / intervals;
        if (step > 0 && step < desired) {
            double power = Math.pow(10, Math.floor(Math.log10(step)));
            double fraction = step / power;
            step = (fraction < 1.5 ? 2 : fraction < 3.5 ? 5 : 10) * power;
        }
        if (!(step > 0)) step = maximum;
        if (step > maximum) step = niceFloor(maximum);
        List<Tick> ticks = new ArrayList<>();
        for (int i = 0; i <= 250; i++) {
            double value = i * step;
            if (!Double.isFinite(value) || value > maximum) break;
            ticks.add(new Tick(value, format(value)));
        }
        return List.copyOf(ticks);
    }
}
