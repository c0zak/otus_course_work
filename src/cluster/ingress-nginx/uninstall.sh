#!/bin/bash

helm -n ingress-nginx uninstall ingress-nginx
kubectl delete namespace ingress-nginx