package top.xeonwang.JudgeServer.common;

import lombok.Data;

@Data
public class ResultVO<T> {
    private String errMsg;
    private Integer errCode;
    private T data;

    public ResultVO(Integer errCode, String errMsg, T data) {
        this.errMsg = errMsg;
        this.errCode = errCode;
        this.data = data;
    }

    public ResultVO() {
        this.errMsg = "";
        this.errCode = 200;
        this.data = null;
    }

    public ResultVO(Integer errCode, String errMsg) {
        this.errMsg = errMsg;
        this.errCode = errCode;
    }

    public ResultVO(T data) {
        this.errMsg = "";
        this.errCode = 200;
        this.data = data;
    }
}
