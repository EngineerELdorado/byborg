package threads;

import dtos.ErrorReportDto;
import dtos.PingResultResource;
import dtos.TcpResult;
import exceptions.FriendlyException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.LocalDateTime.now;

public class PingerCallable implements Callable<Map<String, PingResultResource>> {

    private final String host;
    private final Logger logger;
    private final Properties properties;
    private final Map<String, String> localDB;
    private final ErrorReportDto errorReportDto;
    private final TcpResult tcpResult = new TcpResult();
    private final Map<String, PingResultResource> results;
    private final List<String> icmpResult = new ArrayList<>();
    private final List<String> traceResult = new ArrayList<>();
    private final List<ErrorReportDto> reportedErrors = new ArrayList<>();

    public PingerCallable(String currentHost, Properties properties, Map<String, PingResultResource> results,
                          ErrorReportDto errorReportDto, Logger logger, Map<String, String> localDB) {

        this.localDB = localDB;
        this.logger = logger;
        this.results = results;
        this.host = currentHost;
        this.properties = properties;
        this.errorReportDto = errorReportDto;
    }

    /**
     * We use here completeableFuture to make parallel execution of each protocol ping (ICMP, TCP,TRACEROUTE).
     * And we have to wait for all of them to complete before proceeding.
     *
     */
    @Override
    public Map<String, PingResultResource> call() {

        CompletableFuture.allOf(
                        CompletableFuture.runAsync(this::reachHostWithIcmp),
                        CompletableFuture.runAsync(this::reachHostWithTcpIp),
                        CompletableFuture.runAsync(this::reachHostWithTrace))
                .join();

        return results;
    }

    private void reachHostWithIcmp() {

        //Check if the current host is not currently executing with the icmp ping command
        if (localDB.get(host + "-icmp") == null) {

            String line;
            localDB.put(host + "-icmp", "ICMP-RUNNING");

            try {

                Process process = Runtime.getRuntime().exec(properties.getProperty("icmp") + " " + host);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                while ((line = reader.readLine()) != null) {

                    logger.log(Level.INFO, line);
                    icmpResult.add(line);

                    if (line.toLowerCase().contains("timeout")) {

                        logger.log(Level.SEVERE, String.format("Failure on %s. Cause: timeout ", host));
                        errorReportDto.setHost(host);
                        errorReportDto.setIcmpPing(line);

                        reportError(errorReportDto);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    String errorMessage = String.format("IMCP ping failed for host %s. for unknown reasons", host);
                    logger.log(Level.SEVERE, errorMessage);
                    errorReportDto.setHost(host);
                    errorReportDto.setIcmpPing(errorMessage);
                    reportError(errorReportDto);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                e.printStackTrace();
                throw new FriendlyException(e.getMessage());
            }

            updateResults();
        }
    }

    private void reachHostWithTcpIp() {

        if (localDB.get(host + "-tcp") == null) {
            localDB.put(host + "-tcp", "TCP-RUNNING");
            String hostTimeOut = properties.getProperty(host + "-timeout");
            if (hostTimeOut == null) {
                return;
            }
            int timeout = Integer.parseInt(properties.getProperty(host + "-timeout").replace(host, ""));

            try {
                URL url = new URL("http://" + host);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(timeout);

                long startTime = System.currentTimeMillis();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                long endTime = System.currentTimeMillis();
                long processingTime = endTime - startTime;

                tcpResult.setTime(processingTime);
                tcpResult.setHttpStatus(conn.getResponseCode());
                tcpResult.setUrl(host);
                tcpResult.setResponseBody(response.toString());
            } catch (Exception e) {

                String errorMsg = String.format("Failed to make TCP/IP ping to host %s via HTTP. Possible cause %s",
                        host, e.getMessage());

                errorReportDto.setHost(host);
                errorReportDto.setTcpPing(errorMsg);
                reportError(errorReportDto);
            }

            updateResults();
        }
    }

    private void reachHostWithTrace() {

        if (localDB.get(host + "-trace") == null) {
            localDB.put(host + "-trace", "TRACE-RUNNING");
            String eachLine;

            try {

                Process process = Runtime.getRuntime().exec(properties.getProperty("trace") + " " + host);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                while ((eachLine = reader.readLine()) != null) {

                    logger.log(Level.INFO, eachLine);
                    traceResult.add(eachLine);

                    if (eachLine.toLowerCase().contains("unknown host") ||
                            eachLine.toLowerCase().contains("error")) {

                        logger.severe(eachLine + " on host: " + host);
                        errorReportDto.setHost(host);
                        errorReportDto.setTrace(eachLine);
                        reportError(errorReportDto);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    String errorMessage = String.format("Trace unsuccessful for host %s", host);
                    logger.log(Level.SEVERE, errorMessage);
                    errorReportDto.setHost(host);
                    errorReportDto.setTrace(errorMessage);
                    reportError(errorReportDto);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                e.printStackTrace();
                throw new FriendlyException(e.getMessage());
            }

            updateResults();
        }
    }

    private void reportError(ErrorReportDto errorReportDto) {

        logger.log(Level.SEVERE, errorReportDto.toString());
        String reportServiceEndpoint = properties.getProperty("report-service-endpoint");
        reportedErrors.add(errorReportDto);

        URL urlObject = null;
        try {
            urlObject = new URL(reportServiceEndpoint);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (urlObject == null) {
            return;
        }
        HttpURLConnection connection = null;
        String httpErrorMsg = "Could not call the report service to submit the error";

        try {
            connection = (HttpURLConnection) urlObject.openConnection();
        } catch (IOException e) {
            logger.log(Level.INFO, httpErrorMsg);
        }
        if (connection == null) {
            return;
        }
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            logger.log(Level.INFO, httpErrorMsg);
        }
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(3000);

        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] input = errorReportDto.toString().getBytes(StandardCharsets.UTF_8);
            outputStream.write(input, 0, input.length);
        } catch (IOException e) {
            logHttpRequestFailure(reportServiceEndpoint, e);
        }

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                StandardCharsets.UTF_8))) {

            String response = bufferedReader.readLine();
            logger.log(Level.INFO, response);
        } catch (IOException e) {
            logger.log(Level.INFO, httpErrorMsg);
        }
    }

    private void logHttpRequestFailure(String reportServiceEndpoint, Exception e) {
        logger.log(Level.SEVERE, String.format("Reporting error %s. Possible cause %s",
                reportServiceEndpoint, e.getMessage()));
    }

    /**
     * This method update the results to be returned. It is synchronized to avoid being manipulated by multiple
     * threads at once as we are running our tasks in parallel tasks and the results need to be consistent and
     * reliably accurate.
     */
    private synchronized void updateResults() {
        results.put(host, new PingResultResource(now(), icmpResult, reportedErrors, tcpResult, traceResult));
    }
}
