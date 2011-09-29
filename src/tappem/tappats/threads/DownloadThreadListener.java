package tappem.tappats.threads;

public interface DownloadThreadListener {

	

	void handleDownloadThreadUpdate(DownloadTask dt);
	void handleUpdateUI(String text, int code);
}
