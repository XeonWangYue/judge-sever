package top.xeonwang.JudgeServer.entity.worker;

import lombok.Data;

@Data
public class ProcessStat {
    private Integer pid;
    private Double cpuUsed;
    private Double memUsed;
}
