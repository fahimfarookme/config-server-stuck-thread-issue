#!/bin/bash

source functions.sh

build_project $3
start_server $1 $2 $3
wait_till_started $1

printf "\nSanity testing /health endpoint...\n"
curl -s http://localhost:$1/actuator/health

printf "\nInvoking /health endpoint (in every .2s) before N/W partition...\n"
invoke_health_endpoint $1 0.2s &
pid_curl=$!
sleep 20s

simulate_network_partition $2
printf "\nN/W partition will continue for 180 seconds. Check whether any more /health requests complete?\n"
kill -9 $pid_curl 1>>$log_file 2>>$log_file
invoke_health_endpoint $1 3s &
pid_curl=$!
sleep 180s

$JAVA_HOME/bin/jstack $pid_java > ../thread-dumps/$3.tdump
printf "\nThread dump created - ../thread-dumps/$3.tdump\n"

fix_simulated_network_partition
sleep 20s

kill -9 $pid_curl 1>>$log_file 2>>$log_file
kill -9 $pid_java 1>>$log_file 2>>$log_file
printf "\nDONE!\n"

