all:
	javac -cp /usr/share/java/ij.jar *.java;
	mv *.class loco/
	jar cvf Loco_EFA.jar loco/*.class plugins.config;

install:
	make all 
	cp Loco_EFA.jar ~/.imagej/plugins/

clean:
	rm loco/*.class
	rm Loco_EFA.jar
