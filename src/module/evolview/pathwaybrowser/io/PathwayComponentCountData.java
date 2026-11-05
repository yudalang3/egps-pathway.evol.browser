package module.evolview.pathwaybrowser.io;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Holds parsed gene/component copy counts and optional category groupings across species.
 * Supports both table orientations:
 * <ul>
 *   <li><b>Species-as-rows</b>: Header contains {@code Name} (tree node name), and other columns are gene/component IDs.</li>
 *   <li><b>Genes-as-rows</b> (e.g. {@code components.info.tsv}): First column is {@code name} (gene ID),
 *       optional second column is {@code category} (e.g. {@code cat1..cat9}), and remaining columns are species names.</li>
 * </ul>
 */
public final class PathwayComponentCountData {

    private static final Map<String, String> COMMON_SPECIES_ALIASES = Map.ofEntries(
            Map.entry("human", "homo sapiens"),
            Map.entry("homo sapiens", "human"),
            Map.entry("mouse", "mus musculus"),
            Map.entry("mus musculus", "mouse"),
            Map.entry("chicken", "gallus gallus"),
            Map.entry("gallus gallus", "chicken"),
            Map.entry("zebrafish", "danio rerio"),
            Map.entry("danio rerio", "zebrafish"),
            Map.entry("frog", "xenopus"),
            Map.entry("xenopus", "frog"),
            Map.entry("xenopus tropicalis", "frog"),
            Map.entry("fruitfly", "drosophila melanogaster"),
            Map.entry("drosophila melanogaster", "fruitfly"),
            Map.entry("worm", "caenorhabditis elegans"),
            Map.entry("caenorhabditis elegans", "worm"),
            Map.entry("yeast", "saccharomyces cerevisiae"),
            Map.entry("saccharomyces cerevisiae", "yeast")
    );

    private final List<String> speciesNames;
    private final List<String> geneNames;
    private final Map<String, String> gene2Category;
    private final Map<String, Map<String, Integer>> species2GeneCounts;
    private final Map<String, Map<String, Integer>> species2CategoryCopySum;
    private final Map<String, Map<String, Integer>> species2CategoryPresentGenes;
    private final Map<String, Integer> category2TotalGenes;
    private final List<String> normalizedHeaders;
    private final List<List<String>> normalizedRows;

    private PathwayComponentCountData(
            List<String> speciesNames,
            List<String> geneNames,
            Map<String, String> gene2Category,
            Map<String, Map<String, Integer>> species2GeneCounts,
            Map<String, Map<String, Integer>> species2CategoryCopySum,
            Map<String, Map<String, Integer>> species2CategoryPresentGenes,
            Map<String, Integer> category2TotalGenes,
            List<String> normalizedHeaders,
            List<List<String>> normalizedRows) {
        this.speciesNames = Collections.unmodifiableList(speciesNames);
        this.geneNames = Collections.unmodifiableList(geneNames);
        this.gene2Category = Collections.unmodifiableMap(gene2Category);
        this.species2GeneCounts = Collections.unmodifiableMap(species2GeneCounts);
        this.species2CategoryCopySum = Collections.unmodifiableMap(species2CategoryCopySum);
        this.species2CategoryPresentGenes = Collections.unmodifiableMap(species2CategoryPresentGenes);
        this.category2TotalGenes = Collections.unmodifiableMap(category2TotalGenes);
        this.normalizedHeaders = Collections.unmodifiableList(normalizedHeaders);
        this.normalizedRows = Collections.unmodifiableList(normalizedRows);
    }

    public static PathwayComponentCountData load(File file) throws IOException {
        if (file == null || !file.isFile()) {
            return empty();
        }
        RawTable raw = readRawTable(file);
        return fromRawTable(raw.headers(), raw.rows());
    }

    public static PathwayComponentCountData empty() {
        return new PathwayComponentCountData(
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of()
        );
    }

