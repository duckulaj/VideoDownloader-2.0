package com.hawkins.jobs;

interface DetailedJob extends Runnable {
    int getProgress();

    String getState();

    String getJobName();
}
