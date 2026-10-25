package com.ktg.miniapp.controller;

import com.ktg.miniapp.common.R;
import com.ktg.miniapp.repo.MiniAppRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final MiniAppRepository repo;

    public InvoiceController(MiniAppRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public R list() {
        return R.ok(repo.invoices());
    }

    @GetMapping("/profile")
    public R profile() {
        return R.ok(repo.invoiceProfile());
    }

    @PostMapping
    public R apply(@RequestBody Map<String, Object> payload) {
        return R.ok(repo.createInvoice(payload));
    }
}
