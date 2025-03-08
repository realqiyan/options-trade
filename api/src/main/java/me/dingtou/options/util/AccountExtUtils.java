package me.dingtou.options.util;

import me.dingtou.options.model.AccountExt;
import me.dingtou.options.model.OwnerAccount;

/**
 * 账户扩展字段工具类
 *
 * @author qiyan
 */
public class AccountExtUtils {
    
    /**
     * 获取长桥平台App Key
     *
     * @param account 账户对象
     * @return 长桥平台App Key
     */
    public static String getLongportAppKey(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.LONGPORT_APP_KEY);
    }
    
    /**
     * 获取长桥平台App Secret
     *
     * @param account 账户对象
     * @return 长桥平台App Secret
     */
    public static String getLongportAppSecret(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.LONGPORT_APP_SECRET);
    }
    
    /**
     * 获取长桥平台Access Token
     *
     * @param account 账户对象
     * @return 长桥平台Access Token
     */
    public static String getLongportAccessToken(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.LONGPORT_ACCESS_TOKEN);
    }
    
    /**
     * 获取AI接口Base URL
     *
     * @param account 账户对象
     * @return AI接口Base URL
     */
    public static String getAiBaseUrl(OwnerAccount account) {
        if (account == null) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        String baseUrl = account.getExtValue(AccountExt.AI_BASE_URL);
        return baseUrl != null ? baseUrl : "https://dashscope.aliyuncs.com/compatible-mode/v1";
    }
    
    /**
     * 获取AI模型名称
     *
     * @param account 账户对象
     * @return AI模型名称
     */
    public static String getAiApiModel(OwnerAccount account) {
        if (account == null) {
            return "deepseek-r1";
        }
        String model = account.getExtValue(AccountExt.AI_API_MODEL);
        return model != null ? model : "deepseek-r1";
    }
    
    /**
     * 获取AI接口Key
     *
     * @param account 账户对象
     * @return AI接口Key
     */
    public static String getAiApiKey(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.AI_API_KEY);
    }
    
    /**
     * 获取AI温度参数
     *
     * @param account 账户对象
     * @return AI温度参数
     */
    public static String getAiApiTemperature(OwnerAccount account) {
        if (account == null) {
            return "1.0";
        }
        String temperature = account.getExtValue(AccountExt.AI_API_TEMPERATURE);
        return temperature != null ? temperature : "1.0";
    }
} 