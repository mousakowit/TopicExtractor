package org.vskm.text.topic;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.util.Version;
import org.vskm.text.topic.util.RomanConversion;
import org.vskm.text.topic.util.StringUtil;

/**
 * Extract "topics" from a given text
 * 
 * Assumptions:
 * - Text is plain English
 * - We are looking for word tokens between 1 and 5 words length
 * - A token becomes a topic if
 *   - it is at least min_topic_length characters long
 *   - doesn't contain any lower-case only words (with all the limitations this brings)
 *   - has a certain minimum keyword density
 * - We attempt to group different spellings of the same topic ("Google Nexus S" == "GOOGLE Nexus S")
 * - We try to identify possible duplicates ("Google NEXUS" is a possible duplicate of "Google Nexus S") 
 * 
 * @author sacha
 *
 */
public class TopicExtractor {
	static final int MIN_SHINGLE_SIZE=2;
	static final int MAX_SHINGLE_SIZE=5;
	static double MIN_KEYWORD_DENSITY_SHINGLES = 0.2;
	static double MIN_KEYWORD_DENSITY = 3.0;
	int max_topics = 8;
	int min_topic_length = 6;

	Analyzer analyzer;
	String text;
	int text_length;
	boolean _tokenized=false;
	List<Topic> topics;
	HashMap<String,Topic> unfilteredTopics;

	  /** Put here to include the upper case version as we can't use a lower case filter. An unmodifiable set containing some common English words that are not usually useful
	  for searching.*/
	  public static final CharArraySet ENGLISH_STOP_WORDS_SET;
	  
