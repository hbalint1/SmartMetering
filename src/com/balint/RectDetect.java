package com.balint;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by Balint on 2017.03.21..
 */

public class RectDetect {
    // Output file name.
    private String outName = "result_rectDetect.jpg";

    // Source image path.
    private String imgPath;

    private List<Rect> rectList;

    public Mat greyImg;
    public Mat threshImg;
    public Mat filteredImg;

    /*
    * Constructor method.
    * Sets the target file.
    */
    public RectDetect(String _imgPath){
        imgPath = _imgPath;
        rectList = new ArrayList<>();
    }

    /*
    * Main function of the class.
    * Detects rectangles on given file.
    */
    public void detect(){
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

            // Read the image into a matrix.
            Mat source = Imgcodecs.imread(imgPath, Imgcodecs.CV_LOAD_IMAGE_ANYCOLOR);
            Mat destination = new Mat(source.rows(), source.cols(), source.type());

            // Convert the image to greyScale.
            Imgproc.cvtColor(source, destination, Imgproc.COLOR_RGB2GRAY);

            greyImg = destination.clone();

            // Info: http://docs.opencv.org/2.4/modules/imgproc/doc/miscellaneous_transformations.html
            // http://docs.opencv.org/trunk/d7/d4d/tutorial_py_thresholding.html
            Imgproc.adaptiveThreshold(destination,
                    destination,
                    255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY,
                    11, -15);

            threshImg = destination.clone();

            // Detect contours
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(destination, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            MatOfPoint2f matOfPoint2f = new MatOfPoint2f();

            // Copy the source image so i can show the found rectso n it.
            filteredImg = source.clone();
            for (MatOfPoint contour: contours) {
                Rect rect = Imgproc.boundingRect(contour);
                matOfPoint2f.fromList(contour.toList());

                // Get rid of too large or too small rects. (boundingBox)
                if(rect.width >= 20 ||
                        rect.width <= 7 ||
                        rect.height >= 20 ||
                        rect.height <= 7) {
                    continue;
                }
                else {
                    Rect newRect = new Rect(rect.x, rect.y, rect.width, rect.height);
                    rectList.add(newRect);
                }

                // Draw the leftover rects on image.
                Imgproc.rectangle(filteredImg, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(255, 255, 255), 1);
            }

            Imgcodecs.imwrite(outName, filteredImg);

        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    /*
    * Gives back all the rectangles, that the algorithm found.
    * It's reversed to give the list from first found to last found.
    */
    public List<Rect> getRects() {
        Collections.reverse(rectList);
        return rectList;
    }

    /*
    * Old method.
    * This method takes the approximated polygons on-by-one and checks
    * their point count. Only take care of polys that are between 4 and 6 points.
    * It's a little overcomplicated (the simplified method is the detect),
    * but if anyone interested can try it and have fun.
    */
    public Mat old_detect() {
        try{
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

            // Read the image into a matrix.
            Mat source = Imgcodecs.imread(imgPath, Imgcodecs.CV_LOAD_IMAGE_ANYCOLOR);
            Mat destination = new Mat(source.rows(), source.cols(), source.type());

            // Convert the image to greyScale.
            Imgproc.cvtColor(source, destination, Imgproc.COLOR_RGB2GRAY);

            greyImg = destination.clone();

            //Imgproc.GaussianBlur(destination, destination, new Size(5, 5), 0, 0, Core.BORDER_DEFAULT);

            //Imgcodecs.imwrite("before_canny.jpg", destination);

            // Detect edges
//            int threshold = 100;
//            Mat edges = new Mat();
//            Imgproc.Canny(destination, edges, threshold, threshold*3);

            // Info: http://docs.opencv.org/2.4/modules/imgproc/doc/miscellaneous_transformations.html
            // http://docs.opencv.org/trunk/d7/d4d/tutorial_py_thresholding.html
            Imgproc.adaptiveThreshold(destination,
                    destination,
                    255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY,
                    11, -15);

            threshImg = destination.clone();

            // Detect contours
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            //Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            Imgproc.findContours(destination, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
            MatOfPoint2f approxCurve = new MatOfPoint2f();

            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                MatOfPoint contour = contours.get(idx);
                Rect rect = Imgproc.boundingRect(contour);
                matOfPoint2f.fromList(contour.toList());

                // Felhúztam 0.02-ről 0.06-ra az értéket!!! => most már rect
                Imgproc.approxPolyDP(matOfPoint2f, approxCurve, Imgproc.arcLength(matOfPoint2f, true) * 0.05, true);

                long total = approxCurve.total();
                if (total >= 4 && total <= 6) {
                    List<Double> cos = new ArrayList<>();
                    Point[] points = approxCurve.toArray();


                    // Kirajzoltatom alakzatonként a pontokat, amiket talál.
                    for (Point p: points) {
                        Imgproc.circle(source, p, 3, new Scalar(new Random().nextInt(255)));
                    }

                    for (int j = 2; j < total + 1; j++) {
                        cos.add(angle(points[(int) (j % total)], points[j - 2], points[j - 1]));
                    }
                    Collections.sort(cos);
                    Double minCos = cos.get(0);
                    Double maxCos = cos.get(cos.size() - 1);
                    boolean isRect = total == 4 && minCos >= -0.1 && maxCos <= 0.3;
                    boolean isPolygon = (total == 5 && minCos >= -0.34 && maxCos <= -0.27) || (total == 6 && minCos >= -0.55 && maxCos <= -0.45);
                    if (isRect) {
                        double ratio = Math.abs(1 - (double) rect.width / rect.height);
//                        drawText(rect.tl(), ratio <= 0.02 ? "SQU" : "RECT", source);
                        drawText(rect.tl(), ratio <= 0.02 ? "S" : "R", source);
                        Imgproc.rectangle(source, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
                                new Scalar(255), 1);

                        //Rect newRect = new Rect(rect.x, rect.y, (rect.x + rect.width), (rect.y + rect.height));
                        Rect newRect = new Rect(rect.x, rect.y, rect.width, rect.height);
                        rectList.add(newRect);
//                        System.out.println(
//                                rect.x + " " + rect.y+ " " + (rect.x + rect.width) + " " +(rect.y + rect.height));
                    }
                    else {
                    //if (isPolygon) {
                        drawText(rect.tl(), "P", source);
                    }
                }
            }

            Imgcodecs.imwrite(outName, source);

            return source;
        } catch (Exception ex) {
            System.err.println(ex.getMessage());

            return null;
        }
    }

    private double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
    }

    /*
    * Writes text to image (Mat).
    */
    private void drawText(Point ofs, String text, Mat colorImage) {
        Imgproc.putText(colorImage, text, ofs, Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255,255,25));
    }
}
