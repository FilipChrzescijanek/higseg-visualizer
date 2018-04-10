package pwr.chrzescijanek.filip.higseg.python;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

public class PyConsoleLogger {

    private final InputStream stream;
    private final CountDownLatch latch;
    
    public PyConsoleLogger(InputStream stream, CountDownLatch latch) {
        this.stream = stream;
        this.latch = latch;
    }

    public void startLog() {
        Runnable logging = () -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(getStream()))) {
                for (String line; (line = br.readLine()) != null;) {
                    checkResponse(line);
                }
            } catch (IOException e) {
				e.printStackTrace();
			}
        };

        Thread consoleLogger = new Thread(logging);
        consoleLogger.start();
    }

    private void checkResponse(final String message) {
        if (serverStarted(message) || serverNotFound(message)) {
        	getLatch().countDown();
        }
    }

    public InputStream getStream() {
		return stream;
	}

	public CountDownLatch getLatch() {
		return latch;
	}

	private boolean serverStarted(final String message) {
        return message.contains("Running on");
    }

    private boolean serverNotFound(final String message) {
        return message.contains("Errno 2");
    }


}