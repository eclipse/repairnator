package fr.inria.spirals.repairnator.process.step.logParser;

import java.util.HashMap;

public class Element {
        HashMap<String, Object> dict;

        Element(){
            dict = new HashMap<String, Object>();
        }

        public Element put(String k, Object v){
            dict.put(k, v);
            return this;
        }

        Element remove(String k){
            dict.remove(k);
            return this;
        }

        public <T> T get (String k){
            return (T)dict.get(k);
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();

            for(String key : dict.keySet()){
                sb.append(key + "=" + dict.get(key) + " ");
            }

            return sb.toString();
        }
    }
