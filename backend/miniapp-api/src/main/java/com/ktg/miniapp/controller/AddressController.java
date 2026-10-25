package com.ktg.miniapp.controller;

import com.ktg.miniapp.common.R;
import com.ktg.miniapp.repo.MiniAppRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final MiniAppRepository repo;

    public AddressController(MiniAppRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public R list() {
        return R.ok(repo.addresses());
    }

    @PostMapping
    public R create(@RequestBody Map<String, Object> payload) {
        return R.ok(repo.createAddress(payload));
    }

    @PutMapping("/{id}")
    public R update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return R.ok(repo.updateAddress(id, payload));
    }

    @DeleteMapping("/{id}")
    public R delete(@PathVariable Long id) {
        repo.deleteAddress(id);
        return R.ok();
    }

    @PutMapping("/{id}/default")
    public R setDefault(@PathVariable Long id) {
        return R.ok(repo.setDefaultAddress(id));
    }
}
