package com.hawkins.dmanager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import com.hawkins.dmanager.ui.res.StringResource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueueManager {
	
	

	private static QueueManager _this;
	private ArrayList<DownloadQueue> queueList;

	private QueueManager() {
		queueList = new ArrayList<DownloadQueue>();
		loadQueues();
	}

	public static QueueManager getInstance() {
		if (_this == null) {
			_this = new QueueManager();
		}
		return _this;
	}

	public DownloadQueue getQueueById(String queueId) {
		if (queueId == null)
			return null;
		if (queueId.length() < 1) {
			return queueList.get(0);
		}
		for (int i = 0; i < queueList.size(); i++) {
			DownloadQueue q = queueList.get(i);
			if (q.getQueueId().equals(queueId)) {
				return q;
			}
		}
		return null;
	}

	public ArrayList<DownloadQueue> getQueueList() {
		return queueList;
	}

	public DownloadQueue getDefaultQueue() {
		return queueList.get(0);
	}

	private void loadQueues() {
		File file = new File(Config.getInstance().getDataFolder(), "queues.txt");

		DownloadQueue defaultQ = new DownloadQueue("",
				StringResource.get("DEF_QUEUE"));
		queueList.add(defaultQ);
		if (!file.exists()) {
			return;
		}

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), Charset.forName("UTF-8")));
			int count = Integer.parseInt(reader.readLine().trim());
			for (int i = 0; i < count; i++) {
				String id = reader.readLine().trim();
				String name = reader.readLine().trim();
				DownloadQueue queue = null;
				if ("".equals(id)) {
					queue = defaultQ;
				} else {
					queue = new DownloadQueue(id, name);
				}
				int c = Integer.parseInt(reader.readLine().trim());
				for (int j = 0; j < c; j++) {
					queue.getQueuedItems().add(reader.readLine().trim());
				}
				boolean hasStartTime = Integer.parseInt(reader.readLine()) == 1;
				if (hasStartTime) {
					queue.setStartTime(Long.parseLong(reader.readLine()));
					boolean hasEndTime = Integer.parseInt(reader.readLine()) == 1;
					if (hasEndTime) {
						queue.setEndTime(Long.parseLong(reader.readLine()));
					}
					boolean isPeriodic = Integer.parseInt(reader.readLine()) == 1;
					queue.setPeriodic(isPeriodic);
					if (isPeriodic) {
						queue.setDayMask(Integer.parseInt(reader.readLine()));
					} else {
						if (Integer.parseInt(reader.readLine()) == 1) {
							String ln = reader.readLine();
							queue.setExecDate(dateFormatter.parse(ln));
						}
					}
				}
				if (queue.getQueueId().length() > 0) {
					queueList.add(queue);
				}
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(e.getMessage());
				}
			}
		}
		
	}

	public void saveQueues() {
		int count = queueList.size();
		StringBuilder sb = new StringBuilder();
		BufferedWriter bwr = null;
		
		String newLine = System.getProperty("line.separator");
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			
			bwr = new BufferedWriter(new FileWriter(new File(Config.getInstance().getDataFolder(), "queues.txt")));
			
			sb.append(count + newLine);
			for (int i = 0; i < count; i++) {
				DownloadQueue queue = queueList.get(i);
				sb.append(queue.getQueueId() + newLine);
				sb.append(queue.getName() + newLine);
				ArrayList<String> queuedItems = queue.getQueuedItems();
				sb.append(queuedItems.size() + newLine);
				for (int j = 0; j < queuedItems.size(); j++) {
					sb.append(queuedItems.get(j) + newLine);
				}
				if (queue.getStartTime() != -1) {
					sb.append("1" + newLine);
					sb.append(queue.getStartTime() + newLine);
					if (queue.getEndTime() != -1) {
						sb.append("1" + newLine);
						sb.append(queue.getEndTime() + newLine);
					} else {
						sb.append("0" + newLine);
					}
					sb.append((queue.isPeriodic() ? 1 : 0) + newLine);
					if (queue.isPeriodic()) {
						sb.append(queue.getDayMask() + newLine);
					} else {
						if (queue.getExecDate() != null) {
							sb.append("1" + newLine);
							sb.append(dateFormatter.format(queue
									.getExecDate()) + newLine);
						} else {
							sb.append("0" + newLine);
						}
					}
				} else {
					sb.append("0" + newLine);
				}
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}
		if (bwr != null) {
			try {
				bwr.write(sb.toString());
				bwr.flush();
				bwr.close();
			} catch (IOException e) {
				log.info(e.getMessage());
			}
		}
	}

	public void removeQueue(String queueId) {
		DownloadQueue q = getQueueById(queueId);
		if (q == null)
			return;
		if (q.isRunning()) {
			q.stop();
		}
		for (int i = 0; i < q.getQueuedItems().size(); i++) {
			String id = q.getQueuedItems().get(i);
			DownloadEntry ent = DManagerApp.getInstance().getEntry(id);
			if (ent != null) {
				ent.setQueueId("");
			}
		}
		queueList.remove(q);
	}

	public void createNewQueue() {
		int counter = 1;
		String name = "";
		String qw = StringResource.get("Q_WORD");
		while (true) {
			boolean found = false;
			counter++;
			for (DownloadQueue qi : queueList) {
				if ("".equals(qi.getQueueId()))
					continue;
				if ((qw + " " + counter).equals(qi.getName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				name = qw + " " + counter;
				break;
			}
		}
		DownloadQueue q = new DownloadQueue(UUID.randomUUID().toString(), name);
		queueList.add(q);
		saveQueues();
	}

	// check and remove invalid entries from queued item list (invalid entries
	// might appear from corrupt download list
	public void fixCorruptEntries(Iterator<String> ids, DManagerApp app) {
		DownloadQueue dfq = getDefaultQueue();
		while (ids.hasNext()) {
			String id = ids.next();
			DownloadEntry ent = app.getEntry(id);
			String qId = ent.getQueueId();
			if (qId == null || getQueueById(qId) == null) {
				dfq.getQueuedItems().add(id);
				ent.setQueueId("");
			}
		}
		for (int i = 0; i < queueList.size(); i++) {
			DownloadQueue q = queueList.get(i);
			ArrayList<String> corruptIds = new ArrayList<String>();
			for (int k = 0; k < q.getQueuedItems().size(); k++) {
				String id = q.getQueuedItems().get(k);
				if (app.getEntry(id) == null) {
					corruptIds.add(id);
				}
			}
			q.getQueuedItems().removeAll(corruptIds);
		}
	}
}
