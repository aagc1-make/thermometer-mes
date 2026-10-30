package com.ktg.miniapp.controller;

import com.ktg.miniapp.common.R;
import com.ktg.miniapp.repo.MiniAppRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final MiniAppRepository repo;

    public DeliveryController(MiniAppRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public R list(@RequestParam(required = false) String orderNo) {
        return R.ok(repo.deliveries(orderNo));
    }
}
