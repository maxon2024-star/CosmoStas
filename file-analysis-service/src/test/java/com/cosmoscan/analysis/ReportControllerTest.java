package com.cosmoscan.analysis;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportRepository reportRepository;

    @Test
    void getReportsByWorkId_ShouldReturnOk() throws Exception {
        Report report = new Report();
        report.setId(1L);
        report.setStatus("ПРИНЯТО");

        Mockito.when(reportRepository.findByWorkId(1L)).thenReturn(List.of(report));

        mockMvc.perform(get("/api/reports/works/1"))
                .andExpect(status().isOk());
    }
}