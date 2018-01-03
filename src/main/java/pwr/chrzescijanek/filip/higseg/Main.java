package pwr.chrzescijanek.filip.higseg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import pwr.chrzescijanek.filip.higseg.util.StatsDto;
import pwr.chrzescijanek.filip.higseg.util.Utils;

/**
 * Main application class.
 */
public class Main {

	private static final String LOGGING_FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";

	private static final String LOGGING_FORMAT = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s: %5$s%6$s%n";

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.setProperty(LOGGING_FORMAT_PROPERTY, LOGGING_FORMAT);
		initializeLogger();
	}
    
	@Parameter(names={"--headless", "-h"},   description = "Headless mode")
    private boolean headless = false;
    
	@Parameter(names={"--inputs", "-i"},      description = "Input image files"  )
    private List<String> inputs = new ArrayList<>();
    
	@Parameter(names={"--colors", "-c"},      description = "Conversion colors"  )
    private List<String> colors = new ArrayList<>();
	
	@Parameter(names={"--output", "-o"},     description = "Image output file" )
    private String  outputImagePath = "";
	
	@Parameter(names={"--cells-output", "-co"},    description = "Cells image output file" )
    private String  outputCellsPath = "";
	
	@Parameter(names={"--stats-output", "-so"},    description = "Image stats output file" )
    private String  outputStatsPath = "";

    private final ObjectProperty<Mat> rawImage = new SimpleObjectProperty<>();
    private final ObjectProperty<Mat> cells = new SimpleObjectProperty<>();
    private final Map<String, Integer> imageStats = new HashMap<>();
	
	private Main() {}
    
	private static void initializeLogger() {
		try {
			final Handler fileHandler = new FileHandler("log", 10000, 5, true);
			fileHandler.setFormatter(new SimpleFormatter());
			Logger.getLogger(Main.class.getPackage().getName()).addHandler(fileHandler);
		} catch (final IOException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);
		}
	}

	/**
	 * Starts the application.
	 *
	 * @param args launch arguments
	 * @throws IOException 
	 */
	public static void main(final String... args) throws IOException {
		Main main = new Main();
		JCommander.newBuilder().addObject(main).build().parse(args);
        main.run(args);
	}

	private void run(final String... args) throws IOException {
		if (headless) {
            headless();
		} else {
			Application.launch(MainApplication.class, args);
		}
	}

	private void headless() throws IOException {
		if (inputs.size() != colors.size()) {
			throw new IllegalArgumentException("Number of colors does not match the number of images!");
		}
	    for (int i = 0; i < inputs.size(); i++) {
	    	loadImages(inputs.get(i), colors.get(i));
	    }
	    if (!outputImagePath.isEmpty() && rawImage.isNotNull().get()) {
            Utils.writeImage(rawImage.get(), new File(outputImagePath));
	    }
	    if (!outputCellsPath.isEmpty() && cells.isNotNull().get()) {
            Utils.writeImage(cells.get(), new File(outputCellsPath));
	    }
	    if (!outputStatsPath.isEmpty() && !imageStats.isEmpty()) {
	    	saveStats(imageStats, outputStatsPath);
		}
	}

	private void loadImages(final String filePath, final String hex) {
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
		paintNewImage(image, newData, newWidth, hex);

		paintCurrentImage(currentCells, newCellsData, newWidth);				
		paintNewImage(cells, newCellsData, newWidth, hex);
				
		newImage.put(0, 0, newData);
		this.rawImage.set(newImage);
		newCells.put(0, 0, newCellsData);
		this.cells.set(newCells);
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

	private void paintNewImage(final Mat image, final byte[] newData, final int newWidth, final String hex) {
		final int width = image.width();
		
		Color c = Color.web(hex);
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
		Utils.extractCells(image, result);
        Mat inverted = new Mat();
        Core.bitwise_not(result, inverted);
        imageStats.put(filePath, Imgproc.connectedComponents(inverted, new Mat()));
        return result;
	}
	
	private void saveStats(Map<String, Integer> imageStats, String path) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path)))) {
        	double sum = imageStats.values().stream().mapToDouble(Integer::doubleValue).sum();
        	Map<String, Double> cellRatios = imageStats.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / sum));
			bw.write(new Gson().toJson(new StatsDto(imageStats, cellRatios)));
        }
    }

}
