package top.xeonwang.JudgeServer.common;

import lombok.Data;

@Data
public class ResponseBody<T> {
    private String errMsg;
    private Integer errCode;
    private T data;

    public ResponseBody(Integer errCode, String errMsg, T data) {
        this.errMsg = errMsg;
        this.errCode = errCode;
        this.data = data;
    }

    public ResponseBody() {
        this.errMsg = "";
        this.errCode = 200;
        this.data = null;
    }

    public ResponseBody(Integer errCode, String errMsg) {
        this.errMsg = errMsg;
        this.errCode = errCode;
    }

    public ResponseBody(T data) {
        this.errMsg = "";
        this.errCode = 200;
        this.data = data;
    }
}
