import javax.swing.*;
//nếu muốn chơi nhiều người thì chạy file này
public class MultiplayerGame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Arkanoid - Multiplayer");
        MultiplayerPanel panel = new MultiplayerPanel();
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
