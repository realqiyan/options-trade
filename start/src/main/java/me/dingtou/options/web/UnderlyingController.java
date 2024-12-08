package me.dingtou.options.web;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.UnderlyingAsset;
import me.dingtou.options.service.OptionsReadService;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class UnderlyingController {

    @Autowired
    private OptionsReadService optionsReadService;


    @RequestMapping(value = "/underlying/list", method = RequestMethod.GET)
    public List<UnderlyingAsset> listUnderlyingAsset() throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        List<UnderlyingAsset> underlyingAssets = optionsReadService.queryUnderlyingAsset(owner);
        if (null == underlyingAssets || underlyingAssets.isEmpty()) {
            return Collections.emptyList();
        }
        return underlyingAssets;
    }


}
