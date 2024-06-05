package com.example.filecompressor;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class HuffmanCompressor {

    static class HuffmanNode {
        char character;
        int frequency;
        HuffmanNode left, right;

        HuffmanNode(char character, int frequency) {
            this.character = character;
            this.frequency = frequency;
        }
    }

    public static Map<Character, Integer> buildFrequencyTable(byte[] fileBytes) {
        Map<Character, Integer> frequency = new HashMap<>();
        for (byte b : fileBytes) {
            char character = (char) (b & 0xFF);
            frequency.put(character, frequency.getOrDefault(character, 0) + 1);
        }
        return frequency;
    }

    public static HuffmanNode buildHuffmanTree(Map<Character, Integer> frequency) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>((l, r) -> l.frequency - r.frequency);

        for (Map.Entry<Character, Integer> entry : frequency.entrySet()) {
            pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            HuffmanNode merged = new HuffmanNode('\0', left.frequency + right.frequency);
            merged.left = left;
            merged.right = right;
            pq.add(merged);
        }

        return pq.poll();
    }

    public static void generateHuffmanCodes(HuffmanNode node, String prefix, Map<Character, String> codebook) {
        if (node == null) return;

        if (node.left == null && node.right == null) {
            codebook.put(node.character, prefix);
        }

        generateHuffmanCodes(node.left, prefix + "0", codebook);
        generateHuffmanCodes(node.right, prefix + "1", codebook);
    }

    public static String encodeFile(byte[] fileBytes, Map<Character, String> codebook) {
        StringBuilder encodedText = new StringBuilder();
        for (byte b : fileBytes) {
            char character = (char) (b & 0xFF);
            encodedText.append(codebook.get(character));
        }
        return encodedText.toString();
    }

    public static byte[] decodeFile(String encodedText, HuffmanNode huffmanTree) {
        ByteArrayOutputStream decodedBytes = new ByteArrayOutputStream();
        HuffmanNode node = huffmanTree;
        for (char bit : encodedText.toCharArray()) {
            node = (bit == '0') ? node.left : node.right;
            if (node.left == null && node.right == null) {
                decodedBytes.write((byte) node.character);
                node = huffmanTree;
            }
        }
        return decodedBytes.toByteArray();
    }

    public static void saveEncodedFile(String encodedText, String outputPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            int i = 0;
            while (i < encodedText.length()) {
                int endIndex = Math.min(i + 8, encodedText.length());
                String byteString = encodedText.substring(i, endIndex);
                int byteValue = Integer.parseInt(byteString, 2);
                fos.write(byteValue);
                i += 8;
            }
        }
    }

    public static String readEncodedFile(String filePath) throws IOException {
        StringBuilder bits = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            int byteValue;
            while ((byteValue = fis.read()) != -1) {
                bits.append(String.format("%8s", Integer.toBinaryString(byteValue & 0xFF)).replace(' ', '0'));
            }
        }
        return bits.toString();
    }

    public static byte[] readFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    public static void writeFile(byte[] data, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
        }
    }
}