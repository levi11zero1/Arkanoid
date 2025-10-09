import javax.swing.*;

public class ArkanoidGame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Arkanoid Basic");
        GamePanel panel = new GamePanel();
        frame.add(panel);
        
        frame.setSize(450, 650);
        
        frame.setResizable(false);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
