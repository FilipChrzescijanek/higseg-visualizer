package pwr.chrzescijanek.filip.higseg.util;

import java.util.Map;

public class StatsDto {
	
	private final Map<String, Integer> cellCounts;
	private final Map<String, Double> cellRatios;
	
	public StatsDto(Map<String, Integer> cellCounts, Map<String, Double> cellStats) {
		this.cellCounts = cellCounts;
		this.cellRatios = cellStats;
	}
	
	public Map<String, Integer> getCellCounts() {
		return cellCounts;
	}
	
	public Map<String, Double> getCellRatios() {
		return cellRatios;
	}
	
}
