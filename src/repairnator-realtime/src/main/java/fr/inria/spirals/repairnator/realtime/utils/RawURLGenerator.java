package fr.inria.spirals.repairnator.realtime.utils;

import fr.inria.spirals.repairnator.config.SequencerConfig;

import java.text.MessageFormat;

public class RawURLGenerator {

    static String URLTemplate;
    public static String Generate(String repoSlug, String sha, String filename){
        if (URLTemplate == null){
            SequencerConfig.RAW_URL_SOURCE source = SequencerConfig.getInstance().rawURLSource;
            switch (source){
                case RAW_GITHUB:
                default:
                    URLTemplate = "https://raw.githubusercontent.com/{0}/{1}/{2}";
            }
        }
        return MessageFormat.format(URLTemplate, repoSlug, sha, filename);
    }
}


