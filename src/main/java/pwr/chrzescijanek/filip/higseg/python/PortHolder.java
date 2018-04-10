package pwr.chrzescijanek.filip.higseg.python;

public enum PortHolder {
	INSTANCE;
	
	private int port = -1;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
}
