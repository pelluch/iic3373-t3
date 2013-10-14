import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLImageFormat.*;
import com.jogamp.opencl.CLKernel;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.InputMismatchException;
import java.util.Scanner;

import static java.lang.System.*;

public class HECL {

	private static final int HIST_SIZE = 256;


	private static CLParams clParams = null;

	public static float[] testRgbToSpherical() {


		BufferedImage image = ImageUtils.readImage("lena_f.png");
		CLBuffer<FloatBuffer> resultImage = Converter.convertToSpherical(clParams, image);
		float[] pixels = new float[image.getData().getDataBuffer().getSize()];
		resultImage.getBuffer().get(pixels, 0, pixels.length);
		for(int i = 0; i < 100; ++i) {
			System.out.print(pixels[i] + " ");
		}
		System.out.println();
		return pixels;
	}

	public static void testSphericalToRgb(float[] sphericalImageFloats) {
		BufferedImage image = null;
		image = ImageUtils.readImage("lena_f.png");
		BufferedImage rgbImage = Converter.convertToRGB(clParams, sphericalImageFloats, image.getWidth(), image.getHeight());        
		ImageUtils.show(rgbImage, 0, 0, "RGB Image");
	}

	private static void testCopyImage()
	{
		BufferedImage image = ImageUtils.readImage("lena_f.png");
		ImageUtils.show(image, 0, 0, "Original image");
		CLContext context = clParams.getContext();
		CLCommandQueue queue = clParams.getQueue();

		float[] pixels = image.getRaster().getPixels(0, 0, image.getWidth(), image.getHeight(), (float[])null);

		// We use ChanelOrder.INTENSITY because it's grey
		CLImageFormat format = new CLImageFormat(ChannelOrder.INTENSITY, ChannelType.FLOAT);
		CLImage2d<FloatBuffer> imageA = context.createImage2d(Buffers.newDirectFloatBuffer(pixels), image.getWidth(), image.getHeight(), format); 
		CLImage2d<FloatBuffer> imageB = context.createImage2d(Buffers.newDirectFloatBuffer(pixels.length), image.getWidth(), image.getHeight(), format); 

		out.println("used device memory: "
				+ (imageA.getCLSize()+imageB.getCLSize())/1000000 +"MB");

		// get a reference to the kernel function with the name 'copy_image'
		// and map the buffers to its input parameters.
		CLKernel kernel = clParams.getKernel("copy_image");
		kernel.putArgs(imageA, imageB).putArg(image.getWidth()).putArg(image.getHeight());

		// asynchronous write of data to GPU device,
		// followed by blocking read to get the computed results back.
		long time = nanoTime();
		queue.putWriteImage(imageA, false)
		.put2DRangeKernel(kernel, 0, 0, image.getWidth(), image.getHeight(), 0, 0)
		.putReadImage(imageB, true);
		time = nanoTime() - time;

		// show resulting image.
		FloatBuffer bufferB = imageB.getBuffer();

		CLBuffer<FloatBuffer> buffer = context.createBuffer(bufferB, CLBuffer.Mem.READ_WRITE);
		BufferedImage resultImage = ImageUtils.createImage(image.getWidth(), image.getHeight(), buffer); 
		ImageUtils.show(resultImage, 0, 0, "Image copy");
		out.println("computation took: "+(time/1000000)+"ms");

	}

	public static void testVideo() {
		Video inputVideo = new Video("input/short.mp4");
		inputVideo.processVideo(25);
	}
	
	public static boolean Menu() {

		@SuppressWarnings("resource")
		Scanner reader = new Scanner(System.in);
		System.out.println("Choose an option:\n" + 
				"1. Test RGB to Spherical\n" + 
				"2. Test Spherical to RGB\n" + 
				"3. Test Copy image\n" + 
				"4. Test Video processing\n" + 
				"5. Exit");
		boolean exit = false;
		try {
			if(reader.hasNext()) {
				int choice = reader.nextInt();
				if(choice < 0 || choice > 5) return false;

				switch(choice) {
				case 1:
					testRgbToSpherical();  		
					break;
				case 2:
					testSphericalToRgb(testRgbToSpherical());
					break;
				case 3:
					testCopyImage();
					break;
				case 4:
					testVideo();
					break;
				case 5:
					exit = true;
					break;
				}
			}
			else {
				return false;
			}

		}    	
		catch(InputMismatchException ioException) {
			System.out.println("Invalid, must enter a number.");
			return false;
		}
		finally {
			if(exit && clParams != null) {
				clParams.release();
				clParams = null;
			}
		}
		return exit;    	
	}

