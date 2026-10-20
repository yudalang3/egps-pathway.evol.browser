package module.evolview.moderntreeviewer.para;

/** HTML content for the host's standard tooltip renderer. */
final class TreeSettingsHelp {
    private TreeSettingsHelp() { }

    static String tooltip(String title, String... notes) {
        StringBuilder html = new StringBuilder("<html><body><table width='400'><tr><td><b>");
        html.append(escape(title)).append("</b><ul>");
        for (String note : notes) html.append("<li>").append(escape(note)).append("</li>");
        return html.append("</ul></td></tr></table></body></html>").toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\r\n", "\n").replace("\n", "<br>");
    }
}
