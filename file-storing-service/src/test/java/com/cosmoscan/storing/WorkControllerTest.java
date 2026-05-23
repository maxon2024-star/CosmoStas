package com.cosmoscan.storing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkController.class)
public class WorkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkRepository workRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    // Тест 1: Успешная загрузка файла
    @Test
    void uploadWork_ShouldReturnOk() throws Exception {
        Work mockWork = new Work();
        mockWork.setId(1L);
        Mockito.when(workRepository.save(any())).thenReturn(mockWork);

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Spring Framework".getBytes());

        mockMvc.perform(multipart("/api/works")
                        .file(file)
                        .param("studentName", "Иван Иванов"))
                .andExpect(status().isOk());
    }

    // Тест 2: Успешная выдача (скачивание) файла преподавателю
    @Test
    void downloadWork_ShouldReturnFile(@TempDir Path tempDir) throws Exception {
        // Создаем реальный файл во временной папке для теста
        File testFile = tempDir.resolve("student_work.txt").toFile();
        Files.writeString(testFile.toPath(), "Содержимое работы");

        Work mockWork = new Work();
        mockWork.setId(1L);
        mockWork.setFilePath(testFile.getAbsolutePath());

        // Говорим моку: когда контроллер пойдет искать работу №1, верни нашу фейковую
        Mockito.when(workRepository.findById(1L)).thenReturn(Optional.of(mockWork));

        mockMvc.perform(get("/api/works/1/file"))
                .andExpect(status().isOk());
    }

    // Тест 3: Ошибка 404 (Если работа в БД есть, но физически файл с диска удалили)
    @Test
    void downloadWork_FileNotFoundOnDisk() throws Exception {
        Work mockWork = new Work();
        mockWork.setId(2L);
        mockWork.setFilePath("/fake/path/that/is/missing.txt");

        Mockito.when(workRepository.findById(2L)).thenReturn(Optional.of(mockWork));

        mockMvc.perform(get("/api/works/2/file"))
                .andExpect(status().isNotFound()); // Контроллер должен вернуть 404
    }
}