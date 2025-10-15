package function;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Trình quản lý lưu/tải trạng thái game (Save/Load).
 *
 * Chức năng chính:
 * - Ghi (save) một ảnh chụp trạng thái (snapshot) của game ra tệp văn bản.
 * - Đọc (load) ảnh chụp trạng thái để khôi phục lại game.
 *
 * Cấu trúc lưu trữ:
 * - Thư mục lưu: "saves" (tự tạo nếu chưa tồn tại)
 * - Tệp lưu: "stage.txt"
 * - Định dạng tệp (dòng-đơn giản, dễ đọc/sửa):
 *   level=<số_màn>
 *   ball=<x>,<y>,<dx>,<dy>            // toạ độ và vận tốc bóng (double)
 *   paddle=<x>,<y>                    // toạ độ paddle (x double, y int)
 *   blocks=<n>                        // số lượng block
 *   <x>,<y>,<hitsRemaining>,<destroyedFlag> (lặp lại n dòng)
 *
 * Quy ước/Đảm bảo:
 * - Sử dụng UTF-8 cho ghi/đọc.
 * - Giá trị boolean destroyedFlag ghi 1 nếu đã bị phá hủy, 0 nếu còn.
 * - Khi đọc, nếu tệp thiếu hoặc sai cấu trúc sẽ ném IOException để nơi gọi xử lý.
 *
 * Mở rộng trong tương lai:
 * - Có thể thêm dòng metadata (thời gian lưu, điểm số, vv.) miễn là vẫn tuân thủ parse tuần tự theo từng "step" như hiện tại.
 */
public class SaveManager {
    private static final String SAVE_DIR = "saves";
    private static final String SAVE_FILE = "stage.txt";

    /**
     * Ghi trạng thái game ra tệp.
     * @param state  ảnh chụp trạng thái cần lưu (không sửa đổi state bên trong hàm)
     * @throws IOException nếu có lỗi I/O khi tạo thư mục/ghi tệp
     */
    public static void save(GameState state) throws IOException {
        Path dir = Path.of(SAVE_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path file = dir.resolve(SAVE_FILE);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            // Ghi phần header/các trường chính
            w.write("level=" + state.level); w.newLine();
            w.write(String.format("ball=%.6f,%.6f,%.6f,%.6f", state.ballX, state.ballY, state.ballDx, state.ballDy)); w.newLine();
            w.write(String.format("paddle=%.6f,%d", state.paddleX, state.paddleY)); w.newLine();
            // Ghi danh sách block
            w.write("blocks=" + state.blocks.size()); w.newLine();
            for (GameState.BlockState b : state.blocks) {
                w.write(String.format("%d,%d,%d,%d", b.x, b.y, b.hitsRemaining, b.destroyed ? 1 : 0));
                w.newLine();
            }
        }
    }

    /**
     * Đọc trạng thái game từ tệp.
     * @return GameState được khôi phục từ nội dung tệp
     * @throws IOException nếu tệp không tồn tại hoặc lỗi định dạng/đọc tệp
     */
    public static GameState load() throws IOException {
        Path file = Path.of(SAVE_DIR).resolve(SAVE_FILE);
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
}
