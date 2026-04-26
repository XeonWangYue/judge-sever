package top.xeonwang.JudgeServer.entity.worker;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Report {
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss[.SSS]",
            timezone = "${user.timezone}"
    )
    private LocalDateTime time;

    private List<Double> hostCpu;
    private Double hostMem;
    private Long netSend;
    private Long netRecv;
    private Long diskRead;
    private Long diskWrite;
    private String netSendStr;
    private String netRecvStr;
    private String diskReadStr;
    private String diskWriteStr;
    private Integer totalProcesses;
    private Integer totalThreads;
    private List<ProcessStat> processes;
}