	public static void main(String[] args) {

		clParams = new CLParams("kernels.cl");
		try {
			clParams.init();
		} catch (IOException e) {
			System.out.println("Kernels file not found");
			e.printStackTrace();
		}
		boolean exit = false;
		while(!exit) {
			exit = Menu();
		}
	}


	public static int roundUp(int groupSize, int globalSize) {
		int r = globalSize % groupSize;
		if (r == 0) {
			return globalSize;
		} else {
			return globalSize + groupSize - r;
		}
	}

	private static int[] getHistogram(CLParams clParams, CLImage2d<FloatBuffer> image)
	{   	
		CLContext context = clParams.getContext();
		CLCommandQueue queue = clParams.getQueue();

		// Array to store the histogram results
		int[] histogram = new int[HIST_SIZE];

		CLBuffer<IntBuffer> histBuffer =  context.createBuffer(Buffers.newDirectIntBuffer(histogram),CLBuffer.Mem.READ_WRITE);

		// get a reference to the kernel function with the name 'calc_hist'
		// and map the buffers to its input parameters.
		CLKernel kernel = clParams.getKernel("calc_hist");
		kernel.putArg(image).putArg(image.height).putArg(histBuffer).putArg(HIST_SIZE);

		// asynchronous write of data to GPU device,
		// followed by blocking read to get the computed results back.
		long time = nanoTime();
		queue.putWriteImage(image, false)
		.put2DRangeKernel(kernel, 0, 0, image.getWidth(), 1, 0, 0)
		.putReadBuffer(histBuffer, true);
		time = nanoTime() - time;

		return histogram;
	}

	private static BufferedImage equalizeImage(CLParams clParams, BufferedImage image, CLImageFormat format)
	{
		CLContext context = clParams.getContext();
		CLCommandQueue queue = clParams.getQueue();

		float[] pixels = image.getRaster().getPixels(0, 0, image.getWidth(), image.getHeight(), (float[])null);

		CLImage2d<FloatBuffer> imageA = context.createImage2d(Buffers.newDirectFloatBuffer(pixels), image.getWidth(), image.getHeight(), format); 

		int[] histogram = getHistogram(clParams, imageA);

		CLImage2d<FloatBuffer> imageB = context.createImage2d(Buffers.newDirectFloatBuffer(pixels.length), image.getWidth(), image.getHeight(), format); 
		/*
        out.println("used device memory: "
            + (imageA.getCLSize()+imageB.getCLSize())/1000000 +"MB");

        // get a reference to the kernel function with the name 'calc_hist'
        // and map the buffers to its input parameters.
        CLKernel kernel = clParams.getKernel("copy_image");
        kernel.putArgs(imageA, imageB).putArg(image.getWidth()).putArg(image.getHeight());

        // asynchronous write of data to GPU device,
        // followed by blocking read to get the computed results back.
        long time = nanoTime();
        queue.putWriteImage(imageA, false)
             .put2DRangeKernel(kernel, 0, 0, image.getWidth(), image.getHeight(), 0, 0)
        .putReadImage(imageB, true);
        time = nanoTime() - time;

		 */      // show resulting image.
		FloatBuffer bufferB = imageB.getBuffer();

		CLBuffer<FloatBuffer> buffer = context.createBuffer(bufferB, CLBuffer.Mem.READ_WRITE);
		BufferedImage resultImage = ImageUtils.createImage(image.getWidth(), image.getHeight(), buffer); 

		//    out.println("computation took: "+(time/1000000)+"ms");

		return resultImage;    	
	}



}
