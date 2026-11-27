package com.ktg.mes.ai.controller;

import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MesKnowledgeTextExtractorTest {

    private final MesKnowledgeTextExtractor extractor = new MesKnowledgeTextExtractor();

    @Test
    public void indexesBothMarkdownExtensionsWithParagraphLocations() throws Exception {
        byte[] content = "\uFEFF# 来料检验\r\n\r\n每批抽检 5 件。".getBytes(StandardCharsets.UTF_8);

        for (String filename : new String[]{"规范.md", "规范.MARKDOWN"}) {
            List<MesKnowledgeTextExtractor.Section> sections = extractor.extract(filename, content);
            assertEquals(2, sections.size());
            assertEquals("# 来料检验", sections.get(0).getText());
            assertEquals("每批抽检 5 件。", sections.get(1).getText());
            assertEquals("正文段落 2", sections.get(1).getLocation());
        }
    }

    @Test
    public void csvKeepsQuotedCommasAndMultilineCellsInTheSameRecord() throws Exception {
        byte[] content = ("\uFEFF物料,说明\r\n"
                + "测温枪,\"抽检 5 件,并记录\"\"异常\"\"\r\n复核尺寸\"\r\n"
                + "#备用批次,抽检 8 件\r\n").getBytes(StandardCharsets.UTF_8);

        List<MesKnowledgeTextExtractor.Section> sections = extractor.extract("抽检.csv", content);

        assertEquals(3, sections.size());
        assertEquals("CSV 记录 2", sections.get(1).getLocation());
        assertEquals("测温枪 | 抽检 5 件,并记录\"异常\"\n复核尺寸",
                sections.get(1).getText().replace("\r\n", "\n"));
        assertEquals("CSV 记录 3", sections.get(2).getLocation());
        assertEquals("#备用批次 | 抽检 8 件", sections.get(2).getText());
    }

    @Test
    public void csvSupportsLegacyChineseEncodingAndRejectsEmptyRecords() throws Exception {
        String row = "测温枪,抽检 5 件";
        for (Charset encoding : new Charset[]{Charset.forName("GB18030"), StandardCharsets.UTF_16}) {
            List<MesKnowledgeTextExtractor.Section> sections =
                    extractor.extract("抽检.csv", row.getBytes(encoding));
            assertEquals("测温枪 | 抽检 5 件", sections.get(0).getText());
        }

        byte[] empty = ",,\r\n\" \",".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> extractor.extract("空.csv", empty));
    }
}
