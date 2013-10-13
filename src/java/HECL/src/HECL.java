import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLImageFormat.*;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import static java.lang.System.*;
import static com.jogamp.opencl.CLMemory.Mem.*;
import static java.lang.Math.*;

public class HECL {

	private static final int HIST_SIZE = 256;
	
    private static void show(final BufferedImage image, final int x, final int y, final String title) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("gamma correction example ["+title+"]");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new JLabel(new ImageIcon(image)));
                frame.pack();
                frame.setLocation(x, y);
                frame.setVisible(true);
            }
        });
    }
    
    private static InputStream getStreamFor(String filename) {
        return HECL.class.getResourceAsStream(filename);
    }
    
    public static BufferedImage readImage(String filename) throws IOException {
        return ImageIO.read(getStreamFor(filename));
    }

    private static BufferedImage createImage(int width, int height, CLBuffer<FloatBuffer> buffer) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        float[] pixels = new float[buffer.getBuffer().capacity()];
        buffer.getBuffer().get(pixels).rewind();
        image.getRaster().setPixels(0, 0, width, height, pixels);
        return image;
    }
    
	public static void main(String[] args) {

		CLParams clParams = new CLParams("kernels.cl");

		try{
			// Create the CLParams instance for this program:
			clParams.init();
        
            // load image
            BufferedImage image = readImage("lena_f.png");
            
            // Call copyImage:
            BufferedImage resultImage = copyImage(clParams, image);
            show(resultImage, image.getWidth()/2, 50, "Resulting Image");
            
        } 
		catch(IOException ioException) {
        	
        }
        finally{
            // cleanup all resources associated with this context.
            clParams.release();
        }
		

	}
	
    private static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }
    
    private static BufferedImage copyImage(CLParams clParams, BufferedImage image)
    {
    	CLContext context = clParams.getContext();
    	CLCommandQueue queue = clParams.getQueue();
    	
  
        float[] pixels = image.getRaster().getPixels(0, 0, image.getWidth(), image.getHeight(), (float[])null);

        // copy to direct float buffer
        FloatBuffer fb = Buffers.newDirectFloatBuffer(pixels);
        
        // allocate a OpenCL buffer using the direct fb as working copy
        CLBuffer<FloatBuffer> buffer = context.createBuffer(fb, CLBuffer.Mem.READ_WRITE);
        
        int localWorkSize = queue.getDevice().getMaxWorkGroupSize(); // Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, fb.capacity());  // rounded up to the nearest multiple of the localWorkSize
        
        
        System.out.println("Number of pixels: " + pixels.length);
        System.out.println("Global work size: " + globalWorkSize);
        System.out.println("Local work size: " + localWorkSize);

        CLKernel kernel = clParams.getKernel("copy_image");
        kernel.putArg(buffer).putArg(image.getWidth()).putArg(image.getHeight()).rewind();
        
        long time = nanoTime();
        queue.putWriteBuffer(buffer, false);
        queue.put2DRangeKernel(kernel, 0, 0, image.getWidth(), image.getHeight(), 0, 0);
        queue.putReadBuffer(buffer, true);
        
       // .putReadImage(imageB, true);
        time = nanoTime() - time;
        
        // show resulting image.
       // FloatBuffer bufferB = buffer.getBuffer();
                    
        //CLBuffer<FloatBuffer> buffer = context.createBuffer(bufferB, CLBuffer.Mem.READ_WRITE);
        BufferedImage resultImage = createImage(image.getWidth(), image.getHeight(), buffer); 

        out.println("computation took: "+(time/1000000)+"ms");
        
        return resultImage;
    }

}
