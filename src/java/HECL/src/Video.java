import java.io.File;
import java.io.IOException;


public class Video {

	
	private String videoFilePath;
	//This one has no extension
	private String videoFileName;
	
	public String getVideoFilePath() {
		return videoFilePath;
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
		return true;
	}
	
	private String getAudioExtractionCommand() {
		
		int nChannels = 2;
		String audioFormat = "mp3";		
		String command = "ffmpeg -i " + videoFilePath + " -vn " + 
				"-ac " + nChannels + "-f " + audioFormat + 
				" " + videoFileName + "." + audioFormat;
		return command;
	}
	
	public boolean processVideo(float fps) {
			
		if(!checkVideoPath()) {
			return false;
		}
		
		String audioCommand = getAudioExtractionCommand();
		
		try {
			
			//First we extract sound
			Process process = new ProcessBuilder(audioCommand).start();
					
			
		} catch (IOException e) {
			System.out.println("Error executing command");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	

}
