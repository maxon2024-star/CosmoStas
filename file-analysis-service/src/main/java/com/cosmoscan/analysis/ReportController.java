package com.cosmoscan.analysis;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportRepository reportRepository;

    public ReportController(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @GetMapping("/works/{workId}")
    public ResponseEntity<List<Report>> getReportsByWorkId(@PathVariable Long workId) {
        return ResponseEntity.ok(reportRepository.findByWorkId(workId));
    }
}