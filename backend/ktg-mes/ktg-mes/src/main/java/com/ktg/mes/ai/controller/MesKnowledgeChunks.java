package com.ktg.mes.ai.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MesKnowledgeChunks {

    private MesKnowledgeChunks() {
    }

    public static List<Chunk> split(List<MesKnowledgeTextExtractor.Section> sections) {
        List<Chunk> chunks = new ArrayList<>();

        for (MesKnowledgeTextExtractor.Section section : sections) {
            int[] characters = section.getText().codePoints().toArray();

            for (int offset = 0; offset < characters.length; offset += 500) {
                int length = Math.min(500, characters.length - offset);
                chunks.add(new Chunk(
                        chunks.size() + 1,
                        new String(characters, offset, length),
                        section.getLocation()));
            }
        }

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("文档没有可检索的正文");
        }

        return Collections.unmodifiableList(chunks);
    }

    public static final class Chunk {
        private final int chunkNo;
        private final String text;
        private final String location;

        private Chunk(int chunkNo, String text, String location) {
            this.chunkNo = chunkNo;
            this.text = text;
            this.location = location;
        }

        public int getChunkNo() {
            return chunkNo;
        }

        public String getText() {
            return text;
        }

        public String getLocation() {
            return location;
        }
    }
}