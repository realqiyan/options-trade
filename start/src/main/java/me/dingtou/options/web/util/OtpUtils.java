package me.dingtou.options.web.util;

import com.bastiaanjansen.otp.TOTPGenerator;
import me.dingtou.options.config.ConfigUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * OtpUtils
 *
 * @author qiyan
 */
public class OtpUtils {

    private static final TOTPGenerator TOTP_GENERATOR;

    static {
        try {
            String otpAuth = ConfigUtils.getConfig("otpauth");
            URI uri = new URI(otpAuth);
            TOTP_GENERATOR = TOTPGenerator.fromURI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("otpauth 初始化失败", e);
        }
    }

    /**
     * 校验密码
     *
     * @param password 密码
     */
    public static boolean check(String password) {
        return TOTP_GENERATOR.verify(password, 12);
    }
}
