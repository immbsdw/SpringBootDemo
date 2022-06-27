package cn.com.demo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpTResult<T> {
    /**
     * 是否成功
     */
    private boolean success = true;

    /**
     * 错误信息
     */
    private String errorInfo;

    /**
     * 内部异常
     */
    private Exception exception;

    /**
     * result
     */
    private T result = null;

    /**
     * 是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * 设置错误信息
     *
     * @param errorInfo 错误信息
     */
    public void setErrorInfo(String errorInfo) {
        this.success = false;
        this.errorInfo = errorInfo;
    }

    /**
     * 设置错误信息
     *
     * @param errorInfo 错误信息
     * @param exception
     */
    public void setErrorInfo(String errorInfo, Exception exception) {
        this.setErrorInfo(errorInfo);
        this.exception = exception;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    public String getErrorInfo() {
        return this.errorInfo;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    public Exception getException() {
        return this.exception;
    }

    /**
     * 获取结果信息
     *
     * @return 结果信息
     */
    public T getResult() {
        return this.result;
    }

    /**
     * 设置结果信息
     *
     * @param tInfo 结果信息
     */
    public void setResult(T tInfo) {
        this.success = true;
        this.result = tInfo;
    }

    /**
     * 是否成功
     */
    public OpTResult() {

    }

    @Override
    public String toString() {
        return "OpTResult{" +
                "success=" + success +
                ", errorInfo='" + errorInfo + '\'' +
                ", exception=" + exception +
                ", result=" + result +
                '}';
    }
}
