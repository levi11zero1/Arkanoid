import javax.swing.*;

public class ArkanoidGame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Arkanoid Basic");
        GamePanel panel = new GamePanel();
        frame.add(panel);
        frame.setSize(400, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
