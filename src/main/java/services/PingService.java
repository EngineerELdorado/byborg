package services;

import dtos.ErrorReportDto;
import dtos.PingResultResource;
import exceptions.FriendlyException;
import threads.PingerCallable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PingService {

    private final Logger LOGGER;
    private final String[] hosts;
    private final Properties properties;
    private final ExecutorService executor;
    private final Map<String, String> localDB;
    private final Map<String, PingResultResource> fullResults;

    public PingService(Properties properties, Map<String, String> localDB) {

        this.localDB = localDB;
        this.fullResults = new HashMap<>();
        this.properties = properties;
        this.hosts = properties.getProperty("hosts").split(",");
        this.executor = Executors.newFixedThreadPool(hosts.length);

        this.LOGGER = Logger.getLogger(PingService.class.getName());
        FileHandler handler;
        try {
            handler = new FileHandler(properties.getProperty("log-file"));
        } catch (IOException e) {
            throw new FriendlyException(e.getMessage());
        }

        handler.setLevel(Level.WARNING);
        LOGGER.addHandler(handler);
    }

    /**
     * Here we loop through our configured list of hosts. for each hot we send a new Callable task to the
     * executor service. Once all the hosts have finished their execution, we clear their local generated data
     * we return the aggregated result for further processing.
     * @return resultMap
     */
    public Map<String, PingResultResource> pingHosts() {

        for (String host : hosts) {
            try {
                ErrorReportDto errorReportDto = new ErrorReportDto();
                executor.submit(new PingerCallable(host, properties, fullResults,
                        errorReportDto, LOGGER, localDB)).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new FriendlyException(e.getMessage());
            }
        }

        localDB.clear();
        return fullResults;
    }
}
