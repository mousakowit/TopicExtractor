package org.vskm.text.topic.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileWalker {

    public static List<File> walk( String path ) {

    	ArrayList<File> files = new ArrayList<File>();
    	
        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return files;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk( f.getAbsolutePath() );
            }
            else {
                files.add(f);
            }
        }
        return files;
    }
    
	public static String readFile(File file) {
		Charset charset = Charset.forName("US-ASCII");
		StringBuilder s = new StringBuilder();
		try {
			BufferedReader reader = Files.newBufferedReader(file.toPath(), charset);
		    String line = null;
		    while ((line = reader.readLine()) != null) {
		        s.append(line);
		    }
		    reader.close();
		} catch (IOException x) {
		    System.err.format("IOException: %s%n", x);
		}
		return s.toString();
	}

}