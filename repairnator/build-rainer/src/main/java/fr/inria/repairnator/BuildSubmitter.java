package fr.inria.repairnator;

// Submit build ids received from websocket
public interface BuildSubmitter {
	public void submit(String build_str);
}
