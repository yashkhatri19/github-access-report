package com.example.githubaccessreport.controller;

import com.example.githubaccessreport.dto.AccessReport;
import com.example.githubaccessreport.exception.GlobalExceptionHandler;
import com.example.githubaccessreport.service.AccessReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RootController.class, AccessReportController.class})
@Import(GlobalExceptionHandler.class)
class AccessReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccessReportService accessReportService;

    @Test
    void rootEndpointReturnsSuccessInfo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("github-access-report"));
    }

    @Test
    void faviconEndpointReturnsNoContent() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingResourceReturnsNotFoundStatus() throws Exception {
        mockMvc.perform(get("/unknown-page"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void missingOrgParamReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/access-report"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void validOrgParamReturnsAccessReport() throws Exception {
        AccessReport report = new AccessReport("octocat-inc", Instant.now(), 5, 2, Map.of());
        when(accessReportService.generateReport("octocat-inc")).thenReturn(report);

        mockMvc.perform(get("/api/access-report").param("org", "octocat-inc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization").value("octocat-inc"))
                .andExpect(jsonPath("$.totalRepositories").value(5));
    }
}
