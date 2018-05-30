#!/bin/bash

JAVA_HOME=/usr/local/java/

declare -A env_map
env_map[config_repo_dns_git]=github.com
env_map[config_repo_uri_git]="https://${env_map[config_repo_dns_git]}/fahimfarookme/config-repo"
env_map[config_repo_username_git]=
env_map[config_repo_password_git]=

env_map[config_repo_dns_svn]=""
env_map[config_repo_uri_svn]=""
env_map[config_repo_username_svn]=
env_map[config_repo_password_svn]=

log_file=../config-server.log

