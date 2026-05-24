package module.evolview.common;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.Objects;

public class SwingDebouncer {

    private final Timer timer;

    public SwingDebouncer(int delayMilliseconds, Runnable action) {
        if (delayMilliseconds < 0) {
            throw new IllegalArgumentException("delayMilliseconds must be >= 0");
        }
        Objects.requireNonNull(action, "action");

        timer = new Timer(delayMilliseconds, e -> action.run());
        timer.setRepeats(false);
    }

    public void restart() {
        if (SwingUtilities.isEventDispatchThread()) {
            timer.restart();
        } else {
            SwingUtilities.invokeLater(timer::restart);
        }
    }

    public void stop() {
        if (SwingUtilities.isEventDispatchThread()) {
            timer.stop();
        } else {
            SwingUtilities.invokeLater(timer::stop);
        }
    }
}
