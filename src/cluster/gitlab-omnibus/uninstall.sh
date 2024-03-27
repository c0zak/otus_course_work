#!/bin/bash

kubectl delete namespace gitlab
kubectl delete pv gitlab-pv-config gitlab-pv-data gitlab-pv-logs
kubectl delete clusterrole gitlab-runner
kubectl delete clusterrolebindings.rbac.authorization.k8s.io gitlab-runner
sudo rm -rf /opt/course_storages/gitlab