package dev.blob.common.config;

import dev.blob.system.DependencyStatusService;
import dev.blob.system.SystemController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SystemController.class, properties = "blob.desktop-token=test-session-token")
class DesktopTokenFilterTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private DependencyStatusService dependencyStatusService;

    @Test
    void missingTokenRejectsApiWithEnvelope() throws Exception {
        mvc.perform(get("/api/v1/system/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void wrongTokenRejectsApi() throws Exception {
        mvc.perform(get("/api/v1/system/ping").header("X-Blob-Desktop-Token", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correctTokenReachesController() throws Exception {
        mvc.perform(get("/api/v1/system/ping").header("X-Blob-Desktop-Token", "test-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    void allowedPreflightDoesNotRequireToken() throws Exception {
        mvc.perform(options("/api/v1/system/ping").header("Origin", "blob-app://app")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "X-Blob-Desktop-Token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "blob-app://app"));
    }

    @Test
    void foreignOriginAndBareOptionsCannotBypassBoundary() throws Exception {
        mvc.perform(options("/api/v1/system/ping").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
        mvc.perform(options("/api/v1/system/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productionDoesNotAllowDevelopmentOrigin() throws Exception {
        mvc.perform(get("/api/v1/system/ping").header("Origin", "http://127.0.0.1:5173")
                        .header("X-Blob-Desktop-Token", "test-session-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pathParametersCannotBypassApiToken() throws Exception {
        mvc.perform(get("/api/v1;ignored/system/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void livenessAndNonApiPathsDoNotNeedToken() throws Exception {
        var filter = new DesktopTokenFilter("test-session-token", JsonMapper.builder().build());
        for (String path : new String[]{"/actuator/health/liveness", "/api/v10/status"}) {
            var request = new MockHttpServletRequest("GET", path);
            var chain = new MockFilterChain();
            filter.doFilter(request, new MockHttpServletResponse(), chain);
            assertThat(chain.getRequest()).isSameAs(request);
        }
    }
}
