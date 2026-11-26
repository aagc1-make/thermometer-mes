package com.ktg.mes.ai.controller;

import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class MesKnowledgeTextExtractor {

    static final String SUPPORTED_EXTENSIONS = "txt|md|markdown|csv|pdf|docx";

    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PDF_PAGES = 200;
    private static final int MAX_TEXT_LENGTH = 300000;

    public List<Section> extract(String filename, byte[] bytes) throws IOException {
        if (filename == null || bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("文件为空");
        }
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("文件不能超过 5 MiB");
        }

        String extension = supportedExtension(filename);
        List<Section> sections;

        switch (extension) {
            case "pdf":
                sections = extractPdf(bytes);
                break;
            case "docx":
                sections = extractDocx(bytes);
                break;
            case "csv":
                sections = extractCsv(readText(bytes, "CSV"));
                break;
            default:
                sections = extractParagraphs(readText(bytes, "txt".equals(extension) ? "TXT" : "Markdown"));
        }

        if (sections.isEmpty()) {
            if ("pdf".equals(extension)) {
                throw new IllegalArgumentException(
                        "该 PDF 未提取到文字，请上传文本型 PDF");
            }
            throw new IllegalArgumentException("文档未提取到有效正文");
        }

        return sections;
    }

    static String supportedExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        String extension = dot > 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!extension.matches(SUPPORTED_EXTENSIONS)) {
            throw new IllegalArgumentException("仅支持 TXT、Markdown、CSV、文本型 PDF 和 DOCX 文件");
        }
        return extension;
    }

    private String readText(byte[] bytes, String format) {
        String text;
        try {
            text = "CSV".equals(format)
                    ? decodeCsv(bytes)
                    : decode(bytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException(format + " 编码无效，请另存为 UTF-8", e);
        }

        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        if (text.codePoints().anyMatch(c ->
                Character.isISOControl(c)
                        && c != '\t' && c != '\r' && c != '\n')) {
            throw new IllegalArgumentException(format + " 包含非文本字符");
        }
        return text;
    }

    private String decodeCsv(byte[] bytes) throws CharacterCodingException {
        if (bytes.length >= 2
                && ((bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xfe)
                || (bytes[0] == (byte) 0xfe && bytes[1] == (byte) 0xff))) {
            return decode(bytes, StandardCharsets.UTF_16);
        }
        try {
            return decode(bytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            return decode(bytes, Charset.forName("GB18030"));
        }
    }

    private String decode(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }

    private List<Section> extractParagraphs(String text) {
        List<Section> sections = new ArrayList<>();
        int total = 0;
        String[] paragraphs = text.split("\\R\\s*\\R");
        for (int i = 0; i < paragraphs.length; i++) {
            total = addSection(sections, paragraphs[i],
                    "正文段落 " + (i + 1), total);
        }
        return sections;
    }

    private List<Section> extractCsv(String text) throws IOException {
        List<Section> sections = new ArrayList<>();
        CsvReadConfig config = CsvReadConfig.defaultConfig().disableComment().setSkipEmptyRows(false);
        int total = 0;
        int recordNo = 0;

        try (CsvReader reader = new CsvReader(new StringReader(text), config)) {
            for (CsvRow row : reader) {
                recordNo++;
                if (row.stream().allMatch(this::isBlank)) {
                    continue;
                }
                total = addSection(sections, String.join(" | ", row),
                        "CSV 记录 " + recordNo, total);
            }
        }
        return sections;
    }

    private List<Section> extractPdf(byte[] bytes) throws IOException {
        List<Section> sections = new ArrayList<>();
        int total = 0;

        try (PDDocument document = PDDocument.load(bytes)) {
            if (document.getNumberOfPages() > MAX_PDF_PAGES) {
                throw new IllegalArgumentException("PDF 不能超过 200 页");
            }
            if (!document.getCurrentAccessPermission().canExtractContent()) {
                throw new IllegalArgumentException("该 PDF 不允许提取正文");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                total = addSection(sections, stripper.getText(document),
                        "第 " + page + " 页", total);
            }
        }
        return sections;
    }

    private List<Section> extractDocx(byte[] bytes) throws IOException {
        List<Section> sections = new ArrayList<>();
        int total = 0;
        int paragraphNo = 0;
        int tableNo = 0;

        try (XWPFDocument document =
                     new XWPFDocument(new ByteArrayInputStream(bytes))) {
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    paragraphNo++;
                    total = addSection(sections,
                            ((XWPFParagraph) element).getText(),
                            "正文段落 " + paragraphNo, total);
                } else if (element instanceof XWPFTable) {
                    tableNo++;
                    int rowNo = 0;

                    for (XWPFTableRow row : ((XWPFTable) element).getRows()) {
                        rowNo++;
                        List<String> cells = new ArrayList<>();
                        boolean hasText = false;

                        for (XWPFTableCell cell : row.getTableCells()) {
                            String text = cell.getText().trim();
                            cells.add(text);
                            if (!isBlank(text)) {
                                hasText = true;
                            }
                        }

                        if (hasText) {
                            total = addSection(sections,
                                    String.join(" | ", cells),
                                    "表格 " + tableNo + "，第 " + rowNo + " 行",
                                    total);
                        }
                    }
                }
            }
        }
        return sections;
    }

    private int addSection(List<Section> sections, String text,
                           String location, int total) {
        String content = text.trim();
        if (isBlank(content)) {
            return total;
        }
        if (content.length() > MAX_TEXT_LENGTH - total) {
            throw new IllegalArgumentException("提取后的正文不能超过 30 万字符");
        }
        sections.add(new Section(content, location));
        return total + content.length();
    }

    private boolean isBlank(String text) {
        return text.codePoints().allMatch(c ->
                Character.isWhitespace(c) || Character.isSpaceChar(c));
    }

    public static class Section {

        private final String text;
        private final String location;

        public Section(String text, String location) {
            this.text = text;
            this.location = location;
        }

        public String getText() {
            return text;
        }

        public String getLocation() {
            return location;
        }
    }
}
