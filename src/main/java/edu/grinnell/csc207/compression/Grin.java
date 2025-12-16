package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {
    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to decode
     * @param outfile the file to ouptut to
     */
    public static void decode(String infile, String outfile) throws IOException {
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);


        int magic = in.readBits(32);
        if (magic != 0x736) {
            in.close();
            out.close();
            throw new IllegalArgumentException("Input file is not a valid .grin file.");
        }

        HuffmanTree tree = new HuffmanTree(in);

        tree.decode(in, out);


        in.close();
        out.close();
    }


    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * @param file the file to read
     * @return a freqency map for the given file
     */
    public static Map<Short, Integer> createFrequencyMap(String file) throws IOException {
        BitInputStream in = new BitInputStream(file);
        Map<Short, Integer> freqs = new HashMap<>();

        int bits;
        while ((bits = in.readBits(8)) != -1) {
            short value = (short) bits;
            Integer old = freqs.get(value);

            if (old == null) {
                freqs.put(value, 1);
            } else {
                freqs.put(value, old + 1);
            }
        }

        in.close();
        return freqs;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to encode.
     * @param outfile the file to write the output to.
     */
    public static void encode(String infile, String outfile) throws IOException {
         // 1) Build frequency map from the raw input file (8-bit chunks)
        Map<Short, Integer> freqs = createFrequencyMap(infile);

        // 2) Build Huffman tree from freqs (constructor adds EOF)
        HuffmanTree tree = new HuffmanTree(freqs);

        // 3) Open streams
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);

        // 4) Write header: magic number + serialized tree
        out.writeBits(0x736, 32);
        tree.serialize(out);

        // 5) Write payload: encoded input + EOF code
        tree.encode(in, out);

        // 6) Close streams
        in.close();
        out.close();
    }

    /**
     * The entry point to the program.
     * @param args the command-line arguments.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
            return;
        }
        
        String mode = args[0];
        String infile = args[1];
        String outfile = args[2];

        if (mode.equals("decode")) {
            decode(infile, outfile);
        } else if (mode.equals("encode")) {
            encode(infile, outfile);
        } else {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
        }
    }
}
