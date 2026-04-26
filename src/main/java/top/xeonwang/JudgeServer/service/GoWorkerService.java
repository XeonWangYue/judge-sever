package top.xeonwang.JudgeServer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.xeonwang.JudgeServer.entity.worker.Report;

@Service
@RequiredArgsConstructor
public class GoWorkerService {

    private final TimeSeriesService timeSeriesService;

    public void saveMonitorData(Report rpt, String workerId) {
        timeSeriesService.writeWorkerMonitorData(rpt, workerId);
    }
}
