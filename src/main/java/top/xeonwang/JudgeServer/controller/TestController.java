package top.xeonwang.JudgeServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.xeonwang.JudgeServer.common.ResultVO;
import top.xeonwang.JudgeServer.service.TimeSeriesService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    private final TimeSeriesService service;

    @PostMapping("/influx/write")
    public String write() {
        service.writePoint();
        return "ok";
    }

    @GetMapping("/influx/list")
    public List<Map<String, Object>> list() {
        return service.querySensorData();
    }

    @GetMapping("/influx/woker")
    public ResultVO getWorker() {
        return new ResultVO(service.queryWorkerData());
    }
}
