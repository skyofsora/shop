package com.shop.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.shop.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Controller
@RequestMapping("api")
public class OpenAIController {
    @Autowired
    private OpenAIService openAIService;

    @PostMapping("/ai")
    public @ResponseBody ResponseEntity<?> openAIGPT(@RequestBody JsonNode json) {
        try {
            return ResponseEntity.ok().body(openAIService.getCompletion(json));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/ai")
    public String chatPage() {
        return "ai/chatGPT";
    }
}