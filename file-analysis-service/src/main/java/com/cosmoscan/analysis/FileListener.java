package com.cosmoscan.analysis;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.CircleBackground;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.palette.ColorPalette;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class FileListener {

    private final ReportRepository reportRepository;

    public FileListener(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @RabbitListener(queues = "file.analysis.queue")
    public void processFile(String payload) {
        String[] parts = payload.split(";");
        Long workId = Long.parseLong(parts[0]);
        String filePath = parts[1];

        File file = new File(filePath);
        Report report = new Report();
        report.setWorkId(workId);
        report.setCheckedAt(LocalDateTime.now());

        List<String> issues = new ArrayList<>();

        if (!file.exists()) {
            issues.add("Файл не найден на сервере");
        } else {
            // Проверка размера (<= 1 МБ)
            long sizeInMb = file.length() / (1024 * 1024);
            if (sizeInMb >= 1) {
                issues.add("Размер файла больше 1 МБ");
            }

            // Проверка формата
            String nameLower = filePath.toLowerCase();
            if (nameLower.endsWith(".zip")) {
                issues.add("ZIP архивы запрещены");
            } else if (!nameLower.matches(".*\\.(pdf|docx|txt)$")) {
                issues.add("Недопустимый формат (разрешены pdf, docx, txt)");
            }
        }

        if (issues.isEmpty()) {
            report.setStatus("ПРИНЯТО");
            report.setIssues("Замечаний нет");
            try {
                String cloudPath = generateWordCloud(file);
                report.setWordCloudUrl(cloudPath);
            } catch (Exception e) {
                report.setWordCloudUrl("Ошибка генерации облака слов: " + e.getMessage());
            }
        } else {
            report.setStatus("ТРЕБУЕТСЯ ДОРАБОТКА");
            report.setIssues(String.join(", ", issues));
        }

        reportRepository.save(report);
        System.out.println("Отчет для работы " + workId + " сохранен!");
    }

    private String generateWordCloud(File file) throws IOException {
        String text = extractText(file);

        FrequencyAnalyzer analyzer = new FrequencyAnalyzer();
        analyzer.setWordFrequenciesToReturn(200);
        analyzer.setMinWordLength(3);

        // Анализируем извлеченный текст
        List<WordFrequency> wordFrequencies = analyzer.load(Collections.singletonList(text));

        Dimension dimension = new Dimension(400, 400);
        WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
        wordCloud.setPadding(2);
        wordCloud.setBackground(new CircleBackground(200));
        wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1)));
        wordCloud.setFontScalar(new SqrtFontScalar(10, 40));

        wordCloud.build(wordFrequencies);

        String outPath = file.getParent() + "/cloud_" + System.currentTimeMillis() + ".png";
        wordCloud.writeToFile(outPath);
        return outPath;
    }

    private String extractText(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".txt")) {
            return Files.readString(file.toPath());
        } else if (fileName.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } else if (fileName.endsWith(".docx")) {
            try (FileInputStream fis = new FileInputStream(file);
                 XWPFDocument document = new XWPFDocument(fis);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        }
        return "";
    }
}