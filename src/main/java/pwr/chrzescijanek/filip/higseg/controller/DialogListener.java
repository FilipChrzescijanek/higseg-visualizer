package pwr.chrzescijanek.filip.higseg.controller;

import javafx.stage.Stage;

public class DialogListener {

	private int images = 0;
	private Stage dialog;
	
	public DialogListener(Stage dialog, int images) {
		this.dialog = dialog;
		this.images = images;
	}

	public void decrement() {
		this.images--;
		if (this.images == 0) {
			dialog.close();
		}
	}

}
