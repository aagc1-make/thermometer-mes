package com.ktg.miniapp.controller;

import com.ktg.miniapp.common.R;
import com.ktg.miniapp.repo.MiniAppRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final MiniAppRepository repo;

    public OrderController(MiniAppRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public R list(@RequestParam(required = false) String status) {
        return R.ok(repo.orders(status));
    }

    @GetMapping("/{orderNo}")
    public R detail(@PathVariable String orderNo) {
        return R.ok(repo.order(orderNo));
    }

    @PostMapping("/from-inquiry/{inquiryNo}")
    public R createFromInquiry(@PathVariable String inquiryNo) {
        return R.ok(repo.createOrderFromInquiry(inquiryNo));
    }
}
