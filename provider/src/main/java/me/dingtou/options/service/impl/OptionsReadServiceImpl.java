package me.dingtou.options.service.impl;

import me.dingtou.options.constant.Market;
import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsExpDate;
import me.dingtou.options.model.UnderlyingAsset;
import me.dingtou.options.service.OptionsReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OptionsReadServiceImpl implements OptionsReadService {

    @Autowired
    private OptionsManager optionsManager;

    @Override
    public List<UnderlyingAsset> queryUnderlyingAsset(String owner) {
        //TODO remove mock
        List<UnderlyingAsset> underlyingAssetList = new ArrayList<>(1);
        {
            UnderlyingAsset underlyingAsset = new UnderlyingAsset();
            underlyingAsset.setOwner(owner);
            underlyingAsset.setCode("BABA");
            underlyingAsset.setMarket(Market.US.getCode());
            underlyingAssetList.add(underlyingAsset);
        }
        {
            UnderlyingAsset underlyingAsset = new UnderlyingAsset();
            underlyingAsset.setOwner(owner);
            underlyingAsset.setCode("JD");
            underlyingAsset.setMarket(Market.US.getCode());
            underlyingAssetList.add(underlyingAsset);
        }
        return underlyingAssetList;
    }

    @Override
    public List<OptionsExpDate> queryOptionsExpDate(UnderlyingAsset underlyingAsset) {
        return optionsManager.queryOptionsExpDate(underlyingAsset.getCode(), underlyingAsset.getMarket());
    }

    @Override
    public OptionsChain queryOptionsChain(UnderlyingAsset underlyingAsset, OptionsExpDate optionsExpDate) {
        return optionsManager.queryOptionsChain(underlyingAsset.getCode(), underlyingAsset.getMarket(), optionsExpDate.getStrikeTime());
    }
}
