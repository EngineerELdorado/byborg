package dtos;

import exceptions.FriendlyException;
import io.cucumber.core.internal.com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.core.internal.com.fasterxml.jackson.databind.ObjectMapper;

public class ErrorReportDto {

    private String host;
    private String icmpPing;
    private String tcpPing;
    private String trace;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIcmpPing() {
        return icmpPing;
    }

    public void setIcmpPing(String icmpPing) {
        this.icmpPing = icmpPing;
    }

    public String getTcpPing() {
        return tcpPing;
    }

    public void setTcpPing(String tcpPing) {
        this.tcpPing = tcpPing;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    @Override
    public String toString() {

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new FriendlyException("Error converting ReportDto to JSON string");
        }
    }
}
