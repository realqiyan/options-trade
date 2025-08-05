package me.dingtou.options.web.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.web.model.LoginInfo;
import me.dingtou.options.web.util.SessionUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson2.JSON;

@Slf4j
public class LoginFilter implements Filter {

    public static final String JWT = "jwt";
    private final AuthService authService;
    private final int cookieMaxAgeDays;

    public LoginFilter(AuthService authService, int cookieMaxAgeDays) {
        this.authService = authService;
        this.cookieMaxAgeDays = cookieMaxAgeDays;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setCharacterEncoding("UTF-8");

        // 检查是否为排除的路径
        String requestURI = httpRequest.getRequestURI();
        if (requestURI.equals("/sse") || requestURI.equals("/mcp/message")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = httpRequest.getHeader("apiKey");

        // header auth
        LoginInfo loginInfo = jwtLogin(apiKey);
        if (null == loginInfo) {
            loginInfo = getLoginInfo(httpRequest, httpResponse);
        }
        if (null == loginInfo || !checkLoginInfo(loginInfo)) {
            httpResponse.setContentType("text/html; charset=utf-8");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"Realm\"");
            httpResponse.getWriter().write("401 Unauthorized");
            return;
        }

        // 记录cookie
        saveCookie(httpResponse, loginInfo);

        SessionUtils.setCurrentOwner(loginInfo.getOwner());
        chain.doFilter(request, response);
        SessionUtils.clearCurrentOwner();
    }

    @Override
    public void destroy() {
    }

    /**
     * 检查登录信息
     *
     * @param loginInfo loginInfo
     * @return 结果
     */
    private boolean checkLoginInfo(LoginInfo loginInfo) {
        if (LoginInfo.JWT.equals(loginInfo.getType())) {
            return true;
        } else {
            LoginInfo successLoginInfo = SessionUtils.get(loginInfo.getOwner());
            if (null != successLoginInfo && loginInfo.getOtpPassword().equals(successLoginInfo.getOtpPassword())) {
                return true;
            }
            Boolean auth = authService.auth(loginInfo.getOwner(), loginInfo.getOtpPassword());
            if (Boolean.TRUE.equals(auth)) {
                SessionUtils.login(loginInfo);
            }
            return auth;
        }
    }

    /**
     * 获取登录信息
     *
     * @param httpRequest  request
     * @param httpResponse httpResponse
     * @return LoginInfo
     */
    private LoginInfo getLoginInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            String auth = httpRequest.getHeader("Authorization");
            if ((auth != null) && (auth.length() > 6)) {
                String HeadStr = auth.substring(0, 5).toLowerCase();
                if (HeadStr.compareTo("basic") == 0) {
                    auth = auth.substring(6);
                    String decodedAuth = decode(auth);
                    if (decodedAuth != null) {
                        String[] loginInfo = decodedAuth.split(":");
                        return new LoginInfo(loginInfo[0], loginInfo[1]);
                    }
                }
            }

            Cookie[] cookies = httpRequest.getCookies();
            if (null == cookies) {
                return null;
            }

            for (Cookie cookie : cookies) {
                if (JWT.equals(cookie.getName())) {
                    return jwtLogin(cookie.getValue());
                }
            }

            return null;

        } catch (Exception ex) {
            log.error("getLoginInfo error.", ex);
            return null;
        }

    }

    /**
     * jwt登录
     * 
     * @param jwtStr jwt
     * @return 登录信息
     */
    private LoginInfo jwtLogin(String jwtStr) {
        String owner = decodeJwtOwner(jwtStr);
        if (StringUtils.isBlank(owner)) {
            return null;
        }
        String secretKey = authService.secretKeySha256(owner);
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            JwtParser jwtParser = Jwts.parser()
                    // 设置签名的秘钥
                    .verifyWith(key)
                    .build();

            Jws<Claims> jwt = jwtParser.parseSignedClaims(jwtStr);
            Claims body = jwt.getPayload();
            return new LoginInfo(body.getSubject());
        } catch (Exception e) {
            log.error("parseClaimsJws error, jwtStr:{}, message:{}", jwtStr, e.getMessage());
        }
        return null;
    }

    /**
     * 解析JWT 获取sub属性
     * 
     * @param jwtStr jwt
     * @return sub
     */
    private String decodeJwtOwner(String jwtStr) {
        if (jwtStr == null || jwtStr.isEmpty()) {
            return "";
        }

        // 分割JWT获取payload部分
        String[] parts = jwtStr.split("\\.");
        if (parts.length < 2) {
            return ""; // 无效JWT
        }

        String payload = parts[1];
        try {
            // Base64URL解码
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
            return JSON.parseObject(payloadJson).getString("sub");
        } catch (Exception e) {
            log.error("decodeJwtOwner error, jwtStr:{}", jwtStr, e);
            return "";
        }
    }

    /**
     * 保存cookie
     *
     * @param httpResponse
     * @param loginInfo
     */
    private void saveCookie(HttpServletResponse httpResponse, LoginInfo loginInfo) {
        int oneDay = 24 * 60 * 60;
        String jwt = authService.jwt(loginInfo.getOwner(),
                new Date(System.currentTimeMillis() + oneDay * cookieMaxAgeDays * 1000L));
        Cookie jwtCookie = new Cookie(JWT, jwt);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(oneDay * cookieMaxAgeDays);
        httpResponse.addCookie(jwtCookie);
    }

    /**
     * 解码
     *
     * @param auth 认证信息
     * @return 解码后的信息
     */
    private String decode(String auth) {
        if (auth == null) {
            return null;
        }
        Base64.Decoder decoder = Base64.getDecoder();
        try {
            byte[] b = decoder.decode(auth);
            return new String(b, Charset.forName("UTF-8"));
        } catch (Exception e) {
            return null;
        }
    }

}
