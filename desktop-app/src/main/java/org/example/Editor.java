package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.application.Platform;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Editor {

    @FXML
    private javafx.scene.control.Button newShp; // fx:id="newShp"

    @FXML
    private javafx.scene.control.Button clrDbs; // fx:id="clrDbs"

    @FXML
    private javafx.scene.control.Button clrShp; // fx:id="clrShp"

    @FXML
    private Label status; // Label for displaying status messages

    // New key input field in the GUI
    @FXML
    private TextField keyField; // fx:id="keyField"

    private ShapefileDataStore dataStore;
    private SimpleFeatureType schema;
    private SimpleFeatureStore featureStore;
    private GeometryFactory geometryFactory = new GeometryFactory();

    // MySQL connection details (adjust as needed)
    private String jdbcUrl = "jdbc:mysql://localhost:3306/location1";
    private String dbUser = "root";
    private String dbPassword = "";

    private Timer timer;

    @FXML
    public void handleNewShpAction() {
        // First, validate the key entered by the user.
        String key = keyField.getText().trim();
        if(key.isEmpty()){
            updateStatus("Please enter a key.");
            return;
        }
        if(!validateKey(key)){
            updateStatus("Invalid key. Access denied.");
            return;
        }

        try {
            // Use a FileChooser to let the user choose where to create the new shapefile
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save New Shapefile");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Shapefile", "*.shp"));

            Stage stage = (Stage) newShp.getScene().getWindow();
            File shpFile = fileChooser.showSaveDialog(stage);
            if (shpFile == null) {
                updateStatus("File selection canceled.");
                return;
            }

            // Defining the schema for new shapefile
            schema = DataUtilities.createType("Location", "the_geom:Point");

            // Set up parameters for creating the datastore
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", shpFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            // Create the new shapefile datastore using the factory
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            dataStore.createSchema(schema);
            dataStore.setCharset(Charset.forName("UTF-8"));

            // Retrieve the feature source and ensure it is editable
            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
            if (!(featureSource instanceof SimpleFeatureStore)) {
                updateStatus("Error: Shapefile is not editable.");
                return;
            }
            featureStore = (SimpleFeatureStore) featureSource;

            updateStatus("New shapefile created successfully!");
            // Start the timer to poll the database for new locations
            startPolling();

        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error: " + e.getMessage());
        }
    }

    /**
     * Validates the key by checking whether it exists in the 'users' table.
     * Assumes the 'users' table has a column 'user_key'.
     */
    private boolean validateKey(String key) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            String sql = "SELECT COUNT(*) AS count FROM users WHERE user_key = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("count");
                rs.close();
                stmt.close();
                return count > 0;
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error validating key: " + e.getMessage());
        }
        return false;
    }


    @FXML
    public void handleClearDatabaseAction() {
        // First, get the key from the keyField
        String key = keyField.getText().trim();
        if (key.isEmpty()) {
            updateStatus("Please enter your key to clear the database.");
            return;
        }
        // Validate the key using your validateKey() method
        if (!validateKey(key)) {
            updateStatus("Invalid key. Database will not be cleared.");
            return;
        }

        // If the key is valid, proceed to clear only the records for this key.
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            String sql = "DELETE FROM locations WHERE user_key = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, key);
            int deleted = stmt.executeUpdate();
            stmt.close();
            updateStatus("Database cleared. " + deleted + " record(s) deleted for key: " + key);
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error clearing database: " + e.getMessage());
        }
    }


    // Clearing the shapefile is done by re-creating its schema.
    @FXML
    public void handleClearShapefileAction() {
        try {
            if (dataStore != null && schema != null) {
                dataStore.createSchema(schema);
                updateStatus("Shapefile cleared successfully by resetting the schema.");
            } else {
                updateStatus("No shapefile available to clear.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error clearing shapefile: " + e.getMessage());
        }
    }

    private void startPolling() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            String lastFetchedId = ""; // Track the last fetched record id

            @Override
            public void run() {
                try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
                    // Query the latest record (assumes "id" is auto-incremented)
                    String sql = "SELECT id, x, y FROM locations WHERE user_key = ? ORDER BY id DESC LIMIT 1";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    // Only get locations for the validated key:
                    stmt.setString(1, keyField.getText().trim());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        String currentId = rs.getString("id");
                        if (!currentId.equals(lastFetchedId)) {
                            double y = rs.getDouble("x"); // Adjust if necessary
                            double x = rs.getDouble("y");
                            lastFetchedId = currentId;

                            // Create a new point geometry
                            org.locationtech.jts.geom.Point point = geometryFactory.createPoint(new Coordinate(x, y));

                            // Build the new feature using the shapefile schema
                            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(schema);
                            featureBuilder.add(point);
                            SimpleFeature newFeature = featureBuilder.buildFeature(null);

                            // Append the new feature to the shapefile
                            featureStore.addFeatures(DataUtilities.collection(newFeature));
                            updateStatus("New point added: (" + x + ", " + y + ")");
                        }
                    }
                    rs.close();
                    stmt.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    updateStatus("Error polling database: " + e.getMessage());
                }
            }
        }, 0, 30000); // Poll every 30 seconds
    }
    @FXML
    private void handleSyncAction() {
        // Example logic
        String key = keyField.getText().trim();
        if (key.isEmpty()) {
            updateStatus("Please enter a key.");
            return;
        }

        // TODO: Validate the key in the database
        if (!validateKey(key)) {
            updateStatus("Invalid key. Please check and try again.");
            return;
        }

        updateStatus("Key is valid. Sync complete!");
    }


    private void updateStatus(String message) {
        Platform.runLater(() -> {
            status.setText("Status: " + message);
        });
    }

    // Optionally, add a method to stop the timer when the application is closed.
    public void stopPolling() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
