package fr.inria.spirals.jtravis.entities;

/**
 * Created by urli on 21/12/2016.
 */
public class Job {
    private int id;
    private Build build;
    private Repository repository;
    private Commit commit;
    private Log log;
    private int number;
    private String config;

    // TODO: get other attribute from doc
}
