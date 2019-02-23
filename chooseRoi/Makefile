all:
	javac -cp /usr/share/java/ij.jar *.java;
	mv *.class chooser/
	jar cvf Choose_Roi.jar chooser/*.class plugins.config;

install:
	make all 
	cp Choose_Roi.jar ~/.imagej/plugins/

clean:
	rm chooser/*.class
	rm Choose_Roi.jar
