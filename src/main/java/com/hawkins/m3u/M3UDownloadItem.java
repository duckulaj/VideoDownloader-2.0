package com.hawkins.m3u;

import java.net.URL;

import org.springframework.messaging.simp.SimpMessagingTemplate;

public class M3UDownloadItem {

		private String name = ""; // This stores the destination location and filename on disk
		private String filmName = "";
		private String searchPhrase = "";
		private URL url;
		private Long size = 0L;
		private SimpMessagingTemplate template;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public URL getUrl() {
			return url;
		}
		public void setUrl(URL url) {
			this.url = url;
		}
		public Long getSize() {
			return size;
		}
		public void setSize(Long size) {
			this.size = size;
		}
		public SimpMessagingTemplate getTemplate() {
			return template;
		}
		public void setTemplate(SimpMessagingTemplate template) {
			this.template = template;
		}
		public String getFilmName() {
			return filmName;
		}
		public void setFilmName(String filmName) {
			this.filmName = filmName;
		}
		public String getSearchPhrase() {
			return searchPhrase;
		}
		public void setSearchPhrase(String searchPhrase) {
			this.searchPhrase = searchPhrase;
		}
		
		
		
}
