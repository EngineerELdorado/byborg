import exceptions.FriendlyException;
import services.PingService;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.lang.Long.parseLong;

public class PingMain {
    static Properties props = new Properties();
    static Map<String, String> localFakeDB = new HashMap<>();

    /**
     * We start by loading the configurations into the Properties object
     * Then we start the scheduled job Execution
     *
     * @param args
     */
    public static void main(String[] args) {
        loadProperties();
        startTheJob();
    }

    private static void loadProperties() {

        try {
            try (InputStream input = PingMain.class.getResourceAsStream("config.properties")) {
                props.load(input);
            }
        } catch (IOException ex) {
            throw new FriendlyException("Failed to load properties");
        }
    }

    private static void startTheJob() {
        Timer timer = new Timer();
        PingService pingService = new PingService(props, localFakeDB);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                pingService.pingHosts();
            }
        };

        long delay = parseLong(props.getProperty("delay"));
        long period = parseLong(props.getProperty("period"));
        timer.scheduleAtFixedRate(task, delay, period);
    }
}
