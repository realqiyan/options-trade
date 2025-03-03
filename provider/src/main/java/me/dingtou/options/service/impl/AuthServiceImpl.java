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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public Boolean auth(String owner, String otpPassword) {
        String otpAuth = otpAuth(owner);
        // 内网环境不配置otpAuth时不检查
        if (null == otpAuth) {
            log.warn("otpAuth is null, owner:{}", owner);
            return true;
        }

        try {
            URI uri = new URI(otpAuth);
            TOTPGenerator totpGenerator = TOTPGenerator.fromURI(uri);
            return totpGenerator.verify(otpPassword, 2);
        } catch (URISyntaxException e) {
            log.error("otpAuth 初始化失败 otpAuth:{}", otpAuth, e);
            return false;
        }

    }

    private String otpAuth(String owner) {
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (null == ownerAccount) {
            log.warn("owner:{} not exist", owner);
            return null;
        }
        return ownerAccount.getOtpAuth();
    }

    @Override
    public String secretKeySha256(String owner) {
        String otpAuth = otpAuth(owner);
        if (null == otpAuth) {
            log.warn("otpAuth is null, owner:{}", owner);
            return null;
        }

        //sha256
        try {
            // 获取MessageDigest实例，指定算法为SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 计算输入字符串的哈希值
            byte[] hashBytes = digest.digest(otpAuth.getBytes());
            // 将哈希值转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("secretKeySha256 error", e);
        }

        return null;
    }
}
