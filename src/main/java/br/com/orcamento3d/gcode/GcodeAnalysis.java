package br.com.orcamento3d.gcode;

import java.util.List;

public record GcodeAnalysis(long lineCount, double printTimeMinutes,
                            double filamentMillimeters, double filamentGrams,
                            boolean magnetInsertionDetected, int magnetCount,
                            List<FilamentMetadata> filaments,
                            String printerModel, String printerProfile, String slicer,
                            double nozzleDiameter, double bedWidth, double bedDepth,
                            double printableHeight, double modelHeight, int layerCount,
                            double nozzleTemperature, double bedTemperature) {
    public record FilamentMetadata(String material, String color, String profile,
                                   double grams, double millimeters) {}
}
