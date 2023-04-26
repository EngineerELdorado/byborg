package dtos;

import java.time.LocalDateTime;
import java.util.List;

public record PingResultResource(LocalDateTime lastExecutionTime, List<String> imcpResults,
                                 List<ErrorReportDto> reportedErrors, TcpResult tcpResult, List<String> traceResults) {

}
