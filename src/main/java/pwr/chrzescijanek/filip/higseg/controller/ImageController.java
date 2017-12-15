package pwr.chrzescijanek.filip.higseg.controller;

import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static pwr.chrzescijanek.filip.higseg.util.Utils.startTask;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

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
import javafx.scene.control.Alert;
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
import pwr.chrzescijanek.filip.higseg.util.StageUtils;
import pwr.chrzescijanek.filip.higseg.util.Utils;

/**
 * Application controller class.
 */
public class ImageController extends BaseController implements Initializable {

    private final ObjectProperty<Mat> image = new SimpleObjectProperty<>();
   
	@FXML MenuItem fileMenuExportToPng;
	@FXML GridPane root;
    @FXML MenuBar menuBar;
    @FXML Menu fileMenu;
    @FXML MenuItem fileMenuExit;
    @FXML ColorPicker picker;
    @FXML Menu editMenu;
	@FXML MenuItem alignMenuLoadImages;
    @FXML MenuItem editMenuZoomIn;
    @FXML MenuItem editMenuZoomOut;
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

	@FXML
	void about() {
		final Alert alert = StageUtils.getAboutDialog();
		final DialogPane dialogPane = alert.getDialogPane();
		injectStylesheets(dialogPane);
		alert.show();
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
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				loadImages(selectedFile);
				return null;
			}
		};
	}
	
	private void loadImages(final File selectedFile) {
			final String filePath;
			try {
				filePath = selectedFile.getCanonicalPath();
				final Mat image = getImage(filePath);
				final Mat currentImage = this.image.isNull().get() ? new Mat(0, 0, image.type()) : this.image.get();
				final Mat newImage = new Mat(Math.max(currentImage.rows(), image.rows()), 
						Math.max(currentImage.cols(), image.cols()), image.type());
				
				final byte[] newData = initializeNewData(newImage);
				final int newWidth = newImage.width();
				
				paintCurrentImage(currentImage, newData, newWidth);				
				paintNewImage(image, newData, newWidth);
				
				newImage.put(0, 0, newData);
				Platform.runLater(() -> setImage(newImage));
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
		byte defaultB = (byte) (c.getBlue() * 255);
		byte defaultG = (byte) (c.getGreen() * 255);
		byte defaultR = (byte) (c.getRed() * 255);

		final byte[] data = new byte[(int) image.total() * 3];				
		image.get(0, 0, data);				
		for (int i = 0; i < image.rows(); i++) {
			for (int j = 0; j < image.cols(); j++) {
				byte b = data[(i * width + j) * 3];
				byte g = data[(i * width + j) * 3 + 1];
				byte r = data[(i * width + j) * 3 + 2];
				if (b == 0 && g == 0 && r == 0) {
					newData[(i * newWidth + j) * 3] = b == 0 ? defaultB : newData[(i * newWidth + j) * 3];
					newData[(i * newWidth + j) * 3 + 1] = g == 0 ? defaultG : newData[(i * newWidth + j) * 3 + 1]; 
					newData[(i * newWidth + j) * 3 + 2] = r == 0 ? defaultR : newData[(i * newWidth + j) * 3 + 2];
				}
			}
		}
	}
	
	private Mat getImage(final String filePath) {
		final Mat image = Imgcodecs.imread(filePath, Imgcodecs.CV_LOAD_IMAGE_COLOR);
		if (image.dataAddr() == 0)
			throw new CvException("Failed to load image! Check if file path contains only ASCII symbols");
		return image;
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
        image.set(null);
        alignImageSizeLabel.setText("");
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
        ObjectBinding<Image> binding = Bindings.createObjectBinding(() -> image.isNull().get() ? null : createImage(image.get()), image);
		alignImageView.imageProperty().bind(binding);
	}
	
	private void initializeComponents(final URL location, final ResourceBundle resources) {
		bindScrollPaneSize();
		initializeStyle();
		initializeComboBoxes();
		picker.setValue(Color.BLACK);
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
		fileMenuExportToPng.disableProperty().bind(alignImageIsPresent.not());
		editMenuZoomIn.disableProperty().bind(alignImageIsPresent.not());
		editMenuZoomOut.disableProperty().bind(alignImageIsPresent.not());
	}
	
	void setImage(Mat img) {
        image.set(img);
        alignImageSizeLabel.setText(img.width() + "x" + img.height() + " px");
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
                    Utils.writeImage(image.get(), selectedFile);
                } catch (final IOException e) {
                    handleException(e, "Save failed! Check your write permissions.");
                }
                return null;
            }
        };
    }
    
}

