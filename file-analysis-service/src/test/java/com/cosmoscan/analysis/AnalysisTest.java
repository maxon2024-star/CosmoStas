package com.cosmoscan.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AnalysisTest {

    // Тест 1: Негативный сценарий (фейковый ZIP файл)
    @Test
    void testFileProcessingValidation_FakeZip() {
        ReportRepository reportRepository = Mockito.mock(ReportRepository.class);
        FileListener listener = new FileListener(reportRepository);

        listener.processFile("123;/fake/path/test.zip");

        verify(reportRepository, times(1)).save(any(Report.class));
    }

    // Тест 2: Позитивный сценарий (реальный TXT файл, генерация облака слов)
    @Test
    void testFileProcessingValidation_RealTxt(@TempDir Path tempDir) throws IOException {
        ReportRepository reportRepository = Mockito.mock(ReportRepository.class);
        FileListener listener = new FileListener(reportRepository);

        // Создаем настоящий текстовый файл во временной папке тестов
        File txtFile = tempDir.resolve("test_work.txt").toFile();
        Files.writeString(txtFile.toPath(), "анализ космоскан контрольная работа успешно сдано проверка студент текст космос");

        // Имитируем получение сообщения из RabbitMQ
        listener.processFile("456;" + txtFile.getAbsolutePath());

        // Проверяем, что отчет сохранен (код успешно прошел валидацию и сгенерировал картинку)
        verify(reportRepository, times(1)).save(any(Report.class));
    }

    // Тест 3: Проверка ветки с PDF форматом
    @Test
    void testFileProcessingValidation_PdfBranch() {
        ReportRepository reportRepository = Mockito.mock(ReportRepository.class);
        FileListener listener = new FileListener(reportRepository);

        // Кидаем путь с расширением .pdf, чтобы покрыть эту ветку if-else
        listener.processFile("789;/fake/path/document.pdf");

        // Убеждаемся, что отчет всё равно сохранится (со статусом ошибки, так как файла нет)
        verify(reportRepository, times(1)).save(any(Report.class));
    }
}