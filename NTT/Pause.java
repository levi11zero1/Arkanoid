package NTT;

/**
 * Bộ điều khiển tạm dừng.
 *
 * Cách sử dụng:
 * - Vòng lặp chạy trên luồng: gọi Pause.getInstance().getLock() và chờ khi đang tạm dừng.
 * - Swing Timer: dừng timer trong pause(), khởi động lại trong resume().
 * - Giao diện: gọi Pause.getInstance().toggle() từ bàn phím (ví dụ KeyEvent.VK_P).
 */
public class Pause {

	private static final Pause INSTANCE = new Pause();

	// volatile để các phép đọc an toàn giữa các luồng
	private volatile boolean paused = false;

	// đối tượng khóa cho vòng lặp chạy trên luồng để chờ khi tạm dừng
	private final Object lock = new Object();

	// hook listener tùy chọn cho các thành phần UI/âm thanh
	private PauseListener listener;

	private Pause() {
	}

	public static Pause getInstance() {
		return INSTANCE;
	}

	public boolean isPaused() {
		return paused;
	}

	/**
	 * Tạm dừng trò chơi. Thông báo listener và để các luồng chờ.
	 */
	public void pause() {
		if (!paused) {
			paused = true;
			if (listener != null) {
				try { listener.onPause(); } catch (Throwable t) { /* lỗi ở listener bị bỏ qua */ }
			}
		}
	}

	/**
	 * Tiếp tục trò chơi. Thông báo các luồng đang chờ và listener.
	 */
	public void resume() {
		if (paused) {
			paused = false;
			// đánh thức các luồng đang chờ
			synchronized (lock) {
				lock.notifyAll();
			}
			if (listener != null) {
				try { listener.onResume(); } catch (Throwable t) { /* lỗi ở listener sẽ bị bỏ qua */ }
			}
		}
	}

	public void toggle() {
		if (isPaused()) resume(); else pause();
	}

	/**
	 * Đối tượng khóa dùng cho các vòng lặp chạy trên luồng. 
	 */
	public Object getLock() {
		return lock;
	}

	public void setListener(PauseListener l) {
		this.listener = l;
	}

	public interface PauseListener {
		void onPause();
		void onResume();
	}

}
