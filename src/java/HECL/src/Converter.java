import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLImageFormat.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;


public class Converter {

	public static enum Type { RGB, SPHERICAL, LUV, HSV };
	
	public static CLImage2d<FloatBuffer> convertTo(CLContext context, CLDevice device, CLProgram program, 
			String kernelName, Type inputType, Type outputType) {
		
		CLImage2d<FloatBuffer> imageA = context.createImage2d(Buffers.newDirectFloatBuffer(pixels), image.getWidth(), image.getHeight(), format); 
        CLImage2d<FloatBuffer> imageB = context.createImage2d(Buffers.newDirectFloatBuffer(pixels.length), image.getWidth(), image.getHeight(), format); 
		
        
		
	}
}
