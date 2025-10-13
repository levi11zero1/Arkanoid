import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Iterator;
import java.util.Random;


public class ArkanoidGame {

    private static final String CARD_MENU = "menu";
    private static final String CARD_GAME = "game";
    private static final String CARD_INSTRUCTIONS = "instructions";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Arkanoid Basic");

            // Sử dụng CardLayout để chuyển màn hình Menu/Game/Instructions
            CardLayout cardLayout = new CardLayout();
            JPanel cards = new JPanel(cardLayout);

            // Đường dẫn ảnh nền (bạn có thể thay bằng ảnh của bạn)
            String backgroundPath = "images/arkanoid-background-intro.jpg"; // Đặt ảnh vào cùng thư mục này hoặc cập nhật đường dẫn

            // Tạo MenuPanel
            MenuPanel menu = new MenuPanel(backgroundPath);
            menu.setHorizontalAlignment(MenuPanel.Alignment.LEFT);
            menu.setSideMargins(500, 12);
            menu.moveUp(230);
            cards.add(menu, CARD_MENU);

            // Tạo panel hướng dẫn (ban đầu tạo sẵn để điều hướng)
            InstructionsPanel instructionsPanel = new InstructionsPanel();
            cards.add(instructionsPanel, CARD_INSTRUCTIONS);

            // Lắng nghe nút Chơi
            menu.getPlayButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Khi bấm Chơi, tạo GamePanel mới và chuyển sang thẻ game
                    GamePanel gamePanel = new GamePanel();
                    // Khi Game Over thì quay lại menu
                    gamePanel.setEventsListener(new GamePanel.GameEvents() {
                        @Override
                        public void onGameOver() {
                            // Xóa thẻ game hiện tại (để tránh giữ timer cũ)
                            cards.remove(gamePanel);
                            cardLayout.show(cards, CARD_MENU);
                            menu.requestFocusInWindow();
                        }
                    });

                    cards.add(gamePanel, CARD_GAME);
                    cardLayout.show(cards, CARD_GAME);
                    gamePanel.requestFocusInWindow();
                }
            });

            // Lắng nghe nút Hướng dẫn
            menu.getInstructionsButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cardLayout.show(cards, CARD_INSTRUCTIONS);
                    instructionsPanel.requestFocusInWindow();
                }
            });

            // Nút Quay lại trong màn Hướng dẫn
            instructionsPanel.getBackButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cardLayout.show(cards, CARD_MENU);
                    menu.requestFocusInWindow();
                }
            });

            frame.setContentPane(cards);
            frame.setSize(684, 800);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Hiển thị màn menu đầu tiên
            cardLayout.show(cards, CARD_MENU);
        });
    }
}

/**
 * Bộ điều khiển tạm dừng.
 *
 * Cách sử dụng:
 * - Vòng lặp chạy trên luồng: gọi Pause.getInstance().getLock() và chờ khi đang tạm dừng.
 * - Swing Timer: dừng timer trong pause(), khởi động lại trong resume().
 * - Giao diện: gọi Pause.getInstance().toggle() từ bàn phím (ví dụ KeyEvent.VK_P).
 */
