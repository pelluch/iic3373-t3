import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
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

	private static final int HIST_SIZE = 255;
	
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

		 // set up (uses default CLPlatform and creates context for all devices)
        CLContext context = CLContext.create();
        
        out.println("created "+context);
        
        // always make sure to release the context under all circumstances
        // not needed for this particular sample but recommented
        try{
            
        	
            // select fastest device
            CLDevice device = context.getMaxFlopsDevice();
            out.println("using "+device);

            // create command queue on device.
            CLCommandQueue queue = device.createCommandQueue();

            int elementCount = 1444477;                                  // Length of arrays to process
            int localWorkSize = min(device.getMaxWorkGroupSize(), 256);  // Local work size dimensions
            int globalWorkSize = roundUp(localWorkSize, elementCount);   // rounded up to the nearest multiple of the localWorkSize

            // load sources, create and build program
            CLProgram program = context.createProgram(HECL.class.getResourceAsStream("calc_hist.cl")).build();

            // load image
            BufferedImage image = readImage("lena.png");
            assert image.getColorModel().getNumComponents() == 3;
            
            float[] pixels = image.getRaster().getPixels(0, 0, image.getWidth(), image.getHeight(), (float[])null);
            float[] histogram = new float[HIST_SIZE];
            
            
/*            float[][] imageRows = new float[image.getHeight()][];
            
            for(int i = 0; i < image.getHeight(); i++)
                imageRows[i] = image.getRaster().getPixels(0, i, image.getWidth(), 1, (float[])null);
            
            float[][] histograms = new float[image.getHeight()][HIST_SIZE];
            
            
            // copy to direct float buffer
            FloatBuffer fb = Buffers.newDirectFloatBuffer(pixels);
            
            */
            // copy to direct float buffer
            FloatBuffer fb = Buffers.newDirectFloatBuffer(pixels);
            
            // allocate a OpenCL buffer using the direct fb as working copy
            CLBuffer<FloatBuffer> buffer = context.createBuffer(fb, CLBuffer.Mem.READ_WRITE);
            
            // A, B are input buffers, C is for the result
            CLBuffer<FloatBuffer> clBufferA = context.createFloatBuffer(globalWorkSize, READ_ONLY);
            CLBuffer<FloatBuffer> clBufferB = context.createFloatBuffer(globalWorkSize, READ_ONLY);
            CLBuffer<FloatBuffer> clBufferC = context.createFloatBuffer(globalWorkSize, WRITE_ONLY);

            out.println("used device memory: "
                + (clBufferA.getCLSize()+clBufferB.getCLSize()+clBufferC.getCLSize())/1000000 +"MB");

            // fill input buffers with random numbers
            // (just to have test data; seed is fixed -> results will not change between runs).
            //fillBuffer(clBufferA.getBuffer(), 12345);
            //fillBuffer(clBufferB.getBuffer(), 67890);

            // get a reference to the kernel function with the name 'VectorAdd'
            // and map the buffers to its input parameters.
            CLKernel kernel = program.createCLKernel("calc_hist");
            kernel.putArgs(clBufferA, clBufferB, clBufferC).putArg(elementCount);

            // asynchronous write of data to GPU device,
            // followed by blocking read to get the computed results back.
            long time = nanoTime();
            queue.putWriteBuffer(clBufferA, false)
                 .putWriteBuffer(clBufferB, false)
                 .put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize)
                 .putReadBuffer(clBufferC, true);
            time = nanoTime() - time;

            // print first few elements of the resulting buffer to the console.
            //out.println("a+b=c results snapshot: ");
/*            for(int i = 0; i < 10; i++)
                out.print(clBufferC.getBuffer().get() + ", ");
            out.println("...; " + clBufferC.getBuffer().remaining() + " more");

            out.println("computation took: "+(time/1000000)+"ms");*/
            
        } catch(IOException ioException) {
        	
        }
        finally{
            // cleanup all resources associated with this context.
            context.release();
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

}
