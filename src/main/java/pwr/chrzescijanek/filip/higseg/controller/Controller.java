package pwr.chrzescijanek.filip.higseg.controller;

import static pwr.chrzescijanek.filip.higseg.util.Utils.getImageFiles;
import static pwr.chrzescijanek.filip.higseg.util.Utils.startTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;
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
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import javafx.util.Pair;
import pwr.chrzescijanek.filip.higseg.util.ModelData;
import pwr.chrzescijanek.filip.higseg.util.ModelDto;
import pwr.chrzescijanek.filip.higseg.util.StageUtils;
import pwr.chrzescijanek.filip.higseg.util.Utils;

/**
 * Application controller class.
 */
public class Controller extends BaseController implements Initializable {

	private final ObservableList<ImageController> controllers = FXCollections.observableArrayList();
	private final ObservableList<Pair<File, ColorPicker>> data = FXCollections.observableArrayList();

	@FXML
	GridPane root;
	@FXML
	MenuItem fileMenuSaveStats;
	@FXML
	MenuItem alignMenuLoadModels;
	@FXML
	MenuItem alignMenuClearModels;
	@FXML
	MenuItem alignMenuRestoreModels;
	@FXML
	Button loadModelsButton;
	@FXML
	Menu optionsMenuTheme;
	@FXML
	ToggleGroup themeToggleGroup;
	@FXML
	RadioMenuItem optionsMenuThemeDark;
	@FXML
	RadioMenuItem optionsMenuThemeLight;
	@FXML
	Menu optionsMenuCells;
	@FXML
	ToggleGroup cellsToggleGroup;
	@FXML
	RadioMenuItem optionsMenuCellsFill;
	@FXML
	RadioMenuItem optionsMenuCellsBorder;
	@FXML
	VBox models;

	@Override
	protected GridPane getRoot() {
		return root;
	}

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
	void restoreModels() {
		clearModels();
		addHaematoxylin();
		addDiaminobenzidine();
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
		final List<ModelData> modelsData = getModelsData();
		final String name = getTitle(fileName);
		final ImageController controller = StageUtils.loadImageStage(newStage, viewPath, name);
		controller.setFill(optionsMenuCellsFill.isSelected());
		controllers.add(controller);
		newStage.setOnHidden(e -> {
			controllers.remove(controller);
		});
		startTask(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				controller.addImage(image, filePath, modelsData, newStage, dialogListener);
				return null;
			}
		});
	}

	private List<ModelData> getModelsData() {
		return data.stream()
				.map(p -> new ModelData(loadModel(p), p.getValue().getValue(), getModelName(p.getKey())))
				.collect(Collectors.toList());
	}

	private String getModelName(File f) {
		String name = f.getName();
		return name.substring(0, name.indexOf(".hgmodel"));
	}

	private ModelDto loadModel(Pair<File, ColorPicker> entry) {
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
		fileMenuSaveStats.disableProperty().bind(Bindings.isEmpty(controllers));
		optionsMenuCells.disableProperty().bind(Bindings.isNotEmpty(controllers));
		addHaematoxylin();
		addDiaminobenzidine();
	}

	private void addDiaminobenzidine() {
		addRow("/positive.hgmodel", Color.CHOCOLATE);
	}

	private void addHaematoxylin() {
		addRow("/negative.hgmodel", Color.INDIGO);
	}

	private void addRow(String resource, Color color) {
		File file = new File(getClass().getResource(resource).getFile());
		addRow(file, color);
	}

	private void addRow(File file, Color color) {
		ColorPicker cp = new ColorPicker();
		cp.setValue(color);
		data.add(new Pair<>(file, cp));
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
		alignMenuRestoreModels.disableProperty().bind(images);
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
			addRow(f, Color.BLACK);
		}
	}

	public Stage waitForServer() {
		Stage dialog = showPopup("Starting python server...");
		return dialog;
	}

	@FXML
	void saveStats() {
		List<String> columnNames = data.stream().map(p -> getModelName(p.getKey())).collect(Collectors.toList());
		List<List<String>> values = controllers.stream().map(c -> {
			List<String> row = columnNames.stream().map(n -> c.getImageStats().get(n).toString())
					.collect(Collectors.toList());
			row.add(0, c.getTitle());
			return row;
		}).collect(Collectors.toList());
		columnNames.add(0, "image");
		writeFile(columnNames, values);
	}

	private void writeFile(List<String> columnNames, List<List<String>> values) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.join(",", columnNames));
		sb.append(System.lineSeparator());
		for (List<String> v : values) {
			sb.append(String.join(",", v));
			sb.append(System.lineSeparator());
		}
		File csvFile = Utils.getCsvFile(root.getScene().getWindow());
		if (csvFile != null) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
				bw.write(sb.toString());
			} catch (IOException e) {
				handleException(e, "Save failed! Check your write permissions.");
			}
		}
	}

}
