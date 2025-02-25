package me.dingtou.options.web.filter;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
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
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LoginFilter implements Filter {

    public static final String JWT = "jwt";
    public static final String OWNER = "owner";
    private final AuthService authService;

    public LoginFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setCharacterEncoding("UTF-8");
        LoginInfo loginInfo = getLoginInfo(httpRequest, httpResponse);
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
     * @param httpResponse
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

            LoginInfo user = loginFromCookie(httpRequest.getCookies());

            return user;
        } catch (Exception ex) {
            log.error("getLoginInfo error.", ex);
            return null;
        }

    }

    /**
     * 从cookie中获取登录信息
     *
     * @param cookies cookies
     * @return LoginInfo
     */
    private LoginInfo loginFromCookie(Cookie[] cookies) {
        if (null == cookies) {
            return null;
        }
        Map<String, String> cookieMap = new HashMap<>();
        for (Cookie cookie : cookies) {
            cookieMap.put(cookie.getName(), cookie.getValue());
        }
        if (!cookieMap.containsKey(OWNER) || !cookieMap.containsKey(JWT)) {
            return null;
        }
        String owner = cookieMap.get(OWNER);
        String jwtStr = cookieMap.get(JWT);

        String secretKey = authService.secretKeySha256(owner);
        try {
            Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
            Jws<Claims> jwt = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwtStr);
            Claims body = jwt.getBody();
            return new LoginInfo(body.getSubject());
        } catch (Exception e) {
            log.error("parseClaimsJws error, jwtStr:{}, message:{}", jwtStr, e.getMessage());
        }
        return null;
    }

    /**
     * 保存cookie
     *
     * @param httpResponse
     * @param loginInfo
     */
    private void saveCookie(HttpServletResponse httpResponse, LoginInfo loginInfo) {
        String secretKeySha256 = authService.secretKeySha256(loginInfo.getOwner());
        Key key = Keys.hmacShaKeyFor(secretKeySha256.getBytes());

        int oneDay = 24 * 60 * 60;
        String jwt = Jwts.builder()
                .subject(loginInfo.getOwner())
                .signWith(key)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + oneDay * 7 * 1000))
                .compact();
        Cookie jwtCookie = new Cookie(JWT, jwt);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(oneDay * 7);

        Cookie ownerCookie = new Cookie(OWNER, loginInfo.getOwner());
        ownerCookie.setSecure(true);
        ownerCookie.setPath("/");
        ownerCookie.setHttpOnly(true);
        ownerCookie.setMaxAge(oneDay * 7);

        httpResponse.addCookie(jwtCookie);
        httpResponse.addCookie(ownerCookie);
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
            return new String(b);
        } catch (Exception e) {
            return null;
        }
    }

}