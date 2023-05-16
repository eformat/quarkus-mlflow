package org.acme.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Model {
    public String runtime;
    @JsonProperty("modelFormat")
    public ModelFormat modelFormat;
    @JsonProperty("storage")
    public Storage storage;

    @Override
    public String toString() {
        return "Model{" +
                "runtime='" + runtime + '\'' +
                ", modelFormat=" + modelFormat +
                ", storage=" + storage +
                '}';
    }
}
