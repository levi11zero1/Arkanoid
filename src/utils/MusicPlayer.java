package utils;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;

/**
 * MusicPlayer chỉ hỗ trợ .wav sử dụng Java Sound API (javax.sound.sampled).
 * API đơn giản: playLoop, playOnce, stop.
 */
public class MusicPlayer {
    // Nhạc nền (nếu có) 
    private static Clip currentClip = null;

    /**
     * Play WAV file in loop (background). Stops previous background if any.
     */
    public static void init() {
        // Tất cả các file đã là WAV
    }

    public static void playLoop(String filePath) {
        if (filePath == null) return;
        try { stop(); } catch (Throwable ignored) {}

        try {
            File f = new File(filePath);
            AudioInputStream sourceStream = AudioSystem.getAudioInputStream(f);
            AudioInputStream playbackStream = convertToPlayableStream(sourceStream);
            AudioFormat playFormat = playbackStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, playFormat);
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(playbackStream);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            currentClip = clip;
        } catch (Throwable t) {
            System.err.println("MusicPlayer: cannot play loop via Java Sound: " + t.getMessage());
        }
    }

    /**
     * Chơi một lần.
     * Không dừng nhạc nền (nếu có).
     */
    public static void playOnce(String filePath, Runnable onComplete) {
        if (filePath == null) return;
        try {
            File f = new File(filePath);
            AudioInputStream sourceStream = AudioSystem.getAudioInputStream(f);
            AudioInputStream playbackStream = convertToPlayableStream(sourceStream);
            AudioFormat playFormat = playbackStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, playFormat);
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(playbackStream);
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP || ev.getType() == LineEvent.Type.CLOSE) {
                    try { clip.close(); } catch (Throwable ignored) {}
                    if (onComplete != null) try { onComplete.run(); } catch (Throwable ignored) {}
                }
            });
            clip.start();
        } catch (Throwable t) {
            System.err.println("MusicPlayer: cannot play once via Java Sound: " + t.getMessage());
        }
    }

    /**
     * Dừng nhạc nền (nếu có).
     */
    public static void stop() {
        if (currentClip == null) return;
        try {
            currentClip.stop();
            currentClip.close();
        } catch (Throwable ignored) {
        } finally {
            currentClip = null;
        }
    }

    private static AudioInputStream convertToPlayableStream(AudioInputStream source)
            throws java.io.IOException, javax.sound.sampled.UnsupportedAudioFileException {
        AudioFormat baseFormat = source.getFormat();
        if (baseFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
            return source;
        }

        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false // little-endian
        );

        if (!AudioSystem.isConversionSupported(targetFormat, baseFormat)) {
            return source;
        }

        return AudioSystem.getAudioInputStream(targetFormat, source);
    }
}

