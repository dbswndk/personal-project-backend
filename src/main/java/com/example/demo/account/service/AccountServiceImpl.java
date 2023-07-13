package com.example.demo.account.service;

import com.example.demo.account.controller.form.AccountLoginRequestForm;
import com.example.demo.account.controller.request.AccessRegisterRequest;
import com.example.demo.account.controller.request.AccountRegisterRequest;
import com.example.demo.account.entity.Account;
import com.example.demo.account.entity.AccountRole;
import com.example.demo.account.entity.Role;
import com.example.demo.account.repository.AccountRepository;
import com.example.demo.account.repository.AccountRoleRepository;
import com.example.demo.account.repository.RoleRepository;
import com.example.demo.redis.RedisService;
import com.example.demo.security.jwt.JwtProvider;
import com.example.demo.security.jwt.subject.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService{

    final private AccountRepository accountRepository;
    final private AccountRoleRepository accountRoleRepository;
    final private RoleRepository roleRepository;
    final private PasswordEncoder passwordEncoder;
    final private ObjectMapper objectMapper;
    final private JwtProvider jwtProvider;
    final private RedisService redisService;

    // 회원 가입
    @Override
    public Boolean signUp(AccountRegisterRequest request) {
        final Optional<Account> maybeAccount = accountRepository.findByEmail(request.getEmail());

        if (maybeAccount.isPresent()) {
            return false;
        }

        Account account = request.toAccount();
        String password = passwordEncoder.encode(request.getPassword());
        account.setPassword(password);
        accountRepository.save(account);

        final Role role = roleRepository.findByRoleType(request.getRoleType()).get();
        final AccountRole accountRole = new AccountRole(account, role);
        accountRoleRepository.save(accountRole);

        return true;
    }

    @Override
    public Boolean accessSignUp(AccessRegisterRequest request) {
        final Optional<Account> maybeAccount = accountRepository.findByEmail(request.getEmail());

        if (maybeAccount.isPresent() || !request.getAccessNumber().equals("123-456")) {
            return false;
        }

        Account account = request.toAccount();
        String password = passwordEncoder.encode(request.getPassword());
        account.setPassword(password);
        accountRepository.save(account);

        final Role role = roleRepository.findByRoleType(request.getRoleType()).get();
        final AccountRole accountRole = new AccountRole(account, role);

        accountRoleRepository.save(accountRole);

        return true;
    }

    // 이메일 중복 확인
    @Override
    public Boolean checkEmail(String email) {
        Optional<Account> maybeAccount = accountRepository.findByEmail(email);

        if (maybeAccount.isPresent()) {
            return false;
        } else {
            return true;
        }
    }

    // 로그인
    @Override
    public TokenResponse  login(AccountLoginRequestForm form) {
        Optional<Account> maybeAccount = accountRepository.findByEmail(form.getEmail());

        if (maybeAccount.isPresent()) {
            if (passwordEncoder.matches(form.getPassword(), maybeAccount.get().getPassword())) {
                Account account = maybeAccount.get();
                TokenResponse tokenResponse = jwtProvider.createTokenByLogin(account);

                String accessToken = tokenResponse.getAccessToken();
                String refreshToken = tokenResponse.getRefreshToken();
                form.setEmail(accessToken, refreshToken);

                log.info("accessToken: " + accessToken);
                log.info("refreshToken: " + refreshToken);
                redisService.setKeyAndValue(refreshToken, account.getEmail());
                return tokenResponse;
            }
        }
        return null;
    }

    @Override
    public Long findAccountInfoById(String email) {
        Optional<Account> maybeAccount = accountRepository.findByEmail(email);
        if (maybeAccount.isPresent()){
            return maybeAccount.get().getId();
        }
        return null;
    }

}
