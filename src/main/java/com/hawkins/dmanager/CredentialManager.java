package com.hawkins.dmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.hawkins.dmanager.util.Base64;
import com.hawkins.dmanager.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CredentialManager {
	
	
	private Map<String, PasswordAuthentication> savedCredentials;
	private Map<String, PasswordAuthentication> cachedCredentials;

	private static CredentialManager _this;

	public static CredentialManager getInstance() {
		if (_this == null) {
			_this = new CredentialManager();
		}
		return _this;
	}

	public Set<Entry<String, PasswordAuthentication>> getCredentials() {
		return savedCredentials.entrySet();
	}

	public PasswordAuthentication getCredentialForHost(String host) {
		PasswordAuthentication pauth = savedCredentials.get(host);
		if (pauth == null) {
			return cachedCredentials.get(host);
		}
		return null;
	}

	public PasswordAuthentication getCredentialForProxy() {
		if (!StringUtils.isNullOrEmptyOrBlank(Config.getInstance().getProxyUser())) {
			return new PasswordAuthentication(Config.getInstance().getProxyUser(),
					Config.getInstance().getProxyPass() == null ? null
							: Config.getInstance().getProxyPass().toCharArray());
		} else {
			return null;
		}
	}

	private CredentialManager() {
		savedCredentials = new HashMap<>();
		cachedCredentials = new HashMap<>();
		load();
	}

	private void addCredentialForHost(String host, PasswordAuthentication pauth, boolean save) {
		if (save) {
			savedCredentials.put(host, pauth);
		} else {
			cachedCredentials.put(host, pauth);
		}
	}

	

	

	void addCredentialForHost(String host, PasswordAuthentication pauth) {
		addCredentialForHost(host, pauth, false);
	}

	private void load() {
		BufferedReader br = null;
		try {
			File f = new File(Config.getInstance().getDataFolder(), ".credentials");
			if (!f.exists()) {
				log.info("No saved credentials");
				return;
			}
			br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			if (!savedCredentials.isEmpty())
				savedCredentials.clear();
			while (true) {
				String ln = br.readLine();
				if (ln == null)
					break;
				String str = new String(Base64.decode(ln));
				String[] arr = str.split("\n");
				if (arr.length < 2)
					continue;
				savedCredentials.put(arr[0],
						new PasswordAuthentication(arr[1], arr.length == 3 ? arr[2].toCharArray() : new char[0]));
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
