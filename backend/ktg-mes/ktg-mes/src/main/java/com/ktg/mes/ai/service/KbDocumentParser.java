package com.ktg.mes.ai.service;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.ai.domain.KbDocument;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Component
public class KbDocumentParser {
    static final int CHARACTER_LIMIT = 20_000_000;
    private static final int CHUNK_LENGTH = 1200;

    Parsed parse(Path file, String extension) throws IOException {
        Parsed result = new Parsed();
        switch (extension) {
            case "txt":
            case "md":
            case "csv":
                result = readText(file);
                break;
            case "html":
                Document html = Jsoup.parse(file.toFile(), null);
                html.select("script, style, noscript, template").remove();
                result.add(html.body().wholeText(), null, "正文");
                break;
            case "docx":
                try (InputStream stream = Files.newInputStream(file);
                     XWPFDocument document = new XWPFDocument(stream);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    result.add(extractor.getText(), null, "正文");
                }
                break;
            case "xlsx":
                readWorkbook(file, result);
                break;
            case "pdf":
                readPdf(file, result);
                break;
            default:
                throw new ServiceException("不支持此文档格式");
        }
        if (result.chunks.isEmpty()) {
            throw new ServiceException("pdf".equals(extension)
                ? "PDF 中未找到可提取文字。扫描件需要先完成文字识别。"
                : "文件中没有可提取的正文。");
        }
        return result;
    }

    private Parsed readText(Path file) throws IOException {
        int first;
        int second;
        try (InputStream stream = Files.newInputStream(file)) {
            first = stream.read();
            second = stream.read();
        }
        if ((first == 0xff && second == 0xfe) || (first == 0xfe && second == 0xff)) {
            return readLines(file, StandardCharsets.UTF_16);
        }
        try {
            return readLines(file, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            return readLines(file, Charset.forName("GB18030"));
        }
    }

    private Parsed readLines(Path file, Charset charset) throws IOException {
        Parsed result = new Parsed();
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            StringBuilder group = new StringBuilder();
            int firstLine = 1;
            int lineNo = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (group.length() > 0 && group.length() + line.length() > CHUNK_LENGTH) {
                    result.add(group.toString(), null, "第 " + firstLine + " 行起");
                    group.setLength(0);
                    firstLine = lineNo;
                }
                group.append(line).append('\n');
            }
            result.add(group.toString(), null, "第 " + firstLine + " 行起");
        }
        return result;
    }

    private void readPdf(Path file, Parsed result) throws IOException {
        try (PDDocument pdf = PDDocument.load(file.toFile(), MemoryUsageSetting.setupTempFileOnly())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                result.add(stripper.getText(pdf), page, "第 " + page + " 页");
            }
        }
    }

    private void readWorkbook(Path file, Parsed result) throws IOException {
        try (InputStream stream = Files.newInputStream(file); XSSFWorkbook workbook = new XSSFWorkbook(stream)) {
            DataFormatter formatter = new DataFormatter();
            for (Sheet sheet : workbook) {
                StringBuilder group = new StringBuilder();
                int startRow = 0;
                for (Row row : sheet) {
                    StringJoiner cells = new StringJoiner("\t");
                    for (Cell cell : row) {
                        cells.add(formatter.formatCellValue(cell));
                    }
                    if (group.length() == 0) {
                        startRow = row.getRowNum() + 1;
                    }
                    group.append(cells).append('\n');
                    if (group.length() >= CHUNK_LENGTH) {
                        result.add(group.toString(), null, sheet.getSheetName() + " · 第 " + startRow + " 行起");
                        group.setLength(0);
                    }
                }
                result.add(group.toString(), null, sheet.getSheetName() + " · 第 " + startRow + " 行起");
            }
        }
    }

    static class Parsed {
        final List<KbDocument.Chunk> chunks = new ArrayList<>();
        int characters;

        void add(String text, Integer page, String label) {
            String clean = text.replace("\r\n", "\n").replace('\r', '\n').replace("\u0000", "").trim();
            for (int from = 0; from < clean.length();) {
                int end = Math.min(from + CHUNK_LENGTH, clean.length());
                if (end < clean.length() && Character.isHighSurrogate(clean.charAt(end - 1))) {
                    end--;
                }
                String content = clean.substring(from, end).trim();
                from = end;
                if (!content.isEmpty()) {
                    characters += content.length();
                    if (characters > CHARACTER_LIMIT) {
                        throw new ServiceException("文档正文超过 2000 万字符。");
                    }
                    chunks.add(new KbDocument.Chunk(chunks.size(), page, label, content));
                }
            }
        }
    }
}
