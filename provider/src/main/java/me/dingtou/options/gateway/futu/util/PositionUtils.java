package me.dingtou.options.gateway.futu.util;

import java.math.BigDecimal;

import com.futu.openapi.pb.TrdCommon;
import me.dingtou.options.model.OwnerPosition;

public class PositionUtils {

    public static OwnerPosition convertOwnerPosition(TrdCommon.Position position, String owner) {
        if (null == position) {
            return null;
        }
        OwnerPosition ownerPosition = new OwnerPosition();
        ownerPosition.setOwner(owner);
        ownerPosition.setSecurityCode(position.getCode());
        ownerPosition.setSecurityName(position.getName());
        ownerPosition.setQuantity(BigDecimal.valueOf(position.getQty()));
        ownerPosition.setCanSellQty(BigDecimal.valueOf(position.getCanSellQty()));
        ownerPosition.setCostPrice(BigDecimal.valueOf(position.getCostPrice()));
        ownerPosition.setCurrentPrice(BigDecimal.valueOf(position.getPrice()));
        return ownerPosition;
    }
}
