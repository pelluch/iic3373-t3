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
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import static java.lang.System.*;

public class CLParams {
	private String kernelFile;
	private CLProgram program;
	private CLContext context;
	private CLCommandQueue queue;
	private CLDevice device;
	private Map<String, CLKernel> kernels;
	public CLParams(String kernelFile)
	{
		this.kernelFile = kernelFile;
		this.kernels = new HashMap<String, CLKernel>();
	}
	
	public CLKernel getKernel(String kernelName) {
		if(kernels.containsValue(kernelName))
			return kernels.get(kernelName);
		
		return null;	
	}

	public void init() throws IOException
	{
		// set up (uses default CLPlatform and creates context for all devices)
        context = CLContext.create();
        
        // select fastest device
        device = context.getMaxFlopsDevice();

        // create program and initialize the kernels Map
        program = context.createProgram(CLParams.class.getResourceAsStream(kernelFile)).build();
        kernels = program.createCLKernels();
	}
	
	public void dispose()
	{
        context.release();
	}
}
