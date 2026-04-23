package top.xeonwang.JudgeServer.common;

import lombok.Data;

@Data
public class ResponseBody<T> {
    private String errMsg;
    private String errCode;
    private T data;

    public ResponseBody(String errMsg, String errCode, T data) {
        this.errMsg = errMsg;
        this.errCode = errCode;
        this.data = data;
    }

    public ResponseBody() {
        this.errMsg = "";
        this.errCode = "200";
        this.data = null;
    }

    public ResponseBody(String errMsg, String errCode) {
        this.errMsg = errMsg;
        this.errCode = errCode;
    }

    public ResponseBody(T data) {
        this.errMsg = "";
        this.errCode = "200";
        this.data = data;
    }
}
