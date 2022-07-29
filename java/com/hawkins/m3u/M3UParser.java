package com.hawkins.m3u;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;

import com.hawkins.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class M3UParser {

	private M3UParser() {

	}

	static String[] countriesToInclude = {"[EN]", "[UK]"}; 

	static LinkedList<M3UItem> getAllM3UListFromFile(String m3uFile) {
		long start = System.currentTimeMillis();
		LinkedList<M3UItem> m3uList = new LinkedList<M3UItem>();

		try (BufferedReader br = Files.newBufferedReader(Paths.get(m3uFile))) {

			String line;
			while ((line = br.readLine()) != null) {

				try {
					String[] valuesInQuotes = StringUtils.substringsBetween(line, "\"", "\"");
					if (valuesInQuotes != null) {
						M3UItem channel = new M3UItem();
						channel.setId(valuesInQuotes[0]);
						channel.setName(normaliseName(valuesInQuotes[1]));
						if (Utils.containsWords(channel.getName(), countriesToInclude)) {
								
							channel.setLogo(valuesInQuotes[2]);
	
							if (valuesInQuotes[3].equalsIgnoreCase("#menu-collapse")) {
								log.debug("bollox");
							}
							channel.setGroupTitle(valuesInQuotes[3]);
							channel.setUrl(br.readLine());
							channel.setSearch(normaliseName(valuesInQuotes[1]));

							channel = removeLanguageIdentifier(channel); 
							m3uList.add(channel);
						}

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

	static LinkedList<M3UGroup> getM3UGroupsFromFile(String m3uFile) {
		long start = System.currentTimeMillis();
		LinkedList<M3UGroup> m3uGroupList = new LinkedList<M3UGroup>();


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

							if (Utils.containsWords(group.getName(), countriesToInclude)) {
								// if (channel.getName().contains("[EN]")) {

								group = removeLanguageIdentifier(group); 
								
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

									if(!exists) {

										m3uGroupList.add(group);
									}
								}
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

		LinkedList<M3UGroup> sortedGroups = sortGrouplist(m3uGroupList);

		if (log.isDebugEnabled()) {
			log.debug("Found {} groups", m3uGroupList.size());
			log.debug("getM3uGroupsFromFile executed in {} ms", (System.currentTimeMillis() - start));
		}

		return sortGrouplist(sortedGroups);
	}

	public static LinkedList<M3UItem> sortPlaylist(LinkedList<M3UItem> playlist) {
		/*
		 * return playlist.stream() .sorted(Comparator.comparing(M3UItem::getName))
		 * .collect(Collectors.toList());
		 */

		playlist.sort(Comparator.comparing(M3UItem::getName));
		return playlist;
	}

	public static LinkedList<M3UGroup> sortGrouplist(LinkedList<M3UGroup> grouplist) {
		/*
		 * return (LinkedList<M3UGroup>) grouplist.stream()
		 * .sorted(Comparator.comparing(M3UGroup::getName))
		 * .collect(Collectors.toList());
		 */

		grouplist.sort(Comparator.comparing(M3UGroup::getName));

		return grouplist;

	}

	public static String normaliseName(String filmName) {

		int startIndex = 0;

		int endIndex = StringUtils.indexOfAny(filmName, new String[]{"SD", "FHD", "UHD", "HD"});

		if (endIndex != -1) {
			filmName = filmName.substring(startIndex, endIndex);
		}

		startIndex = filmName.indexOf(':');
		if (startIndex != -1) {
			filmName = filmName.substring(startIndex + 1).trim();
		}

		if (filmName.contains("(MULTISUB)")) {
			return filmName.replace("(MULTISUB)", "").trim();
		} else {
			return filmName.trim();
		}
		
	}

	public static M3UGroup getGroupByName(M3UGroupList groups, String groupName) {

		M3UGroup thisGroup = groups.getGroupList().stream()
				.filter(group -> groupName.equals(group.getName()))
				.findAny()
				.orElse(null);

		return thisGroup; 
	}

	private static M3UItem removeLanguageIdentifier(M3UItem thisItem) {

		thisItem.setName(replaceAndStrip(thisItem.getName()));
		thisItem.setGroupTitle(replaceAndStrip(thisItem.getGroupTitle()));
		thisItem.setGroupType(replaceAndStrip(thisItem.getGroupType()));
		thisItem.setSearch(replaceAndStrip(thisItem.getSearch()));
		thisItem.setTitle(replaceAndStrip(thisItem.getTitle()));


		return thisItem;
	}

	private static M3UGroup removeLanguageIdentifier(M3UGroup thisGroup) {

		thisGroup.setName(Utils.replaceAndStrip(thisGroup.getName(),countriesToInclude));

		return thisGroup;
	}
	
	private static String replaceAndStrip(String thisString) {
		
		return Utils.replaceAndStrip(thisString,countriesToInclude);
	}

	
	
	

}