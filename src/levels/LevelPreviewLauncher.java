package levels;


public final class LevelPreviewLauncher {
    private LevelPreviewLauncher() {
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            try {
                int lvl = Integer.parseInt(args[0]);
                LevelPreview.showMap(lvl);
                return;
            } catch (NumberFormatException ex) {
                System.err.println("Invalid level argument: " + args[0] + " — falling back to interactive prompt.");
            }
        }

        LevelPreview.showMapInteractive();
    }
}
