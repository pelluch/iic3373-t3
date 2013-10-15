import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLKernel;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import static java.lang.System.*;

public class Converter {


	public static enum ConversionType { HSV, SPHERICAL };
	
	public static String typeToString(ConversionType type) 
	{
		if(type == ConversionType.HSV)
		{
			return "hsv";
		}
		else
		{
			return "spherical";
		}
		
	}
	public static CLBuffer<FloatBuffer> convertRgbTo(CLParams clParams, BufferedImage image, ConversionType type ) {

		out.println("Transforming image to spherical coords");
		int width = image.getWidth();
		int height = image.getHeight();

		float[] pixels = image.getRaster().getPixels(0, 0, width, height, (float[])null);

		// copy to direct float buffer
		FloatBuffer fb = Buffers.newDirectFloatBuffer(pixels);

		// allocate a OpenCL buffer using the direct fb as working copy
		CLBuffer<FloatBuffer> buffer = clParams.getContext().createBuffer(fb, CLBuffer.Mem.READ_WRITE);

		CLKernel kernel = clParams.getKernel("convert_rgb_to_" + typeToString(type));
		kernel.putArg(buffer).putArg(image.getWidth()).putArg(image.getHeight()).rewind();

		long time = nanoTime();
		clParams.getQueue().putWriteBuffer(buffer, false);
		clParams.getQueue().put2DRangeKernel(kernel, 0, 0, image.getWidth(), image.getHeight(), 0, 0);
		clParams.getQueue().putReadBuffer(buffer, true);

		time = nanoTime() - time;

		out.println("computation took: "+(time/1000000)+"ms");
		return buffer;
	}

	public static BufferedImage convertImageToRGB(CLParams clParams, float[] sphericalImageFloats, int width, int height,
			ConversionType inputType) {

		FloatBuffer fb = Buffers.newDirectFloatBuffer(sphericalImageFloats);
		CLBuffer<FloatBuffer> sphericalImageBuffer = clParams.getContext().createBuffer(fb, CLBuffer.Mem.READ_WRITE);
        
		out.println("Transforming image to rgb coords");
		CLKernel kernel = clParams.getKernel("convert_" + typeToString(inputType) + "_to_rgb");
		kernel.putArg(sphericalImageBuffer).putArg(width).putArg(height).rewind();

		long time = nanoTime();
		clParams.getQueue().putWriteBuffer(sphericalImageBuffer, false);
		clParams.getQueue().put2DRangeKernel(kernel, 0, 0, width, height, 0, 0);
		clParams.getQueue().putReadBuffer(sphericalImageBuffer, true);
		time = nanoTime() - time;
		out.println("computation took: "+(time/1000000)+"ms");

		BufferedImage rgbImage = ImageUtils.createImage(width, height, sphericalImageBuffer, BufferedImage.TYPE_INT_RGB);

		return rgbImage;   
	}



}
