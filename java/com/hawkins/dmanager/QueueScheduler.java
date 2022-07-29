package com.hawkins.dmanager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.hawkins.dmanager.util.DManagerUtils;
import com.hawkins.dmanager.util.DateTimeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueueScheduler implements Runnable {
	
	

	private boolean stop;
	private long lastKeepAwakePing = 0L;

	private static QueueScheduler _this;

	private QueueScheduler() {
		stop = false;
	}

	public static QueueScheduler getInstance() {
		if (_this == null) {
			_this = new QueueScheduler();
		}
		return _this;
	}

	public void start() {
		lastKeepAwakePing = System.currentTimeMillis();
		new Thread(this).start();
	}

	@Override
	public void run() {
		try {
			Calendar cal = Calendar.getInstance();

			while (!stop) {
				try {
					long currentTime = System.currentTimeMillis();
					if (currentTime - lastKeepAwakePing > 3000) {
						if (!DManagerApp.getInstance().isAllFinished()) {
							DManagerUtils.keepAwakePing();
							lastKeepAwakePing = currentTime;
						}
					}

					ArrayList<DownloadQueue> queues = QueueManager.getInstance().getQueueList();
					for (int i = 0; i < queues.size(); i++) {
						DownloadQueue queue = queues.get(i);
						if (queue.isRunning() || queue.getStartTime() == -1) {
							continue;
						}
						Date now = new Date();
						cal.setTime(now);
						Date onlyDate = DateTimeUtils.getDatePart(cal);
						long seconds = DateTimeUtils.getTimePart(now);

						if (seconds > queue.getStartTime()) {
							if (queue.getEndTime() > 0) {
								if (queue.getEndTime() < seconds) {
									continue;
								}
							}
						} else {
							continue;
						}

						if (queue.isPeriodic()) {
							int day = cal.get(Calendar.DAY_OF_WEEK);
							int mask = 0x01 << (day - 1);

							if ((queue.getDayMask() & mask) != mask) {
								continue;
							}
						} else {
							Date execDate = queue.getExecDate();
							if (execDate == null) {
								continue;
							}
							cal.setTime(execDate);
							Date onlyDate2 = DateTimeUtils.getDatePart(cal);
							if (onlyDate.compareTo(onlyDate2) < 0) {
								continue;
							}
						}
						queue.start();
					}

					for (int i = 0; i < queues.size(); i++) {
						DownloadQueue queue = queues.get(i);
						if (!queue.isRunning()) {
							continue;
						}
						if (queue.getEndTime() < 1) {
							continue;
						}
						Date now = new Date();
						long seconds = DateTimeUtils.getTimePart(now);
						if (queue.getEndTime() < seconds) {
							queue.stop();
						}
					}
					Thread.sleep(1000);
				} catch (Exception e2) {
					log.info("error in scheduler: " + e2);
					log.info(e2.getMessage());
				}

				// long now = System.currentTimeMillis();
			}
		} catch (Exception e) {
			log.info("error in scheduler: " + e);
			log.info(e.getMessage());
		}
	}
}
