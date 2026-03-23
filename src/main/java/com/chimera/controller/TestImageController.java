package com.chimera.controller;

import com.chimera.model.ImageRequest;
import com.chimera.model.ImageResult;
import com.chimera.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestImageController {
    private final ImageGenerationService imageService;

    @GetMapping("/image")
    public String generateImage(@RequestParam String prompt,
                                @RequestParam(defaultValue = "1024") int width,
                                @RequestParam(defaultValue = "1024") int height) {
        ImageRequest req = new ImageRequest(prompt, null, null, width, height);
        ImageResult res = imageService.generateImage(req).join();
        return "Generated image URL: " + res.imageUrl();
    }
}