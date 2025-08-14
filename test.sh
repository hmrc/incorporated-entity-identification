#!/usr/bin/env bash

sbt coverage test it/test coverageReport dependencyUpdates
