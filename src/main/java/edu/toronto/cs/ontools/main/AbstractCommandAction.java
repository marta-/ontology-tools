package edu.toronto.cs.ontools.main;

public abstract class AbstractCommandAction implements CommandAction {
	private boolean debugMode = false;

	@Override
	public boolean isDebugMode() {
		return this.debugMode;
	}

	@Override
	public void setDebugMode(boolean debug) {
		this.debugMode = debug;
	}

}
