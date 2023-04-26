import dtos.PingResultResource;
import exceptions.FriendlyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.PingService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;

class PingServiceAppTest {

    private PingService pingService;

    @BeforeEach
    void init() {
        Map<String, String> localDB = new Hashtable<>();
        Properties props = new Properties();

        try {
            try (InputStream input = getClass().getResourceAsStream("config.properties")) {
                props.load(input);
            }
        } catch (IOException ex) {
            fail("Test failed. IO Exception occurred while reading the input stream");
        }
        pingService = new PingService(props, localDB);
    }

    @Test
    void testPinging() {

        //Arrange (Done in the @BeforeAll method)

        //Act
        Map<String, PingResultResource> pingResult = pingService.pingHosts();

        //Assert
        Assertions.assertAll(() -> {

            Assertions.assertFalse(pingResult.values().isEmpty());

            Assertions.assertDoesNotThrow(() -> {
                Collection<PingResultResource> entries = pingResult.values();
                for (PingResultResource result : entries) {
                    if (result.imcpResults().isEmpty()) {
                        throw new FriendlyException("Test failed.No Results was returned for ICMP pings");
                    }
                    if (result.traceResults().isEmpty()) {
                        throw new FriendlyException("Test failed.No Results was returned for TRACE pings");
                    }
                    if (result.tcpResult() == null) {
                        throw new FriendlyException("Test failed. TCP ping didn't return any result");
                    }

                }
            });
        });
    }
}
