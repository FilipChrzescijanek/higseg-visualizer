package pwr.chrzescijanek.filip.higseg.util;

import javafx.scene.paint.Color;

public class ModelData {

	private final ModelDto model;
	private final Color color;
	private final String name;
	
	public ModelData(ModelDto model, Color color, String name) {
		this.model = model;
		this.color = color;
		this.name = name;
	}
	
	public ModelDto getModel() {
		return model;
	}
	
	public Color getColor() {
		return color;
	}
	
	public String getName() {
		return name;
	}
	
}
