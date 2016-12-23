package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.LogHelper;
import fr.inria.spirals.jtravis.pojos.LogPojo;

/**
 * Created by urli on 21/12/2016.
 */
public class Log extends LogPojo {

    @Override
    public String getBody() {
        if (super.getBody() != null && !super.getBody().equals("")) {
            return super.getBody();
        } else {
            String body = LogHelper.getRawLogFromEmptyLog(this);
            this.setBody(body);
            return body;
        }
    }
}
