package top.xeonwang.JudgeServer.entity.worker;

import lombok.Data;

@Data
public class ServerMsg<T> {
    private String msgType;
    private T data;
}
