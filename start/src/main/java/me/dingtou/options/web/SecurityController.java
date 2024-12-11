package me.dingtou.options.web;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.Security;
import me.dingtou.options.service.OptionsService;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class SecurityController {

    @Autowired
    private OptionsService optionsService;


    @RequestMapping(value = "/security/list", method = RequestMethod.GET)
    public List<Security> listSecurity() throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        List<Security> securityList = optionsService.querySecurity(owner);
        if (null == securityList || securityList.isEmpty()) {
            return Collections.emptyList();
        }
        return securityList;
    }


}
