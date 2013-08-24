TopicExtractor
==============

Extract "topics" from a given text
 
 - Assumptions:
   - Text is plain English
   - We are looking for word tokens between 1 and 5 words length
   - A token becomes a topic if
     - it is at least min_topic_length characters long
     - doesn't contain any lower-case only words (with all the limitations this brings)
     - has a certain minimum keyword density
   - We attempt to group different spellings of the same topic ("Google Nexus S" == "GOOGLE Nexus S")
   - We try to identify possible duplicates ("Google NEXUS" is a possible duplicate of "Google Nexus S") 
