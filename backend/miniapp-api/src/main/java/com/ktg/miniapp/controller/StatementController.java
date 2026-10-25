package com.ktg.miniapp.controller;

import com.ktg.miniapp.common.R;
import com.ktg.miniapp.repo.MiniAppRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statements")
public class StatementController {

    private final MiniAppRepository repo;

    public StatementController(MiniAppRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public R list() {
        return R.ok(repo.statements());
    }
}
