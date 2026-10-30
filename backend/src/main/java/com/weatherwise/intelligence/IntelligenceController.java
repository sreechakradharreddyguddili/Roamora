package com.weatherwise.intelligence;

import com.weatherwise.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intelligence")
public class IntelligenceController {

    private final IntelligenceChatService intelligenceChatService;

    public IntelligenceController(IntelligenceChatService intelligenceChatService) {
        this.intelligenceChatService = intelligenceChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<String>> chat(
            @Valid @RequestBody IntelligenceChatRequest request
    ) {
        String answer = intelligenceChatService.answer(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Roamora Intelligence response", answer)
        );
    }

    @PostMapping("/crew-plan")
    public ResponseEntity<ApiResponse<String>> crewPlan(
            @Valid @RequestBody IntelligenceChatRequest request
    ) {
        String answer = intelligenceChatService.crewPlan(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Roamora CrewAI travel plan", answer)
        );
    }
}
