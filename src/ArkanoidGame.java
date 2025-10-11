import game.GameFrame;

public class ArkanoidGame {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            new GameFrame().setVisible(true);
        });
    }
}