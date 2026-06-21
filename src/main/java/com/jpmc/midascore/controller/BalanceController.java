package com.jpmc.midascore.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;

@RestController
public class BalanceController {
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/balance")
    public Balance getBalance(@RequestParam long userId) {
        UserRecord user = userRepository.findById(userId);

        if (user == null) {
            return new Balance(0);
        } else {
            return new Balance(user.getBalance());
        }
    }
}