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
        writeStateToFile(state, file);

        // Tự động dọn dẹp: chỉ giữ lại 3 bản save gần nhất (không tính file legacy)
        try {
            pruneOldSaves(3);
        } catch (Exception ignored) {}
    }

    private static void writeStateToFile(GameState state, Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("level=" + state.level); w.newLine();
            w.write(String.format("ball=%.6f,%.6f,%.6f,%.6f", state.ballX, state.ballY, state.ballDx, state.ballDy)); w.newLine();
            w.write(String.format("paddle=%.6f,%d", state.paddleX, state.paddleY)); w.newLine();
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

        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int step = 0;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
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
        return new GameState(level, ballX, ballY, ballDx, ballDy, paddleX, paddleY, blocks);
    }

    // Liệt kê các file save, trả về danh sách đã sắp xếp mới nhất trước
    public static List<Path> listSaves() throws IOException {
        Path dir = Path.of(SAVE_DIR);
        List<Path> list = new ArrayList<>();
        if (!Files.exists(dir)) return list;
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase().startsWith("stage_") && p.getFileName().toString().toLowerCase().endsWith(".txt"))
                  .forEach(list::add);
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

