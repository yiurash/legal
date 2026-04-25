package com.feldsher.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dialogue_message")
public class DialogueMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String role;

    private String content;

    private String messageType;

    private String metadataJson;

    private LocalDateTime createdAt;

    private Integer deleted;
}
