#!/bin/bash

helm -n cert-manager uninstall cert-manager
kubectl delete namespace cert-manager