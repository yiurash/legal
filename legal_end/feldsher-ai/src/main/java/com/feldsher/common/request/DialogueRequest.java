package com.feldsher.common.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialogueRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    @NotBlank(message = "用户输入不能为空")
    private String userInput;

    private String type;
}
