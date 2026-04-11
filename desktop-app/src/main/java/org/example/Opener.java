package org.example;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.style.Style;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;

import java.io.File;
import java.util.Scanner;

public class Opener {
    public static void main(String[] args) throws Exception {
        // display a data store file chooser dialog for shapefiles
        Scanner scanner = new Scanner(System.in);
        System.out.print("Paste file location: ");
        String filePath = scanner.nextLine();
        File file = new File(filePath);

        if (file == null) {
            return;
        }

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();

        // Create a map content and add the shapefile to it
        MapContent map = new MapContent();
        map.setTitle("Quickstart");

        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);

        // Display the map
        JMapFrame.showMap(map);
    }
}