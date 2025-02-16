package me.dingtou.options.service.impl;

import com.bastiaanjansen.otp.TOTPGenerator;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public Boolean auth(String owner, String otpPassword) {
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (null == ownerAccount) {
            log.warn("owner:{} not exist", owner);
            return false;
        }
        String otpAuth = ownerAccount.getOtpAuth();
        // 内网环境不配置otpAuth时不检查
        if (null == otpAuth) {
            log.warn("otpAuth is null, owner:{}", owner);
            return true;
        }

        try {
            URI uri = new URI(otpAuth);
            TOTPGenerator totpGenerator = TOTPGenerator.fromURI(uri);
            return totpGenerator.verify(otpPassword, 12);
        } catch (URISyntaxException e) {
            log.error("otpAuth 初始化失败 otpAuth:{}", otpAuth, e);
            return false;
        }

    }
}
