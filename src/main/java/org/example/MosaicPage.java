package org.example;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;  // 使用javafx的Image
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;

public class MosaicPage {

    private final Stage stage;
    private final List<File> selectedFiles;
    private ImageView imageView;

    public MosaicPage(Stage stage, List<File> selectedFiles) {
        this.stage = stage;
        this.selectedFiles = selectedFiles;
        initUI();
    }

    private void initUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> {
            // 返回选择页面
            List<File> photoFiles = loadPhotoFiles(new File("photos"));
            SelectPage selectPage = new SelectPage(stage, photoFiles);
            selectPage.show();
        });

        root.getChildren().addAll(backBtn, imageView);

        Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Image Mosaic");
        stage.show();

        createMosaic();
    }

    private List<File> loadPhotoFiles(File photosDir) {
        File[] files = photosDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg");
        });
        List<File> fileList = new java.util.ArrayList<>();
        if (files != null) {
            for (File f : files) {
                fileList.add(f);
            }
        }
        return fileList;
    }

    private void createMosaic() {
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        try {
            int mosaicWidth = 600;
            int mosaicHeight = 600;
            int tileSize = 60;

            BufferedImage mosaicImage = new BufferedImage(mosaicWidth, mosaicHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = mosaicImage.createGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, mosaicWidth, mosaicHeight);

            BufferedImage mask;
            if (new Random().nextBoolean()) {
                mask = createHeartMask(mosaicWidth, mosaicHeight);
            } else {
                mask = createStarMask(mosaicWidth, mosaicHeight);
            }

            BufferedImage[] thumbnails = new BufferedImage[selectedFiles.size()];
            for (int i = 0; i < selectedFiles.size(); i++) {
                BufferedImage img = javax.imageio.ImageIO.read(selectedFiles.get(i));
                thumbnails[i] = resizeImage(img, tileSize, tileSize);
            }

            int cols = mosaicWidth / tileSize;
            int rows = mosaicHeight / tileSize;

            int idx = 0;
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    int px = x * tileSize;
                    int py = y * tileSize;

                    int maskAlpha = (mask.getRGB(px + tileSize / 2, py + tileSize / 2) >> 24) & 0xff;
                    if (maskAlpha > 128) {
                        BufferedImage tile = thumbnails[idx % thumbnails.length];
                        g.drawImage(tile, px, py, null);
                        idx++;
                    }
                }
            }

            g.dispose();

            Image fxImage = SwingFXUtils.toFXImage(mosaicImage, null);
            imageView.setImage(fxImage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BufferedImage createHeartMask(int width, int height) {
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();

        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(0, 0, 0, 255));

        GeneralPath heart = new GeneralPath();

        double xCenter = width / 2.0;
        double yCenter = height / 2.0;
        double size = Math.min(width, height) * 0.8;

        heart.moveTo(xCenter, yCenter + size / 4);

        for (double t = 0; t <= Math.PI; t += 0.01) {
            double x = size * 16 * Math.pow(Math.sin(t), 3) / 16;
            double y = -size * (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16;
            heart.lineTo(xCenter + x, yCenter + y);
        }
        heart.closePath();

        g.fill(heart);
        g.dispose();

        return mask;
    }

    private BufferedImage createStarMask(int width, int height) {
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();

        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(0, 0, 0, 255));

        double cx = width / 2.0;
        double cy = height / 2.0;
        double outerRadius = Math.min(width, height) * 0.4;
        double innerRadius = outerRadius * 0.5;

        GeneralPath star = new GeneralPath();
        int points = 5;

        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI / points * i;
            double r = (i % 2 == 0) ? outerRadius : innerRadius;
            double x = cx + Math.cos(angle) * r;
            double y = cy - Math.sin(angle) * r;
            if (i == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();

        g.fill(star);
        g.dispose();

        return mask;
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    public void show() {
        stage.show();
    }
}
