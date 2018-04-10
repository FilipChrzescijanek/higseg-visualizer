package pwr.chrzescijanek.filip.higseg;

import static pwr.chrzescijanek.filip.higseg.util.StageUtils.prepareStage;
import static pwr.chrzescijanek.filip.higseg.util.Utils.startTask;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import pwr.chrzescijanek.filip.higseg.controller.Controller;
import pwr.chrzescijanek.filip.higseg.inject.Injector;
import pwr.chrzescijanek.filip.higseg.python.PortHolder;
import pwr.chrzescijanek.filip.higseg.python.PyConsoleLogger;
import pwr.chrzescijanek.filip.higseg.view.FXView;

public class MainApplication extends Application {

	private Optional<Process> process = Optional.empty();

	/**
	 * Resets state on application stop.
	 *
	 * @throws Exception
	 *             unhandled exception
	 */
	@Override
	public void stop() throws Exception {
		Injector.reset();
		process.ifPresent(Process::destroyForcibly);
	}

	/**
	 * Prepares primary stage and shows GUI.
	 *
	 * @param primaryStage
	 *            application's primary stage
	 * @throws Exception
	 *             unhandled exception
	 * 
	 */
	@Override
	public void start(final Stage primaryStage) throws Exception {
		final FXView fxView = new FXView("/static/main.fxml");
		prepareStage(primaryStage, "higseg-visualizer", fxView);
		primaryStage.setOnCloseRequest(event -> Platform.exit());
		primaryStage.show();

		startServerTask(fxView);
	}

	private void startServerTask(final FXView fxView) {
		Controller c = (Controller) fxView.getController();
		Stage dialog = c.waitForServer();

		startTask(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				startServer();
				Platform.runLater(() -> dialog.close());
				return null;
			}
		});
	}

	private void startServer() throws IOException {
		Integer port = findOpenPort();
		ProcessBuilder pb = new ProcessBuilder("py", "quantify.py", port + "");
		process = Optional.of(pb.start());
		logConsole();
		PortHolder.INSTANCE.setPort(port);
	}

	private void logConsole() {
		process.ifPresent(p -> {
			final CountDownLatch latch = new CountDownLatch(1);
			new PyConsoleLogger(p.getInputStream(), latch).startLog();
			new PyConsoleLogger(p.getErrorStream(), latch).startLog();
			waitForServerResponse(latch);
		});
	}

	private void waitForServerResponse(final CountDownLatch latch) {
		try {
			latch.await(15, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static Integer findOpenPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

}
