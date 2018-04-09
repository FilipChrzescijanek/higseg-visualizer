package pwr.chrzescijanek.filip.higseg.controller;

import static pwr.chrzescijanek.filip.higseg.util.Utils.getImageFiles;
import static pwr.chrzescijanek.filip.higseg.util.Utils.startTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import pwr.chrzescijanek.filip.higseg.util.ModelData;
import pwr.chrzescijanek.filip.higseg.util.ModelDto;
import pwr.chrzescijanek.filip.higseg.util.StageUtils;
import pwr.chrzescijanek.filip.higseg.util.Utils;

/**
 * Application controller class.
 */
public class Controller extends BaseController implements Initializable {

	private final ObservableList<ImageController> controllers = FXCollections.observableArrayList();
	private final ObservableMap<File, ColorPicker> data = FXCollections.observableHashMap();

	@FXML
	GridPane root;
	@FXML
	MenuItem alignMenuLoadModels;
	@FXML
	MenuItem alignMenuClearModels;
	@FXML
	Button loadModelsButton;
	@FXML
	Menu optionsMenuTheme;
	@FXML
	RadioMenuItem optionsMenuThemeDark;
	@FXML
	ToggleGroup themeToggleGroup;
	@FXML
	RadioMenuItem optionsMenuThemeLight;
	@FXML
	VBox models;

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
		Platform.exit();
	}

	@FXML
	void loadImages() {
		final List<File> selectedFiles = getImageFiles(root.getScene().getWindow());
		if (selectedFiles != null && !selectedFiles.isEmpty()) {
			final Stage dialog = showPopup("Processing images...");
			DialogListener dl = new DialogListener(dialog, selectedFiles.size());
			loadImages(selectedFiles, dl);
		}
	}

	@FXML
	void clearModels() {
		data.clear();
		models.getChildren().clear();
	}

	private void loadImages(final List<File> selectedFiles, DialogListener dialogListener) {
		for (final File f : selectedFiles) {
			String filePath = "";
			try {
				filePath = f.getCanonicalPath();
				final Mat image = getImage(filePath);
				addNewImage(filePath, image, dialogListener);
			} catch (IOException | CvException e) {
				handleException(e,
						"Loading failed!\nImage " + filePath
								+ " might be corrupted, paths may contain non-ASCII symbols or "
								+ "you do not have sufficient read permissions.");
			}
		}
	}

	private Mat getImage(final String filePath) {
		final Mat image = Imgcodecs.imread(filePath, Imgcodecs.CV_LOAD_IMAGE_COLOR);
		if (image.dataAddr() == 0)
			throw new CvException("Failed to load image! Check if file path contains only ASCII symbols");
		return image;
	}

	private void addNewImage(final String filePath, final Mat image, DialogListener dialogListener) {
		final String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
		final Stage newStage = new Stage();
		final String viewPath = "/static/image.fxml";
		String name = getTitle(fileName);
		final ImageController controller = StageUtils.loadImageStage(newStage, viewPath, name);
		controllers.add(controller);
		newStage.setOnHidden(e -> {
			controllers.remove(controller);
		});
		startTask(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				controller.addImage(image, filePath, getModelsData(), newStage, dialogListener);
				return null;
			}
		});
	}

	private List<ModelData> getModelsData() {
		return data.entrySet().stream()
				.map(e -> new ModelData(loadModel(e), e.getValue().getValue(), getModelName(e.getKey())))
				.collect(Collectors.toList());
	}
	
	private String getModelName(File f) {
		String name = f.getName();
		return name.substring(0, name.indexOf(".hgmodel"));
	}

	private ModelDto loadModel(Entry<File, ColorPicker> entry) {
		try {
			return Utils.loadModel(entry.getKey());
		} catch (IOException ex) {
			handleException(ex,
					"Loading failed!\nModel " + entry.getKey().getName()
							+ " might be corrupted, paths may contain non-ASCII symbols or "
							+ "you do not have sufficient read permissions.");
		}
		return null;
	}

	private String getTitle(final String fileName) {
		long count = controllers.stream().map(c -> ((Stage) c.root.getScene().getWindow()).getTitle())
				.filter(t -> t.startsWith(fileName)).count();
		String name = fileName;
		if (count > 0) {
			name = name + " (" + (count + 1) + ")";
		}
		return name;
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		initializeComponents(location, resources);
		setBindings();
	}

	private void setBindings() {
		setEnablementBindings();
	}

	private void initializeComponents(final URL location, final ResourceBundle resources) {
		initializeStyle();

		addDiaminobenzidine();
		addHaematoxylin();
	}

	private void addDiaminobenzidine() {
		addRow("/positive.hgmodel", Color.CHOCOLATE);
	}

	private void addHaematoxylin() {
		addRow("/negative.hgmodel", Color.INDIGO);
	}

	private void addRow(String resource, Color color) {
		ColorPicker cp = new ColorPicker();
		cp.setValue(color);
		File file = new File(getClass().getResource(resource).getFile());
		data.put(file, cp);
		Label label = new Label(getModelName(file));
		label.setMinWidth(200.0);
		label.setMaxWidth(200.0);
		HBox hBox = new HBox(label, cp);
		hBox.setMaxWidth(300.0);
		models.getChildren().add(hBox);
	}

	private void initializeStyle() {
		injectStylesheets(root);
		if (isLightThemeSelected()) {
			themeToggleGroup.selectToggle(optionsMenuThemeLight);
		} else {
			themeToggleGroup.selectToggle(optionsMenuThemeDark);
		}
	}

	private void setEnablementBindings() {
		final BooleanBinding images = Bindings.isNotEmpty(controllers);
		final BooleanBinding noModels = Bindings.isEmpty(data);

		loadModelsButton.disableProperty().bind(images);
		alignMenuLoadModels.disableProperty().bind(images);
		alignMenuClearModels.disableProperty().bind(Bindings.or(noModels, images));
	}

	@FXML
	void loadModels() {
		final List<File> selectedFiles = Utils.getModelFiles(root.getScene().getWindow());
		if (selectedFiles != null && !selectedFiles.isEmpty()) {
			addModels(selectedFiles);
		}
	}

	private void addModels(final List<File> selectedFiles) {
		for (File f : selectedFiles) {
			ColorPicker cp = new ColorPicker();
			data.put(f, cp);
			models.getChildren().add(new HBox(new Label(getModelName(f)), cp));
		}
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

}
