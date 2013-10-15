import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class Video {


	private String videoFilePath;
	//This one has no extension
	private String videoFileName;
	private String videoDirectory;
	private String audioFilePath;
	
	public String getVideoFilePath() {
		return videoFilePath;
	}


	public List<String> getImageList() {
		
		File directory = new File(videoDirectory);
		if(!directory.isDirectory()) return null;
		
		List<String> imageList = new ArrayList<String>();
		
		File[] fileList = directory.listFiles();
		for(int i = 0; i < fileList.length; ++i) {
			String fileName = fileList[i].getName();
			if(fileName.endsWith(".jpeg")) {
				imageList.add(fileName);
			}
		}
		return imageList;
	}
	
	public Video(String videoFilePath) {
		this.videoFilePath = videoFilePath;
	}

	private boolean checkVideoPath() {

		if(videoFilePath == null) return false;


		File videoFile = new File(videoFilePath);
		if(!videoFile.exists()) {
			System.out.println("Error: Input video does not exist");
			return false;
		}				


		int pos = videoFilePath.lastIndexOf('.');
		if(pos == -1) return false;

		this.videoFileName = videoFilePath.substring(0, pos);
		this.videoDirectory = videoFile.getParentFile().getAbsolutePath();
		return true;
	}

	private String getAudioExtractionCommand() {

		int nChannels = 2;		
		String audioFormat = "mp3";		
		audioFilePath = videoFileName + "." + audioFormat;
		String command = "/usr/bin/ffmpeg -y -i " + videoFilePath + " -vn " + 
				"-ac " + nChannels + " -f " + audioFormat + 
				" " + videoFileName + "." + audioFormat;
		return command;
	}

	public boolean extractFrames(float fps, int startingSeconds, int duration) {

		String command = "/usr/bin/ffmpeg -y -i " + videoFilePath + " -r " + fps + 
				" -ss " + startingSeconds + " -t " + duration + 
				" -q:v 1 input/image-%3d.jpeg";

		ProcessBuilder extractFrameBuilder = new ProcessBuilder(command.split(" "));
		extractFrameBuilder.redirectOutput(Redirect.INHERIT);
		extractFrameBuilder.redirectError(Redirect.INHERIT);
		extractFrameBuilder.redirectInput(Redirect.INHERIT);
		try {
			Process process = extractFrameBuilder.start();
			try {
				int exitValue = process.waitFor();
				if(exitValue != 0) return false;
			} catch (InterruptedException e) {
				System.out.println("Thread interrutped while waiting for frame extraction");
			}
		} catch (IOException e) {
			System.out.println("Error extracting frames, wrong command");
			return false;
		}	
		
		return true;

	}
	
	public static Video createFromFrames(float fps, String framePattern, String outputFileName) {
		
		Video output = null;
		String command = "/usr/bin/ffmpeg -start_number 1 -y -i " + framePattern + " -r " + fps + 
				" -vcodec mjpeg -q:v 1 " + outputFileName;

		ProcessBuilder videoBuilder = new ProcessBuilder(command.split(" "));
		videoBuilder.redirectOutput(Redirect.INHERIT);
		videoBuilder.redirectError(Redirect.INHERIT);
		videoBuilder.redirectInput(Redirect.INHERIT);
		try {
			Process process = videoBuilder.start();
			try {
				int exitValue = process.waitFor();
				if(exitValue != 0) return null;
			} catch (InterruptedException e) {
				System.out.println("Thread interrutped while waiting for video creation");
			}
		} catch (IOException e) {
			System.out.println("Error extracting frames, wrong command");
			return null;
		}	
		
		output = new Video(outputFileName);
		return output;
	}
	
	
	public boolean extractAudio() {

		String audioCommand = getAudioExtractionCommand();

		try {

			//First we extract sound
			ProcessBuilder audioBuilder = new ProcessBuilder(audioCommand.split(" "));
			audioBuilder.redirectOutput(Redirect.INHERIT);
			audioBuilder.redirectError(Redirect.INHERIT);
			audioBuilder.redirectInput(Redirect.INHERIT);
			Process process = audioBuilder.start();	
			try {
				int exitValue = process.waitFor();
				if(exitValue != 0) return false;
			} catch (InterruptedException e) {
				System.out.println("Thread interrutped while waiting for audio extraction");
			}


		} catch (IOException e) {
			System.out.println("Error executing command");
			e.printStackTrace();
			return false;
		}
		
		return true;


	}
	
	public static Video createFromStreams(String inputVideo, String inputAudio, String outputFileName) {	
		
		Video output = null;
		String audioCommand = "/usr/bin/ffmpeg -y -i " + inputVideo + " -i " + inputAudio + 
				" -map 0:0 -map 1:0,1 -c:v copy -c:a copy " + outputFileName;

		try {

			//First we extract sound
			ProcessBuilder audioBuilder = new ProcessBuilder(audioCommand.split(" "));
			audioBuilder.redirectOutput(Redirect.INHERIT);
			audioBuilder.redirectError(Redirect.INHERIT);
			audioBuilder.redirectInput(Redirect.INHERIT);
			Process process = audioBuilder.start();	
			try {
				int exitValue = process.waitFor();
				if(exitValue != 0) return null;
			} catch (InterruptedException e) {
				System.out.println("Thread interrutped while waiting for audio extraction");
			}


		} catch (IOException e) {
			System.out.println("Error executing command");
			e.printStackTrace();
			return null;
		}
		output = new Video(outputFileName);
		return output;
	}
	
	public boolean processVideo(CLParams clParams, float fps) {
		
		if(!checkVideoPath()) return false;
		if(!extractAudio()) return false;
		if(!extractFrames(fps, 0, 40)) return false;
		if(!processSequence(clParams)) return false;
		Video equalized = createFromFrames(fps, "input/image-%3d.jpeg", "input/equalized.mp4");
		if(equalized == null) return false;
		if(createFromStreams("input/equalized.mp4", audioFilePath, "input/final_video.mp4") == null) return false;
		
		return true;
	}

	// array of supported extensions (use a List if you prefer)
    private final String[] EXTENSIONS = new String[]{
        "jpeg", "jpg" // and other formats you need
    };
    
    // filter to identify images based on their extensions
    private final FilenameFilter IMAGE_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(final File dir, String name) {
            for (final String ext : EXTENSIONS) {
                if (name.endsWith("." + ext)) {
                    return (true);
                }
            }
            return (false);
        }
    };
	
    // Looks for video name in input directory
	private boolean processSequence(CLParams clParams)
	{
		File dir = new File("input");
		
		if (dir.isDirectory()) { // make sure it's a directory
            for (final File f : dir.listFiles(IMAGE_FILTER)) {
                BufferedImage img = null;

                try {
                    img = ImageIO.read(f);

                    BufferedImage result = Equalization.equalizeBufferImage(clParams, img);
                    
                    // done that, we write the file back
                    ImageIO.write(result, "jpeg", f);

                    // you probably want something more involved here
                    // to display in your UI
                    System.out.println("image: " + f.getName());
                    System.out.println(" width : " + img.getWidth());
                    System.out.println(" height: " + img.getHeight());
                    System.out.println(" size  : " + f.length());
                } catch (final IOException e) {
                    // handle errors here
                	return false;
                }
            }
            
            return true;
        }
		return false;
	}
	



}
