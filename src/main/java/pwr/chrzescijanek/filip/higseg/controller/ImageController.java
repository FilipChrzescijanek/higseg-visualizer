package pwr.chrzescijanek.filip.higseg.controller;

import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static pwr.chrzescijanek.filip.higseg.util.Utils.startTask;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.google.gson.Gson;

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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import pwr.chrzescijanek.filip.higseg.util.StageUtils;
import pwr.chrzescijanek.filip.higseg.util.StatsDto;
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
   
	@FXML MenuItem fileMenuExportToPng;
	@FXML MenuItem fileMenuSaveStats;
	@FXML GridPane root;
    @FXML MenuBar menuBar;
    @FXML Menu fileMenu;
    @FXML MenuItem fileMenuExit;
    @FXML ColorPicker picker;
    @FXML Menu editMenu;
	@FXML MenuItem alignMenuLoadImages;
    @FXML MenuItem editMenuZoomIn;
    @FXML MenuItem editMenuZoomOut;
    @FXML CheckMenuItem editMenuCells;
	@FXML Menu optionsMenu;
	@FXML Menu optionsMenuTheme;
	@FXML RadioMenuItem optionsMenuThemeDark;
	@FXML ToggleGroup themeToggleGroup;
	@FXML RadioMenuItem optionsMenuThemeLight;
	@FXML HBox modeBox;
	@FXML Menu helpMenu;
	@FXML MenuItem helpMenuHelp;
	@FXML MenuItem helpMenuAbout;
    @FXML BorderPane borderPane;
	@FXML HBox alignTopHBox;
	@FXML Label alignInfo;
	@FXML ScrollPane alignScrollPane;
	@FXML Group alignImageViewGroup;
	@FXML AnchorPane alignImageViewAnchor;
	@FXML ImageView alignImageView;
	@FXML GridPane alignBottomGrid;
	@FXML Label alignImageSizeLabel;
	@FXML ComboBox<String> alignScaleCombo;
	@FXML Label alignMousePositionLabel;
	@FXML CheckBox showCells;

	@FXML
	void about() {
		final Alert alert = StageUtils.getAboutDialog();
		final DialogPane dialogPane = alert.getDialogPane();
		injectStylesheets(dialogPane);
		alert.show();
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
		}
		else {
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
	
	private void validateVerticalScrollbar(final ImageView imageView, final ScrollPane imageScrollPane, final double
			scale, final double
			                                       oldScale, final double vValue) {
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
	
	private double calculateValue(final double scale, final double oldScale, final double value, final double
			oldDenominator, final double newDenominator) {
		return ((scale - 1) + (value * oldDenominator - (oldScale - 1)) / oldScale * scale) / newDenominator;
	}
	
	@FXML
	void zoomOut() {
		updateScrollbars(alignImageView, alignScrollPane, -1);
	}

	@FXML
	void loadImages() {
		final File selectedFile = Utils.getLoadImageFile(root.getScene().getWindow());
		if (selectedFile != null) {
			final Task<? extends Void> task = createLoadImagesTask(selectedFile);
			startTask(task);
		}
	}
	
	private Task<? extends Void> createLoadImagesTask(final File selectedFile) {
		final Stage dialog = showPopup("Loading images...");
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				loadImages(selectedFile);
				Platform.runLater(() -> dialog.close());
				return null;
			}
		};
	}

	private Stage showPopup(final String info) {
		final Stage dialog = StageUtils.initDialog(root.getScene().getWindow());
		final HBox box = Utils.getHBoxWithLabelAndProgressIndicator(info);
		final Scene scene = new Scene(box);
		injectStylesheets(box);
		dialog.setScene(scene);
		dialog.show();
		return dialog;
	}
	
	private void loadImages(final File selectedFile) {
			final String filePath;
			try {
				filePath = selectedFile.getCanonicalPath();
				final Mat image = getImage(filePath);
				final Mat cells = saveStats(filePath, image);
				final Mat currentImage = this.rawImage.isNull().get() ? new Mat(0, 0, CvType.CV_8UC3) : this.rawImage.get();
				final Mat currentCells = this.cells.isNull().get() ? new Mat(0, 0, CvType.CV_8UC3) : this.cells.get();
				final Mat newImage = new Mat(Math.max(currentImage.rows(), image.rows()), 
						Math.max(currentImage.cols(), image.cols()), currentImage.type());
				final Mat newCells = new Mat(Math.max(currentCells.rows(), cells.rows()), 
						Math.max(currentCells.cols(), cells.cols()), currentCells.type());
				
				final byte[] newData = initializeNewData(newImage);
				final byte[] newCellsData = initializeNewData(newCells);
				
				final int newWidth = newImage.width();
				
				paintCurrentImage(currentImage, newData, newWidth);				
				paintNewImage(image, newData, newWidth);

				paintCurrentImage(currentCells, newCellsData, newWidth);				
				paintNewImage(cells, newCellsData, newWidth);
				
				newImage.put(0, 0, newData);
				this.rawImage.set(newImage);
				newCells.put(0, 0, newCellsData);
				this.cells.set(newCells);
				Platform.runLater(() -> setImage());
			} catch (IOException | CvException e) {
				handleException(e,
				                "Loading failed!\nImages might be corrupted, paths may contain non-ASCII symbols or "
				                + "you do not have sufficient read permissions.");
			}
	}

	private byte[] initializeNewData(final Mat newImage) {
		final byte[] newData = new byte[(int) newImage.total() * 3];
		
		for (int i = 0; i < newData.length; i++) {
			newData[i] = (byte) 255;
		}
		
		return newData;
	}
	

	private void paintCurrentImage(final Mat currentImage, final byte[] newData, final int newWidth) {
		final byte[] currentData = new byte[(int) currentImage.total() * 3];
		currentImage.get(0, 0, currentData);
		
		final int currentWidth = currentImage.width();
		
		for (int i = 0; i < currentImage.rows(); i++) {
			for (int j = 0; j < currentImage.cols(); j++) {
				byte b = currentData[(i * currentWidth + j) * 3];
				byte g = currentData[(i * currentWidth + j) * 3 + 1];
				byte r = currentData[(i * currentWidth + j) * 3 + 2];
				newData[(i * newWidth + j) * 3] = b;
				newData[(i * newWidth + j) * 3 + 1] = g; 
				newData[(i * newWidth + j) * 3 + 2] = r;
			}
		}
	}

	private void paintNewImage(final Mat image, final byte[] newData, final int newWidth) {
		final int width = image.width();
		
		Color c = picker.getValue();
		double defaultB = c.getBlue() * 255;
		double defaultG = c.getGreen() * 255;
		double defaultR = c.getRed() * 255;

		final byte[] data = new byte[(int) image.total()];				
		image.get(0, 0, data);				
		for (int i = 0; i < image.rows(); i++) {
			for (int j = 0; j < image.cols(); j++) {
				double ratio = 1.0 - Byte.toUnsignedInt(data[(i * width + j)]) / 255.0;
				newData[(i * newWidth + j) * 3 + 0] = (byte) (ratio * defaultB + (1.0 - ratio) * newData[(i * newWidth + j) * 3 + 0]);
				newData[(i * newWidth + j) * 3 + 1] = (byte) (ratio * defaultG + (1.0 - ratio) * newData[(i * newWidth + j) * 3 + 1]); 
				newData[(i * newWidth + j) * 3 + 2] = (byte) (ratio * defaultR + (1.0 - ratio) * newData[(i * newWidth + j) * 3 + 2]);
			}
		}
	}
	
	private Mat getImage(final String filePath) {
		final Mat image = Imgcodecs.imread(filePath, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
		if (image.dataAddr() == 0)
			throw new CvException("Failed to load image! Check if file path contains only ASCII symbols");
		return image;
	}

	private Mat saveStats(final String filePath, final Mat image) {
		Mat result = new Mat();
        Imgproc.threshold(image, result, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
		Imgproc.dilate(result, result, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3)), new Point(3.0/2, 3.0/2), 6);
        Imgproc.erode(result, result, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3)), new Point(3.0/2, 3.0/2), 4);
        Mat inverted = new Mat();
        Core.bitwise_not(result, inverted);
        imageStats.put(filePath, Imgproc.connectedComponents(inverted, new Mat()));
        logInfo(filePath);
        return result;
	}

	@FXML
    void saveStats() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(Utils.getTxtFile(root.getScene().getWindow())))) {
        	double sum = imageStats.values().stream().mapToDouble(Integer::doubleValue).sum();
        	Map<String, Double> cellRatios = imageStats.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / sum));
			bw.write(new Gson().toJson(new StatsDto(imageStats, cellRatios)));
        } catch (IOException e) {
        	handleException(e, "Save failed! Check your write permissions.");
		}
    }

	private void logInfo(final String filePath) {
		double sum = imageStats.values().stream().mapToDouble(Integer::doubleValue).sum();
		LOGGER.info("Loaded image: " + filePath);
        LOGGER.info("Cells: " + imageStats);
        LOGGER.info("Percentage: " + imageStats.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / sum)));
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

	@FXML
	void clear() {
        rawImage.set(null);
        cells.set(null);
        alignImageSizeLabel.setText("");
        imageStats.clear();
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
        ObjectBinding<Image> rawImageBinding = Bindings.createObjectBinding(() -> rawImage.isNull().get() ? null : createImage(rawImage.get()), rawImage);
        ObjectBinding<Image> cellsBinding = Bindings.createObjectBinding(() -> cells.isNull().get() ? null : createImage(cells.get()), cells);
		fxRawImage.bind(rawImageBinding);
		fxCells.bind(cellsBinding);
	}
	
	private void initializeComponents(final URL location, final ResourceBundle resources) {
		bindScrollPaneSize();
		initializeStyle();
		initializeComboBoxes();
		picker.setValue(Color.BLACK);
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
		alignScaleCombo.itemsProperty().get().addAll(
			"25%", "50%", "75%", "100%", "125%", "150%", "175%", "200%", "250%", "500%", "1000%"
		);
		alignScaleCombo.setValue("100%");
	}
	
	private void initializeStyle() {
		injectStylesheets(root);
		if (isLightThemeSelected()) {
			themeToggleGroup.selectToggle(optionsMenuThemeLight);
		}
		else {
			themeToggleGroup.selectToggle(optionsMenuThemeDark);
		}
	}
	
	private void setImageViewControls(final ImageView imageView, final ScrollPane imageScrollPane,
	                                  final Group imageViewGroup, final ComboBox<String> scaleCombo,
	                                  final Label mousePositionLabel) {
		setImageViewGroupListeners(imageView, imageScrollPane, imageViewGroup, mousePositionLabel);
		setImageScrollPaneEventFilter(imageView, imageScrollPane);
		setImageViewScaleListener(imageView, imageScrollPane, scaleCombo);
		setComboBoxListener(imageView, scaleCombo);
	}
	
	private void setImageViewGroupListeners(final ImageView imageView, final ScrollPane imageScrollPane,
	                                        final Group imageViewGroup, final Label mousePositionLabel) {
		imageViewGroup.setOnMouseMoved(event -> mousePositionLabel.setText(
				(((int) event.getX())) + " : " + (((int) event.getY()))));
		imageViewGroup.setOnMouseExited(event -> mousePositionLabel.setText("- : -"));
		imageViewGroup.setOnMouseDragged(event -> {
			mousePositionLabel.setText(
					(((int) event.getX())) + " : " + (((int) event.getY())));
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
	
	private void updateScrollbars(final ImageView imageView, final ScrollPane imageScrollPane,
	                              final double oldScale, final double hValue, final double vValue, final double
			                              scale) {
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
		fileMenuSaveStats.disableProperty().bind(alignImageIsPresent.not());
		editMenuZoomIn.disableProperty().bind(alignImageIsPresent.not());
		editMenuZoomOut.disableProperty().bind(alignImageIsPresent.not());
		editMenuCells.disableProperty().bind(alignImageIsPresent.not());
	}
	
	void setImage() {
        Mat mat = showCells.isSelected() ? cells.get() : rawImage.get();
        Image img = showCells.isSelected() ? fxCells.get() : fxRawImage.get();
		alignImageView.setImage(img);
        alignImageSizeLabel.setText(mat.width() + "x" + mat.height() + " px");
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

