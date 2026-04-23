package top.xeonwang.JudgeServer.controller;

import top.xeonwang.JudgeServer.common.ResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/commit")
public class CommitController {

    @PostMapping("/{questionId}")
    public ResponseBody<String> uploadMultiFile(
            @PathVariable Long questionId,
            @RequestParam("code") MultipartFile code,
            @RequestParam("language") String language
    ) {
        log.info("Question Id: {}, code length: {}", questionId, code.getSize());

        return new ResponseBody<>();
    }

}
