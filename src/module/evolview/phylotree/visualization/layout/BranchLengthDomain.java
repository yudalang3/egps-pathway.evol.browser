package module.evolview.phylotree.visualization.layout;

import java.util.ArrayDeque;
import module.evolview.model.tree.GraphicsNode;

/** Validates stored distances without modifying either real or displayed branch lengths. */
public final class BranchLengthDomain {
    private BranchLengthDomain() { }

    public static double maximum(GraphicsNode root, boolean visibleOnly) {
        record Pending(GraphicsNode node, double distance) { }
        ArrayDeque<Pending> pending = new ArrayDeque<>();
        pending.push(new Pending(root, 0));
        double maximum = 0;
        while (!pending.isEmpty()) {
            Pending next = pending.pop();
            GraphicsNode node = next.node();
            maximum = Math.max(maximum, next.distance());
            if (visibleOnly && node.getCollapse()) continue;
            for (int i = 0; i < node.getChildCount(); i++) {
                GraphicsNode child = (GraphicsNode) node.getChildAt(i);
                double length = visibleOnly ? child.getDisplayedBranchLength() : child.getRealBranchLength();
                double distance = next.distance() + length;
                if (!Double.isFinite(length) || length < 0 || !Double.isFinite(distance))
                    throw new IllegalArgumentException("Branch lengths must be finite and nonnegative, without cumulative overflow. Node: " + child.getName());
                pending.push(new Pending(child, distance));
            }
        }
        return maximum;
    }
}
