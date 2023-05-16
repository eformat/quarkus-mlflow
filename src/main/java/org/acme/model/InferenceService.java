package org.acme.model;

import io.kubernetes.client.openapi.models.V1ObjectMeta;

public class InferenceService {
    public String apiVersion;
    public String kind;

    public V1ObjectMeta metadata;

    public Spec spec;

    @Override
    public String toString() {
        return "InferenceService{" +
                "apiVersion='" + apiVersion + '\'' +
                ", kind='" + kind + '\'' +
                ", metadata=" + metadata +
                ", spec=" + spec +
                '}';
    }
}
