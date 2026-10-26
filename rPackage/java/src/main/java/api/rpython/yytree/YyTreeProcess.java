package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/** One sequential JSON-lines session per R process. stdout is reserved for replies. */
public final class YyTreeProcess {
    private YyTreeProcess() { }

    public static void main(String[] args) throws Exception {
        PrintStream protocol = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        System.setOut(System.err);
        Logger log = LoggerFactory.getLogger(YyTreeProcess.class);
        YyTreeBridge bridge = new YyTreeBridge();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                JSONObject response = new JSONObject(true);
                boolean shutdown = false;
                try {
                    JSONObject request = JSON.parseObject(line);
                    response.put("id", request.get("id"));
                    JSONArray arguments = request.getJSONArray("args");
                    String command = request.getString("command");
                    String result = switch (command) {
                        case "probeRuntime" -> {
                            JSONObject probe = JSON.parseObject(bridge.probeRuntime());
                            probe.put("pid", ProcessHandle.current().pid());
                            probe.put("maxMemory", Runtime.getRuntime().maxMemory());
                            probe.put("jvmArguments", ManagementFactory.getRuntimeMXBean().getInputArguments());
                            yield probe.toJSONString();
                        }
                        case "showPlot" -> bridge.showPlot(arguments.getString(0), arguments.getBooleanValue(1));
                        case "showParameterError" -> bridge.showParameterError(arguments.getString(0));
                        case "renderPng" -> bridge.renderPng(arguments.getString(0), arguments.getString(1));
                        case "testInteraction" -> bridge.testInteraction(arguments.getString(0), arguments.size() > 1 ? arguments.getString(1) : null);
                        case "previewDiagnostics" -> previewDiagnostics();
                        case "closeAll", "shutdown" -> {
                            bridge.closeAll();
                            shutdown = command.equals("shutdown");
                            yield null;
                        }
                        default -> throw new IllegalArgumentException("Unknown command: " + command);
                    };
                    response.put("ok", true);
                    response.put("result", result);
                } catch (Exception | StackOverflowError e) {
                    log.error("yytree request failed", e);
                    response.put("ok", false);
                    Throwable cause = e;
                    while (cause instanceof InvocationTargetException && cause.getCause() != null)
                        cause = cause.getCause();
                    String message = cause.toString();
                    if (cause instanceof StackOverflowError)
                        message += ". Increase max_memory_stack with config_yytree(..., restart = TRUE). Increasing max_memory_heap does not increase the thread stack.";
                    response.put("error", message);
                }
                protocol.println(response.toJSONString());
                if (protocol.checkError() || shutdown) break;
            }
        } finally {
            bridge.closeAll();
        }
    }

    private static String previewDiagnostics() throws Exception {
        AtomicReference<String> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JSONObject data = new JSONObject();
            data.put("theme", UIManager.getLookAndFeel().getName());
            if (!GraphicsEnvironment.isHeadless()) {
                data.put("display_scale", GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX());
            }
            result.set(data.toJSONString());
        });
        return result.get();
    }
}
