package com.example.demo.account.service;

import com.example.demo.account.controller.form.AccountLoginRequestForm;
import com.example.demo.account.controller.request.AccessRegisterRequest;
import com.example.demo.account.controller.request.AccountRegisterRequest;
import com.example.demo.account.controller.response.LoginResponse;
import com.example.demo.account.controller.response.MyPageResponse;
import com.example.demo.security.jwt.subject.TokenResponse;

public interface AccountService {
    Boolean signUp(AccountRegisterRequest request);

    Boolean accessSignUp(AccessRegisterRequest accessRegisterRequest);

    Boolean checkEmail(String email);

    LoginResponse login(AccountLoginRequestForm form, Long accountId);

    MyPageResponse findAccountInfo(String accessToken);

}
