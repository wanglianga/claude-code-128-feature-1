package com.dentallab.service;

import com.dentallab.config.BizException;
import com.dentallab.model.Account;
import com.dentallab.model.AuthToken;
import com.dentallab.model.Role;
import com.dentallab.repo.AccountRepo;
import com.dentallab.repo.AuthTokenRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final AccountRepo accountRepo;
    private final AuthTokenRepo tokenRepo;
    private final long tokenTtlHours;

    public AuthService(AccountRepo accountRepo, AuthTokenRepo tokenRepo,
                       @Value("${app.token-ttl-hours:12}") long tokenTtlHours) {
        this.accountRepo = accountRepo;
        this.tokenRepo = tokenRepo;
        this.tokenTtlHours = tokenTtlHours;
    }

    @Transactional
    public Map<String, Object> login(String username, String password) {
        Account account = accountRepo.findByUsername(username)
                .orElseThrow(() -> BizException.unauthorized("用户名或密码错误"));
        if (!account.getPassword().equals(password)) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        AuthToken token = new AuthToken();
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setAccount(account);
        token.setExpiresAt(LocalDateTime.now().plusHours(tokenTtlHours));
        tokenRepo.save(token);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token.getToken());
        result.put("expiresAt", token.getExpiresAt());
        result.put("account", Views.account(account));
        return result;
    }

    @Transactional(readOnly = true)
    public Account authenticate(String token) {
        AuthToken authToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> BizException.unauthorized("令牌无效，请重新登录"));
        if (authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw BizException.unauthorized("令牌已过期，请重新登录");
        }
        Account account = authToken.getAccount();
        // 在事务内初始化懒加载代理，账号会跨请求传递使用
        account.getRole();
        if (account.getClinic() != null) {
            account.getClinic().getName();
        }
        return account;
    }

    /** 要求当前账号具备其中任一角色。 */
    public void requireRole(Account account, Role... roles) {
        for (Role role : roles) {
            if (account.getRole() == role) {
                return;
            }
        }
        throw BizException.forbidden("角色[" + account.getRole().getLabel() + "]无权执行该操作，需要角色: "
                + Arrays.stream(roles).map(Role::getLabel).reduce((a, b) -> a + "/" + b).orElse(""));
    }
}
