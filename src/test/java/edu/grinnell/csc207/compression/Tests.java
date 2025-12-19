package edu.grinnell.csc207.compression;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * Basic unit tests for the Grin compression program.
 */
public class Tests {

    @Test
    public void roundTripSmallFile() throws Exception {
        Files.write(Path.of("in.txt"), "abc a".getBytes());

        Grin.encode("in.txt", "out.grin");
        Grin.decode("out.grin", "out.txt");

        assertArrayEquals(
            Files.readAllBytes(Path.of("in.txt")),
            Files.readAllBytes(Path.of("out.txt"))
        );
    }



    @Test
    public void singleCharacterFile() throws Exception {
        Files.write(Path.of("single.txt"), "aaaaaa".getBytes());

        Grin.encode("single.txt", "single.grin");
        Grin.decode("single.grin", "single.out");

        assertArrayEquals(
            Files.readAllBytes(Path.of("single.txt")),
            Files.readAllBytes(Path.of("single.out"))
        );
    }

    @Test
    public void allByteValues() throws Exception {
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }

        Files.write(Path.of("bytes.bin"), data);

        Grin.encode("bytes.bin", "bytes.grin");
        Grin.decode("bytes.grin", "bytes.out");

        assertArrayEquals(
            Files.readAllBytes(Path.of("bytes.bin")),
            Files.readAllBytes(Path.of("bytes.out"))
        );
    }

    @Test
    public void invalidMagicNumberThrows() throws Exception {
        Files.write(Path.of("notgrin.txt"), "hello".getBytes());

        assertThrows(
            IllegalArgumentException.class,
            () -> Grin.decode("notgrin.txt", "out.txt")
        );
    }
}
