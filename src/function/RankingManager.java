package function;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Quản lý bảng xếp hạng (ranking) đơn giản bằng file CSV.
 * Sắp xếp: số màn (desc) -> số block phá (desc) -> thời gian (asc) -> thời điểm ghi (asc).
 */
public class RankingManager {
    private static final String SAVE_DIR = "saves";
    private static final String RANK_FILE = "ranking.csv"; // đặt trong thư mục saves

    public static class Entry {
        public final String player;
        public final int levels;
        public final int blocks;
        public final long elapsedMs;
        public final long timestamp;
        public Entry(String player, int levels, int blocks, long elapsedMs, long timestamp) {
            this.player = player;
            this.levels = levels;
            this.blocks = blocks;
            this.elapsedMs = elapsedMs;
            this.timestamp = timestamp;
        }
    }

    public static void addEntry(String player, int levels, int blocks, long elapsedMs) throws IOException {
        Path dir = Path.of(SAVE_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        Path file = dir.resolve(RANK_FILE);
        String safePlayer = sanitize(player);
        String line = String.format("%d,%s,%d,%d,%d", System.currentTimeMillis(), escape(safePlayer), levels, blocks, elapsedMs);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8, Files.exists(file) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE)) {
            w.write(line);
            w.newLine();
        }
        // After appending, prune to top 100 best runs
        try {
            pruneToTop(100);
        } catch (Exception ignored) {}
    }

    public static List<Entry> loadAll() {
        Path file = Path.of(SAVE_DIR).resolve(RANK_FILE);
        List<Entry> list = new ArrayList<>();
        if (!Files.exists(file)) return list;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = parseCsv(line);
                if (parts.length < 5) continue;
                long ts = parseLong(parts[0], 0);
                String player = unescape(parts[1]);
                int levels = (int) parseLong(parts[2], 0);
                int blocks = (int) parseLong(parts[3], 0);
                long elapsed = parseLong(parts[4], 0);
                list.add(new Entry(player, levels, blocks, elapsed, ts));
            }
        } catch (IOException ignored) {}
        return list;
    }

    public static List<Entry> getSorted(int limit) {
        List<Entry> list = loadAll();
        list.sort(bestComparator());
        if (limit > 0 && list.size() > limit) return new ArrayList<>(list.subList(0, limit));
        return list;
    }

    private static Comparator<Entry> bestComparator() {
        return Comparator
            .comparingInt((Entry e) -> e.levels).reversed()
            .thenComparing(Comparator.comparingInt((Entry e) -> e.blocks).reversed())
            .thenComparingLong(e -> e.elapsedMs)
            .thenComparingLong(e -> e.timestamp);
    }

    /**
     * Keep only top 'keep' best runs in the ranking file.
     */
    public static void pruneToTop(int keep) throws IOException {
        if (keep <= 0) keep = 1;
        List<Entry> all = loadAll();
        if (all.size() <= keep) return;
        all.sort(bestComparator());
        List<Entry> top = new ArrayList<>(all.subList(0, keep));
        // overwrite file
        Path file = Path.of(SAVE_DIR).resolve(RANK_FILE);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING, java.nio.file.StandardOpenOption.CREATE)) {
            for (Entry e : top) {
                String line = String.format("%d,%s,%d,%d,%d", e.timestamp, escape(e.player), e.levels, e.blocks, e.elapsedMs);
                w.write(line);
                w.newLine();
            }
        }
    }

    private static String sanitize(String s) {
        if (s == null) return "Player";
        String t = s.trim();
        if (t.isEmpty()) return "Player";
        return t;
    }

    // very small CSV utilities (only handles commas and quotes for one field)
    private static String escape(String s) {
        if (s.contains(",") || s.contains("\"")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
    private static String unescape(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            String inner = s.substring(1, s.length()-1);
            return inner.replace("\"\"", "\"");
        }
        return s;
    }
    private static String[] parseCsv(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i=0;i<line.length();i++) {
            char c = line.charAt(i);
            if (inQ) {
                if (c=='"') {
                    if (i+1<line.length() && line.charAt(i+1)=='"') { cur.append('"'); i++; }
                    else { inQ=false; }
                } else cur.append(c);
            } else {
                if (c==',') { parts.add(cur.toString()); cur.setLength(0); }
                else if (c=='"') inQ=true;
                else cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }
    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }
}
