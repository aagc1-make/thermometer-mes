package com.ktg.mes.ai.service;

import com.ktg.mes.ai.domain.KbCitation;
import com.ktg.mes.ai.domain.KbDocument;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ktg.mes.ai.service.KbRagEngines.object;

public final class KbGraphExtraction {
    private final Alias aliases = new Alias();

    KbGraphExtraction(List<Entity> anchors) {
        for (Entity entity : anchors) {
            if (entity.code == null || entity.code.trim().isEmpty()) continue;
            if (entity.name == null || entity.name.trim().isEmpty()) entity.name = entity.code;
            if (!entity.is("Dept")) add(entity.code, entity);
            add(entity.name, entity);
        }
    }

    List<Map<String, Object>> extract(KbDocument.Version version, List<KbCitation> chunks) {
        Entity document = new Entity();
        document.setType("Document");
        document.setCode(version.getDocId().toString());
        document.setName(version.getTitle());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (KbCitation chunk : chunks) {
            Map<String, Map<String, Object>> links = new LinkedHashMap<>();
            for (String sentence : chunk.getContent().split("[。；！？;!?\\n]+|\\.(?=\\s|$)")) {
                List<Match> matches = find(sentence);
                for (int i = 0; i < matches.size(); i++) {
                    Match next = matches.get(i);
                    link(links, document, next.entity, "MENTIONS", chunk.getChunkId());
                    if (i == 0) continue;
                    Match prior = matches.get(i - 1);
                    String between = sentence.substring(prior.end, next.start).toUpperCase(Locale.ROOT);
                    relate(links, prior.entity, next.entity, between, chunk.getChunkId());
                }
            }
            rows.addAll(links.values());
        }
        return rows;
    }

    private void add(String value, Entity entity) {
        Alias cursor = aliases;
        for (char c : value.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            cursor = cursor.children.computeIfAbsent(c, key -> new Alias());
        }
        cursor.entities.putIfAbsent(entity.key(), entity);
    }

    private List<Match> find(String sentence) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        if (lower.length() != sentence.length()) return new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        for (int start = 0; start < lower.length(); start++) {
            Alias cursor = aliases;
            Match longest = null;
            for (int i = start; i < lower.length(); i++) {
                cursor = cursor.children.get(lower.charAt(i));
                if (cursor == null) break;
                if (cursor.entities.size() == 1 && boundary(lower, start, i)) {
                    longest = new Match(cursor.entities.values().iterator().next(), start, i + 1);
                }
            }
            if (longest != null) {
                int last = matches.size() - 1;
                if (last >= 0 && matches.get(last).entity.key().equals(longest.entity.key())) {
                    Match previous = matches.get(last);
                    matches.set(last, new Match(longest.entity, previous.start, longest.end));
                } else {
                    matches.add(longest);
                }
                start = longest.end - 1;
            }
        }
        return matches;
    }

    private static boolean boundary(String value, int start, int end) {
        return (!asciiWord(value.charAt(start)) || start == 0 || !asciiWord(value.charAt(start - 1)))
            && (!asciiWord(value.charAt(end)) || end == value.length() - 1 || !asciiWord(value.charAt(end + 1)));
    }

    private static boolean asciiWord(char c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '-' || c == '_';
    }

    private static void relate(Map<String, Map<String, Object>> links, Entity first, Entity second,
                               String between, long chunkId) {
        if (between.length() > 40 || between.matches("(?s).*(?:不(?:使用|适用|需要|用于|包含)|无需|禁止|除外|未使用|[,，、]).*")) return;
        if (first.is("Item") && second.is("Product", "Workorder")
            && between.matches(".*(?:USED_IN|用于|应用于|装入|装配到|供给|用在).*")) {
            link(links, first, second, "USED_IN", chunkId);
        } else if (first.is("Product", "Workorder") && second.is("Item")
            && between.matches(".*(?:使用|采用|需要|由|包含).*")) {
            link(links, second, first, "USED_IN", chunkId);
        } else if (first.is("Product") && second.is("Process")
            && between.matches(".*(?:HAS_PROCESS|工序|经过|进行|流程包含).*")) {
            link(links, first, second, "HAS_PROCESS", chunkId);
        } else if (first.is("Process", "Product") && second.is("InspectionItem")
            && between.matches(".*(?:CHECKED_BY|检验|检测|检查|测量).*")) {
            link(links, first, second, "CHECKED_BY", chunkId);
        } else if (first.is("Process") && second.is("Equipment")
            && between.matches(".*(?:REQUIRES|需要|使用|采用|借助).*")) {
            link(links, first, second, "REQUIRES", chunkId);
        } else if (first.is("Defect") && second.is("Item", "Process")
            && between.matches(".*(?:MAY_CAUSE|可能是由|可能源于|可能涉及|可能由|可能与).*")) {
            link(links, first, second, "MAY_CAUSE", chunkId);
        }
    }

    private static void link(Map<String, Map<String, Object>> links, Entity from, Entity to, String kind, long chunkId) {
        links.putIfAbsent(from.key() + "|" + kind + "|" + to.key(),
            object("from", from.data(), "to", to.data(), "kind", kind, "chunkId", chunkId));
    }

    @Data
    public static class Entity {
        private String code;
        private String name;
        private String type;

        private String key() { return type + ":" + code; }
        private boolean is(String... types) {
            for (String candidate : types) if (candidate.equals(type)) return true;
            return false;
        }
        private Map<String, Object> data() {
            return object("id", key(), "name", name, "type", type, "code", "Document".equals(type) ? null : code);
        }
    }

    private static class Alias {
        final Map<Character, Alias> children = new LinkedHashMap<>();
        final Map<String, Entity> entities = new LinkedHashMap<>();
    }

    private static class Match {
        final Entity entity;
        final int start;
        final int end;

        Match(Entity entity, int start, int end) {
            this.entity = entity;
            this.start = start;
            this.end = end;
        }
    }
}
