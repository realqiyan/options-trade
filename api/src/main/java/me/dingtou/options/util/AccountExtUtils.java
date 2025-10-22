package me.dingtou.options.util;

import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.constant.CandlestickPeriod;
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
        return account.getExtValue(AccountExt.LONGPORT_APP_KEY, null);
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
        return account.getExtValue(AccountExt.LONGPORT_APP_SECRET, null);
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
        return account.getExtValue(AccountExt.LONGPORT_ACCESS_TOKEN, null);
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
        return account.getExtValue(AccountExt.AI_BASE_URL, "https://dashscope.aliyuncs.com/compatible-mode/v1");
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
        return account.getExtValue(AccountExt.AI_API_MODEL, "deepseek-r1");
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
        return account.getExtValue(AccountExt.AI_API_KEY, null);
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
        return account.getExtValue(AccountExt.AI_API_TEMPERATURE, "1.0");
    }

    /**
     * 获取AI接口Base URL
     *
     * @param account 账户对象
     * @return AI接口Base URL
     */
    public static String getSummaryBaseUrl(OwnerAccount account) {
        if (account == null) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return account.getExtValue(AccountExt.AI_SUMMARY_BASE_URL, "https://dashscope.aliyuncs.com/compatible-mode/v1");
    }

    /**
     * 获取AI模型名称
     *
     * @param account 账户对象
     * @return AI模型名称
     */
    public static String getSummaryApiModel(OwnerAccount account) {
        if (account == null) {
            return "deepseek-r1";
        }
        return account.getExtValue(AccountExt.AI_SUMMARY_API_MODEL, "deepseek-r1");
    }

    /**
     * 获取AI接口Key
     *
     * @param account 账户对象
     * @return AI接口Key
     */
    public static String getSummaryApiKey(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.AI_SUMMARY_API_KEY, null);
    }

    /**
     * 获取AI温度参数
     *
     * @param account 账户对象
     * @return AI温度参数
     */
    public static String getSummaryApiTemperature(OwnerAccount account) {
        if (account == null) {
            return "1.0";
        }
        return account.getExtValue(AccountExt.AI_SUMMARY_API_TEMPERATURE, "1.0");
    }

    /**
     * 获取AI总结返回提示词
     *
     * @param account 账户对象
     * @return AI总结返回提示词
     */
    public static String getSummaryResult(OwnerAccount account) {
        if (account == null) {
            return "严格按照交易规则进行综合分析和总结";
        }
        return account.getExtValue(AccountExt.AI_SUMMARY_RESULT, "严格按照交易规则进行综合分析和总结");
    }

    /**
     * 获取MCP服务器配置
     *
     * @param account 账户对象
     * @return MCP服务器配置
     */
    public static String getSystemPrompt(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.AI_MCP_SETTINGS, null);
    }

    /**
     * 获取K线周期
     *
     * @param account 账户对象
     * @return K线周期，默认为周K线
     */
    public static CandlestickPeriod getKlinePeriod(OwnerAccount account) {
        if (account == null) {
            return CandlestickPeriod.WEEK;
        }
        String period = account.getExtValue(AccountExt.KLINE_PERIOD, CandlestickPeriod.WEEK.name());
        try {
            return CandlestickPeriod.valueOf(period);
        } catch (IllegalArgumentException e) {
            return CandlestickPeriod.WEEK;
        }
    }

    /**
     * 获取是否开启邮件通知
     *
     * @param account 账户对象
     * @return 是否开启邮件通知，默认为N
     */
    public static boolean isEmailNotify(OwnerAccount account) {
        if (account == null) {
            return false;
        }
        return account.getExtValue(AccountExt.EMAIL_NOTIFY, "N").equals("Y");
    }

    /**
     * 获取邮件通知接收人邮箱
     *
     * @param account 账户对象
     * @return 邮件通知接收人邮箱
     */
    public static String getEmailNotifyReceiver(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.EMAIL_NOTIFY_RECEIVER, null);
    }

    /**
     * 获取SMTP主机
     *
     * @param account 账户对象
     * @return SMTP主机
     */
    public static String getSmtpHost(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.SMTP_HOST, null);
    }

    /**
     * 获取SMTP端口
     *
     * @param account 账户对象
     * @return SMTP端口
     */
    public static String getSmtpPort(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.SMTP_PORT, null);
    }

    /**
     * 获取SMTP用户名
     *
     * @param account 账户对象
     * @return SMTP用户名
     */
    public static String getSmtpUsername(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.SMTP_USERNAME, null);
    }

    /**
     * 获取SMTP密码
     *
     * @param account 账户对象
     * @return SMTP密码
     */
    public static String getSmtpPassword(OwnerAccount account) {
        if (account == null) {
            return null;
        }
        return account.getExtValue(AccountExt.SMTP_PASSWORD, null);
    }
}