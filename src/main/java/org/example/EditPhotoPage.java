package org.example;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

public class EditPhotoPage {

    private Stage stage;
    private File imageFile;
    private ImageView imageView;
    private WritableImage currentImage;
    private Stack<WritableImage> undoStack = new Stack<>();
    private Stack<WritableImage> redoStack = new Stack<>();

    public EditPhotoPage(Stage owner, File imageFile) {
        this.imageFile = imageFile;

        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Edit Photo");

        initUI();
        loadImage();
    }

    private void initUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);

        HBox buttons = new HBox(10);

        Button undoBtn = new Button("Undo");
        Button redoBtn = new Button("Redo");
        Button brightnessBtn = new Button("Brightness +30");
        Button contrastBtn = new Button("Contrast 1.2x");
        Button grayscaleBtn = new Button("Grayscale");
        Button rotateBtn = new Button("Rotate 90°");
        Button cropBtn = new Button("Crop 100x100");
        Button saveBtn = new Button("Save");

        buttons.getChildren().addAll(undoBtn, redoBtn, brightnessBtn, contrastBtn, grayscaleBtn, rotateBtn, cropBtn, saveBtn);

        root.getChildren().addAll(imageView, buttons);

        undoBtn.setOnAction(e -> undo());
        redoBtn.setOnAction(e -> redo());
        brightnessBtn.setOnAction(e -> {
            pushUndo();
            adjustBrightness(30);
        });
        contrastBtn.setOnAction(e -> {
            pushUndo();
            adjustContrast(1.2);
        });
        grayscaleBtn.setOnAction(e -> {
            pushUndo();
            convertToGrayscale();
        });
        rotateBtn.setOnAction(e -> {
            pushUndo();
            rotateImage();
        });
        cropBtn.setOnAction(e -> {
            pushUndo();
            cropImage(100, 100);
        });
        saveBtn.setOnAction(e -> saveImage());

        Scene scene = new Scene(root, 650, 700);
        stage.setScene(scene);
    }

    public void show() {
        stage.show();
    }

    private void loadImage() {
        try {
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            currentImage = SwingFXUtils.toFXImage(bufferedImage, null);
            imageView.setImage(currentImage);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Failed to load image.");
        }
    }

    private void pushUndo() {
        undoStack.push(cloneImage(currentImage));
        redoStack.clear();
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(cloneImage(currentImage));
            currentImage = undoStack.pop();
            imageView.setImage(currentImage);
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(cloneImage(currentImage));
            currentImage = redoStack.pop();
            imageView.setImage(currentImage);
        }
    }

    private WritableImage cloneImage(WritableImage img) {
        return new WritableImage(img.getPixelReader(), (int) img.getWidth(), (int) img.getHeight());
    }

    private void adjustBrightness(int offset) {
        PixelReader reader = currentImage.getPixelReader();
        WritableImage newImg = new WritableImage((int) currentImage.getWidth(), (int) currentImage.getHeight());
        PixelWriter writer = newImg.getPixelWriter();

        for (int y = 0; y < currentImage.getHeight(); y++) {
            for (int x = 0; x < currentImage.getWidth(); x++) {
                javafx.scene.paint.Color c = reader.getColor(x, y);
                double r = Math.min(1, c.getRed() + offset / 255.0);
                double g = Math.min(1, c.getGreen() + offset / 255.0);
                double b = Math.min(1, c.getBlue() + offset / 255.0);
                writer.setColor(x, y, new javafx.scene.paint.Color(r, g, b, c.getOpacity()));
            }
        }
        currentImage = newImg;
        imageView.setImage(currentImage);
    }

    private void adjustContrast(double factor) {
        PixelReader reader = currentImage.getPixelReader();
        WritableImage newImg = new WritableImage((int) currentImage.getWidth(), (int) currentImage.getHeight());
        PixelWriter writer = newImg.getPixelWriter();

        for (int y = 0; y < currentImage.getHeight(); y++) {
            for (int x = 0; x < currentImage.getWidth(); x++) {
                javafx.scene.paint.Color c = reader.getColor(x, y);
                double r = clamp((c.getRed() - 0.5) * factor + 0.5);
                double g = clamp((c.getGreen() - 0.5) * factor + 0.5);
                double b = clamp((c.getBlue() - 0.5) * factor + 0.5);
                writer.setColor(x, y, new javafx.scene.paint.Color(r, g, b, c.getOpacity()));
            }
        }
        currentImage = newImg;
        imageView.setImage(currentImage);
    }

    private void convertToGrayscale() {
        PixelReader reader = currentImage.getPixelReader();
        WritableImage newImg = new WritableImage((int) currentImage.getWidth(), (int) currentImage.getHeight());
        PixelWriter writer = newImg.getPixelWriter();

        for (int y = 0; y < currentImage.getHeight(); y++) {
            for (int x = 0; x < currentImage.getWidth(); x++) {
                javafx.scene.paint.Color c = reader.getColor(x, y);
                double gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                writer.setColor(x, y, new javafx.scene.paint.Color(gray, gray, gray, c.getOpacity()));
            }
        }
        currentImage = newImg;
        imageView.setImage(currentImage);
    }

    private void rotateImage() {
        WritableImage rotated = new WritableImage((int) currentImage.getHeight(), (int) currentImage.getWidth());
        PixelReader reader = currentImage.getPixelReader();
        PixelWriter writer = rotated.getPixelWriter();

        int w = (int) currentImage.getWidth();
        int h = (int) currentImage.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                writer.setColor(h - y - 1, x, reader.getColor(x, y));
            }
        }
        currentImage = rotated;
        imageView.setImage(currentImage);
    }

    private void cropImage(int cropWidth, int cropHeight) {
        if (cropWidth > currentImage.getWidth()) cropWidth = (int) currentImage.getWidth();
        if (cropHeight > currentImage.getHeight()) cropHeight = (int) currentImage.getHeight();

        WritableImage cropped = new WritableImage(currentImage.getPixelReader(), 0, 0, cropWidth, cropHeight);
        currentImage = cropped;
        imageView.setImage(currentImage);
    }

    private void saveImage() {
        try {
            BufferedImage bImage = SwingFXUtils.fromFXImage(currentImage, null);
            ImageIO.write(bImage, "png", imageFile);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Image saved successfully.");
            alert.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save image.");
            alert.showAndWait();
        }
    }

    private double clamp(double val) {
        return Math.max(0, Math.min(1, val));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.showAndWait();
    }
}
