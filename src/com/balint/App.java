package com.balint;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Balint on 2017.03.19..
 */

public class App {
    private JLabel sourceImgLabel;
    private JPanel mainPanel;
    private JLabel outputImgLabel;
    private JSlider boxSizeSlider;
    private JLabel greyScaleImgLabel;
    private JLabel thresholdImgLabel;
    private JLabel filteredImgLabel;

    /*
    * Entry point of the app.
    * You have to set the file to detect here.
    */
    public static void main(String[] args) {
        String fileName = "gazora2.jpg";
//        String fileName = "gazora_nagy01.jpg";

        JFrame frame = new JFrame("App");
        App app = new App();
        app.mainPanel.setPreferredSize(new Dimension(400, 100));
        frame.setContentPane(app.mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();

        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        app.setImage(app.sourceImgLabel, img);

        // Add event listener to slider as lambda expression.
        app.boxSizeSlider.addChangeListener(e -> {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                app.detect(app, fileName, frame);
            }
        });

        app.detect(app, fileName, frame);
    }

    /*
    * Make a detection on the given file.
    */
    private void detect(App app, String fileName, JFrame frame) {
        RectDetect rectFinder = new RectDetect(fileName);
        rectFinder.detect();
        ArrayList<Rect> detectedRects = (ArrayList<Rect>) rectFinder.getRects();

        for(Rect rect : detectedRects) {
            System.out.println(
//                    rect.x + " " + rect.y+ " " + (rect.x + rect.width) + " " +(rect.y + rect.height));
                    rect.x + " " + rect.y+ " " + rect.width + " " + rect.height);
        }

        // Set the given back images
        app.setImage(app.greyScaleImgLabel, app.toBufferedImage(rectFinder.greyImg));
        app.setImage(app.thresholdImgLabel, app.toBufferedImage(rectFinder.threshImg));
        app.setImage(app.filteredImgLabel, app.toBufferedImage(rectFinder.filteredImg));
        app.setImage(app.outputImgLabel, app.toBufferedImage(rectFinder.filteredImg));

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
        frame.setSize(dim.width/2, dim.height/2);
        frame.setVisible(true);


        // TESS4J ********************************************
        File imageFile = new File(fileName);
        ITesseract instance = new Tesseract();
        instance.setTessVariable("tessedit_char_whitelist", "0123456789");

        try {
            String result = "";

            for(Rect rect : detectedRects) {
//                Rectangle r = new Rectangle(rect.x, rect.y, rect.width-3, rect.height-2);
                Rectangle r = new Rectangle(rect.x, rect.y, rect.width+app.boxSizeSlider.getValue(), rect.height+app.boxSizeSlider.getValue());
                result += instance.doOCR(imageFile, r).trim() + " ";
            }
            System.out.print(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
    }

    /*
    * Labels can be used to show pictures.
    * Sets a given image as the icon of a target JLabel.
    */
    private void setImage(JLabel target, Image image) {
        target.setIcon(new ImageIcon(image));
    }

    /*
    * Converts Matrix type used by OpenCV to Image type for Swing.
    */
    private Image toBufferedImage(Mat m){
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if ( m.channels() > 1 ) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte [] b = new byte[bufferSize];
        m.get(0,0,b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;

    }

}
