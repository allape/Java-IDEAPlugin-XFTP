package net.allape.model;

public class Transfer {

    /**
     * 传输类型
     */
    public enum Type {

        DOWNLOAD,
        UPLOAD,

    }

    /**
     * 传输结果
     */
    public enum Result {

        // 成功
        SUCCESS,
        // 失败
        FAIL,
        // 传输中
        TRANSFERRING,
        // 取消
        CANCELLED,

    }

    /**
     * 传输类型
     */
    private Type type;

    /**
     * 数据源
     */
    private String source;

    /**
     * 目标
     */
    private String target;

    /**
     * 大小
     */
    private long size;

    /**
     * 传输了的大小
     */
    private long transferred;

    /**
     * 传输结果
     */
    private Result result;

    /**
     * 错误信息
     */
    private String exception;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTransferred() {
        return transferred;
    }

    public void setTransferred(long transferred) {
        this.transferred = transferred;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }
}
