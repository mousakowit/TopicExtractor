package org.vskm.text.topic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.vskm.text.topic.util.FileWalker;

public class Corpus {

	List<File> files;
	HashMap<String,Topic> corpusTopics;
	
	public Corpus(String pathToFiles) {
		this.files = FileWalker.walk(pathToFiles);
		this.corpusTopics = new HashMap<String,Topic>();
	}

	public List<Entry<String, Topic>> getTopics(int maxTopics) {
		
		List<Map.Entry<String, Topic>> entryList = new ArrayList<Map.Entry<String, Topic>>(this.corpusTopics.entrySet());

	    Collections.sort(entryList, new Comparator<Map.Entry<String, Topic>>() {
	        @Override
	        public int compare(Entry<String, Topic> o1, Entry<String, Topic> o2) {
	            return o1.getValue().compareTo(o2.getValue());
	        }

	    });
		
		return entryList.subList(0, Math.min(corpusTopics.size(), maxTopics));
	}

	public void extractTopics() {
		// TODO: fix limit of 200 
		for (File file : files.subList(0, 200)) {
			// System.out.println("Extracting: " + file.getAbsolutePath());

			// Extract the text topics
			TopicExtractor te = new TopicExtractor(FileWalker.readFile(file));
			List<Topic> fileTopics = te.getTopics();

			for (Topic topic : fileTopics) {
				if (corpusTopics.containsKey(topic.getName())) {
					// The file's topic already exists. Increase our corpus topic's counter and add the variants
					corpusTopics.get(topic.getName()).addVariations(topic.getVariations());
				} else {
					// This is a new topic, add it to the corpus topics
					corpusTopics.put(topic.getName(), topic);
				}
				/**
				System.out.println(topic);
				System.out.println("Clean: "+topic.getCleanName());
				if (topic.getSimilarTopic() != null) {
					System.out.println(" -> possible duplicate of " +topic.getSimilarTopic());
				}
				**/
			}
			/**
			Set<Entry<String, Integer>> entries = topicNames.entrySet();
			Iterator<Entry<String, Integer>> e = entries.iterator();
			while (e.hasNext()) {
				Entry<String,Integer> entry = e.next();
				if (entry.getValue() > 1) {
					System.out.println(entry.getKey() + ": " + entry.getValue());
				}
			}
			**/
		}
	
	}

	
}
