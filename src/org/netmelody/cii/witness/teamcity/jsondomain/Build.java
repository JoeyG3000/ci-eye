package org.netmelody.cii.witness.teamcity.jsondomain;

import org.netmelody.cii.domain.Status;

public final class Build {
    public long id;
    public String number;
    public String status;
    public String buildTypeId;
    public String href;
    public String webUrl;
    public boolean running;
    public int percentageComplete;
    
    public Status status() {
        if (status == null || "SUCCESS".equals(status)) {
            return Status.GREEN;
        }
        return Status.BROKEN;
    }
}