#!/bin/bash

mkdir -p /opt/course_storages/gitlab/data
mkdir -p /opt/course_storages/gitlab/logs
mkdir -p /opt/course_storages/gitlab/config
chmod 777 -R /opt/course_storages/

kubectl create namespace gitlab
kubectl apply -f gitlab-omnibus.yml -n gitlab

pod_name=$(kubectl -n gitlab get pods -l app=gitlab -o jsonpath='{.items[0].metadata.name}')

check_status() {
 kubectl -n gitlab get pod $pod_name | grep -q 'Running'
 return $?
}

check_logs() {
 kubectl -n gitlab logs $pod_name | grep -q '"GET /sidekiq HTTP/1.1" 200'
 return $?
}


echo "----------------------------------------"
echo "Wait Gitlab pod startup"
echo

while true; do
 check_status
 if [ $? -eq 0 ]; then
    echo
    echo "Pod $pod_name successfully started"
    break
 else
    echo -n "."
    sleep 1
 fi
done

echo
echo "----------------------------------------"
echo "Wait Gitlab startup"
echo

while true; do
 check_logs
 if [ $? -eq 0 ]; then
    echo
    echo "Gitlab successfully started"
    break
 else
    echo -n "."
    sleep 5
 fi
done

echo
echo "----------------------------------------"
echo "Login: root"
kubectl -n gitlab exec $pod_name -- /bin/sh -c "cat /etc/gitlab/initial_root_password" | grep Password:
echo "----------------------------------------"
echo

kubectl apply -f ingress.yml -n gitlab