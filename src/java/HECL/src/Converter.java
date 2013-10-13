import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLImageFormat.*;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;


public class Converter {


	
	public static CLImage2d<FloatBuffer> convertToSpherical(CLParams clParams, BufferedImage inputImage) {
				
		int width = inputImage.getWidth();
		int height = inputImage.getHeight();
		
		float[] pixels = inputImage.getRaster().getPixels(0, 0, width, height, (float[])null);
		CLImageFormat format = new CLImageFormat(ChannelOrder.RGB, ChannelType.FLOAT);
		
		CLImage2d<FloatBuffer> outputImage = clParams.getContext().createImage2d(
				Buffers.newDirectFloatBuffer(pixels),
				width,
				height,
				format); 		
		
		
		
		return outputImage;
		
	}
}
