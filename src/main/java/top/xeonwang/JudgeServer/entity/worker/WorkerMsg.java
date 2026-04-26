package top.xeonwang.JudgeServer.entity.worker;

import lombok.Data;

@Data
public class WorkerMsg<T> {
    private String msgType;
    private T data;
}
