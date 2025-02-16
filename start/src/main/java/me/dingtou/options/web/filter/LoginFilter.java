package me.dingtou.options.web.filter;


import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.web.model.LoginInfo;
import me.dingtou.options.web.util.SessionUtils;


import java.io.IOException;
import java.util.Base64;

@Slf4j
public class LoginFilter implements Filter {

    private final AuthService authService;

    public LoginFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        LoginInfo loginInfo = getLoginInfo(request);
        if (null == loginInfo || !checkLoginInfo(loginInfo)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.setContentType("text/html; charset=utf-8");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"Realm\"");
            httpResponse.getWriter().write("401 Unauthorized");
            return;
        }

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


    /**
     * 获取登录信息
     *
     * @param request request
     * @return LoginInfo
     */
    private LoginInfo getLoginInfo(ServletRequest request) {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
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
            return null;
        } catch (Exception ex) {
            log.error("getLoginInfo error.", ex);
            return null;
        }

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