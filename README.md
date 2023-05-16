# quarkus-mlflow

⚠️ This is a Prototype ⚠️

A simple service that polls a [mlflow](https://github.com/mlflow/mlflow) server and creates a [model mesh](https://github.com/kserve/modelmesh-serving) `InferenceService`.

[![mlflow-model-mesh](http://img.youtube.com/vi/5Y_Sukskk_E/0.jpg)](http://www.youtube.com/watch?v=5Y_Sukskk_E "Model Mesh MlFlow")

See Links:
- https://github.com/eformat/rhods-mlserver-example

## Demo

Port forward [mlflow](https://ai-on-openshift.io/tools-and-applications/mlflow/mlflow/) running on OpenShift

```bash
oc -n daintree-dev port-forward svc/mlflow 5500:5500
```

Run this app with oc logged into your cluster already

```bash
mvn quarkus:dev
```

Setup mlserver serving runtime from here:
- https://github.com/eformat/rhods-mlserver-example
