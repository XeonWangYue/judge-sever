package top.xeonwang.JudgeServer.entity.ws;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WsUser {
    private String userId;
    private String sessionId;
}
