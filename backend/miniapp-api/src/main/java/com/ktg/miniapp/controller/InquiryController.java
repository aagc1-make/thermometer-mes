package com.ktg.miniapp.controller;

import com.ktg.miniapp.common.R;
import com.ktg.miniapp.repo.MiniAppRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final MiniAppRepository repo;

    public InquiryController(MiniAppRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public R list(@RequestParam(required = false) String status) {
        return R.ok(repo.inquiries(status));
    }

    @GetMapping("/{inquiryNo}")
    public R detail(@PathVariable String inquiryNo) {
        return R.ok(repo.inquiry(inquiryNo));
    }

    @PostMapping
    public R submit(@RequestBody Map<String, Object> payload) {
        return R.ok(repo.createInquiry(payload));
    }
}
