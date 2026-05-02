package top.xeonwang.JudgeServer.controller;

import lombok.RequiredArgsConstructor;
import top.xeonwang.JudgeServer.common.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.xeonwang.JudgeServer.service.GoWorkerService;
import top.xeonwang.JudgeServer.utils.SpringContextUtil;

@Slf4j
@RestController
@RequestMapping("/commit")
@RequiredArgsConstructor
public class CommitController {

    private final GoWorkerService goWorkerService;

    @PostMapping("/{questionId}")
    public ResultVO<String> uploadMultiFile(
            @PathVariable Long questionId,
            @RequestParam("code") MultipartFile code,
            @RequestParam("language") String language
    ) {
        log.info("Question Id: {}, code length: {}", questionId, code.getSize());
        return new ResultVO<>();
    }

}
