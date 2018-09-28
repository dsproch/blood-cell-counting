import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.GrayFilter;


public class CellCounter {
	
	public static void main(String[] args) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File(args[0]));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		BufferedImage bi = (BufferedImage) thresholdImage(image, 170);
		
		writeImageToFile(bi, "out");
		System.out.println("Wrote out.jpg");
		
		BufferedImage lines = getBoundaryImage(bi, 1);
		writeImageToFile(lines, "lines");
		System.out.println("Wrote lines.jpg");
				
		BufferedImage centers = countCircles(lines, 24, 27);
		writeImageToFile(centers, "centers");
		System.out.println("Wrote centers.jpg");
		System.out.println("All done.");
	}
	
	public static BufferedImage countCircles(Image img, int minRadius, int maxRadius) {
		int threshold = 128;
		BufferedImage input = toBufferedImage(img);
		BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		output.getGraphics().drawImage(input, 0, 0, null);
		WritableRaster wr = output.getRaster();
		int inPic[][] = new int[input.getHeight()][input.getWidth()];
		int outPic[][] = new int[input.getHeight()][input.getWidth()];
		
		for(int y=0; y<input.getHeight(); y++) {
			wr.getPixels(0, y, input.getWidth(), 1, inPic[y]);
			outPic[y][0] = 0;
			outPic[y][input.getWidth()-1] = 0;
		}
		ArrayList<Point> points = new ArrayList<Point>();
		int numCircles = 0;
		for(int y=0; y<input.getHeight(); y++) {
			for(int x=0; x<input.getWidth(); x++) {
				int correctPoints = 0;
				if(!isWithinRange(points, x, y, minRadius*2-3)) {
					for(int radius=minRadius; radius<=maxRadius; radius++) {
						for(Point p : getCheckPoints(x, y, radius, 40, input.getWidth(), input.getHeight())) {
							if(inPic[p.y][p.x] > threshold) {
								correctPoints++;
							}
						}
						if(correctPoints > 25) {
							numCircles++;
							outPic[y][x] = 255;
							
							//Mark Center
							int thickness = 2;
							for(int j = Math.max(0, x-thickness); j<Math.min(input.getWidth()-1, x+thickness); j++) {
								for(int k = Math.max(0, y-thickness); k<Math.min(input.getHeight()-1, y+thickness); k++) {
									outPic[k][j] = 255;
								}
							}
							points.add(new Point(x,y));
							break;
						}
					}
					
				}				
			}
			wr.setPixels(0, y, input.getWidth(), 1, outPic[y]);	
		}
		
		System.out.println(numCircles);
		return output;
		
	}
	
	private static boolean isWithinRange(ArrayList<Point> points, int x, int y, int dist) {
		for(Point p : points) {
			if(Math.sqrt(Math.pow(p.x-x, 2) + Math.pow(p.y-y, 2)) <= dist) {
				return true;
			}
		}
		
		return false;
	}
	
	private static ArrayList<Point> getCheckPoints(int cx, int cy, int r, int numPoints, int xBound, int yBound) {
		ArrayList<Point> points = new ArrayList<Point>();
		
		for(double angle=0; angle<360; angle+=(360.0/numPoints)) {
			double a = Math.toRadians(angle);
			int x = (int) (cx + r * Math.cos(a));
			int y = (int) (cy + r * Math.sin(a));
			if(!(x < 0 || x >= xBound || y < 0 || y >= yBound)) {
				points.add(new Point(x, y));
			}
			
			
		}
		
		
		
		return points;
	}
	
	public static BufferedImage getBoundaryImage(Image img, int thickness) {
		int threshold = 128;
		BufferedImage input = toBufferedImage(img);
		BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		output.getGraphics().drawImage(input, 0, 0, null);
		WritableRaster wr = output.getRaster();
		int inPic[][] = new int[input.getHeight()][input.getWidth()];
		int outPic[][] = new int[input.getHeight()][input.getWidth()];
		
		for(int y=0; y<input.getHeight(); y++) {
			wr.getPixels(0, y, input.getWidth(), 1, inPic[y]);
			outPic[y][0] = 0;
			outPic[y][input.getWidth()-1] = 0;
		}
		for(int y=1; y<input.getHeight()-1; y++) {
			for(int x=1; x<inPic[0].length-1; x++) {
				if(inPic[y][x] > threshold && 
							(inPic[y+1][x] < threshold || inPic[y-1][x] < threshold || 
									inPic[y][x+1] < threshold || inPic[y][x-1] < threshold)) {
					outPic[y][x] = 255;
					for(int j = Math.max(0, x-thickness); j<Math.min(input.getWidth()-1, x+thickness); j++) {
						for(int k = Math.max(0, y-thickness); k<Math.min(input.getHeight()-1, y+thickness); k++) {
							outPic[k][j] = 255;
						}
					}
							
				}
			}
			wr.setPixels(0, y, input.getWidth(), 1, outPic[y]);	
		}		
		
		return output;
	}
	
	
	
	public static void writeImageToFile(BufferedImage img, String filename) {
		File outputfile = new File(filename + ".jpg");
		try {
			ImageIO.write(img, "jpg", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static Image createGrayScaleImage(Image img) {
		ImageFilter filter = new GrayFilter(true, 50);  
		ImageProducer producer = new FilteredImageSource(img.getSource(), filter);  
		Image image = Toolkit.getDefaultToolkit().createImage(producer);  
		
		return image;
	}
	
	public static Image thresholdImage(Image img, int threshold) {
		BufferedImage input = toBufferedImage(img);
		BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		output.getGraphics().drawImage(input, 0, 0, null);
		WritableRaster wr = output.getRaster();
		int row[] = new int[input.getWidth()];
		for(int y=0; y<input.getHeight(); y++) {
			wr.getPixels(0, y, input.getWidth(), 1, row);
			for(int k=0; k<row.length; k++) {
				if(row[k] > threshold) {
					row[k] = 255;
				} else {
					row[k] = 0;
				}
			}
			wr.setPixels(0, y, input.getWidth(), 1, row);
		}
		
		
		return output;
	}
	
	public static BufferedImage toBufferedImage(Image img)
	{
	    if(img instanceof BufferedImage)
	    {
	        return (BufferedImage) img;
	    }

	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    // Draw the image on to the buffered image
	    Graphics2D g2d = bimage.createGraphics();
	    g2d.drawImage(img, 0, 0, null);
	    g2d.dispose();

	    // Return the buffered image
	    return bimage;
	}
	

}
