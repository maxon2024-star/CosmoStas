package com.cosmoscan.storing;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/works")
public class WorkController {

    private final RabbitTemplate rabbitTemplate;
    private final WorkRepository workRepository;

    @Value("${upload.path:/app/uploads}")
    private String uploadPath;

    public WorkController(RabbitTemplate rabbitTemplate, WorkRepository workRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.workRepository = workRepository;
    }

    // Загрузка файла студентом
    @PostMapping
    public ResponseEntity<String> uploadWork(@RequestParam("studentName") String studentName,
                                             @RequestParam("file") MultipartFile file) {
        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File dest = new File(dir, filename);
            file.transferTo(dest);

            Work work = new Work();
            work.setStudentName(studentName);
            work.setFilePath(dest.getAbsolutePath());
            work.setUploadTime(LocalDateTime.now());
            work = workRepository.save(work);

            String payload = work.getId() + ";" + dest.getAbsolutePath();
            rabbitTemplate.convertAndSend("file.exchange", "file.routing.key", payload);

            return ResponseEntity.ok("Работа №" + work.getId() + " принята. Идет проверка...");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Ошибка сохранения файла");
        }
    }

    // Выдача файла преподавателю
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadWork(@PathVariable Long id) {
        Work work = workRepository.findById(id).orElseThrow(() -> new RuntimeException("Работа не найдена"));
        File file = new File(work.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}