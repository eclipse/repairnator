package fr.inria.spirals.repairnator.config;

/**
 * Configuration manager for Sequencer repair step:
 * Config values are read directly as environment variables,
 * so that we avoid adding more options through the launcher's
 * command line args.
 */
public final class SequencerConfig {

    public final String dockerTag;
    public final int threads;
    public final int beam_size;
    public final int timeout;

    private static SequencerConfig instance;

    private SequencerConfig(){
        this.dockerTag = getEnvOrDefault("SEQUENCER_DOCKER_TAG", "repairnator/sequencer:2.0");
        this.threads = Integer.parseInt(getEnvOrDefault("SEQUENCER_THREADS", "4"));
        this.beam_size = Integer.parseInt(getEnvOrDefault("SEQUENCER_BEAM_SIZE", "50"));
        this.timeout = Integer.parseInt(getEnvOrDefault("SEQUENCER_TIMEOUT", "120"));
    }

    private String getEnvOrDefault(String name, String dfault){

        String env = System.getenv(name);
        if(env == null || env.equals(""))
            return dfault;

        return env;
    }

    public static SequencerConfig getInstance(){
        if (instance == null)
            instance = new SequencerConfig();

        return instance;
    }


}
