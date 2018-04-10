package pwr.chrzescijanek.filip.higseg.controller;

import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static pwr.chrzescijanek.filip.higseg.util.Utils.startTask;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import pwr.chrzescijanek.filip.fuzzyclassifier.Classifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestDataSet;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestRecord;
import pwr.chrzescijanek.filip.higseg.python.PortHolder;
import pwr.chrzescijanek.filip.higseg.util.Coordinates;
import pwr.chrzescijanek.filip.higseg.util.ModelData;
import pwr.chrzescijanek.filip.higseg.util.ModelDto;
import pwr.chrzescijanek.filip.higseg.util.Utils;

/**
 * Application controller class.
 */
public class ImageController extends BaseController implements Initializable {

	private static final Logger LOGGER = Logger.getLogger(ImageController.class.getName());

	private final ObjectProperty<Mat> rawImage = new SimpleObjectProperty<>();
	private final ObjectProperty<Mat> cells = new SimpleObjectProperty<>();
	private final ObjectProperty<Image> fxRawImage = new SimpleObjectProperty<>();
	private final ObjectProperty<Image> fxCells = new SimpleObjectProperty<>();
	private final Map<String, Integer> imageStats = new HashMap<>();

	@FXML
	MenuItem fileMenuExportToPng;
	@FXML
	GridPane root;
	@FXML
	MenuBar menuBar;
	@FXML
	Menu fileMenu;
	@FXML
	MenuItem fileMenuExit;
	@FXML
	Menu editMenu;
	@FXML
	MenuItem alignMenuLoadImages;
	@FXML
	MenuItem editMenuZoomIn;
	@FXML
	MenuItem editMenuZoomOut;
	@FXML
	CheckMenuItem editMenuCells;
	@FXML
	HBox modeBox;
	@FXML
	Menu helpMenu;
	@FXML
	MenuItem helpMenuHelp;
	@FXML
	MenuItem helpMenuAbout;
	@FXML
	BorderPane borderPane;
	@FXML
	HBox alignTopHBox;
	@FXML
	Label alignInfo;
	@FXML
	ScrollPane alignScrollPane;
	@FXML
	Group alignImageViewGroup;
	@FXML
	AnchorPane alignImageViewAnchor;
	@FXML
	ImageView alignImageView;
	@FXML
	GridPane alignBottomGrid;
	@FXML
	Label alignImageSizeLabel;
	@FXML
	ComboBox<String> alignScaleCombo;
	@FXML
	Label alignMousePositionLabel;
	@FXML
	CheckBox showCells;
	@FXML
	TextArea stats;
	
	public Map<String, Integer> getImageStats() {
		return imageStats;
	}
	
	public String getTitle() {
		return ((Stage) root.getScene().getWindow()).getTitle();
	}

	@FXML
	void showCells() {
		setImage();
	}

	@FXML
	void applyDarkTheme() {
		setDarkTheme();
	}

	@FXML
	void applyLightTheme() {
		setLightTheme();
	}

	@FXML
	void exit() {
		root.getScene().getWindow().hide();
	}

	@FXML
	void zoomIn() {
		updateScrollbars(alignImageView, alignScrollPane, 1);
	}

	private void updateScrollbars(final ImageView imageView, final ScrollPane imageScrollPane, final double deltaY) {
		final double oldScale = imageView.getScaleX();
		final double hValue = imageScrollPane.getHvalue();
		final double vValue = imageScrollPane.getVvalue();
		if (deltaY > 0) {
			imageView.setScaleX(imageView.getScaleX() * 1.05);
		} else {
			imageView.setScaleX(imageView.getScaleX() / 1.05);
		}
		final double scale = imageView.getScaleX();
		validateScrollbars(imageView, imageScrollPane, scale, oldScale, hValue, vValue);
	}

	private void validateScrollbars(final ImageView imageView, final ScrollPane imageScrollPane, final double scale,
			final double oldScale, final double hValue, final double vValue) {
		validateHorizontalScrollbar(imageView, imageScrollPane, scale, oldScale, hValue);
		validateVerticalScrollbar(imageView, imageScrollPane, scale, oldScale, vValue);
	}

	private void validateHorizontalScrollbar(final ImageView imageView, final ScrollPane imageScrollPane,
			final double scale, final double oldScale, final double hValue) {
		if ((scale * imageView.getImage().getWidth() > imageScrollPane.getWidth())) {
			final double oldHDenominator = calculateDenominator(oldScale, imageView.getImage().getWidth(),
					imageScrollPane.getWidth());
			final double newHDenominator = calculateDenominator(scale, imageView.getImage().getWidth(),
					imageScrollPane.getWidth());
			imageScrollPane.setHvalue(calculateValue(scale, oldScale, hValue, oldHDenominator, newHDenominator));
		}
	}

