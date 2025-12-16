package edu.grinnell.csc207.compression;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;


/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits.  However, we also need to encode a special EOF character to
 * denote the end of a .grin file.  Thus, we need 9 bits to store each
 * byte value.  This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */

public class HuffmanTree {

    private static class Node {
        short value; 
        int freq;
        Node left;
        Node right;

        // Leaf node
        Node(short value, int freq) {
            this.value = value;
            this.freq = freq;
            this.left = null;
            this.right = null;
        }

        // Internal node
        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.freq = left.freq + right.freq;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }
    }

    private Node root;

    private static final short EOF_value = (short) 256;

    
    /**
     * Constructs a new HuffmanTree from a frequency map.
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree(Map<Short, Integer> freqs) {

        freqs.put(EOF_value, 1);

        PriorityQueue<Node> pq = new PriorityQueue<Node>(
            new Comparator<Node>() {
                @Override
                public int compare(Node n1, Node n2) {
                    return Integer.compare(n1.freq, n2.freq);
                }
            }
        );

        for (Map.Entry<Short, Integer> entry : freqs.entrySet()) {
            short value = entry.getKey();
            int freq = entry.getValue();
            pq.add(new Node(value, freq));
        }

        if (pq.isEmpty()) {
            this.root = null;
            return;
        }

        while (pq.size() > 1) {
            Node n1 = pq.poll();
            Node n2 = pq.poll();
            Node parent = new Node(n1, n2);
            pq.add(parent);
        }

        this.root = pq.poll();
    }

    /**
     * Helper: read a serialized Huffman tree in preorder from the bit stream.
     * @param in the input bit stream
     * @return the root of the reconstructed tree
     */
    private Node readTree(BitInputStream in) {
        int flag = in.readBit();
        if (flag == -1) {
            throw new IllegalStateException();
        }

        if (flag == 0) {
            int val = in.readBits(9);
            if (val == -1) {
                throw new IllegalStateException();
            }
            return new Node((short) val, 0);
        } else {
            Node left = readTree(in);
            Node right = readTree(in);
            return new Node(left, right);
        }
    }



    /**
     * Constructs a new HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree(BitInputStream in) {
        this.root = readTree(in);
    }



    /**
     * Helper: write the tree to the output stream in preorder.
     * @param node the current node
     * @param out the output bit stream
     */
    private void writeTree(Node node, BitOutputStream out) {
        if (node.isLeaf()) {
            out.writeBit(0);
            out.writeBits(node.value, 9);
        } else {
            out.writeBit(1);
            writeTree(node.left, out);
            writeTree(node.right, out);
        }
    }






    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     */
    public void serialize(BitOutputStream out) {
        writeTree(this.root, out);
    }
   


    /**
     * Build a map from value (0–256) to its Huffman code as a string of '0'/'1'.
     * @return the map from value to code
     */
    private Map<Short, String> buildCodeMap() {
        Map<Short, String> codes = new HashMap<>();
        buildCodesRecursive(this.root, "", codes);
        return codes;
    }

    /** 
     * Recursive DFS that assigns codes.
     * @param node current node
     * @param path current path
     * @param map map from value to code
    */
    private void buildCodesRecursive(Node node, String path, Map<Short, String> map) {
        if (node.isLeaf()) {
            map.put(node.value, path);
            return;
        }
        // go left = 0
        buildCodesRecursive(node.left, path + "0", map);
        // go right = 1
        buildCodesRecursive(node.right, path + "1", map);
    }





    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * @param in the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode(BitInputStream in, BitOutputStream out) {
        Map<Short, String> codeMap = buildCodeMap();

        int b;
        while ((b = in.readBits(8)) != -1) {
            short value = (short) b;
            String code = codeMap.get(value);
            for (int i = 0; i < code.length(); i++) {
                out.writeBit(code.charAt(i) == '1' ? 1 : 0);
            }
        }

 
        String eofCode = codeMap.get(EOF_value);

        for (int i = 0; i < eofCode.length(); i++) {
            out.writeBit(eofCode.charAt(i) == '1' ? 1 : 0);
        }

    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode(BitInputStream in, BitOutputStream out) {
        if (root == null) {
            return;
        }

        Node current = root;

        while (in.hasBits()) {
            int bit = in.readBit();
            if (bit == -1) {
                return;
            }

            current = (bit == 0) ? current.left : current.right;

            if (current.isLeaf()) {
                if (current.value == EOF_value) {
                    return;
                }

                out.writeBits(current.value & 0xFF, 8);
                current = root;
            }
        }
    }
}
