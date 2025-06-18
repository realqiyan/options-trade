package me.dingtou.options.gateway.futu.util;

import java.math.BigDecimal;

import com.futu.openapi.pb.TrdCommon;
import me.dingtou.options.model.OwnerPosition;

/**
 * 持仓转换
 */
public class PositionUtils {

    /**
     * 持仓转换
     * 
     * @param position 原始持仓
     * @param owner    owner
     * @return 转换后的持仓
     */
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
