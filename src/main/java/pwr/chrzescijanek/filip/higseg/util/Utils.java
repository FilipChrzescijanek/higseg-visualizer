package pwr.chrzescijanek.filip.higseg.util;

import static org.opencv.imgcodecs.Imgcodecs.imwrite;

import java.io.File;
import java.io.IOException;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

/**
 * Provides utility methods for handling controllers.
 */
public final class Utils {

	private Utils() {}

	/**
	 * Writes image of sample to given directory.
	 *
	 * @param selectedDirectory directory
	 * @throws IOException if image could not be written
	 */
	public static void writeImage(final Mat image, final File selectedDirectory, final String title) throws IOException {
		imwrite(selectedDirectory.getCanonicalPath()
		        + File.separator + title, image);
	}
	
	public static void writeImage(final Mat image, final File selectedFile) throws IOException {
		imwrite(selectedFile.getCanonicalPath(), image);
	}
	
	public static void extractCells(Mat image, Mat result) {        
		Imgproc.threshold(image, result, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
		Imgproc.dilate(result, result, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3)), new Point(3.0/2, 3.0/2), 2);
	    Imgproc.erode(result, result, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3)), new Point(3.0/2, 3.0/2), 2);
		Imgproc.dilate(result, result, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3)), new Point(3.0/2, 3.0/2), 7);
	    Imgproc.erode(result, result, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)), new Point(5.0/2, 5.0/2), 3);
	}

	/**
	 * Shows file chooser dialog and gets CSV file.
	 *
	 * @param window application window
	 * @return CSV file
	 */
	public static File getTxtFile(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export statistics to TXT file");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Text files", "*.txt"));
		return fileChooser.showSaveDialog(window);
	}

	/**
	 * Shows file chooser dialog and gets CSV file.
	 *
	 * @param window application window
	 * @return CSV file
	 */
	public static File getImageFile(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save result");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.bmp", "*.tif"));
		return fileChooser.showSaveDialog(window);
	}

	/**
	 * Shows file chooser dialog and gets image files.
	 *
	 * @param window application window
	 * @return image files
	 */
	public static File getLoadImageFile(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Load images");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.bmp", "*.tif"));
		return fileChooser.showOpenDialog(window);
	}

	/**
	 * Shows file chooser dialog and gets image files.
	 *
	 * @param window application window
	 * @return image files
	 */
	public static File getModelFile(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Load model");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Model Files", "*.hgmodel"));
		return fileChooser.showOpenDialog(window);
	}

	/**
	 * Shows file chooser dialog and gets image files.
	 *
	 * @param window application window
	 * @return image files
	 */
	public static File saveModelFile(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save model");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Model Files", "*.hgmodel"));
		return fileChooser.showSaveDialog(window);
	}

	/**
	 * Shows directory chooser dialog and gets directory.
	 *
	 * @param window application window
	 * @return directory
	 */
	public static File getDirectory(final Window window) {
		final DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Choose directory");
		return chooser.showDialog(window);
	}

	/**
	 * @return help view
	 */
	public static WebView getHelpView() {
		final WebView view = new WebView();
		view.getEngine().load(Utils.class.getResource("/help.html").toExternalForm());
		return view;
	}

	/**
	 * @param info label
	 * @return customized, centered horizontal box with given label and progress indicator
	 */
	public static HBox getHBoxWithLabelAndProgressIndicator(final String info) {
		final Label label = new Label(info);
		label.setAlignment(Pos.CENTER);
		final HBox box = new HBox(label, new ProgressIndicator(-1.0));
		box.setSpacing(30.0);
		box.setAlignment(Pos.CENTER);
		box.setPadding(new Insets(25));
		box.getStyleClass().add("modal-dialog");
		return box;
	}

	/**
	 * Starts given task
	 *
	 * @param task task to start
	 */
	public static void startTask(final Task<? extends Void> task) {
		final Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();
	}

	/**
	 * @param color JavaFX color
	 * @return given color in web color format
	 */
	public static String getWebColor(final Color color) {
		return String.format("#%02X%02X%02X%02X",
		                     (int) (color.getRed() * 255),
		                     (int) (color.getGreen() * 255),
		                     (int) (color.getBlue() * 255),
		                     (int) (color.getOpacity() * 255));
	}

}
