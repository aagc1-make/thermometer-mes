package com.ktg.miniapp.controller;

import com.ktg.miniapp.common.R;
import com.ktg.miniapp.repo.MiniAppRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerController {

    private final MiniAppRepository repo;

    public CustomerController(MiniAppRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/profile")
    public R profile() {
        return R.ok(repo.customerProfile());
    }
}