	private void validateVerticalScrollbar(final ImageView imageView, final ScrollPane imageScrollPane,
			final double scale, final double oldScale, final double vValue) {
		if ((scale * imageView.getImage().getHeight() > imageScrollPane.getHeight())) {
			final double oldVDenominator = calculateDenominator(oldScale, imageView.getImage().getHeight(),
					imageScrollPane.getHeight());
			final double newVDenominator = calculateDenominator(scale, imageView.getImage().getHeight(),
					imageScrollPane.getHeight());
			imageScrollPane.setVvalue(calculateValue(scale, oldScale, vValue, oldVDenominator, newVDenominator));
		}
	}

	private double calculateDenominator(final double scale, final double imageSize, final double paneSize) {
		return (scale * imageSize - paneSize) * 2 / paneSize;
	}

	private double calculateValue(final double scale, final double oldScale, final double value,
			final double oldDenominator, final double newDenominator) {
		return ((scale - 1) + (value * oldDenominator - (oldScale - 1)) / oldScale * scale) / newDenominator;
	}

	@FXML
	void zoomOut() {
		updateScrollbars(alignImageView, alignScrollPane, -1);
	}

	void addImage(final Mat image, final String filePath, List<ModelData> models, Stage newStage,
			DialogListener dialogListener) {
		final byte[] data = new byte[(int) image.total() * 3];
		image.get(0, 0, data);

		for (ModelData m : models) {
			final Mat cells = getCells(image, m.getModel(), m.getName());
			paint(cells, data, m.getColor());
		}

		final Mat newImage = new Mat(image.size(), image.type());
		newImage.put(0, 0, data);

		this.rawImage.set(image);
		this.cells.set(newImage);
		Platform.runLater(() -> {
			setImage();
			logInfo(filePath);
			newStage.show();
			dialogListener.decrement();
		});
	}

	private void paint(final Mat image, final byte[] newData, Color c) {
		double defaultB = c.getBlue() * 255;
		double defaultG = c.getGreen() * 255;
		double defaultR = c.getRed() * 255;

		final byte[] data = new byte[(int) image.total()];
		image.get(0, 0, data);

		int width = image.width();

		for (int i = 0; i < image.rows(); i++) {
			for (int j = 0; j < image.cols(); j++) {
				if (Byte.toUnsignedInt(data[(i * width + j)]) > 0) {
					newData[(i * width + j) * 3 + 0] = (byte) defaultB;
					newData[(i * width + j) * 3 + 1] = (byte) defaultG;
					newData[(i * width + j) * 3 + 2] = (byte) defaultR;
				}
			}
		}
	}

	private Map<List<String>, TestRecord> getMapping(List<String> attributes,
			Map<List<String>, Set<Coordinates>> initialMappings) {
		Set<List<String>> uniqueValues = initialMappings.keySet();
		return Utils.getMapping(attributes, uniqueValues);
	}

