package org.acme.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.model.InferenceService;
import org.jboss.logging.Logger;
import org.mlflow.api.proto.ModelRegistry;
import org.mlflow.tracking.MlflowClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/api")
public class MlFlowResource {

    private static final Logger log = Logger.getLogger(MlFlowResource.class);

    private MlflowClient mlflowClient;

    @Inject
    private OpenShiftClient openshiftClient;

    private final String NAMESPACE = "mlflow-demo";
    private final String MLFLOW_URI = "http://localhost:5500";

    @Startup
    public void start() {
        initializeClients();
    }

    @GET
    @Path("latest/{stage}/{modelName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestVersions(@PathParam("stage") String stage, @PathParam("modelName") String modelName) {
        // stage must be one of None, Staging, Production, Archived
        if (stage == null || stage.isEmpty()) stage = "Staging";
        List<ModelRegistry.ModelVersion> models = mlflowClient.getLatestVersions(modelName, List.of(stage));
        final String regex = "/(.*)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        HashMap ret = new HashMap();
        models.stream().forEach(m -> {
            log.info("  Exp: " + m);
            dump(m);
            ret.put("action", "create");
            ret.put("name", m.getName());
            ret.put("namespace", NAMESPACE);
            ret.put("version", m.getVersion());
            ret.put("stage", m.getCurrentStage());
            ret.put("source", m.getSource());
            Matcher matcher;
            matcher = pattern.matcher(m.getSource());
            if (matcher.find()) {
                createIsvc(NAMESPACE, m.getName().toLowerCase() + "-v" + m.getVersion().toLowerCase(), "aws-connection-minio", matcher.group(1) + "/model.pkl");
            }
        });
        return Response.ok(ret).status(Response.Status.CREATED).build();
    }

    private void initializeClients() {
        mlflowClient = new MlflowClient(MLFLOW_URI);
    }

    private void dump(ModelRegistry.ModelVersion m) {
        log.info("Name: " + m.getName());
        log.info("Tags Count: " + m.getTagsCount());
        log.info("CurrentStage: " + m.getCurrentStage());
        log.info("Source: " + m.getSource());
    }


    private void createIsvc(String namespace, String serviceName, String storageKey, String storagePath) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("is.json");
        InferenceService inferenceService;
        try {
            inferenceService = new ObjectMapper().registerModule(new JavaTimeModule()).readValue(inputStream, InferenceService.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        inferenceService.metadata.name(serviceName);
        inferenceService.metadata.getLabels().put("name", serviceName);
        inferenceService.metadata.namespace(namespace);
        inferenceService.spec.predictor.model.modelFormat.name = "sklearn";
        inferenceService.spec.predictor.model.storage.key = storageKey;
        inferenceService.spec.predictor.model.storage.path = storagePath;
        log.info(inferenceService);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        String yaml;
        try {
            yaml = mapper.writeValueAsString(inferenceService);
            log.info(yaml);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        openshiftClient.resource(yaml).createOrReplace();
    }

    @DELETE
    @Path("latest/{stage}/{modelName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteModel(@PathParam("stage") String stage, @PathParam("modelName") String modelName) {
        if (stage == null || stage.isEmpty()) stage = "Staging";
        List<ModelRegistry.ModelVersion> models = mlflowClient.getLatestVersions(modelName, List.of(stage));
        final String regex = "/(.*)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        HashMap ret = new HashMap();
        models.stream().forEach(m -> {
            log.info("  Exp: " + m);
            dump(m);
            ret.put("action", "deleted");
            ret.put("name", m.getName());
            ret.put("namespace", NAMESPACE);
            ret.put("version", m.getVersion());
            ret.put("stage", m.getCurrentStage());
            ret.put("source", m.getSource());
            Matcher matcher;
            matcher = pattern.matcher(m.getSource());
            if (matcher.find()) {
                deleteIsvc(NAMESPACE, m.getName().toLowerCase() + "-v" + m.getVersion().toLowerCase());
            }
        });
        return Response.ok(ret).status(Response.Status.ACCEPTED).build();
    }

    private void deleteIsvc(String namespace, String serviceName) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("is.json");
        InferenceService inferenceService;
        try {
            inferenceService = new ObjectMapper().registerModule(new JavaTimeModule()).readValue(inputStream, InferenceService.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        inferenceService.metadata.name(serviceName);
        inferenceService.metadata.getLabels().put("name", serviceName);
        inferenceService.metadata.namespace(namespace);
        log.info(inferenceService);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        String yaml;
        try {
            yaml = mapper.writeValueAsString(inferenceService);
            log.info(yaml);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        openshiftClient.resource(yaml).delete();
    }

    @Scheduled(every="10s")
    void reconcile() {
        getLatestVersions("Staging","TestModel");
    }
}
