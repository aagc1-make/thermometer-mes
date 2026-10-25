package com.ktg.miniapp.controller;

import com.ktg.miniapp.common.R;
import com.ktg.miniapp.repo.MiniAppRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ServiceController {

    private final MiniAppRepository repo;

    public ServiceController(MiniAppRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/health")
    public R health() {
        return R.ok("miniapp-api is running");
    }

    @GetMapping("/contacts")
    public R contacts() {
        return R.ok(repo.contacts());
    }

    @GetMapping("/sales")
    public R sales() {
        return R.ok(repo.sales());
    }

    @GetMapping("/help/faq")
    public R faq() {
        return R.ok(repo.faqs());
    }
}
