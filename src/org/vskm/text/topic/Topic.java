package org.vskm.text.topic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;


/***
 * Topics have one "search" name and a list of synonyms with a counter.
 * @author sacha
 *
 */
public class Topic implements Comparable<Topic> {
	
	// The clean name of this topic
	String name;
	
	// Holds the search name of another topic that is a possible duplicate
	String similarTo;
	
	// The number of times the topic was identified.
	int count;
	
	// a Set of other variations (aka spellings) of the topic
	HashMap<String,Integer> variations;
	
	/**
	 * @param name the clean name of this Topic
	 */
	public Topic(String name) {
		super();
		this.name = name;
		this.variations = new HashMap<String,Integer>();
	}

	// Return the search name
	public String getName() {
		return name;
	}

	// Return the number of times this topic was found in the text
	public Integer getCount() {
		return Integer.valueOf(count);
	}

	public void isSimilarTo(String s) {
		this.similarTo = s;
	}

	public String getSimilarTopic() {
		return this.similarTo;
	}

	private void _increaseCount(Integer i) {
		count = count + i;
	}
	
	protected void setCount(int count) {
		this.count = count;
	}

	// Return the different spelling variations
	protected HashMap<String, Integer> getVariations() {
		return this.variations;
	}

	// Add more spelling variations and the number of their occurrences
	protected boolean addVariation(String s, Integer i) {
		_increaseCount(i);
		if (variations.containsKey(s)) {
			variations.put(s, variations.get(s) +i);
			return true;
		} else {
			variations.put(s, i);
		}
		return false;
	}

	// Add a single spelling variation
	protected boolean addVariation(String s) {
		return addVariation(s, 1);
	}

	protected void addVariations(HashMap<String,Integer> v) {
		Set<Entry<String, Integer>> s = v.entrySet();
		Iterator<Entry<String, Integer>> i = s.iterator();
		while (i.hasNext()) {
			Entry<String, Integer> e = i.next();
			addVariation(e.getKey(), e.getValue());
		}
	}

	protected void setVariations(HashMap<String,Integer> v) {
		this.variations = v;
	}

	// Returns itself so we can chain the constructor with a variation of a topic name
	protected Topic withVariation(String s) {
		addVariation(s);
		return this;
	}

	public boolean similarTo(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Topic other = (Topic) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			try {
				if (!name.matches(".*" + other.name + ".*")) {
					return false;
				}			
			} catch (StringIndexOutOfBoundsException ex) {
				return false;
			}				
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName() + " [" + getCount() + "]: ");

		Iterator<Entry<String, Integer>> i = variations.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String, Integer> e = i.next();
			sb.append(e.getKey() + " ("+e.getValue()+")");
			if (i.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	@Override
	public int compareTo(Topic o) {
		
		// TODO: Here it's worth tuning the parameters to see in which cases a shorter topic
		// should be sorted lower than a longer one
		
		if (o.getCount() == this.getCount())	{ 
			return Integer.valueOf(o.getName().length()).compareTo(Integer.valueOf(this.getName().length()));
		} else {
			return o.getCount().compareTo(this.getCount());
		}
	}

	/**
	 * Returns the "best" name for a topic. Currently measures only number of occurrences
	 * @return the clean name for this topic
	 */
	public String getCleanName() {

		int numOccurance = 0;
		String bestMatch = "";
		Iterator<Entry<String, Integer>> i = variations.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String, Integer> e = i.next();
			if (e.getValue() > numOccurance) {
				bestMatch = e.getKey();
			}
		}
		
		return bestMatch;
	}
}
