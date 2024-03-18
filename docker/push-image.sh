#!/bin/bash

docker tag rats:1.0.2 localhost:8124/rats:latest
docker push localhost:8124/rats:latest