	  static {
	    final List<String> stopWords = Arrays.asList(
	      "a", "an", "and", "are", "as", "at", "be", "but", "by",
	      "for", "if", "in", "into", "is", "it",
	      "no", "not", "of", "on", "or", "such",
	      "that", "the", "their", "then", "there", "these",
	      "they", "this", "to", "was", "will", "with",
	      "A", "An", "And", "Are", "As", "At", "Be", "But", "By",
	      "For", "If", "In", "Into", "Is", "It",
	      "No", "Not", "Of", "On", "Or", "Such",
	      "That", "The", "Their", "Then", "There", "These",
	      "They", "This", "To", "Was", "Will", "With"
	    );
	    final CharArraySet stopSet = new CharArraySet(Version.LUCENE_44, 
	        stopWords, false);
	    ENGLISH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet); 
	  }


	/**
	 * @param text A Lucene document containing the text to be analyzed
	 */
	public TopicExtractor(Document text) {
		super();
		this.text = text.toString();
		_init();
	}

	/**
	 * Constructor taking a string as input
	 * @param text A string containing the text to be analyzed
	 */
	public TopicExtractor(String text) {
		super();
		this.text = text;	
		_init();
	}

	private void _init()
	{
		// Create the Lucene Analyser. We're using a modified StandardAnalzyer without the lower case filter
		this.analyzer = new CaseSensitiveAnalyzer(Version.LUCENE_44, ENGLISH_STOP_WORDS_SET);
		this.unfilteredTopics = new HashMap<String,Topic>();
		this.topics = new ArrayList<Topic>();

		this.text_length = text.split("\\s").length;
	}


	/***
	 * Returns a list of identified Topics
	 * @return Sorted ArrayList of Topic objects
	 */
	public List<Topic> getTopics() {
		
		if (!_tokenized) {
			List<String> tokens = _tokenizeText(this.text);
			//List<String> tokens = _tokenizeNGrams(this.text, MIN_SHINGLE_SIZE, MAX_SHINGLE_SIZE);
			tokens.addAll(_tokenizeShingles(this.text, MIN_SHINGLE_SIZE, MAX_SHINGLE_SIZE));

			_tokenized=true;
			if (tokens != null) {
				Iterator<String> i = tokens.iterator();
				while (i.hasNext()) {
					String token = i.next();
					// Check if token is a topic candidate
					if (_isTopicCandidate(token)) {
						
						// Create a unique search name (remove whitespace, convert to upper case etc.)
						String searchName = _makeSearchName(token);
						if (unfilteredTopics.containsKey(searchName)) {
							unfilteredTopics.get(searchName).addVariation(token);
						} else {
							unfilteredTopics.put(searchName,new Topic(searchName).withVariation(token));
						}
					} else {
						//System.out.println("Removed" + token);
					}
					
				}
			}
			_sortTopics();
		}

		return this.topics.subList(0, Math.min(this.topics.size(), max_topics));
	}

	/**
	 * Apply different rules to determine if the token is suitable as a topic.
	 * @param s the token
	 * @return true if the term matches our requirements for a topic
	 */
	private boolean _isTopicCandidate(String s) {

		// Only accept Terms that have at least one captical letter and don't contain a removed stop word
		Matcher numbersInFront = Pattern.compile("^(\\d+)").matcher(s);
		Matcher monthYear = Pattern.compile("^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{2,4}").matcher(s);
		//Matcher capitals = Pattern.compile("(\\b[A-Z]+)").matcher(s);
		Matcher lower = Pattern.compile("\\b[a-z]+\\b").matcher(s);
		Matcher stopwordsFiltered = Pattern.compile("(\\b_)").matcher(s);
		
		if (
			s.length() > min_topic_length &&
			!lower.find() && 
			!stopwordsFiltered.find() && 
			!numbersInFront.find() && 
			!monthYear.find()) {		
			// TODO: Apply further filters
			return true;
		}	

		return false;
	}

	/***
	 * Create a "search name" that can be used to identify different spellings of the same token.
	 * @param s the string to be normalized
	 * @return a "searchable" version of the string
	 */
	private String _makeSearchName(String s) {
		
		// Convert all numbers into Roman numbers
		Pattern numbers = Pattern.compile("(\\d+)");
	    Matcher matcher = numbers.matcher(s);
	    if (matcher.find()){    	
	      try {
	    	  s = matcher.replaceAll(RomanConversion.binaryToRoman(Integer.valueOf(matcher.group()))); 
	      }
	      catch (NumberFormatException e) {}
	    } 

	    // Remove miscellaneous characters
		Pattern miscchars = Pattern.compile("[\\s-_,.\\!#;]+");
	    matcher = miscchars.matcher(s);
	    if (matcher.find()){    	
	    	s = matcher.replaceAll(""); 
	    } 

		return s.toUpperCase();
		
	}

	/**
	 * Tokenize the text into single word tokens.
	 * @param t the text
	 * @return a list of token strings
	 */
	private List<String> _tokenizeText(String t) {
		List<String> result = new ArrayList<String>();
		try {
			TokenStream stream  = new LengthFilter(Version.LUCENE_44, analyzer.tokenStream(null, new StringReader(t)),min_topic_length,20);
			CharTermAttribute cattr = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			while (stream.incrementToken()) {
				result.add(cattr.toString());
			}
			stream.end();
			stream.close();
		} catch (IOException e) {
			// not thrown b/c we're using a string reader...
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * Tokenize the text into multi word tokens (shingles).
	 * @param t the text
	 * @param minWordSize
	 * @param maxWordSize
	 * @return a list of token strings
	 */
	private List<String> _tokenizeShingles(String t, int minWordSize, int maxWordSize) {
		if (minWordSize <= maxWordSize && minWordSize >= MIN_SHINGLE_SIZE && maxWordSize <= MAX_SHINGLE_SIZE) {
			List<String> result = new ArrayList<String>();
			try {
				ShingleAnalyzerWrapper shingleWrapper = new ShingleAnalyzerWrapper(analyzer, minWordSize, maxWordSize, " ", false, false);
				TokenStream stream  = shingleWrapper.tokenStream(null, new StringReader(t));
				CharTermAttribute cattr = stream.addAttribute(CharTermAttribute.class);
				stream.reset();
				while (stream.incrementToken()) {
					result.add(cattr.toString());
				}
				stream.end();
				stream.close();
				shingleWrapper.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return result;
		} else {
			return null;
		}
	}

	/**
	 * Sort the identified Topics by their prominence and filter by keyword density threshold.
	 */
	private void _sortTopics() {
		
		// Calculate keyword density and sort the results
		Iterator<Entry<String, Topic>> i = this.unfilteredTopics.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String,Topic> e = i.next();
			float density = (float) e.getValue().getCount() * 100 / this.text_length;
			
			if (StringUtil.isShingle(e.getValue().getCleanName()) && density > MIN_KEYWORD_DENSITY_SHINGLES ) {
				// Add the topic if the min density for shingles is given
				this.topics.add(e.getValue());
			} else if (density > MIN_KEYWORD_DENSITY) {
				// For single word topics we apply a higher threshold
				this.topics.add(e.getValue());
			}
		}

		// Sort (mainly) by number of occurrence 
		Collections.sort(this.topics);
		
		Topic last = new Topic("foobar");
		for (Topic t : this.topics){
			if (last.similarTo(t)) {
				t.isSimilarTo(last.getName());
			} else {
				last = t;
			}
		}
		
	}
	
	
}
