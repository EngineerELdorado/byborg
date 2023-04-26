package dtos;

public class TcpResult {

    private String url;
    private Long time;
    private int httpStatus;
    private String responseBody;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getUrl() {
        return url;
    }

    public Long getTime() {
        return time;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}
