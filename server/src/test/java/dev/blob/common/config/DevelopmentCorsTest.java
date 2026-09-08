package dev.blob.common.config;

import dev.blob.system.DependencyStatusService;
import dev.blob.system.SystemController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SystemController.class, properties = {
        "blob.desktop-token=development-session", "blob.allow-dev-origin=true",
        "blob.dev-origin=http://127.0.0.1:5199"
})
class DevelopmentCorsTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private DependencyStatusService dependencyStatusService;

    @Test
    void explicitDevelopmentOriginPermitsPreflightAndTokenAuthenticatedCalls() throws Exception {
        mvc.perform(options("/api/v1/system/ping").header("Origin", "http://127.0.0.1:5199")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "X-Blob-Desktop-Token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5199"));
        mvc.perform(get("/api/v1/system/ping").header("Origin", "http://127.0.0.1:5199")
                        .header("X-Blob-Desktop-Token", "development-session"))
                .andExpect(status().isOk());
    }

    @Test
    void developmentModeStillRequiresTokenAndRejectsOtherOrigins() throws Exception {
        mvc.perform(get("/api/v1/system/ping").header("Origin", "http://127.0.0.1:5199"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/system/ping").header("Origin", "http://127.0.0.1:5173")
                        .header("X-Blob-Desktop-Token", "development-session"))
                .andExpect(status().isForbidden());
    }
}
