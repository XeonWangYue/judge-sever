package top.xeonwang.JudgeServer.service;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.Point;
import com.influxdb.v3.client.query.QueryOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.xeonwang.JudgeServer.entity.worker.Report;
import top.xeonwang.JudgeServer.utils.InfluxUtil;
import top.xeonwang.JudgeServer.utils.JsonUtil;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TimeSeriesService {

    private final InfluxDBClient rankTsClient;

    // 写入 Point（推荐方式）
    public void writePoint() {
        Point point = Point.measurement("sensor")
                .setTag("device", "dev-001")
                .setField("temperature", 25.6)
                .setField("humidity", 60.2)
                .setTimestamp(Instant.now());
        rankTsClient.writePoint(point);
    }

    // SQL 查询示例
    public List<Map<String, Object>> querySensorData() {
        String sql = """
                SELECT time, device, temperature, humidity
                FROM sensor
                WHERE device = 'dev-001'
                ORDER BY time DESC
                LIMIT 10
                """;
        try (Stream<Object[]> stream = rankTsClient.query(sql, QueryOptions.INFLUX_QL)) {
            return stream.map(row -> Map.of(
                    "time", row[0],
                    "device", row[1],
                    "temperature", row[2],
                    "humidity", row[3]
            )).collect(Collectors.toList());
        }
    }

    public void writeWorkerMonitorData(Report rpt, String workerId) {
        Point point = Point.measurement("worker")
                .setTag("workerId", workerId)
                .setField(
                        "hostInfo",
                        JsonUtil.toJsonString(
                                Map.of(
                                        "cpu", rpt.getHostCpu(),
                                        "memory", rpt.getHostMem(),
                                        "processes", rpt.getTotalProcesses(),
                                        "threads", rpt.getTotalThreads()
                                )
                        )
                )
                .setField("subprocess", JsonUtil.toJsonString(rpt.getProcesses()))
                .setTimestamp(Instant.now());
        rankTsClient.writePoint(point);
    }

    public List<Map<String, Object>> queryWorkerData() {
        String sql = """
                SELECT time, workerId, hostInfo, subprocess
                FROM worker
                ORDER BY time DESC
                LIMIT 10
                """;
        try (Stream<Object[]> stream = rankTsClient.query(sql, QueryOptions.INFLUX_QL)) {
            return stream.map(row -> Map.of(
                    "time", row[1],
                    "workerId", row[2],
                    "hostInfo", row[3],
                    "subprocess", row[4]
            )).collect(Collectors.toList());
        }
    }
}
