package me.dingtou.options.dao;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.dingtou.options.model.OwnerChatRecord;

/**
 * AI助手沟通记录DAO
 *
 * @author qiyan
 */
public interface OwnerChatRecordDAO extends BaseMapper<OwnerChatRecord> {

    @Select("""
            SELECT 
                owner,
                session_id,
                MAX(title) AS title,
                MAX(create_time) AS create_time,
                MAX(update_time) AS update_time,
                MAX(LEFT(content, 80)) AS content
            FROM
                `owner_chat_record`
            WHERE 
                OWNER = #{owner}
            GROUP BY 
                owner,
                session_id
            ORDER BY
                MAX(update_time)
            DESC
            LIMIT #{limit}
            """)
    List<OwnerChatRecord> summaryChatRecord(String owner, int limit);
}