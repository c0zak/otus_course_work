#!/bin/bash

sudo firewall-cmd --zone=public --add-port=22/tcp --permanent
sudo firewall-cmd --zone=public --add-port=53/tcp --permanent
sudo firewall-cmd --zone=public --add-port=53/udp --permanent
sudo firewall-cmd --zone=public --add-port=80/tcp --permanent
sudo firewall-cmd --zone=public --add-port=443/tcp --permanent
sudo firewall-cmd --zone=public --add-port=2375/tcp --permanent
sudo firewall-cmd --zone=public --add-port=2379-2380/tcp --permanent
sudo firewall-cmd --zone=public --add-port=6443/tcp --permanent
sudo firewall-cmd --zone=public --add-port=8472/udp --permanent
sudo firewall-cmd --zone=public --add-port=9099/tcp --permanent
sudo firewall-cmd --zone=public --add-port=9100/tcp --permanent
sudo firewall-cmd --zone=public --add-port=10250-10252/tcp --permanent
sudo firewall-cmd --zone=public --add-port=10254/tcp --permanent
sudo firewall-cmd --zone=public --add-port=22496-22497/tcp --permanent
sudo firewall-cmd --zone=public --add-port=30000-32767/tcp --permanent
sudo firewall-cmd --add-masquerade --permanent
sudo firewall-cmd --zone=public --add-rich-rule='rule family="ipv4" source address="192.168.2.0/24" accept' --permanent
sudo firewall-cmd --zone=public --add-rich-rule='rule family="ipv4" source address="192.168.2.0/24" protocol value="udp" accept' --permanent
sudo firewall-cmd --zone=public --add-rich-rule='rule family="ipv4" source address="10.233.0.0/16" accept' --permanent
sudo firewall-cmd --reload

sudo nft add rule inet firewalld filter_FWD_public_allow ip saddr 10.233.0.0/16 accept
sudo nft add rule inet firewalld filter_FWD_public_allow ip daddr 10.233.0.0/16 accept