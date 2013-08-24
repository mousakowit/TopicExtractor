TopicExtractor
==============

Extract "topics" from a given text
 
 - Assumptions:
   - Texts are plain English stripped off HTML and other markup
   - We are looking for word tokens between 1 and 5 words length
   - A token becomes a topic if
     - it is at least min_topic_length characters long
     - doesn't contain any lower-case only words (with all the limitations this brings)
     - has a certain minimum keyword density
   - We attempt to group different spellings of the same topic ("Google Nexus S" == "GOOGLE Nexus S")
   - We try to identify possible duplicates ("Google NEXUS" is a possible duplicate of "Google Nexus S") 
   - Topics get a "clean" name by measuring the number of occurrences only - for now
   - Sorting of Topics in the Corpus should probably not be made this way if you have a lot of data
   
  
  - Architecture
    - A Corpus class reads a number of texts and calls a TopicExtractor for each of it
    - The TopicExtractor uses a Lucene analyser to identify shingles (multi word tokens) and single word of a minimum length
    - Topics are identified and filtered by a number of attributes, mainly case and density and returned to the Corpus
    - Existing Topics in the Corpus are merged with the ones returned by the TopicExtractor
    - The "top x" Topics can be requested from the Corpus
 
 
  - Additional thoughts:
    - The results need a lot of fine tuning to be convincing. It would probably be good to tokenize based on sentences rather than on full texts to prevent processing shingles crossing sentence borders.
    - If the Corpus groups texts of a certain domain it might be good to increase the list of stop words appearing in say 60% of all texts (if all articles are about computers a Topic has less meaning if it contains that word)
    
    