import java.io.File;
import java.io.IOException;

import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.ArrayList;

public class Video {


	private String videoFilePath;
	//This one has no extension
	private String videoFileName;
	private String videoDirectory;

	
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
	public boolean processVideo(float fps) {
		
		if(!checkVideoPath()) return false;
		if(!extractAudio()) return false;
		if(!extractFrames(fps, 0, 240)) return false;
		
		if(createFromFrames(fps, "input/image-%3d.jpeg", "equalized.mp4") == null) return false;
		
		return true;
	}




}