class Pause {

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
                try {
                    listener.onPause();
                } catch (Throwable t) { /* lỗi ở listener bị bỏ qua */ }
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
                try {
                    listener.onResume();
                } catch (Throwable t) { /* lỗi ở listener sẽ bị bỏ qua */ }
            }
        }
    }

    public void toggle() {
        if (isPaused()) resume();
        else pause();
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
class Block {
    private int x, y, width, height;
    private boolean destroyed = false;

    public Block(int x, int y, int width, int height) {
        this.x = x; this.y = y;
        this.width = width; this.height = height;
    }

    public void draw(Graphics g) {
        if (!destroyed) {
            g.setColor(Color.RED);
            g.fillRect(x, y, width, height);
        }
    }

    public boolean isHit(int ballX, int ballY, int ballSize) {
        if (!destroyed && ballX + ballSize > x && ballX < x + width &&
                ballY + ballSize > y && ballY < y + height) {
            destroyed = true;
            return true;
        }
        return false;
    }
}

/**
 * Đại diện cho một viên gạch trong game.
 *
 * Lưu vị trí và kích thước của viên gạch (x, y, width, height).
 *
 * Kiểm tra va chạm với bóng bằng hàm isHit(...).
 *
 * Ẩn viên gạch khi bị phá bằng cách đánh dấu destroyed = true.
 *
 * Vẽ viên gạch lên màn hình bằng hàm draw(Graphics g) nếu chưa bị phá.
 *
 */
class Ball {
    public int x, y, size = 20;
    public int dx = 2, dy = -2;
    public int sizenor = 20;
    private javax.swing.Timer sizeTimer;

    public void applyPowerUp(PowerUp.Type type) {
        if (sizeTimer != null && sizeTimer.isRunning()) {
            sizeTimer.stop();
            resetSize();
        }
        if (type == PowerUp.Type.BALL_EXPAND) size *= 1.5;
        else if (type == PowerUp.Type.BALL_SHRINK) size /= 1.5;

        sizeTimer = new javax.swing.Timer(10000, e -> { resetSize(); sizeTimer.stop(); });
        sizeTimer.setRepeats(false);
        sizeTimer.start();
    }


    public Ball(int x, int y) {
        this.x = x; this.y = y;
    }

    public void move() {
        x += dx;
        y += dy;
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillOval(x, y, size, size);
    }
    public void resetSize() {
        size = sizenor;
    }

    public void bounceX() { dx = -dx; }
    public void bounceY() { dy = -dy; }
}

/**
 * Quản lý vị trí bóng trên màn hình (x, y).
 *
 * Di chuyển bóng theo hướng (dx, dy) bằng hàm move().
 *
 * Vẽ bóng lên giao diện bằng hàm draw(Graphics g).
 *
 * Xử lý va chạm bằng cách đảo chiều chuyển động:
 *
 * bounceX() → đổi hướng ngang khi chạm tường trái/phải.
 *
 * bounceY() → đổi hướng dọc khi chạm tường trên, paddle, hoặc block.
 *
 */
class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Ball ball;
    private Paddle paddle;
    private List<Block> blocks;
    private Timer timer;
    private GameEvents eventsListener;
    private java.util.List<PowerUp> activePowerUps = new ArrayList<>();
    private javax.swing.Timer spawnTimer;
    private Random random = new Random();  // ✅ chỉ tạo 1 lần duy nhất


    public GamePanel() {
        ball = new Ball(200, 300);
        paddle = new Paddle(150, 550);
        blocks = new ArrayList<>();
        // Trong constructor GamePanel()
        spawnTimer = new javax.swing.Timer(14000, e -> spawnRandomPowerUp()); // mỗi 30s
        spawnTimer.setRepeats(true);
        spawnTimer.start();



        // Tạo lưới block: 5 hàng × 8 cột
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 8; col++) {
                int x = 50 + col * 45;
                int y = 50 + row * 25;
                blocks.add(new Block(x, y, 40, 20));
            }
        }

        timer = new Timer(10, this);
        timer.start();

        // Kết nối pause với Swing timer: dừng timer khi tạm dừng, khởi động lại khi tiếp tục
        Pause.getInstance().setListener(new Pause.PauseListener() {
            @Override
            public void onPause() {
                timer.stop();
            }

            @Override
            public void onResume() {
                timer.start();
            }
        });


        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        ball.draw(g);
        paddle.draw(g);
        for (Block block : blocks) {
            block.draw(g);
        }
        for (PowerUp p : activePowerUps) {
            g.setColor(p.getColor());
            g.fillRect(p.getX(), p.getY(), p.getWidth(), p.getHeight());
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ball.move();

        // Va chạm với mép màn hình
        if (ball.x <= 0 || ball.x + ball.size >= getWidth()) ball.bounceX();
        if (ball.y <= 0) ball.bounceY();

        // Va chạm với paddle
        if (paddle.isHit(ball.x, ball.y, ball.size)) ball.bounceY();

        // Va chạm với viên gạch (block)
        for (Block block : blocks) {
            if (block.isHit(ball.x, ball.y, ball.size)) {
                ball.bounceY();
                break; // tránh va chạm nhiều viên gạch cùng lúc
            }
        }

        // Game Over nếu bóng rơi xuống dưới
        if (ball.y > getHeight()) {
            timer.stop();
            JOptionPane.showMessageDialog(this, "Game Over!");
            if (eventsListener != null) {
                // Chuyển về menu thông qua listener
                eventsListener.onGameOver();
            }
        }


        for (Iterator<PowerUp> it = activePowerUps.iterator(); it.hasNext();) {
            PowerUp p = it.next();
            p.updatePosition();

            if (p.getBounds().intersects(paddle.getBounds())) {
                applyPowerUpEffect(p);
                it.remove();
                continue;
            }
            if (p.isOutOfBounds(getHeight())) {
                it.remove();
            }
        }


        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        //chuyển trạng thái tạm dừng
        if (code == KeyEvent.VK_P) {
            Pause.getInstance().toggle();
        }

        // khi đang tạm dừng, bỏ qua phím di chuyển
        if (Pause.getInstance().isPaused()) return;

        if (code == KeyEvent.VK_LEFT) paddle.moveLeft();
        if (code == KeyEvent.VK_RIGHT) paddle.moveRight(getWidth());

    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    // Cho phép ArkanoidGame đăng ký lắng nghe sự kiện trong game
    public void setEventsListener(GameEvents listener) {
        this.eventsListener = listener;
    }

    public interface GameEvents {
        void onGameOver();
    }
    private void spawnRandomPowerUp() {
        PowerUp.Type[] types = PowerUp.Type.values();
        PowerUp.Type randomType = types[random.nextInt(types.length)];
        int spawnX = random.nextInt(getWidth() - 20);
        PowerUp p = new PowerUp(randomType, spawnX, 0);

        int spawnY = 0;

        activePowerUps.add(p);
    }
    private void applyPowerUpEffect(PowerUp p) {
        PowerUp.Type type = p.getType();
        if (type == PowerUp.Type.PADDLE_EXPAND || type == PowerUp.Type.PADDLE_SHRINK) {
            paddle.applyPowerUp(type);
        } else if (type == PowerUp.Type.BALL_EXPAND || type == PowerUp.Type.BALL_SHRINK) {
            ball.applyPowerUp(type);
        }
    }

}

