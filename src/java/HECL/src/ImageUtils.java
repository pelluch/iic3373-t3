import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.jogamp.opencl.CLBuffer;


public class ImageUtils {

	//Creates a BufferedImage from an OpenCL buffer
    public static BufferedImage createImage(int width, int height, CLBuffer<FloatBuffer> buffer, int imageType) {
    	if(!(imageType == BufferedImage.TYPE_INT_RGB || imageType == BufferedImage.TYPE_BYTE_GRAY))
    		return null;
        BufferedImage image = new BufferedImage(width, height, imageType);
        float[] pixels = new float[buffer.getBuffer().capacity()];
        buffer.getBuffer().get(pixels).rewind();
        image.getRaster().setPixels(0, 0, width, height, pixels);
        return image;
    }
    
    public static InputStream getStreamFor(String filename) {
        return HECL.class.getResourceAsStream(filename);
    }
    
    //Save image to file fileName
    public static boolean saveImage(BufferedImage image, String fileName) {
    	 File outputfile = new File(fileName);
    	 try {
			ImageIO.write(image, "jpeg", outputfile);
		} catch (IOException e) {
			System.out.println("Error writing buffered image to disk");
			e.printStackTrace();
			return false;
		}
    	 
    	 return true;
    	
    }
    
    //Reads an image from disk to a BufferedImage object
    public static BufferedImage readImage(String filename) {
        try {
			return ImageIO.read(getStreamFor(filename));
		} catch (IOException e) {
			System.out.println("Error loading " + filename);
			return null;
		}
    }

    //Show a BufferedImage object
	public static void show(final BufferedImage image, final int x, final int y, final String title) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("gamma correction example ["+title+"]");
                frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                frame.add(new JLabel(new ImageIcon(image)));
                frame.pack();
                frame.setLocation(x, y);
                frame.setVisible(true);
            }
        });
    }
	
}
