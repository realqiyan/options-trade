package me.dingtou.options.config.security;

import javax.annotation.Nullable;

import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntity;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntityRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import me.dingtou.options.service.AuthService;

/**
 * API密钥存储库实现类
 * 
 * 该类实现了Spring MCP安全框架的ApiKeyEntityRepository接口，
 * 负责管理API密钥的查找和验证功能。通过整合AuthService来验证
 * API密钥的有效性，并将有效的密钥包装成ApiKeyEntity对象。
 * 
 * 主要功能：
 * 1. 根据keyId查找并验证API密钥
 * 2. 验证失败时抛出异常
 * 3. 提供ApiKeyEntity接口的实现
 */
public class ApiKeyRepository implements ApiKeyEntityRepository {

    /**
     * 认证服务，用于验证API密钥的有效性
     */
    private AuthService authService;

    /**
     * 编码器名称，用于标识密码编码器的类型
     */
    private String encoderName;

    /**
     * 密码编码器，用于对API密钥进行加密
     */
    private PasswordEncoder passwordEncoder;

    /**
     * 构造函数
     * 
     * @param authService     认证服务实例，用于验证API密钥
     * @param encoderName     编码器名称，用于标识密码编码器的类型
     * @param passwordEncoder 密码编码器实例，用于对API密钥进行加密
     */
    public ApiKeyRepository(AuthService authService, String encoderName, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.encoderName = encoderName;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 根据keyId查找API密钥实体
     * 
     * 该方法会通过AuthService验证传入的keyId，如果验证成功，
     * 则返回一个包含密钥信息的ApiKey对象；如果验证失败，
     * 则抛出IllegalArgumentException异常。
     * 
     * @param keyId API密钥ID
     * @return ApiKeyEntity对象，包含有效的API密钥信息；如果密钥无效则返回null
     * @throws IllegalArgumentException 当API密钥无效时抛出此异常
     */
    @Override
    public @Nullable ApiKeyEntity findByKeyId(String keyId) {
        // 使用认证服务验证API密钥，获取对应的所有者代码
        String ownerCode = authService.encodeOwner(keyId);

        // 如果ownerCode为null，说明API密钥无效
        if (ownerCode == null) {
            throw new IllegalArgumentException("apiKey无效");
        }
        String encode = "{" + encoderName + "}" + passwordEncoder.encode(ownerCode);
        // 返回有效的ApiKey对象
        return new ApiKey(keyId, encode);
    }

    /**
     * API密钥实体实现类
     * 
     * 实现了Spring MCP安全框架的ApiKeyEntity接口，
     * 封装了API密钥的ID、密钥内容以及安全相关状态。
     */
    static class ApiKey implements ApiKeyEntity {

        /**
         * API密钥ID
         */
        private String id;

        /**
         * API密钥的密钥内容（一般为加密后的密钥）
         */
        private String secret;

        /**
         * 标记是否已擦除敏感信息，用于安全清理
         */
        private boolean erase = false;

        /**
         * 构造函数，创建API密钥实体
         * 
         * @param id     API密钥ID
         * @param secret 密钥内容
         */
        public ApiKey(String id, String secret) {
            this.id = id;
            this.secret = secret;
        }

        /**
         * 获取API密钥ID
         * 
         * @return API密钥的标识符
         */
        @Override
        public String getId() {
            return id;
        }

        /**
         * 获取密钥内容
         * 
         * @return 密钥的密文内容，可能为null
         */
        @Override
        public @Nullable String getSecret() {
            return secret;
        }

        /**
         * 检查是否已擦除敏感信息
         * 
         * @return true表示已擦除，false表示未擦除
         */
        public boolean isErase() {
            return erase;
        }

        /**
         * 擦除敏感信息
         * 
         * 将erase标记设置为true，用于安全清理敏感信息，
         * 但不会立即修改secret字段的值。
         */
        @Override
        public void eraseCredentials() {
            this.erase = true;
        }

        /**
         * 创建当前对象的副本
         * 
         * @return 新的ApiKey对象，包含相同的数据
         */
        @Override
        public ApiKey copy() {
            return new ApiKey(this.id, this.secret);
        }
    }
}