	private Mat process(Mat image, Map<TestRecord, Set<Coordinates>> mapping, String morphOperations) {
		Mat result = Utils.createMat(image, mapping);
		Imgproc.threshold(result, result, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
		for (char c : morphOperations.toCharArray()) {
			if (c == 'e') {
				Imgproc.erode(result, result, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3)),
						new Point(3.0 / 2, 3.0 / 2), 1);
			} else if (c == 'd') {
				Imgproc.dilate(result, result, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3)),
						new Point(3.0 / 2, 3.0 / 2), 1);
			}
		}
		return result;
	}

	private Mat getCells(final Mat image, final ModelDto model, final String modelName) {
		Classifier c = Utils.getClassifier(model);

		List<String> attributes = Arrays.asList("Hue", "Saturation", "Value");
		Map<List<String>, Set<Coordinates>> initialMappings = Utils.getInitialMapping(image);

		Map<List<String>, TestRecord> mapping = getMapping(attributes, initialMappings);
		List<TestRecord> uniqueTestRecords = new ArrayList<>(mapping.values());

		c.test(new TestDataSet(attributes, uniqueTestRecords));

		Mat result = process(image, initialMappings.entrySet().parallelStream()
				.collect(Collectors.toMap(e -> mapping.get(e.getKey()), e -> e.getValue())), model.getMorphOps());

		Mat labels = new Mat();
		Mat inverted = new Mat();
		Core.bitwise_not(result, inverted);
		clearBorders(inverted);
		int connectedComponents = Imgproc.connectedComponents(inverted, labels) - 1;

		Map<Integer, int[]> stats = new HashMap<>();

		for (int i = 0; i < connectedComponents; i++) {
			int[] array = new int[] {-1, -1, -1, -1};
			stats.put(i, array);
		}

		final int[] currentData = new int[(int) labels.total()];
		labels.get(0, 0, currentData);

		int width = labels.width();

		for (int i = 0; i < labels.rows(); i++) {
			for (int j = 0; j < labels.cols(); j++) {
				int label = currentData[(i * width + j)] - 1;
				if (label >= 0) {
					if (stats.get(label)[0] < 0 || stats.get(label)[0] > j) {
						stats.get(label)[0] = j;
					}
					if (stats.get(label)[1] < j) {
						stats.get(label)[1] = j;
					}
					if (stats.get(label)[2] < 0 || stats.get(label)[2] > i) {
						stats.get(label)[2] = i;
					}
					if (stats.get(label)[3] < i) {
						stats.get(label)[3] = i;
					}
				}
			}
		}

		int margin = 5;

		Map<Integer, Mat> images = new HashMap<>();
		stats.forEach((k, v) -> {
			Mat mat = new Mat(v[3] - v[2] + 1 + 2 * margin, v[1] - v[0] + 1 + 2 * margin, CvType.CV_8UC1,
					new Scalar(0));
			images.put(k, mat);
		});

		for (int i = 0; i < labels.rows(); i++) {
			for (int j = 0; j < labels.cols(); j++) {
				int label = currentData[(i * width + j)] - 1;
				if (label >= 0) {
					int startRow = stats.get(label)[2];
					int startCol = stats.get(label)[0];
					images.get(label).put(i - startRow + margin, j - startCol + margin, new byte[] { -1 });
				}
			}
		}

		List<Mat> imgs = new ArrayList<>(images.values());

		int size = 56;
		List<Mat> newImages = new ArrayList<>();
		imgs.forEach(v -> {
			double w = v.width();
			double h = v.height();
			double scale = w > h ? size / w : size / h;
			Imgproc.resize(v, v, new Size(), scale, scale, Imgproc.INTER_CUBIC);
			Mat newImage = new Mat(size, size, CvType.CV_8UC1, new Scalar(0));
			int newW = v.width();
			int newH = v.height();
			v.copyTo(newImage.rowRange((size - newH) / 2, (size - newH) / 2 + newH).colRange((size - newW) / 2,
					(size - newW) / 2 + newW));

			newImages.add(newImage);
		});

		imageStats.put(modelName, quantify(newImages));
		return inverted;
	}

	private void clearBorders(Mat inverted) {
		int totalRows = inverted.rows();
		int totalCols = inverted.cols();
		for (int i = 0; i < totalRows; i++) {
			Imgproc.floodFill(inverted, new Mat(), new Point(0, i), new Scalar(0));
			Imgproc.floodFill(inverted, new Mat(), new Point(totalCols - 1, i), new Scalar(0));
		}
		for (int i = 0; i < totalCols; i++) {
			Imgproc.floodFill(inverted, new Mat(), new Point(i, 0), new Scalar(0));
			Imgproc.floodFill(inverted, new Mat(), new Point(i, totalRows - 1), new Scalar(0));
		}
	}

	private Integer quantify(List<Mat> newImages) {
		String tmp = "tmp" + new Date().getTime();
		String dirName = tmp + "/unknown";
		saveImages(newImages, dirName);
		int quantity = callPython(newImages, tmp);
		cleanUp(tmp);
		return quantity;
	}

	private void saveImages(List<Mat> newImages, String dirName) {
		File dir = new File(dirName);
		dir.mkdirs();
		for (int i = 0; i < newImages.size(); i++) {
			Imgcodecs.imwrite(dirName + "/image_" + i + ".png", newImages.get(i));
		}
	}

	private int callPython(List<Mat> newImages, String tmp) {
		int quantity = newImages.size();
		try {
			String line = sendGet(tmp);
			quantity = Integer.parseInt(line);
		} catch (IOException | IllegalStateException e) {
			handleException(e, "Connection to python server failed! Check if all of the dependencies were installed properly.");
		}
		return quantity;
	}
	
	private String sendGet(String dir) throws IOException {
		if (PortHolder.INSTANCE.getPort() < 0) {
			throw new IllegalStateException("Server is not ready!");
		}
		String url = "http://localhost:" + PortHolder.INSTANCE.getPort() + "/?dir=" + dir;
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		int responseCode = con.getResponseCode();
		if (responseCode != 200) {
			throw new IllegalStateException("Something went wrong during GET request processing!");
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return response.toString();
	}

	private void cleanUp(String tmp) {
		Path root = Paths.get(tmp);
		try {
			Files.walk(root).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		} catch (IOException e) {
			handleException(e, "Deleting temporary directory failed! You may have to clean-up manually.");
		}
	}

	private void logInfo(final String filePath) {
		double sum = imageStats.values().stream().mapToDouble(Integer::doubleValue).sum();
		LOGGER.info("Loaded image: " + filePath);
		StringBuilder sb = new StringBuilder();
		imageStats.forEach((k, v) -> {
			String formatted = String.format("%.2f", (v / sum) * 100).replaceFirst("\\.?0*$", "");
			String row = String.format("%s: %d (%s%%)", k.substring(k.lastIndexOf(File.separator) + 1), v, formatted);
			LOGGER.info(row);
			sb.append(row);
			sb.append(System.lineSeparator());
		});
		stats.setText(sb.toString());
	}

	@FXML
	void exportToPng() {
		final File selectedFile = Utils.getImageFile(root.getScene().getWindow());
		final Task<? extends Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				writeImage(selectedFile);
				return null;
			}
		};
		startTask(task);
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		initializeComponents(location, resources);
		setBindings();
		addListeners();
	}

	private void addListeners() {
		addOnMouseReleasedListeners();
		setImageViewControls(alignImageView, alignScrollPane, alignImageViewGroup, alignScaleCombo,
				alignMousePositionLabel);
	}

	private void setBindings() {
		setVisibilityBindings();
		ObjectBinding<Image> rawImageBinding = Bindings
				.createObjectBinding(() -> rawImage.isNull().get() ? null : createImage(rawImage.get()), rawImage);
		ObjectBinding<Image> cellsBinding = Bindings
				.createObjectBinding(() -> cells.isNull().get() ? null : createImage(cells.get()), cells);
		fxRawImage.bind(rawImageBinding);
		fxCells.bind(cellsBinding);
	}

	private void initializeComponents(final URL location, final ResourceBundle resources) {
		bindScrollPaneSize();
		initializeStyle();
		initializeComboBoxes();
		showCells.selectedProperty().bindBidirectional(editMenuCells.selectedProperty());
	}

	private void bindScrollPaneSize() {
		alignScrollPane.prefHeightProperty().bind(root.heightProperty());
		alignScrollPane.prefWidthProperty().bind(root.widthProperty());
	}

	private void addOnMouseReleasedListeners() {
		root.setOnMouseReleased(event -> {
			alignImageViewGroup.getScene().setCursor(Cursor.DEFAULT);
		});
	}

	private void initializeComboBoxes() {
		initializeScaleComboBoxes();
	}

	private void initializeScaleComboBoxes() {
		alignScaleCombo.itemsProperty().get().addAll("25%", "50%", "75%", "100%", "125%", "150%", "175%", "200%",
				"250%", "500%", "1000%");
		alignScaleCombo.setValue("100%");
	}

	private void initializeStyle() {
		injectStylesheets(root);
	}

	private void setImageViewControls(final ImageView imageView, final ScrollPane imageScrollPane,
			final Group imageViewGroup, final ComboBox<String> scaleCombo, final Label mousePositionLabel) {
		setImageViewGroupListeners(imageView, imageScrollPane, imageViewGroup, mousePositionLabel);
		setImageScrollPaneEventFilter(imageView, imageScrollPane);
		setImageViewScaleListener(imageView, imageScrollPane, scaleCombo);
		setComboBoxListener(imageView, scaleCombo);
	}

	private void setImageViewGroupListeners(final ImageView imageView, final ScrollPane imageScrollPane,
			final Group imageViewGroup, final Label mousePositionLabel) {
		imageViewGroup.setOnMouseMoved(
				event -> mousePositionLabel.setText((((int) event.getX())) + " : " + (((int) event.getY()))));
		imageViewGroup.setOnMouseExited(event -> mousePositionLabel.setText("- : -"));
		imageViewGroup.setOnMouseDragged(event -> {
			mousePositionLabel.setText((((int) event.getX())) + " : " + (((int) event.getY())));
		});
		imageViewGroup.setOnScroll(event -> {
			if (event.isControlDown() && imageView.getImage() != null) {
				final double deltaY = event.getDeltaY();
				updateScrollbars(imageView, imageScrollPane, deltaY);
			}
		});
	}

	private void setImageScrollPaneEventFilter(final ImageView imageView, final ScrollPane imageScrollPane) {
		imageScrollPane.addEventFilter(ScrollEvent.ANY, event -> {
			if (event.isControlDown() && imageView.getImage() != null) {
				final double deltaY = event.getDeltaY();
				updateScrollbars(imageView, imageScrollPane, deltaY);
				event.consume();
			}
		});
	}

	private void setImageViewScaleListener(final ImageView imageView, final ScrollPane imageScrollPane,
			final ComboBox<String> scaleCombo) {
		imageView.scaleXProperty().addListener((observable, oldValue, newValue) -> {
			final double oldScale = oldValue.doubleValue();
			final double hValue = imageScrollPane.getHvalue();
			final double vValue = imageScrollPane.getVvalue();
			final double scale = newValue.doubleValue();
			imageView.setScaleY(scale);
			setImageViewTranslates(imageView);
			updateScrollbars(imageView, imageScrollPane, oldScale, hValue, vValue, scale);
			updateComboBox(scaleCombo, newValue);
		});
	}

	private void setImageViewTranslates(final ImageView view) {
		view.setTranslateX(view.getImage().getWidth() * 0.5 * (view.getScaleX() - 1.0));
		view.setTranslateY(view.getImage().getHeight() * 0.5 * (view.getScaleY() - 1.0));
	}

	private void updateScrollbars(final ImageView imageView, final ScrollPane imageScrollPane, final double oldScale,
			final double hValue, final double vValue, final double scale) {
		if (Math.round(oldScale * 100) != Math.round(scale * 100)) {
			validateScrollbars(imageView, imageScrollPane, scale, oldScale, hValue, vValue);
		}
	}

	private void updateComboBox(final ComboBox<String> scaleCombo, final Number newValue) {
		final String asString = String.format("%.0f%%", newValue.doubleValue() * 100);
		if (!scaleCombo.getValue().equals(asString))
			scaleCombo.setValue(asString);
	}

	private void setComboBoxListener(final ImageView imageView, final ComboBox<String> scaleCombo) {
		scaleCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("[1-9]\\d*%"))
				scaleCombo.setValue(oldValue);
			else
				imageView.setScaleX(Double.parseDouble(newValue.substring(0, newValue.length() - 1)) / 100.0);
		});
	}

	private void setVisibilityBindings() {
		onAlignImageIsPresent();
	}

	private void onAlignImageIsPresent() {
		final BooleanBinding alignImageIsPresent = alignImageView.imageProperty().isNotNull();
		alignImageViewGroup.visibleProperty().bind(alignImageIsPresent);
		alignScaleCombo.visibleProperty().bind(alignImageIsPresent);
		alignMousePositionLabel.visibleProperty().bind(alignImageIsPresent);
		showCells.visibleProperty().bind(alignImageIsPresent);
		fileMenuExportToPng.disableProperty().bind(alignImageIsPresent.not());
		editMenuZoomIn.disableProperty().bind(alignImageIsPresent.not());
		editMenuZoomOut.disableProperty().bind(alignImageIsPresent.not());
		editMenuCells.disableProperty().bind(alignImageIsPresent.not());
	}

	void setImage() {
		Mat mat = showCells.isSelected() ? cells.get() : rawImage.get();
		Image img = showCells.isSelected() ? fxCells.get() : fxRawImage.get();
		alignImageView.setImage(img);
		if (mat != null) {
			alignImageSizeLabel.setText(mat.width() + "x" + mat.height() + " px");
		}
	}

	private Image createImage(final Mat image) {
		final MatOfByte byteMat = new MatOfByte();
		imencode(".png", image, byteMat);
		return new Image(new ByteArrayInputStream(byteMat.toArray()));
	}

	void writeImage(File selectedFile) {
		if (selectedFile != null) {
			final Task<Void> task = createWriteImagesTask(selectedFile);
			startTask(task);
		}
	}

	private Task<Void> createWriteImagesTask(final File selectedFile) {
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				try {
					Mat img = showCells.isSelected() ? cells.get() : rawImage.get();
					Utils.writeImage(img, selectedFile);
				} catch (final IOException e) {
					handleException(e, "Save failed! Check your write permissions.");
				}
				return null;
			}
		};
	}

}
