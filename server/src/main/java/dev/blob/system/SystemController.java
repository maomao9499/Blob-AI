package dev.blob.system;

import dev.blob.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/ping")
    ApiResponse<Map<String, String>> ping() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }
}
