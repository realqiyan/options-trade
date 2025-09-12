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
                o.owner,
                o.session_id,
                o.title,
                o.create_time,
                o.update_time,
                LEFT(o.content, 80) AS content
            FROM 
                `owner_chat_record` o
            JOIN (
                SELECT 
                    session_id, 
                    MAX(id) AS max_id
                FROM 
                    `owner_chat_record`
                WHERE 
                    owner = #{owner}
                GROUP BY 
                    session_id
            ) latest 
            ON o.session_id = latest.session_id AND o.id = latest.max_id
            WHERE 
                o.owner = #{owner}
            ORDER BY
                o.update_time DESC
            LIMIT #{limit}
            """)
    List<OwnerChatRecord> summaryChatRecord(String owner, int limit);
}