    public static PathwayComponentCountData fromRawTable(List<String> headers, List<List<String>> rows) {
        if (headers == null || headers.isEmpty() || rows == null) {
            return empty();
        }

        int exactNameIdx = headers.indexOf("Name");
        int geneRowNameIdx = -1;
        int categoryIdx = -1;
        if (exactNameIdx < 0) {
            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i) == null ? "" : headers.get(i).trim();
                if (geneRowNameIdx < 0 && (h.equalsIgnoreCase("name") || h.equalsIgnoreCase("gene") || h.equalsIgnoreCase("component"))) {
                    geneRowNameIdx = i;
                } else if (categoryIdx < 0 && (h.equalsIgnoreCase("category") || h.equalsIgnoreCase("stage") || h.equalsIgnoreCase("group"))) {
                    categoryIdx = i;
                }
            }
        }

        if (exactNameIdx < 0 && geneRowNameIdx >= 0) {
            return parseGenesAsRows(headers, rows, geneRowNameIdx, categoryIdx);
        }
        return parseSpeciesAsRows(headers, rows, exactNameIdx);
    }

    private static PathwayComponentCountData parseGenesAsRows(
            List<String> headers,
            List<List<String>> rows,
            int geneColIdx,
            int catColIdx) {
        List<Integer> speciesCols = new ArrayList<>();
        List<String> speciesNames = new ArrayList<>();
        for (int c = 0; c < headers.size(); c++) {
            if (c == geneColIdx || c == catColIdx) {
                continue;
            }
            String sp = headers.get(c) == null ? "" : headers.get(c).trim();
            if (!sp.isEmpty()) {
                speciesCols.add(c);
                speciesNames.add(sp);
            }
        }

        List<String> geneNames = new ArrayList<>();
        Map<String, String> gene2Category = new LinkedHashMap<>();
        Map<String, Integer> category2TotalGenes = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> species2GeneCounts = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> species2CategoryCopySum = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> species2CategoryPresentGenes = new LinkedHashMap<>();

        for (String sp : speciesNames) {
            species2GeneCounts.put(sp, new LinkedHashMap<>());
            species2CategoryCopySum.put(sp, new LinkedHashMap<>());
            species2CategoryPresentGenes.put(sp, new LinkedHashMap<>());
        }

        for (List<String> row : rows) {
            if (row == null || geneColIdx >= row.size()) {
                continue;
            }
            String gene = row.get(geneColIdx) == null ? "" : row.get(geneColIdx).trim();
            if (gene.isEmpty()) {
                continue;
            }
            geneNames.add(gene);
            String cat = (catColIdx >= 0 && catColIdx < row.size() && row.get(catColIdx) != null)
                    ? row.get(catColIdx).trim()
                    : "";
            if (!cat.isEmpty()) {
                gene2Category.put(gene, cat);
                category2TotalGenes.merge(cat, 1, Integer::sum);
            }

            for (int i = 0; i < speciesCols.size(); i++) {
                int col = speciesCols.get(i);
                String sp = speciesNames.get(i);
                String cell = col < row.size() ? row.get(col) : "";
                int count = parseCount(cell);
                species2GeneCounts.get(sp).put(gene, count);
                if (!cat.isEmpty()) {
                    species2CategoryCopySum.get(sp).merge(cat, count, Integer::sum);
                    if (count > 0) {
                        species2CategoryPresentGenes.get(sp).merge(cat, 1, Integer::sum);
                    } else {
                        species2CategoryPresentGenes.get(sp).putIfAbsent(cat, 0);
                    }
                }
            }
        }

        // Build transposed species-as-rows table for PathwayComponentPanel
        List<String> normHeaders = new ArrayList<>(geneNames.size() + 1);
        normHeaders.add("Name");
        normHeaders.addAll(geneNames);

        List<List<String>> normRows = new ArrayList<>(speciesNames.size());
        for (String sp : speciesNames) {
            List<String> r = new ArrayList<>(normHeaders.size());
            r.add(sp);
            Map<String, Integer> gMap = species2GeneCounts.getOrDefault(sp, Map.of());
            for (String g : geneNames) {
                r.add(String.valueOf(gMap.getOrDefault(g, 0)));
            }
            normRows.add(Collections.unmodifiableList(r));
        }

        return new PathwayComponentCountData(
                speciesNames,
                geneNames,
                gene2Category,
                species2GeneCounts,
                species2CategoryCopySum,
                species2CategoryPresentGenes,
                category2TotalGenes,
                normHeaders,
                normRows
        );
    }

    private static PathwayComponentCountData parseSpeciesAsRows(
            List<String> headers,
            List<List<String>> rows,
            int nameColIdx) {
        if (nameColIdx < 0) {
            return new PathwayComponentCountData(
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    headers,
                    rows
            );
        }

        List<Integer> geneCols = new ArrayList<>();
        List<String> geneNames = new ArrayList<>();
        for (int c = 0; c < headers.size(); c++) {
            if (c == nameColIdx) {
                continue;
            }
            String g = headers.get(c) == null ? "" : headers.get(c).trim();
            if (!g.isEmpty()) {
                geneCols.add(c);
                geneNames.add(g);
            }
        }

        List<String> speciesNames = new ArrayList<>();
        Map<String, Map<String, Integer>> species2GeneCounts = new LinkedHashMap<>();

        for (List<String> row : rows) {
            if (row == null || nameColIdx >= row.size()) {
                continue;
            }
            String sp = row.get(nameColIdx) == null ? "" : row.get(nameColIdx).trim();
            if (sp.isEmpty() || species2GeneCounts.containsKey(sp)) {
                continue;
            }
            speciesNames.add(sp);
            Map<String, Integer> gMap = new LinkedHashMap<>();
            for (int i = 0; i < geneCols.size(); i++) {
                int col = geneCols.get(i);
                String g = geneNames.get(i);
                String cell = col < row.size() ? row.get(col) : "";
                gMap.put(g, parseCount(cell));
            }
            species2GeneCounts.put(sp, gMap);
        }

        return new PathwayComponentCountData(
                speciesNames,
                geneNames,
                Map.of(),
                species2GeneCounts,
                Map.of(),
                Map.of(),
                Map.of(),
                headers,
                rows
        );
    }

    private static int parseCount(String value) {
        if (value == null) {
            return 0;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(trimmed));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isEmpty() {
        return species2GeneCounts.isEmpty() && geneNames.isEmpty();
    }

    public List<String> getSpeciesNames() {
        return speciesNames;
    }

    public List<String> getGeneNames() {
        return geneNames;
    }

    public Set<String> getGeneNameSetUpper() {
        Set<String> set = new HashSet<>();
        for (String g : geneNames) {
            set.add(g.toUpperCase(Locale.ROOT));
        }
        return set;
    }

    public Map<String, String> getGene2Category() {
        return gene2Category;
    }

    public Map<String, Integer> getCategory2TotalGenes() {
        return category2TotalGenes;
    }

    public List<String> getNormalizedHeaders() {
        return normalizedHeaders;
    }

    public List<List<String>> getNormalizedRows() {
        return normalizedRows;
    }

    public Map<String, Integer> getGeneCountsForSpecies(String nodeName) {
        return lookupSpeciesMap(species2GeneCounts, nodeName);
    }

    public Map<String, Integer> getCategoryCopySumForSpecies(String nodeName) {
        return lookupSpeciesMap(species2CategoryCopySum, nodeName);
    }

    public Map<String, Integer> getCategoryPresentGenesForSpecies(String nodeName) {
        return lookupSpeciesMap(species2CategoryPresentGenes, nodeName);
    }

    private static Map<String, Integer> lookupSpeciesMap(
            Map<String, Map<String, Integer>> map,
            String nodeName) {
        Map<String, Integer> found = lookupBySpeciesName(map, nodeName);
        return found != null ? found : Collections.emptyMap();
    }

    public static <T> T lookupBySpeciesName(Map<String, T> map, String nodeName) {
        if (nodeName == null || nodeName.isBlank() || map == null || map.isEmpty()) {
            return null;
        }
        String trimmed = nodeName.trim();
        T direct = map.get(trimmed);
        if (direct != null) {
            return direct;
        }
        String normalized = trimmed.replace('_', ' ');
        for (Map.Entry<String, T> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key != null && (key.equalsIgnoreCase(trimmed) || key.equalsIgnoreCase(normalized))) {
                return entry.getValue();
            }
        }
        String alias = COMMON_SPECIES_ALIASES.get(normalized.toLowerCase(Locale.ROOT));
        if (alias != null) {
            for (Map.Entry<String, T> entry : map.entrySet()) {
                String key = entry.getKey();
                if (key != null && (key.equalsIgnoreCase(alias)
                        || key.toLowerCase(Locale.ROOT).startsWith(alias.toLowerCase(Locale.ROOT)))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Resolves the copy count for a slide shape's gene identifier from a species' gene count map.
     * Supports exact match (case-insensitive) as well as family-prefix match (e.g. slide has {@code AXIN}
     * and table has {@code AXIN2}, or slide has {@code WNT} and table has {@code WNT3A}).
     */
    public static OptionalInt resolveGeneCount(Map<String, Integer> speciesGeneCounts, String shapeGeneId) {
        if (speciesGeneCounts == null || speciesGeneCounts.isEmpty() || shapeGeneId == null || shapeGeneId.isBlank()) {
            return OptionalInt.empty();
        }
        String target = shapeGeneId.trim();
        Integer exact = speciesGeneCounts.get(target);
        if (exact != null) {
            return OptionalInt.of(exact);
        }
        for (Map.Entry<String, Integer> entry : speciesGeneCounts.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(target)) {
                return OptionalInt.of(entry.getValue());
            }
        }
        // Prefix fallback (e.g. AXIN <-> AXIN2, WNT <-> WNT3A, DVL <-> DVL3)
        if (target.length() >= 3) {
            String upperTarget = target.toUpperCase(Locale.ROOT);
            int sum = 0;
            boolean matched = false;
            for (Map.Entry<String, Integer> entry : speciesGeneCounts.entrySet()) {
                String upperKey = entry.getKey().toUpperCase(Locale.ROOT);
                if (upperKey.startsWith(upperTarget) || upperTarget.startsWith(upperKey)) {
                    sum += entry.getValue();
                    matched = true;
                }
            }
            if (matched) {
                return OptionalInt.of(sum);
            }
        }
        return OptionalInt.empty();
    }

    public record RawTable(List<String> headers, List<List<String>> rows) {}

    public static RawTable readRawTable(File file) throws IOException {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return readExcel(file, name.endsWith(".xlsx"));
        }
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new RawTable(List.of(), List.of());
        }
        String first = lines.getFirst();
        if (!first.isEmpty() && first.charAt(0) == '\uFEFF') {
            first = first.substring(1);
        }
        List<String> headers = List.of(first.split("\t", -1));
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            String[] normalized = new String[headers.size()];
            for (int c = 0; c < normalized.length; c++) {
                normalized[c] = (c < parts.length && parts[c] != null) ? parts[c] : "";
            }
            rows.add(List.of(normalized));
        }
        return new RawTable(headers, rows);
    }

    private static RawTable readExcel(File file, boolean isXlsx) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = isXlsx ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
            if (workbook.getNumberOfSheets() == 0) {
                return new RawTable(List.of(), List.of());
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return new RawTable(List.of(), List.of());
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return new RawTable(List.of(), List.of());
            }
            int colCount = headerRow.getLastCellNum();
            if (colCount <= 0) {
                return new RawTable(List.of(), List.of());
            }
            List<String> headers = new ArrayList<>(colCount);
            for (int c = 0; c < colCount; c++) {
                headers.add(cellToString(headerRow.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
            }
            List<List<String>> rows = new ArrayList<>();
            int firstDataRow = sheet.getFirstRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int r = firstDataRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String[] cells = new String[colCount];
                for (int c = 0; c < colCount; c++) {
                    cells[c] = cellToString(row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
                }
                rows.add(List.of(cells));
            }
            return new RawTable(headers, rows);
        }
    }

    private static String cellToString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield new DecimalFormat("0").format(val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