/**
 * Quản lý toàn bộ logic game: bóng, paddle, blocks.
 *
 * Xử lý va chạm, di chuyển, vẽ các thành phần.
 *
 * Kiểm tra điều kiện thắng/thua (bóng rơi xuống → thua).
 *
 * Lắng nghe phím điều khiển trái/phải từ người chơi.
 *
 * Vẽ toàn bộ giao diện game trong paintComponent(...).
 */
class StyledButton extends JButton {
    private Color bg = new Color(0x282B88);        // màu nền mặc định (primary)
    private Color fg = Color.BLACK;                // màu chữ
    private Color bgHover = new Color(0x3793F0);   // khi hover
    private Color bgPress = new Color(0x2576C4);   // khi nhấn
    private Color border = new Color(0x282B88);    // viền
    private int cornerRadius = 18;
    private boolean hovered = false;

    public StyledButton(String text) {
        super(text);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(fg);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(getFont().deriveFont(Font.BOLD, 16f));
        setMargin(new Insets(8, 16, 8, 16));

        // Lắng nghe hover
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
    }

    public void setColors(Color background, Color hover, Color press, Color foreground, Color borderColor) {
        if (background != null) this.bg = background;
        if (hover != null) this.bgHover = hover;
        if (press != null) this.bgPress = press;
        if (foreground != null) { this.fg = foreground; setForeground(foreground); }
        if (borderColor != null) this.border = borderColor;
        repaint();
    }

