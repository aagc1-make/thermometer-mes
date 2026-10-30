package com.ktg.miniapp.controller;

import com.ktg.miniapp.common.R;
import com.ktg.miniapp.repo.MiniAppRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final MiniAppRepository repo;

    public CatalogController(MiniAppRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/products")
    public R products() {
        return R.ok(repo.catalogProducts());
    }
}
