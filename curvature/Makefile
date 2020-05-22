all:
	javac -cp /usr/share/java/ij.jar *.java;
	mv *.class curv/
	jar cvf Roi_Curvature.jar curv/*.class plugins.config;

install:
	make all 
	cp Roi_Curvature.jar ~/.imagej/plugins/

clean:
	rm curv/*.class
	rm Roi_Curvature.jar
