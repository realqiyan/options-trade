package me.dingtou.options.service;

import com.bastiaanjansen.otp.TOTPGenerator;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerAccount;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

import javax.crypto.SecretKey;

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
            log.warn("auth,otpAuth is null, owner:{}", owner);
            return true;
        }

        try {
            URI uri = new URI(otpAuth);
            TOTPGenerator totpGenerator = TOTPGenerator.fromURI(uri);
            return totpGenerator.verify(otpPassword, 1);
        } catch (URISyntaxException e) {
            log.error("otpAuth 初始化失败 otpAuth:{}", otpAuth, e);
            return false;
        }

    }

    @Override
    public String secretKeySha256(String owner) {
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (null == ownerAccount) {
            return StringUtils.EMPTY;
        }
        String otpAuth = ownerAccount.getOtpAuth();
        if (null == otpAuth) {
            log.warn("secretKeySha256,otpAuth is null, owner:{}", owner);
            return null;
        }

        // sha256
        try {
            // 获取MessageDigest实例，指定算法为SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 计算输入字符串的哈希值
            byte[] hashBytes = digest.digest(otpAuth.getBytes("UTF-8"));
            // 将哈希值转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("secretKeySha256 error", e);
        }

        return null;
    }

    @Override
    public String jwt(String owner, Date expireDate) {
        String secretKeySha256 = secretKeySha256(owner);
        SecretKey key = Keys.hmacShaKeyFor(secretKeySha256.getBytes(StandardCharsets.UTF_8));

        // 设置jwt的body
        JwtBuilder builder = Jwts.builder()
                .signWith(key)
                .subject(owner)
                .issuedAt(new Date())
                // 设置过期时间
                .expiration(expireDate);

        return builder.compact();
    }
}