    public void setCornerRadius(int r) {
        this.cornerRadius = Math.max(0, r);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Chọn màu theo trạng thái
        ButtonModel model = getModel();
        Color fill = bg;
        if (model.isArmed() || model.isPressed()) fill = bgPress;
        else if (hovered) fill = bgHover;

        // Đổ bóng nhẹ
        g2.setComposite(AlphaComposite.SrcOver.derive(0.25f));
        Shape shadow = new RoundRectangle2D.Float(2, 3, w - 4, h - 4, cornerRadius, cornerRadius);
        g2.setColor(Color.BLACK);
        g2.fill(shadow);

        // Nền bo góc
        g2.setComposite(AlphaComposite.SrcOver);
        Shape rr = new RoundRectangle2D.Float(0, 0, w - 3, h - 3, cornerRadius, cornerRadius);
        g2.setColor(fill);
        g2.fill(rr);

        // Viền
        g2.setColor(border);
        g2.draw(rr);

        g2.dispose();
        // Vẽ text/icon mặc định (nền đã vẽ tay)
        super.paintComponent(g);
    }
}
class Paddle {
    public int x, y, width = 100, height = 15;
    private javax.swing.Timer sizeTimer;
    public int normalWidth = width;

    public void applyPowerUp(PowerUp.Type type) {
        if (sizeTimer != null && sizeTimer.isRunning()) {
            sizeTimer.stop();
            resetSize();
        }
        if (type == PowerUp.Type.PADDLE_EXPAND) width *= 1.4;
        else if (type == PowerUp.Type.PADDLE_SHRINK) width /= 1.2;

        sizeTimer = new javax.swing.Timer(10000, e -> { resetSize(); sizeTimer.stop(); });
        sizeTimer.setRepeats(false);
        sizeTimer.start();
    }
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public Paddle(int x, int y) {
        this.x = x; this.y = y;
    }

    public void resetSize() {
        width = normalWidth;
    }
    public void moveLeft() {
        if (x > 0) x -= 20;
    }

    public void moveRight(int frameWidth) {
        if (x + width < frameWidth) x += 20;
    }

    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(x, y, width, height);
    }

    public boolean isHit(int ballX, int ballY, int ballSize) {
        return ballX + ballSize > x && ballX < x + width &&
                ballY + ballSize > y && ballY < y + height;
    }
}

/**
 * Đại diện cho thanh paddle do người chơi điều khiển.
 *
 * Lưu vị trí và kích thước (x, y, width, height).
 *
 * Di chuyển trái/phải bằng moveLeft() và moveRight(...).
 *
 * Kiểm tra va chạm với bóng bằng isHit(...).
 *
 * Vẽ paddle lên màn hình bằng draw(Graphics g).
 */
class InstructionsPanel extends JPanel {
    private final StyledButton backButton = new StyledButton("Quay lại");

    public InstructionsPanel() {
        setLayout(new BorderLayout());

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setOpaque(false);
        area.setForeground(Color.WHITE);
        area.setFont(area.getFont().deriveFont(Font.PLAIN, 16f));

        area.setText(
                "HƯỚNG DẪN CHƠI\n\n" +
                        "- Phím MŨI TÊN TRÁI/PHẢI: di chuyển paddle.\n" +
                        "- Phím P: tạm dừng/tiếp tục.\n" +
                        "- Nhiệm vụ: đỡ bóng và phá hết các viên gạch.\n\n" +
                        "Bấm 'Quay lại' để trở về Menu."
        );

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(12,12,12,12);
        content.add(area, gbc);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        backButton.setCornerRadius(18);
        bottom.add(backButton);

        add(content, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 46), 0, getHeight(), new Color(15, 15, 23));
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    public JButton getBackButton() { return backButton; }
    public StyledButton getBackStyledButton() { return backButton; }
}
