package com.hawkins.dmanager;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileWriter;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hawkins.dmanager.downloaders.Downloader;
import com.hawkins.dmanager.downloaders.dash.DashDownloader;
import com.hawkins.dmanager.downloaders.hds.HdsDownloader;
import com.hawkins.dmanager.downloaders.hls.HlsDownloader;
import com.hawkins.dmanager.downloaders.http.HttpDownloader;
import com.hawkins.dmanager.downloaders.metadata.DashMetadata;
import com.hawkins.dmanager.downloaders.metadata.HdsMetadata;
import com.hawkins.dmanager.downloaders.metadata.HlsMetadata;
import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.dmanager.monitoring.BrowserMonitor;
import com.hawkins.dmanager.network.http.HttpContext;
import com.hawkins.dmanager.ui.res.StringResource;
import com.hawkins.dmanager.util.DManagerUtils;
import com.hawkins.dmanager.util.LinuxUtils;
import com.hawkins.dmanager.util.ParamUtils;
import com.hawkins.dmanager.util.StringUtils;
import com.hawkins.utils.FileUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DManagerApp implements DownloadListener, DownloadWindowListener, Comparator<String> {

	
	public static final String APP_VERSION = "0.0.1";

	private ArrayList<ListChangeListener> listChangeListeners;
	private Map<String, DownloadEntry> downloads;
	private static DManagerApp xdmthis;
	private HashMap<String, Downloader> downloaders;
	private long lastSaved;
	private QueueManager qMgr;
	private LinkRefreshCallback refreshCallback;
	private ArrayList<String> pendingDownloads;// this buffer is used when there
	// is a limit on maximum
	// simultaneous downloads and
	// more downloads are started
	// than permissible limit. If
	// queues are also running then
	// this buffer will be processed
	// first
	private static HashMap<String, String> paramMap;

	private int pendingNotification = -1; // if main window in not created
	// notification is stored in this
	// variable
	private SimpMessagingTemplate template;
	
	public static void instanceStarted() {
		log.info("instance starting...");
		DManagerApp.getInstance();
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				
			}
		});
		if (Config.getInstance().isFirstRun()) {
			if (DManagerUtils.detectOS() == DManagerUtils.WINDOWS) {
				if (!DManagerUtils.isAlreadyAutoStart()) {
					DManagerUtils.addToStartup();
				}
			} else {
				DManagerUtils.addToStartup();
			}
		}
		log.info("instance started.");
		
	}

	public static void instanceAlreadyRunning() {
		log.info("instance already runninng");
		ParamUtils.sendParam(paramMap);
		System.exit(0);
	}

	public static void start(String args[]) {
		paramMap = new HashMap<>();
		boolean expect = false;
		String key = null;
		for (int i = 0; i < args.length; i++) {
			if (expect) {
				if (key != null) {
					paramMap.put(key, args[i]);
				}
				expect = false;
				continue;
			}
			if ("-u".equals(args[i])) {
				key = "url";
				expect = true;
			} else if ("-m".equals(args[i])) {
				paramMap.put("background", "true");
				expect = false;
			}
		}
		log.info("starting monitoring...");
		BrowserMonitor.getInstance().startMonitoring();
	}

	private DManagerApp() {
		listChangeListeners = new ArrayList<>();
		downloads = new HashMap<>();
		downloaders = new HashMap<>();
		loadDownloadList();
		lastSaved = System.currentTimeMillis();
		pendingDownloads = new ArrayList<>();
		qMgr = QueueManager.getInstance();
		qMgr.fixCorruptEntries(getDownloadIds(), this);
		QueueScheduler.getInstance().start();
		Config.getInstance().load();
		// Config.getInstance().save();
		HttpContext.getInstance().init();
	}

	public void exit() {
		saveDownloadList();
		qMgr.saveQueues();
		Config.getInstance().save();
		System.exit(0);
	}

	public void downloadFinished(String id) {
		DownloadEntry ent = downloads.get(id);
		ent.setState(DManagerConstants.FINISHED);
		ent.setProgress(100);
		Downloader downloader = downloaders.remove(id);
		if (downloader != null && downloader.getSize() < 0) {
			ent.setSize(downloader.getDownloaded());
		}

		FileUtils.copyToriginalFileName(ent);

		if (downloader != null) {
			downloader.sendProgress(id, ent, template);
		}
		notifyListeners(null);
		saveDownloadList();
		if (Config.getInstance().isExecAntivir() && (!StringUtils.isNullOrEmptyOrBlank(Config.getInstance().getAntivirExe()))) {
				execAntivir();
		}

		processNextItem(id);
		if (isAllFinished()) {
			if (Config.getInstance().isAutoShutdown()) {
				initShutdown();
			}
			if (Config.getInstance().isExecCmd()) {
				execCmd();
			}
		}
	}

	public void downloadFailed(String id) {
		downloaders.remove(id);
		if (id == null) {
			log.info("Download failed, id null");
			return;
		}

		DownloadEntry ent = downloads.get(id);
		ent.setState(DManagerConstants.FAILED);
		notifyListeners(id);
		saveDownloadList();
		log.info("DownloadEntry with id {} removed", ent.getId());
		processNextItem(id);
	}

	public void downloadStopped(String id) {
		Downloader downloader = downloaders.get(id);
		downloaders.remove(id);
		DownloadEntry ent = downloads.get(id);
		ent.setState(DManagerConstants.PAUSED);
		notifyListeners(id);
		saveDownloadList();
		processNextItem(id);
		loadDownloadList();
		downloader.sendProgress(id, ent, this.template);
	}

	public void downloadConfirmed(String id) {
		
		if (log.isDebugEnabled()) {
			log.debug("confirmed {}", id);
		}
		
		Downloader downloader = downloaders.get(id);
		DownloadEntry ent = downloads.get(id);
		ent.setSize(downloader.getSize());
		if (downloader.isFileNameChanged()) {
			ent.setFile(downloader.getNewFile());
			ent.setCategory(DManagerUtils.findCategory(downloader.getNewFile()));
			updateFileName(ent);
		}
		
		notifyListeners(id);
		saveDownloadList();
		loadDownloadList();
		downloader.sendProgress(id, ent, this.template);
	}

	public void downloadUpdated(String id) {
		DownloadEntry ent = downloads.get(id);
		Downloader downloader = downloaders.get(id);
		if (downloader == null) {
			log.info("################# sync error ##############");
		} else {
			ent.setSize(downloader.getSize());
			ent.setDownloaded(downloader.getDownloaded());
			ent.setProgress(downloader.getProgress());
			ent.setState(downloader.isAssembling() ? DManagerConstants.ASSEMBLING : DManagerConstants.DOWNLOADING);
			ent.setDownloadSpeed(downloader.getDownloadSpeed());
			ent.setEta(downloader.getEta());
		}
		
		notifyListeners(id);
		long now = System.currentTimeMillis();
		if (now - lastSaved > 10000) {
			saveDownloadList();
			loadDownloadList();
			lastSaved = now;
		}
		
		if (downloader != null ) {
			downloader.sendProgress(id, ent, this.template);
		}
	}

	public static DManagerApp getInstance() {
		if (xdmthis == null) {
			xdmthis = new DManagerApp();
		}
		return xdmthis;
	}

	public void addDownload(final HttpMetadata metadata, final String file) {
		if (refreshCallback != null) {
			if (refreshCallback.isValidLink(metadata)) {
				return;
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// new NewDownloadWindow(metadata, file).setVisible(true);
			}
		});
	}

	public void addMedia(final HttpMetadata metadata, final String file, final String info) {
		if (Config.getInstance().isShowVideoNotification()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					// VideoPopup.getInstance().addVideo(metadata, file, info);
				}
			});
		}
	}

	public void createDownload(String file, String folder, HttpMetadata metadata, boolean now, String queueId,
			int formatIndex, int streamIndex, SimpMessagingTemplate template) {

		this.template = template;
		metadata.save();
		DownloadEntry ent = new DownloadEntry();
		ent.setId(metadata.getId());
		ent.setOutputFormatIndex(formatIndex);
		ent.setState(DManagerConstants.PAUSED);
		ent.setFile(file);
		ent.setOriginalFileName(file);
		ent.setFolder(folder);
		ent.setCategory(DManagerUtils.findCategory(file));
		ent.setDate(System.currentTimeMillis());
		putInQueue(queueId, ent);
		ent.setStartedByUser(now);
		downloads.put(metadata.getId(), ent);
		saveDownloadList();
		if (!now) {
			DownloadQueue q = qMgr.getQueueById(queueId);
			if (q != null && q.isRunning()) {
				log.info("Queue is running, if no pending download pickup next available download");
				q.next();
			}
		}
		if (now) {
			startDownload(metadata.getId(), metadata, ent, streamIndex, template);
		}
		notifyListeners(null);
	}

	// could be new or resume
	private void startDownload(String id, HttpMetadata metadata, DownloadEntry ent, int streams, SimpMessagingTemplate template) {
		if (!checkAndBufferRequests(id)) {
			
			if (log.isDebugEnabled()) {
				log.debug("starting " + id + " with: " + metadata + " is dash: " + (metadata instanceof DashMetadata));
			}
			
			Downloader downloader = null;

			if (metadata instanceof DashMetadata) {
				log.info("Dash download with stream: " + streams);
				if (streams == 1) {
					DashMetadata dm = (DashMetadata) metadata;
					dm.setUrl(dm.getUrl2());// set video url as main url
					dm.setUrl2(null);
				} else if (streams == 2) {
					DashMetadata dm = (DashMetadata) metadata;
					dm.setUrl2(null);
				} else {
					log.info("Dash download created");

					// create dash downloader
					DashMetadata dm = (DashMetadata) metadata;
					downloader = new DashDownloader(id, Config.getInstance().getTemporaryFolder(), dm);
				}
			}
			if (metadata instanceof HlsMetadata) {
				log.info("Hls download created");
				downloader = new HlsDownloader(id, Config.getInstance().getTemporaryFolder(), (HlsMetadata) metadata);
			}
			if (metadata instanceof HdsMetadata) {
				log.info("Hls download created");
				downloader = new HdsDownloader(id, Config.getInstance().getTemporaryFolder(), (HdsMetadata) metadata);
			}
			if (downloader == null) {
				downloader = new HttpDownloader(id, Config.getInstance().getTemporaryFolder(), metadata);
			}

			downloader.setOuputMediaFormat(ent.getOutputFormatIndex());
			downloaders.put(id, downloader);
			downloader.registerListener(this);
			ent.setState(DManagerConstants.DOWNLOADING);
			downloader.start();
			downloader.sendProgress(id, ent, this.template);
			
		} else {
			log.info(id + ": Maximum download limit reached, queueing request");
		}
	}

	public void pauseDownload(String id) {
		Downloader downloader = downloaders.get(id);
		if (downloader != null) {
			DownloadEntry de = downloads.get(id);
			if (de != null) {
				de.setState(DManagerConstants.PAUSED);
			}
			downloader.stop();
			downloader.sendProgress(id, de, this.template);
			downloader.unregisterListener();
		}
	}

	public void resumeDownload(String id, boolean startedByUser) {
		DownloadEntry ent = downloads.get(id);
		ent.setStartedByUser(startedByUser);
		if (ent.getState() == DManagerConstants.PAUSED || ent.getState() == DManagerConstants.FAILED) {
			if (!checkAndBufferRequests(id)) {
				ent.setState(DManagerConstants.DOWNLOADING);
				HttpMetadata metadata = HttpMetadata.load(id);
				if (Config.getInstance().showDownloadWindow() && ent.isStartedByUser()) {
					/*
					 * DownloadWindow wnd = new DownloadWindow(id, this); downloadWindows.put(id,
					 * wnd); wnd.setVisible(true);
					 */
				}
				Downloader downloader = null;
				if (metadata instanceof DashMetadata) {
					DashMetadata dm = (DashMetadata) metadata;
					log.info("Dash download- url1: " + dm.getUrl() + " url2: " + dm.getUrl2());
					downloader = new DashDownloader(id, Config.getInstance().getTemporaryFolder(), dm);
				}
				if (metadata instanceof HlsMetadata) {
					HlsMetadata hm = (HlsMetadata) metadata;
					log.info("HLS download- url1: " + hm.getUrl());
					downloader = new HlsDownloader(id, Config.getInstance().getTemporaryFolder(), hm);
				}
				if (metadata instanceof HdsMetadata) {
					HdsMetadata hm = (HdsMetadata) metadata;
					log.info("HLS download- url1: " + hm.getUrl());
					downloader = new HdsDownloader(id, Config.getInstance().getTemporaryFolder(), hm);
				}
				if (downloader == null) {
					log.info("normal download");
					downloader = new HttpDownloader(id, Config.getInstance().getTemporaryFolder(), metadata);
				}
				downloaders.put(id, downloader);
				downloader.setOuputMediaFormat(ent.getOutputFormatIndex());
				//d.registerListener(this);
				downloader.resume();

			} else {
				log.info("{}: Maximum download limit reached, queueing request", id);
			}
			notifyListeners(null);
		}
	}

	public void restartDownload(String id) {
		DownloadEntry ent = downloads.get(id);
		if (ent.getState() == DManagerConstants.PAUSED || ent.getState() == DManagerConstants.FAILED
				|| ent.getState() == DManagerConstants.FINISHED) {
			ent.setState(DManagerConstants.PAUSED);
			clearData(id);
			resumeDownload(id, true);
		} else {
			return;
		}
	}

	/*
	 * synchronized public void addListener(ListChangeListener listener) {
	 * listChangeListeners.add(listener); }
	 * 
	 * synchronized public void removeListener(ListChangeListener listener) {
	 * listChangeListeners.remove(listener); }
	 */
	private void notifyListeners(String id) {
		if (listChangeListeners != null) {
			for (int i = 0; i < listChangeListeners.size(); i++)
				if (id != null)
					listChangeListeners.get(i).listItemUpdated(id);
				else
					listChangeListeners.get(i).listChanged();
		}
	}

	public DownloadEntry getEntry(String id) {
		return downloads.get(id);
	}

	private void clearData(String id) {
		File folder = new File(Config.getInstance().getTemporaryFolder(), id);
		File[] files = folder.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
		}
		folder.delete();
	}

	@Override
	public String getOutputFolder(String id) {
		DownloadEntry ent = downloads.get(id);
		if (ent == null) {
			return Config.getInstance().getDownloadFolder();
		} else {
			String folder = (ent.getFolder() == null || ent.getFolder().length() < 1)
					? Config.getInstance().getDownloadFolder() : ent.getFolder();
					return folder;
		}
	}

	@Override
	public String getOutputFile(String id, boolean update) {
		DownloadEntry ent = downloads.get(id);
		if (update) {
			updateFileName(ent);
		}
		return ent.getFile();
	}

	private void loadDownloadList() {
		File file = new File(Config.getInstance().getDataFolder(), "downloads.json");
		loadDownloadList(file);
	}

	private void loadDownloadList(File file) {
		if (!file.exists()) {
			return;
		}

		
	}

	private void saveDownloadList() {
		File file = new File(Config.getInstance().getDataFolder(), "downloads.json");
		saveDownloadList(file);
	}

	private void saveDownloadList(File file) {

		JsonArray downloadListJsonArray = new JsonArray();

		try {
			Iterator<String> keyIterator = downloads.keySet().iterator();

			while (keyIterator.hasNext()) {
				JsonObject downloadListJson = new JsonObject();
				String key = keyIterator.next();
				DownloadEntry ent = downloads.get(key);
				downloadListJson.addProperty("id", ent.getId());
				downloadListJson.addProperty("file", ent.getFile());
				downloadListJson.addProperty("originalFileName", ent.getOriginalFileName());
				downloadListJson.addProperty("category", ent.getCategory());
				downloadListJson.addProperty("state", ent.getState());
				downloadListJson.addProperty("folder", ent.getFolder());
				downloadListJson.addProperty("date", ent.getDate());
				downloadListJson.addProperty("downloaded", ent.getDownloaded());
				downloadListJson.addProperty("size", ent.getSize());
				downloadListJson.addProperty("progress", ent.getProgress());
				downloadListJson.addProperty("metaDataFound", ent.isMetaDataFound());
				if (ent.getQueueId() != null) {
					downloadListJson.addProperty("queueid", ent.getQueueId());
				}
				downloadListJson.addProperty("formatIndex", ent.getOutputFormatIndex());

				downloadListJsonArray.add(downloadListJson);

			}
			
			if (log.isDebugEnabled()) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				log.debug(gson.toJson(downloadListJsonArray));
			}
			
			FileWriter jsonFile = new FileWriter(new File(Config.getInstance().getDataFolder(), "downloads.json"));
			jsonFile.write(downloadListJsonArray.toString());
			jsonFile.close();


		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}

	public void hidePrgWnd(String id) {
		/*
		 * DownloadWindow wnd = downloadWindows.get(id); if (wnd != null) {
		 * downloadWindows.remove(id); wnd.close(XDMConstants.PAUSED, 0); }
		 */
	}

	private synchronized int getActiveDownloadCount() {
		int count = 0;
		Iterator<String> keyIterator = downloads.keySet().iterator();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			DownloadEntry ent = downloads.get(key);
			int state = ent.getState();
			if (state == DManagerConstants.FINISHED || state == DManagerConstants.PAUSED || state == DManagerConstants.FAILED)
				continue;
			count++;
		}
		return count;
	}

	private synchronized boolean checkAndBufferRequests(String id) {
		int actCount = getActiveDownloadCount();
		if (Config.getInstance().getMaxDownloads() > 0 && actCount >= Config.getInstance().getMaxDownloads()) {
			log.info("active: " + actCount + " max: " + Config.getInstance().getMaxDownloads());
			if (!pendingDownloads.contains(id)) {
				pendingDownloads.add(id);
			}
			return true;
		}
		return false;
	}

	private synchronized void processNextItem(String lastId) {
		processPendingRequests();
		if (lastId == null)
			return;
		DownloadEntry ent = getEntry(lastId);
		if (ent == null) {
			return;
		}
		DownloadQueue queue = null;
		if ("".equals(ent.getQueueId())) {
			queue = qMgr.getDefaultQueue();
		} else {
			queue = qMgr.getQueueById(ent.getQueueId());
		}
		if (queue != null && queue.isRunning()) {
			queue.next();
		}
	}

	private void processPendingRequests() {
		int activeCount = getActiveDownloadCount();
		int maxDownloadCount = Config.getInstance().getMaxDownloads();
		List<String> tobeStartedIds = new ArrayList<String>();
		if (maxDownloadCount - activeCount > 0) {
			for (int i = 0; i < Math.min(maxDownloadCount, pendingDownloads.size()); i++) {
				String ent = pendingDownloads.get(i);
				tobeStartedIds.add(ent);
			}
		}
		if (tobeStartedIds.size() > 0) {
			for (int i = 0; i < tobeStartedIds.size(); i++) {
				String id = tobeStartedIds.get(i);
				pendingDownloads.remove(id);
				DownloadEntry ent = getEntry(id);
				if (ent != null) {
					resumeDownload(id, ent.isStartedByUser());
				}
			}
		}
	}

	public boolean queueItemPending(String queueId) {
		if (queueId == null)
			return false;
		for (int i = 0; i < pendingDownloads.size(); i++) {
			String id = pendingDownloads.get(i);
			DownloadEntry ent = getEntry(id);
			if (ent == null || ent.getQueueId() == null)
				continue;
			if (ent.getQueueId().equals(queueId)) {
				return true;
			}
		}
		return false;
	}

	public ArrayList<DownloadQueue> getQueueList() {
		return qMgr.getQueueList();
	}

	private DownloadQueue getQueueById(String queueId) {
		return qMgr.getQueueById(queueId);
	}

	private void putInQueue(String queueId, DownloadEntry ent) {
		DownloadQueue q = getQueueById(queueId);
		String id = ent.getId();
		if (q == null) {
			log.info("No queue found for: '" + queueId + "'");
			return;
		}
		String qid = ent.getQueueId();
		DownloadQueue oldQ = getQueueById(qid);
		log.debug("Adding to: '" + queueId + "'");
		if (!q.getQueueId().equals(qid)) {
			if (oldQ != null) {
				// remove from previous queue
				oldQ.removeFromQueue(id);
			}
			ent.setQueueId(queueId);
			q.addToQueue(id);
		}
	}

	@Override
	public int compare(String key1, String key2) {
		DownloadEntry ent1 = getEntry(key1);
		DownloadEntry ent2 = getEntry(key2);
		if (ent1 == null)
			return -1;
		if (ent2 == null)
			return 1;
		if (ent1.getDate() > ent2.getDate()) {
			return 1;
		} else if (ent1.getDate() < ent2.getDate()) {
			return -1;
		} else {
			return 0;
		}
	}

	private Iterator<String> getDownloadIds() {
		return downloads.keySet().iterator();
	}

	public boolean isAllFinished() {
		if (getActiveDownloadCount() != 0) {
			return false;
		}
		if (pendingDownloads.size() != 0) {
			return false;
		}
		for (int i = 0; i < QueueManager.getInstance().getQueueList().size(); i++) {
			DownloadQueue q = QueueManager.getInstance().getQueueList().get(i);
			if (q.hasPendingItems()) {
				return false;
			}
		}
		return true;
	}

	private void initShutdown() {
		if (DManagerUtils.detectOS() == DManagerUtils.LINUX) {
			LinuxUtils.initShutdown();
		}
		log.info("Initiating shutdown");
	}

	/*
	 * private int deleteDownloads(ArrayList<String> ids) { int c = 0; for (int i =
	 * 0; i < ids.size(); i++) { String id = ids.get(i); DownloadEntry ent =
	 * getEntry(id); if (ent != null) { if (ent.getState() == XDMConstants.FINISHED
	 * || ent.getState() == XDMConstants.PAUSED || ent.getState() ==
	 * XDMConstants.FAILED) { this.downloads.remove(id); if
	 * (pendingDownloads.contains(id)) { pendingDownloads.remove(id); } String qId =
	 * ent.getQueueId(); if (qId != null) { DownloadQueue q = getQueueById(qId); if
	 * (q != null) { if (q.getQueueId().length() > 0) { q.removeFromQueue(id); } } }
	 * deleteFiles(id); c++; } } } saveDownloadList(); notifyListeners(null); return
	 * ids.size() - c; }
	 */

	/*
	 * private void deleteFiles(String id) { log.info("Deleting metadata for " +
	 * id); File mf = new File(Config.getInstance().getMetadataFolder(), id);
	 * boolean deleted = mf.delete(); log.info("Deleted manifest " + id + " " +
	 * deleted); File df = new File(Config.getInstance().getTemporaryFolder(), id);
	 * File[] files = df.listFiles(); if (files != null && files.length > 0) { for
	 * (File f : files) { deleted = f.delete(); log.info("Deleted tmp file " + id
	 * + " " + deleted); } } deleted = df.delete();
	 * log.info("Deleted tmp folder " + id + " " + deleted); }
	 */

	/*
	 * public void registerRefreshCallback(LinkRefreshCallback callback) {
	 * this.refreshCallback = callback; }
	 */

	/*
	 * public void unregisterRefreshCallback() { this.refreshCallback = null; }
	 */

	/*
	 * public void deleteCompleted() { Iterator<String> allIds =
	 * downloads.keySet().iterator(); ArrayList<String> idList = new
	 * ArrayList<String>(); while (allIds.hasNext()) { String id = allIds.next();
	 * DownloadEntry ent = downloads.get(id); if (ent.getState() ==
	 * XDMConstants.FINISHED) { idList.add(id); } } deleteDownloads(idList); }
	 */

	public boolean promptCredential(String id, String msg, boolean proxy) {
		DownloadEntry ent = getEntry(id);
		if (ent == null)
			return false;
		if (!ent.isStartedByUser())
			return false;
		PasswordAuthentication pauth = getCredential(msg, proxy);
		if (pauth == null) {
			return false;
		}
		if (proxy) {
			Config.getInstance().setProxyUser(pauth.getUserName());
			if (pauth.getPassword() != null) {
				Config.getInstance().setProxyPass(new String(pauth.getPassword()));
			}
		} else {
			log.info("saving password for: " + msg);
			CredentialManager.getInstance().addCredentialForHost(msg, pauth);
		}
		return true;
	}

	private PasswordAuthentication getCredential(String msg, boolean proxy) {
		JTextField user = new JTextField(30);
		JPasswordField pass = new JPasswordField(30);

		String prompt = proxy ? StringResource.get("PROMPT_PROXY")
				: String.format(StringResource.get("PROMPT_SERVER"), msg);

		Object[] obj = new Object[5];
		obj[0] = prompt;
		obj[1] = StringResource.get("DESC_USER");
		obj[2] = user;
		obj[3] = StringResource.get("DESC_PASS");
		obj[4] = pass;

		if (JOptionPane.showOptionDialog(null, obj, StringResource.get("PROMPT_CRED"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION) {
			PasswordAuthentication pauth = new PasswordAuthentication(user.getText(), pass.getPassword());
			return pauth;
		}
		return null;
	}

	private void execCmd() {
		if (!StringUtils.isNullOrEmptyOrBlank(Config.getInstance().getCustomCmd())) {
			DManagerUtils.exec(Config.getInstance().getCustomCmd());
		}
	}

	private void execAntivir() {
		DManagerUtils.exec(Config.getInstance().getAntivirExe() + " "
				+ (Config.getInstance().getAntivirCmd() == null ? "" : Config.getInstance().getAntivirCmd()));
	}

	private void updateFileName(DownloadEntry ent) {
		
		String id = ent.getId();
		File f = new File(getOutputFolder(id), ent.getOriginalFileName());
		
		log.info("checking for file named {}",f.getName());
		int c = 1;
		while (f.exists()) {
			String ext = DManagerUtils.getExtension(f.getAbsolutePath());
			if (ext == null) {
				ext = "";
			}
			String f2 = DManagerUtils.getFileNameWithoutExtension(ent.getFile());
			f = new File(getOutputFolder(id), f2 + "_" + c + ext);
			c++;
		}
		
		if (ent.getFile() != f.getName()) {
			log.info("Updating file name - old: {}  new: {}", ent.getFile(), f.getName());
			ent.setFile(f.getName());
		}
	}

	public int getNotification() {
		return pendingNotification;
	}

}
