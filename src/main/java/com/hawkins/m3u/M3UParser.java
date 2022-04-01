package com.hawkins.m3u;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class M3UParser {
	
	private M3UParser() {
		
	}
	
	static List<M3UItem> getAllM3UListFromFile(String m3uFile) {
		long start = System.currentTimeMillis();
		List<M3UItem> m3uList = new ArrayList<>();
		
		try (BufferedReader br = Files.newBufferedReader(Paths.get(m3uFile))) {
	
			String line;
			while ((line = br.readLine()) != null) {
				
				try {
					String[] valuesInQuotes = StringUtils.substringsBetween(line, "\"", "\"");
					if (valuesInQuotes != null) {
						M3UItem channel = new M3UItem();
						channel.setId(valuesInQuotes[0]);
						channel.setName(valuesInQuotes[1]);
						channel.setLogo(valuesInQuotes[2]);
						
						if (valuesInQuotes[3].equalsIgnoreCase("#menu-collapse")) {
							log.debug("bollox");
						}
						channel.setGroupTitle(valuesInQuotes[3]);
						channel.setUrl(br.readLine());
						channel.setSearch(normaliseName(valuesInQuotes[1]));
						m3uList.add(channel);
		
					}
				} catch (ArrayIndexOutOfBoundsException ae) {
					if (log.isDebugEnabled()) {
						log.debug("line is {}", line);
						log.debug("valuesInQuotes[3] is invalid");
					}
				}
			}
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("getAllM3uListFromFile executed in {} ms", (System.currentTimeMillis() - start));
		}
		return m3uList;
	}

	static List<M3UGroup> getM3UGroupsFromFile(String m3uFile) {
		long start = System.currentTimeMillis();
		List<M3UGroup> m3uGroupList = new ArrayList<>();


		try (BufferedReader br = Files.newBufferedReader(Paths.get(m3uFile))) {

			String line;
			while ((line = br.readLine()) != null) {
				
				if (log.isDebugEnabled()) {
					log.debug("line is {}", line);
				}
				
				String[] valuesInQuotes = StringUtils.substringsBetween(line, "\"", "\"");
				if (valuesInQuotes != null && valuesInQuotes.length == 4) {

					M3UGroup group = new M3UGroup();
					group.setId(null);
					
					try {
						
						// Have surrounded this with a try....catch in case the line is malformed
						
						if (valuesInQuotes[3].equalsIgnoreCase("modal")) {
							log.debug("bollox");
						} else {
							group.setName(valuesInQuotes[3]);
							
							if (log.isDebugEnabled()) {
								log.debug("groupName is {}", group.getName());
							}
	
							if (m3uGroupList.isEmpty()) {
								m3uGroupList.add(group);
							} else {
								boolean exists = false;
								for (M3UGroup thisGroup : m3uGroupList){
									if (thisGroup.getName().equalsIgnoreCase(group.getName())) {
										exists = true;
										break;
									}
								}
								
								if(!exists) m3uGroupList.add(group);
							}
						}
					} catch (ArrayIndexOutOfBoundsException ae) {
						if (log.isDebugEnabled()) {
							log.debug("line is {}", line);
							log.debug("valuesInQuotes[3] is invalid");
						}
					}
					
					
				}
			}
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
		}

		List<M3UGroup> sortedGroups = sortGrouplist(m3uGroupList);
		
		if (log.isDebugEnabled()) {
			log.debug("Found {} groups", m3uGroupList.size());
			log.debug("getM3uGroupsFromFile executed in {} ms", (System.currentTimeMillis() - start));
		}
		
		return sortGrouplist(sortedGroups);
	}
	
	public static List<M3UItem> sortPlaylist(List<M3UItem> playlist) {
		return playlist.stream()
			  .sorted(Comparator.comparing(M3UItem::getName))
			  .collect(Collectors.toList());
	
	}
	
	public static List<M3UGroup> sortGrouplist(List<M3UGroup> grouplist) {
		return grouplist.stream()
			  .sorted(Comparator.comparing(M3UGroup::getName))
			  .collect(Collectors.toList());
		
	}
	
	private static String normaliseName(String filmName) {
		
		int startIndex = 0;
		
		int endIndex = StringUtils.indexOfAny(filmName, new String[]{"SD", "FHD", "UHD", "HD"});
		
		if (endIndex != -1) {
			filmName = filmName.substring(startIndex, endIndex);
		}
			
		startIndex = filmName.indexOf(':');
		if (startIndex != -1) {
			filmName = filmName.substring(startIndex + 1).trim();
		}
		
		return filmName;
	}
	
	public static M3UGroup getGroupByName(M3UGroupList groups, String groupName) {
		
		M3UGroup thisGroup = groups.getGroupList().stream()
				  .filter(group -> groupName.equals(group.getName()))
				  .findAny()
				  .orElse(null);
		
		return thisGroup; 
	}
}