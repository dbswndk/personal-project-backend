package com.example.demo.account.controller;

import com.example.demo.account.controller.form.AccountSignUpRequestForm;
import com.example.demo.account.service.AccountService;
import com.example.demo.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountController {

    final private AccountService accountService;

    final private RedisService redisService;

    @PostMapping("/sign-up")
    private Boolean signUp(@RequestBody AccountSignUpRequestForm form) {
        log.info("signUp(): " + form);

        return accountService.signUp(form.toAccountSignUpRequest());
    }
}
