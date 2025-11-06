package function;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
public class SaveManager {
    private static final String SAVE_DIR = "saves";

    // Tạo tên file theo timestamp: stage_yyyyMMdd_HHmmss.txt
    private static String makeTimestampedFileName() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String stamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "stage_" + stamp + ".txt";
    }

    public static void save(GameState state) throws IOException {
        Path dir = Path.of(SAVE_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        // luôn tạo file mới theo timestamp
        Path file = dir.resolve(makeTimestampedFileName());
        writeStateToFile(state, file, null);

        // Tự động dọn dẹp: chỉ giữ lại 3 bản save gần nhất (không tính file legacy)
        try {
            pruneOldSaves(3);
        } catch (Exception ignored) {}
    }

    /**
     * Save with a custom user-provided name. The name will be sanitized to a safe filename and '.txt' appended.
     * If a file with the same name already exists, a numeric suffix _1, _2, ... will be appended.
     * This method DOES NOT prune old saves; only timestamped auto-saves are pruned elsewhere.
     */
    public static void save(GameState state, String customName) throws IOException {
        Path dir = Path.of(SAVE_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        String base = sanitizeName(customName);
        if (base.isBlank()) base = "save";
        String fileName = ensureTxtExtension(base);
        Path target = dir.resolve(fileName);
        int i = 1;
        while (Files.exists(target)) {
            String candidate = base + "_" + i;
            target = dir.resolve(ensureTxtExtension(candidate));
            i++;
        }
        writeStateToFile(state, target, null);
    }

    // Metadata ghi kèm để nối tiếp phiên chơi sau khi Load
    public static class Metadata {
        public String player;
        public long elapsedMs;
        public int levelsCompleted;
        public int blocksDestroyed;
        public int lives; // optional: saved lives snapshot
        public Metadata() {}
        public Metadata(String player, long elapsedMs, int levelsCompleted, int blocksDestroyed) {
            this.player = player; this.elapsedMs = elapsedMs; this.levelsCompleted = levelsCompleted; this.blocksDestroyed = blocksDestroyed; this.lives = -1;
        }

        public Metadata(String player, long elapsedMs, int levelsCompleted, int blocksDestroyed, int lives) {
            this.player = player; this.elapsedMs = elapsedMs; this.levelsCompleted = levelsCompleted; this.blocksDestroyed = blocksDestroyed; this.lives = lives;
        }
    }

    public static void save(GameState state, String customName, Metadata meta) throws IOException {
        Path dir = Path.of(SAVE_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        String base = sanitizeName(customName);
        if (base.isBlank()) base = "save";
        String fileName = ensureTxtExtension(base);
        Path target = dir.resolve(fileName);
        int i = 1;
        while (Files.exists(target)) {
            String candidate = base + "_" + i;
            target = dir.resolve(ensureTxtExtension(candidate));
            i++;
        }
        writeStateToFile(state, target, meta);
    }

    private static String ensureTxtExtension(String name) {
        String lower = name.toLowerCase();
        if (!lower.endsWith(".txt")) return name + ".txt";
        return name;
    }

    private static String sanitizeName(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        
        String cleaned = trimmed.replaceAll("[\\\\/:*?\"<>|]+", "_");
        
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replace(' ', '_');
        
        if (cleaned.length() > 64) cleaned = cleaned.substring(0, 64);
        
        String upper = cleaned.toUpperCase();
        String[] reserved = {"CON","PRN","AUX","NUL","COM1","COM2","COM3","COM4","COM5","COM6","COM7","COM8","COM9","LPT1","LPT2","LPT3","LPT4","LPT5","LPT6","LPT7","LPT8","LPT9"};
        for (String r : reserved) {
            if (upper.equals(r)) {
                cleaned = cleaned + "_";
                break;
            }
        }
        return cleaned;
    }

    private static void writeStateToFile(GameState state, Path file, Metadata meta) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            if (meta != null) {
                if (meta.player != null) { w.write("meta_player=" + meta.player); w.newLine(); }
                w.write("meta_elapsed=" + meta.elapsedMs); w.newLine();
                w.write("meta_levels=" + meta.levelsCompleted); w.newLine();
                w.write("meta_blocks=" + meta.blocksDestroyed); w.newLine();
                if (meta.lives >= 0) { w.write("meta_lives=" + meta.lives); w.newLine(); }
            }
            w.write("level=" + state.level); w.newLine();
            w.write(String.format("ball=%.6f,%.6f,%.6f,%.6f", state.ballX, state.ballY, state.ballDx, state.ballDy)); w.newLine();
            w.write(String.format("paddle=%.6f,%d", state.paddleX, state.paddleY)); w.newLine();
            w.write("ball_attached=" + (state.ballAttached ? 1 : 0)); w.newLine();
            w.write("blocks=" + state.blocks.size()); w.newLine();
            for (GameState.BlockState b : state.blocks) {
                w.write(String.format("%d,%d,%d,%d", b.x, b.y, b.hitsRemaining, b.destroyed ? 1 : 0));
                w.newLine();
            }
        }
    }

    

    // Load từ một file cụ thể
    public static GameState load(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Save file not found: " + file.toAbsolutePath());
        }
        int level = 1;
        double ballX = 0, ballY = 0, ballDx = 0, ballDy = 0;
        double paddleX = 0; int paddleY = 0;
        int blockCount = 0;
        List<GameState.BlockState> blocks = new ArrayList<>();

        boolean ballAttached = false;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int step = 0;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("meta_")) continue; // bỏ qua metadata ở đây
                if (line.startsWith("ball_attached=")) {
                    try {
                        int v = Integer.parseInt(line.substring("ball_attached=".length()));
                        ballAttached = v != 0;
                    } catch (Exception ignored) {}
                    continue;
                }
                if (step == 0 && line.startsWith("level=")) {
                    level = Integer.parseInt(line.substring(6));
                    step = 1; continue;
                }
                if (step == 1 && line.startsWith("ball=")) {
                    String[] parts = line.substring(5).split(",");
                    ballX = Double.parseDouble(parts[0]);
                    ballY = Double.parseDouble(parts[1]);
                    ballDx = Double.parseDouble(parts[2]);
                    ballDy = Double.parseDouble(parts[3]);
                    step = 2; continue;
                }
                if (step == 2 && line.startsWith("paddle=")) {
                    String[] parts = line.substring(7).split(",");
                    paddleX = Double.parseDouble(parts[0]);
                    paddleY = Integer.parseInt(parts[1]);
                    step = 3; continue;
                }
                if (step == 3 && line.startsWith("blocks=")) {
                    blockCount = Integer.parseInt(line.substring(7));
                    step = 4; continue;
                }
                if (step == 4 && blockCount > 0) {
                    String[] parts = line.split(",");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int hits = Integer.parseInt(parts[2]);
                    boolean destroyed = Integer.parseInt(parts[3]) != 0;
                    blocks.add(new GameState.BlockState(x, y, hits, destroyed));
                    if (blocks.size() == blockCount) break;
                }
            }
        }
        return new GameState(level, ballX, ballY, ballDx, ballDy, paddleX, paddleY, blocks, ballAttached);
    }

    // Đọc metadata (nếu có) từ file save
    public static Metadata readMetadata(Path file) throws IOException {
        if (!Files.exists(file)) return null;
        Metadata meta = new Metadata();
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("level=")) break; // sau phần header không còn metadata
                if (line.startsWith("meta_player=")) meta.player = line.substring("meta_player=".length());
                else if (line.startsWith("meta_elapsed=")) { try { meta.elapsedMs = Long.parseLong(line.substring("meta_elapsed=".length())); } catch (Exception ignored) {} }
                else if (line.startsWith("meta_levels=")) { try { meta.levelsCompleted = Integer.parseInt(line.substring("meta_levels=".length())); } catch (Exception ignored) {} }
                else if (line.startsWith("meta_blocks=")) { try { meta.blocksDestroyed = Integer.parseInt(line.substring("meta_blocks=".length())); } catch (Exception ignored) {} }
                else if (line.startsWith("meta_lives=")) { try { meta.lives = Integer.parseInt(line.substring("meta_lives=".length())); } catch (Exception ignored) { meta.lives = -1; } }
            }
        }
        if ((meta.player == null || meta.player.isBlank()) && meta.elapsedMs == 0 && meta.levelsCompleted == 0 && meta.blocksDestroyed == 0) {
            return null;
        }
        return meta;
    }

    // Liệt kê các file save, trả về danh sách đã sắp xếp mới nhất trước
    public static List<Path> listSaves() throws IOException {
        Path dir = Path.of(SAVE_DIR);
        List<Path> list = new ArrayList<>();
        if (!Files.exists(dir)) return list;
        try (var stream = Files.list(dir)) {
            stream.filter(p -> {
                String fn = p.getFileName().toString().toLowerCase();
                return fn.endsWith(".txt");
            }).forEach(list::add);
        }
        // sort by last modified desc
        list.sort((a, b) -> {
            try {
                long ma = Files.getLastModifiedTime(a).toMillis();
                long mb = Files.getLastModifiedTime(b).toMillis();
                return Long.compare(mb, ma);
            } catch (IOException e) {
                return 0;
            }
        });
        return list;
    }

    // Xóa các save cũ, chỉ giữ lại 'keep' bản mới nhất (chỉ áp dụng cho file stage_*.txt)
    public static void pruneOldSaves(int keep) throws IOException {
        if (keep < 0) keep = 0;
        List<Path> saves = listSaves();
        if (saves.size() <= keep) return;
        for (int i = keep; i < saves.size(); i++) {
            try {
                Files.deleteIfExists(saves.get(i));
            } catch (IOException ignored) {
                // bỏ qua lỗi xóa (có thể do quyền/đang bị mở); vẫn tiếp tục các file khác
            }
        }
    }
}

